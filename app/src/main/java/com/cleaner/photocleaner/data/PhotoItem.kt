package com.cleaner.photocleaner.data

import android.net.Uri

data class PhotoItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateTaken: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val relativePath: String,
    val isScreenshot: Boolean = false,
    var dHash: Long = 0L,
    var blurScore: Float = 0f,
    var isBlurry: Boolean = false
)

data class DuplicateGroup(
    val groupId: String,
    val bestPhoto: PhotoItem,
    val duplicates: List<PhotoItem>
) {
    val allPhotos: List<PhotoItem>
        get() = listOf(bestPhoto) + duplicates
}
