
package com.example.braintumourai

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImagePreprocessorTest {

    @Test
    fun preprocessImage_returnsCorrectNumberOfValues() {

        val bitmap = Bitmap.createBitmap(
            100,
            100,
            Bitmap.Config.ARGB_8888
        )

        val processedPixels: FloatArray =
            ImagePreprocessor.preprocessImage(bitmap, 224)

        assertEquals(150528, processedPixels.size)

        bitmap.recycle()
    }

    @Test
    fun preprocessImage_normalisesPixelValues() {

        val bitmap = Bitmap.createBitmap(
            10,
            10,
            Bitmap.Config.ARGB_8888
        )

        bitmap.eraseColor(android.graphics.Color.WHITE)

        val processedPixels: FloatArray =
            ImagePreprocessor.preprocessImage(bitmap, 10)

        assertTrue(
            processedPixels.all { it >= 0.0f && it <= 1.0f }
        )

        assertEquals(1.0f, processedPixels[0], 0.001f)
        assertEquals(1.0f, processedPixels[1], 0.001f)
        assertEquals(1.0f, processedPixels[2], 0.001f)

        bitmap.recycle()
    }
}
