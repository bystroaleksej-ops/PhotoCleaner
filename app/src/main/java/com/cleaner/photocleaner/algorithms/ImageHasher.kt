package com.cleaner.photocleaner.algorithms

import android.graphics.Bitmap
import android.graphics.Color

object ImageHasher {

    /**
     * Computes a 64-bit difference hash (dHash) from a bitmap.
     * Resizes to 9x8, converts to grayscale, and compares adjacent pixels.
     */
    fun computeDHash(bitmap: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
        var hash = 0L

        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val leftPixel = scaled.getPixel(x, y)
                val rightPixel = scaled.getPixel(x + 1, y)

                val leftBrightness = getBrightness(leftPixel)
                val rightBrightness = getBrightness(rightPixel)

                if (leftBrightness > rightBrightness) {
                    hash = hash or (1L shl (y * 8 + x))
                }
            }
        }

        if (scaled != bitmap) {
            scaled.recycle()
        }

        return hash
    }

    /**
     * Calculates the Hamming distance between two 64-bit hashes.
     * A lower distance means higher visual similarity.
     * Distance <= 5 typically indicates identical or burst-shot photos.
     */
    fun hammingDistance(hash1: Long, hash2: Long): Int {
        return java.lang.Long.bitCount(hash1 xor hash2)
    }

    private fun getBrightness(color: Int): Int {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        // Standard relative luminance formula
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }
}
