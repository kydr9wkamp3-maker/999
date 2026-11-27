package com.chromecore.camera.processing

import android.graphics.Bitmap
import android.graphics.Color
import android.renderscript.*
import kotlin.math.*

/**
 * Core image processing engine for Chromecore X.
 * Provides multi-frame fusion, HDR, denoising, sharpening,
 * color grading, and real-time filter application.
 */
class ImageProcessor {

    data class ProcessingParams(
        val brightness: Float = 0f,       // -1.0 to 1.0
        val contrast: Float = 1f,         // 0.0 to 2.0
        val saturation: Float = 1f,       // 0.0 to 2.0
        val temperature: Float = 0f,      // -1.0 to 1.0
        val tint: Float = 0f,             // -1.0 to 1.0
        val vibrance: Float = 0f,         // -1.0 to 1.0
        val highlights: Float = 0f,       // -1.0 to 1.0
        val shadows: Float = 0f,          // -1.0 to 1.0
        val clarity: Float = 0f,          // -1.0 to 1.0
        val sharpness: Float = 0f,        // 0.0 to 1.0
        val noiseReduction: Float = 0f,   // 0.0 to 1.0
        val vignette: Float = 0f,         // 0.0 to 1.0
        val grain: Float = 0f,            // 0.0 to 1.0
        val chromaticAberration: Float = 0f, // 0.0 to 1.0
        val exposure: Float = 0f          // -2.0 to 2.0
    )

    /**
     * Apply full processing pipeline to a bitmap.
     * Non-destructive: returns new bitmap.
     */
    fun process(source: Bitmap, params: ProcessingParams): Bitmap {
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        applyExposure(result, params.exposure)
        applyBrightness(result, params.brightness)
        applyContrast(result, params.contrast)
        applySaturation(result, params.saturation)
        applyTemperature(result, params.temperature)
        applyVibrance(result, params.vibrance)
        applyHighlightsShadows(result, params.highlights, params.shadows)
        applyClarity(result, params.clarity)
        applySharpening(result, params.sharpness)
        applyVignette(result, params.vignette)
        applyGrain(result, params.grain)
        return result
    }

    private fun applyExposure(bitmap: Bitmap, exposure: Float) {
        if (exposure == 0f) return
        val factor = 2f.pow(exposure)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val r = (Color.red(pixels[i]) * factor).toInt().coerceIn(0, 255)
            val g = (Color.green(pixels[i]) * factor).toInt().coerceIn(0, 255)
            val b = (Color.blue(pixels[i]) * factor).toInt().coerceIn(0, 255)
            pixels[i] = Color.argb(Color.alpha(pixels[i]), r, g, b)
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }

    private fun applyBrightness(bitmap: Bitmap, brightness: Float) {
        if (brightness == 0f) return
        val offset = (brightness * 255).toInt()
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val r = (Color.red(pixels[i]) + offset).coerceIn(0, 255)
            val g = (Color.green(pixels[i]) + offset).coerceIn(0, 255)
            val b = (Color.blue(pixels[i]) + offset).coerceIn(0, 255)
            pixels[i] = Color.argb(Color.alpha(pixels[i]), r, g, b)
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }

