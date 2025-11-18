package com.ilhanakd.aiphotovideoenhancer.data.processing

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

object ImageProcessor {

    fun applyEnhancements(source: Bitmap, sharpness: Int, denoise: Int, brightness: Float, contrast: Float): Bitmap {
        val mutable = source.copy(Bitmap.Config.ARGB_8888, true)
        val blurred = if (denoise > 0) blur(mutable, denoise.coerceIn(0, 100)) else mutable.copy(Bitmap.Config.ARGB_8888, true)
        val sharpened = if (sharpness > 0) unsharpMask(mutable, blurred, sharpness) else mutable.copy(Bitmap.Config.ARGB_8888, true)
        val adjusted = adjustBrightnessContrast(sharpened, brightness, contrast)
        if (blurred != mutable && !blurred.isRecycled) blurred.recycle()
        if (sharpened != mutable && sharpened != adjusted && !sharpened.isRecycled) sharpened.recycle()
        return adjusted
    }

    private fun blur(bitmap: Bitmap, strength: Int): Bitmap {
        val radius = (strength / 10).coerceAtLeast(1)
        val kernelSize = radius * 2 + 1
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)
        val kernel = FloatArray(kernelSize) { i -> gaussian(i - radius, radius.toFloat()) }
        val norm = kernel.sum()

        // horizontal pass
        for (y in 0 until height) {
            for (x in 0 until width) {
                var rSum = 0f
                var gSum = 0f
                var bSum = 0f
                for (k in -radius..radius) {
                    val px = (x + k).coerceIn(0, width - 1)
                    val color = pixels[y * width + px]
                    val weight = kernel[k + radius]
                    rSum += Color.red(color) * weight
                    gSum += Color.green(color) * weight
                    bSum += Color.blue(color) * weight
                }
                val index = y * width + x
                output[index] = Color.rgb((rSum / norm).toInt(), (gSum / norm).toInt(), (bSum / norm).toInt())
            }
        }

        val vertical = IntArray(width * height)
        // vertical pass
        for (x in 0 until width) {
            for (y in 0 until height) {
                var rSum = 0f
                var gSum = 0f
                var bSum = 0f
                for (k in -radius..radius) {
                    val py = (y + k).coerceIn(0, height - 1)
                    val color = output[py * width + x]
                    val weight = kernel[k + radius]
                    rSum += Color.red(color) * weight
                    gSum += Color.green(color) * weight
                    bSum += Color.blue(color) * weight
                }
                val index = y * width + x
                vertical[index] = Color.rgb((rSum / norm).toInt(), (gSum / norm).toInt(), (bSum / norm).toInt())
            }
        }
        return Bitmap.createBitmap(vertical, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun gaussian(x: Int, sigma: Float): Float {
        val exponent = -(x * x) / (2 * sigma * sigma)
        return exp(exponent)
    }

    private fun unsharpMask(original: Bitmap, blurred: Bitmap, strength: Int): Bitmap {
        val width = original.width
        val height = original.height
        val origPixels = IntArray(width * height)
        val blurPixels = IntArray(width * height)
        original.getPixels(origPixels, 0, width, 0, 0, width, height)
        blurred.getPixels(blurPixels, 0, width, 0, 0, width, height)
        val factor = strength / 100f * 1.5f
        val outPixels = IntArray(width * height)
        for (i in origPixels.indices) {
            val r = Color.red(origPixels[i])
            val g = Color.green(origPixels[i])
            val b = Color.blue(origPixels[i])
            val rBlur = Color.red(blurPixels[i])
            val gBlur = Color.green(blurPixels[i])
            val bBlur = Color.blue(blurPixels[i])
            val rSharp = clamp(r + ((r - rBlur) * factor))
            val gSharp = clamp(g + ((g - gBlur) * factor))
            val bSharp = clamp(b + ((b - bBlur) * factor))
            outPixels[i] = Color.argb(255, rSharp, gSharp, bSharp)
        }
        return Bitmap.createBitmap(outPixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun adjustBrightnessContrast(bitmap: Bitmap, brightness: Float, contrast: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val adjusted = IntArray(width * height)
        val contrastFactor = contrast
        val brightnessShift = (brightness * 255).toInt()
        for (i in pixels.indices) {
            val color = pixels[i]
            var r = (Color.red(color) - 128) * contrastFactor + 128 + brightnessShift
            var g = (Color.green(color) - 128) * contrastFactor + 128 + brightnessShift
            var b = (Color.blue(color) - 128) * contrastFactor + 128 + brightnessShift
            r = r.coerceIn(0f, 255f)
            g = g.coerceIn(0f, 255f)
            b = b.coerceIn(0f, 255f)
            adjusted[i] = Color.argb(Color.alpha(color), r.toInt(), g.toInt(), b.toInt())
        }
        return Bitmap.createBitmap(adjusted, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun clamp(value: Float): Int = max(0f, min(255f, value)).toInt()
}
