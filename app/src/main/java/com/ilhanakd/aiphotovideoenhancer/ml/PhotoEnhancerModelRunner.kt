package com.ilhanakd.aiphotovideoenhancer.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.ilhanakd.aiphotovideoenhancer.data.processing.ImageProcessor
import com.ilhanakd.aiphotovideoenhancer.domain.model.EnhanceParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class PhotoEnhancerModelRunner(private val context: Context) {

    private var interpreter: Interpreter? = null

    private suspend fun loadModelIfExists(): Boolean = withContext(Dispatchers.IO) {
        if (interpreter != null) return@withContext true
        val assetManager = context.assets
        return@withContext try {
            val descriptor = assetManager.openFd(MODEL_PATH)
            descriptor.use {
                val input = FileInputStream(it.fileDescriptor)
                val channel = input.channel
                val startOffset = it.startOffset
                val declaredLength = it.declaredLength
                val buffer: MappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
                interpreter = Interpreter(buffer)
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Photo model not available, falling back to classic filters", e)
            false
        }
    }

    suspend fun enhancePhoto(bitmap: Bitmap, params: EnhanceParams): Bitmap = withContext(Dispatchers.Default) {
        val useModel = loadModelIfExists()
        if (useModel && interpreter != null) {
            return@withContext runModel(bitmap)
        }
        return@withContext ImageProcessor.applyEnhancements(
            bitmap,
            params.sharpness,
            params.denoise,
            params.brightness,
            params.contrast
        )
    }

    private fun runModel(bitmap: Bitmap): Bitmap {
        // Placeholder: a real implementation would prepare tensors. Here we simply return the original bitmap.
        return bitmap.copy(Bitmap.Config.ARGB_8888, true)
    }

    companion object {
        private const val MODEL_PATH = "models/photo_enhancer.tflite"
        private const val TAG = "PhotoEnhancerModelRunner"
    }
}
