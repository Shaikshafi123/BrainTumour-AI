
package com.example.braintumourai

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImagePreprocessorInstrumentedTest {

    @Test
    fun preprocessImage_returnsCorrectNumberOfValues() {

        val bitmap = Bitmap.createBitmap(
            100,
            100,
            Bitmap.Config.ARGB_8888
        )

        val processedPixels =
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

        bitmap.eraseColor(Color.WHITE)

        val processedPixels =
            ImagePreprocessor.preprocessImage(bitmap, 10)

        assertTrue(
            processedPixels.all { it in 0f..1f }
        )

        assertEquals(1f, processedPixels[0], 0.001f)
        assertEquals(1f, processedPixels[1], 0.001f)
        assertEquals(1f, processedPixels[2], 0.001f)

        bitmap.recycle()
    }
}
