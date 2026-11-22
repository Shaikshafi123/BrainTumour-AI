package com.example.braintumourai

import android.graphics.Bitmap

object ImagePreprocessor {

    fun preprocessImage(
        bitmap: Bitmap,
        targetSize: Int
    ): FloatArray {

        require(targetSize > 0) {
            "Target size must be greater than zero."
        }

        require(!bitmap.isRecycled) {
            "The source image is no longer available."
        }

        val resizedBitmap = Bitmap.createScaledBitmap(
            bitmap,
            targetSize,
            targetSize,
            true
        )

        val pixels = IntArray(targetSize * targetSize)

        resizedBitmap.getPixels(
            pixels,
            0,
            targetSize,
            0,
            0,
            targetSize,
            targetSize
        )

        val processedData = FloatArray(
            targetSize * targetSize * 3
        )

        var index = 0

        for (pixel in pixels) {

            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF

            processedData[index++] = red / 255.0f
            processedData[index++] = green / 255.0f
            processedData[index++] = blue / 255.0f
        }

        if (resizedBitmap !== bitmap) {
            resizedBitmap.recycle()
        }

        return processedData
    }
}