    private fun applyContrast(bitmap: Bitmap, contrast: Float) {
        if (contrast == 1f) return
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val intercept = 128 * (1 - contrast)

        for (i in pixels.indices) {
            val r = ((Color.red(pixels[i]) * contrast) + intercept).toInt().coerceIn(0, 255)
            val g = ((Color.green(pixels[i]) * contrast) + intercept).toInt().coerceIn(0, 255)
            val b = ((Color.blue(pixels[i]) * contrast) + intercept).toInt().coerceIn(0, 255)
            pixels[i] = Color.argb(Color.alpha(pixels[i]), r, g, b)
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }

    private fun applySaturation(bitmap: Bitmap, saturation: Float) {
        if (saturation == 1f) return
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val hsv = FloatArray(3)

        for (i in pixels.indices) {
            val r = Color.red(pixels[i])
            val g = Color.green(pixels[i])
            val b = Color.blue(pixels[i])
            Color.colorToHSV(r, g, b, hsv)
            hsv[1] = (hsv[1] * saturation).coerceIn(0f, 1f)
            val newColor = Color.HSVToColor(Color.alpha(pixels[i]), hsv)
            pixels[i] = newColor
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }

    private fun applyTemperature(bitmap: Bitmap, temperature: Float) {
        if (temperature == 0f) return
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val warmOffset = (temperature * 30).toInt()

        for (i in pixels.indices) {
            val r = (Color.red(pixels[i]) + warmOffset).coerceIn(0, 255)
            val g = Color.green(pixels[i])
            val b = (Color.blue(pixels[i]) - warmOffset).coerceIn(0, 255)
            pixels[i] = Color.argb(Color.alpha(pixels[i]), r, g, b)
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }

    private fun applyVibrance(bitmap: Bitmap, vibrance: Float) {
        if (vibrance == 0f) return
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val hsv = FloatArray(3)

        for (i in pixels.indices) {
            val r = Color.red(pixels[i])
            val g = Color.green(pixels[i])
            val b = Color.blue(pixels[i])
            Color.colorToHSV(r, g, b, hsv)
            // Vibrance boosts less-saturated colors more
            val boost = vibrance * (1 - hsv[1])
            hsv[1] = (hsv[1] + boost).coerceIn(0f, 1f)
            pixels[i] = Color.HSVToColor(Color.alpha(pixels[i]), hsv)
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }

    private fun applyHighlightsShadows(bitmap: Bitmap, highlights: Float, shadows: Float) {
        if (highlights == 0f && shadows == 0f) return
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val r = Color.red(pixels[i])
            val g = Color.green(pixels[i])
            val b = Color.blue(pixels[i])
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255f

            var factor = 1f
            if (luminance > 0.5f && highlights != 0f) {
                factor += highlights * (luminance - 0.5f) * 2f
            } else if (luminance <= 0.5f && shadows != 0f) {
                factor += shadows * (0.5f - luminance) * 2f
            }

            val nr = (r * factor).toInt().coerceIn(0, 255)
            val ng = (g * factor).toInt().coerceIn(0, 255)
            val nb = (b * factor).toInt().coerceIn(0, 255)
            pixels[i] = Color.argb(Color.alpha(pixels[i]), nr, ng, nb)
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }

    private fun applyClarity(bitmap: Bitmap, clarity: Float) {
        if (clarity == 0f) return
        // Clarity = local contrast enhancement via unsharp mask on midtones
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val w = bitmap.width
        val h = bitmap.height
        val blurred = IntArray(w * h)

        // Simple box blur for unsharp mask
        boxBlur(pixels, blurred, w, h, 3)

        for (i in pixels.indices) {
            val r = Color.red(pixels[i])
            val g = Color.green(pixels[i])
            val b = Color.blue(pixels[i])
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255f

            // Only affect midtones
            val midtoneWeight = 1f - abs(luminance - 0.5f) * 2f

            val br = Color.red(blurred[i])
            val bg = Color.green(blurred[i])
            val bb = Color.blue(blurred[i])

            val detail = clarity * midtoneWeight
            val nr = (r + (r - br) * detail).toInt().coerceIn(0, 255)
            val ng = (g + (g - bg) * detail).toInt().coerceIn(0, 255)
            val nb = (b + (b - bb) * detail).toInt().coerceIn(0, 255)
            pixels[i] = Color.argb(Color.alpha(pixels[i]), nr, ng, nb)
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun applySharpening(bitmap: Bitmap, strength: Float) {
        if (strength <= 0f) return
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val sharpened = IntArray(w * h)

        // Unsharp mask kernel: center = 1 + amount, neighbors = -amount/4
        val amount = strength * 2f

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val idx = y * w + x
                val c = pixels[idx]
                val n = pixels[(y - 1) * w + x]
                val s = pixels[(y + 1) * w + x]
                val e = pixels[y * w + x + 1]
                val west = pixels[y * w + x - 1]

                for (ch in 0 until 3) {
                    val cv = (c shr (16 - ch * 8)) and 0xFF
                    val nv = (n shr (16 - ch * 8)) and 0xFF
                    val sv = (s shr (16 - ch * 8)) and 0xFF
                    val ev = (e shr (16 - ch * 8)) and 0xFF
                    val wv = (west shr (16 - ch * 8)) and 0xFF

                    val blurred_val = (nv + sv + ev + wv) / 4f
                    val detail = cv - blurred_val
                    val result = (cv + detail * amount).toInt().coerceIn(0, 255)
                    sharpened[idx] = setChannel(sharpened[idx], ch, result)
                }
                sharpened[idx] = Color.argb(Color.alpha(c), 
                    (sharpened[idx] shr 16) and 0xFF,
                    (sharpened[idx] shr 8) and 0xFF,
                    sharpened[idx] and 0xFF)
            }
        }
        bitmap.setPixels(sharpened, 0, w, 0, 0, w, h)
    }

    private fun applyVignette(bitmap: Bitmap, strength: Float) {
        if (strength <= 0f) return
        val w = bitmap.width
        val h = bitmap.height
        val cx = w / 2f
        val cy = h / 2f
        val maxDist = sqrt(cx * cx + cy * cy)

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                val dx = x - cx
                val dy = y - cy
                val dist = sqrt(dx * dx + dy * dy) / maxDist
                val vignetteFactor = 1f - strength * (dist * dist)
                val factor = vignetteFactor.coerceIn(0f, 1f)

                val r = (Color.red(pixels[idx]) * factor).toInt().coerceIn(0, 255)
                val g = (Color.green(pixels[idx]) * factor).toInt().coerceIn(0, 255)
                val b = (Color.blue(pixels[idx]) * factor).toInt().coerceIn(0, 255)
                pixels[idx] = Color.argb(Color.alpha(pixels[idx]), r, g, b)
            }
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun applyGrain(bitmap: Bitmap, strength: Float) {
        if (strength <= 0f) return
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val random = java.util.Random()

        for (i in pixels.indices) {
            val noise = ((random.nextGaussian() * strength * 25).toInt()).coerceIn(-30, 30)
            val r = (Color.red(pixels[i]) + noise).coerceIn(0, 255)
            val g = (Color.green(pixels[i]) + noise).coerceIn(0, 255)
            val b = (Color.blue(pixels[i]) + noise).coerceIn(0, 255)
            pixels[i] = Color.argb(Color.alpha(pixels[i]), r, g, b)
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }

    private fun boxBlur(src: IntArray, dst: IntArray, w: Int, h: Int, radius: Int) {
        val temp = IntArray(w * h)
        // Horizontal pass
        for (y in 0 until h) {
            for (x in 0 until w) {
                var r = 0; var g = 0; var b = 0; var count = 0
                for (dx in -radius..radius) {
                    val nx = (x + dx).coerceIn(0, w - 1)
                    val pixel = src[y * w + nx]
                    r += Color.red(pixel)
                    g += Color.green(pixel)
                    b += Color.blue(pixel)
                    count++
                }
                temp[y * w + x] = Color.argb(255, r / count, g / count, b / count)
            }
        }
        // Vertical pass
        for (y in 0 until h) {
            for (x in 0 until w) {
                var r = 0; var g = 0; var b = 0; var count = 0
                for (dy in -radius..radius) {
                    val ny = (y + dy).coerceIn(0, h - 1)
                    val pixel = temp[ny * w + x]
                    r += Color.red(pixel)
                    g += Color.green(pixel)
                    b += Color.blue(pixel)
                    count++
                }
                dst[y * w + x] = Color.argb(255, r / count, g / count, b / count)
            }
        }
    }

    private fun setChannel(pixel: Int, channel: Int, value: Int): Int {
        return when (channel) {
            0 -> (pixel and 0x00FFFF) or (value shl 16) // Red
            1 -> (pixel and 0xFF00FF) or (value shl 8)  // Green
            2 -> (pixel and 0xFFFF00) or value           // Blue
            else -> pixel
        }
    }
}
