package com.cleaner.photocleaner.algorithms

import com.cleaner.photocleaner.data.DuplicateGroup
import com.cleaner.photocleaner.data.PhotoItem
import kotlin.math.abs

object PhotoClassifier {

    /**
     * Groups photos into duplicate/series clusters based on dHash Hamming distance
     * and capture timestamps.
     */
    fun findDuplicates(photos: List<PhotoItem>): List<DuplicateGroup> {
        val visited = BooleanArray(photos.size)
        val groups = mutableListOf<DuplicateGroup>()

        for (i in photos.indices) {
            if (visited[i]) continue

            val current = photos[i]
            val cluster = mutableListOf<PhotoItem>()
            cluster.add(current)
            visited[i] = true

            for (j in (i + 1) until photos.size) {
                if (visited[j]) continue
                val candidate = photos[j]

                val distance = ImageHasher.hammingDistance(current.dHash, candidate.dHash)
                val timeDiffSeconds = abs(current.dateTaken - candidate.dateTaken) / 1000

                // Strict visual match OR burst series match (within 5 seconds with close hash)
                val isDuplicate = (distance <= 5) || (timeDiffSeconds <= 5 && distance <= 10)

                if (isDuplicate) {
                    cluster.add(candidate)
                    visited[j] = true
                }
            }

            if (cluster.size > 1) {
                // Determine the best photo in the cluster:
                // Prioritize sharpness score, then resolution, then size
                val best = cluster.maxWithOrNull(
                    compareBy<PhotoItem> { it.blurScore }
                        .thenBy { it.width * it.height }
                        .thenBy { it.size }
                ) ?: cluster.first()

                val duplicates = cluster.filter { it.id != best.id }
                groups.add(
                    DuplicateGroup(
                        groupId = "group_${current.id}",
                        bestPhoto = best,
                        duplicates = duplicates
                    )
                )
            }
        }

        return groups
    }

    fun findBlurryPhotos(photos: List<PhotoItem>): List<PhotoItem> {
        return photos.filter { it.isBlurry }
    }

    fun findScreenshots(photos: List<PhotoItem>): List<PhotoItem> {
        return photos.filter { it.isScreenshot }
    }
}
