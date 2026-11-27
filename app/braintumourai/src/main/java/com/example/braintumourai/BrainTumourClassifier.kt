
package com.example.braintumourai

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class TumourPrediction(
    val label: String,
    val confidence: Float
)

class BrainTumourClassifier(context: Context) {

    private val labels: List<String>
    private val interpreter: Interpreter

    init {
        labels = context.assets
            .open("labels.txt")
            .bufferedReader()
            .readLines()
            .filter { it.isNotBlank() }

        val modelBytes = context.assets
            .open("brain_tumour_model.tflite")
            .use { it.readBytes() }

        val modelBuffer = ByteBuffer
            .allocateDirect(modelBytes.size)
            .order(ByteOrder.nativeOrder())

        modelBuffer.put(modelBytes)
        modelBuffer.rewind()

        interpreter = Interpreter(modelBuffer)

        require(interpreter.getInputTensor(0).shape().contentEquals(
            intArrayOf(1, 224, 224, 3)
        )) {
            "Unexpected model input shape"
        }

        require(labels.size == 4) {
            "Expected four class labels"
        }
    }

    fun classify(bitmap: Bitmap): TumourPrediction {

        val resized = Bitmap.createScaledBitmap(
            bitmap, 224, 224, true
        )

        val inputBuffer = ByteBuffer
            .allocateDirect(224 * 224 * 3 * 4)
            .order(ByteOrder.nativeOrder())

        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = resized.getPixel(x, y)

                inputBuffer.putFloat(
                    ((pixel shr 16) and 0xFF).toFloat()
                )
                inputBuffer.putFloat(
                    ((pixel shr 8) and 0xFF).toFloat()
                )
                inputBuffer.putFloat(
                    (pixel and 0xFF).toFloat()
                )
            }
        }

        inputBuffer.rewind()

        val output = Array(1) { FloatArray(4) }

        interpreter.run(inputBuffer, output)

        val scores = output[0]
        val bestIndex = scores.indices.maxByOrNull {
            scores[it]
        } ?: 0

        return TumourPrediction(
            label = labels[bestIndex],
            confidence = scores[bestIndex]
        )
    }

    fun close() {
        interpreter.close()
    }
}
