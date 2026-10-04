package com.cleaner.photocleaner.data

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import com.cleaner.photocleaner.algorithms.BlurDetector
import com.cleaner.photocleaner.algorithms.ImageHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class GalleryRepository(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Loads photos from MediaStore, computes hashes and blur scores for each.
     */
    suspend fun loadAndAnalyzePhotos(
        onProgress: (scanned: Int, total: Int) -> Unit
    ): List<PhotoItem> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<PhotoItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.RELATIVE_PATH
            } else {
                MediaStore.Images.Media.DATA
            }
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            val total = cursor.count
            var count = 0

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "photo_$id.jpg"
                val dateTaken = cursor.getLong(dateColumn)
                val size = cursor.getLong(sizeColumn)
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                val bucket = cursor.getString(bucketColumn) ?: ""

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val isScreenshot = name.contains("screenshot", ignoreCase = true) ||
                        bucket.contains("screenshot", ignoreCase = true)

                val item = PhotoItem(
                    id = id,
                    uri = contentUri,
                    displayName = name,
                    dateTaken = dateTaken,
                    size = size,
                    width = width,
                    height = height,
                    relativePath = bucket,
                    isScreenshot = isScreenshot
                )

                // Analyze thumbnail
                try {
                    val thumbBitmap = loadThumbnail(contentUri)
                    if (thumbBitmap != null) {
                        item.dHash = ImageHasher.computeDHash(thumbBitmap)
                        val blur = BlurDetector.computeSharpnessScore(thumbBitmap)
                        item.blurScore = blur
                        item.isBlurry = BlurDetector.isBlurry(blur) && !isScreenshot
                        thumbBitmap.recycle()
                    }
                } catch (e: Exception) {
                    // Ignore corrupted or unreadable images
                }

                photos.add(item)
                count++
                if (count % 5 == 0 || count == total) {
                    onProgress(count, total)
                }
            }
        }

        photos
    }

    private fun loadThumbnail(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.loadThumbnail(uri, Size(256, 256), null)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Thumbnails.getThumbnail(
                    contentResolver,
                    ContentUris.parseId(uri),
                    MediaStore.Images.Thumbnails.MINI_KIND,
                    null
                )
            }
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Android 11+ (including Android 15) safe trash/delete request.
     * Moves items to the system Recycle Bin or requests permission to delete.
     */
    fun createTrashOrDeleteIntent(uris: List<Uri>): PendingIntent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 to 15: MediaStore.createTrashRequest moves photos to the 30-day trash bin
            return MediaStore.createTrashRequest(contentResolver, uris, true)
        }
        return null
    }
}
