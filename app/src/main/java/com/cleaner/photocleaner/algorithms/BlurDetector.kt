package com.cleaner.photocleaner.algorithms

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.pow

object BlurDetector {

    // Threshold below which an image is considered blurry
    const val BLUR_THRESHOLD = 70.0f

    /**
     * Estimates sharpness using the Variance of Laplacian method.
     * Scales image down to 200x200 for fast mobile processing.
     */
    fun computeSharpnessScore(bitmap: Bitmap): Float {
        val targetSize = 200
        val scaled = Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
        val width = scaled.width
        val height = scaled.height

        val gray = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = scaled.getPixel(x, y)
                gray[y * width + x] = (0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)).toInt()
            }
        }

        // Apply 3x3 Laplacian filter
        var sum = 0.0
        var count = 0
        val laplacianValues = FloatArray((width - 2) * (height - 2))

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val center = gray[y * width + x]
                val top = gray[(y - 1) * width + x]
                val bottom = gray[(y + 1) * width + x]
                val left = gray[y * width + (x - 1)]
                val right = gray[y * width + (x + 1)]

                val lap = (top + bottom + left + right - 4 * center).toFloat()
                laplacianValues[count] = lap
                sum += lap
                count++
            }
        }

        if (scaled != bitmap) {
            scaled.recycle()
        }

        if (count == 0) return 0f

        val mean = sum / count
        var varianceSum = 0.0
        for (i in 0 until count) {
            varianceSum += (laplacianValues[i] - mean).pow(2.0)
        }

        return (varianceSum / count).toFloat()
    }

    fun isBlurry(score: Float): Boolean {
        return score < BLUR_THRESHOLD
    }
}
