package com.chromecore.camera.filters

import android.graphics.Bitmap
import android.graphics.Color
import com.chromecore.camera.processing.ImageProcessor
import kotlin.math.*

/**
 * Chromecore X Filter Engine.
 * Applies named filter presets and custom creative effects.
 * Each filter is a named, reproducible transformation.
 */
class FilterEngine {

    data class FilterPreset(
        val id: String,
        val name: String,
        val category: FilterCategory,
        val thumbnail: String = "",
        val params: ImageProcessor.ProcessingParams = ImageProcessor.ProcessingParams(),
        val customApplier: ((Bitmap, Float) -> Unit)? = null
    )

    enum class FilterCategory {
        CINEMATIC, CHROME, RETRO, FILM, GLITCH, PIXEL, PORTRAIT,
        COLOR_GRADE, MONOCHROME, CREATIVE, NIGHT, METALHEART
    }

    private val imageProcessor = ImageProcessor()

    /**
     * Apply a named preset to a bitmap.
     * @param intensity 0.0 = no effect, 1.0 = full effect
     */
    fun applyFilter(source: Bitmap, preset: FilterPreset, intensity: Float = 1f): Bitmap {
        if (intensity <= 0f) return source.copy(Bitmap.Config.ARGB_8888, true)

        val result = source.copy(Bitmap.Config.ARGB_8888, true)

        if (preset.customApplier != null) {
            preset.customApplier.invoke(result, intensity)
        } else {
            val scaledParams = scaleParams(preset.params, intensity)
            val processed = imageProcessor.process(result, scaledParams)
            // Copy processed pixels into result
            val pixels = IntArray(processed.width * processed.height)
            processed.getPixels(pixels, 0, processed.width, 0, 0, processed.width, processed.height)
            result.setPixels(pixels, 0, result.width, 0, 0, result.width, result.height)
            processed.recycle()
        }

        return result
    }

    private fun scaleParams(params: ImageProcessor.ProcessingParams, intensity: Float): ImageProcessor.ProcessingParams {
        return params.copy(
            brightness = params.brightness * intensity,
            contrast = 1f + (params.contrast - 1f) * intensity,
            saturation = 1f + (params.saturation - 1f) * intensity,
            temperature = params.temperature * intensity,
            vibrance = params.vibrance * intensity,
            highlights = params.highlights * intensity,
            shadows = params.shadows * intensity,
            clarity = params.clarity * intensity,
            sharpness = params.sharpness * intensity,
            noiseReduction = params.noiseReduction * intensity,
            vignette = params.vignette * intensity,
            grain = params.grain * intensity,
            exposure = params.exposure * intensity
        )
    }

    /**
     * Complete preset library.
     */
    fun getPresets(): List<FilterPreset> = listOf(
        // CHROMECORE
        FilterPreset("chromecore_signature", "Liquid Chrome", FilterCategory.CHROME,
            params = ImageProcessor.ProcessingParams(
                contrast = 1.3f, saturation = 0.7f, temperature = -0.1f,
                clarity = 0.3f, sharpness = 0.4f, vignette = 0.15f
            )
        ),
        FilterPreset("chromecore_mirror", "Mirror Finish", FilterCategory.CHROME,
            params = ImageProcessor.ProcessingParams(
                contrast = 1.4f, saturation = 0.5f, brightness = 0.1f,
                sharpness = 0.5f, highlights = -0.2f
            )
        ),
        FilterPreset("chromecore_graphite", "Graphite", FilterCategory.CHROME,
            params = ImageProcessor.ProcessingParams(
                contrast = 1.2f, saturation = 0.4f, brightness = -0.15f,
                temperature = -0.05f, grain = 0.05f
            )
        ),

        // METALHEART
        FilterPreset("metalheart_core", "Metalheart", FilterCategory.METALHEART,
            params = ImageProcessor.ProcessingParams(
                contrast = 1.35f, saturation = 0.6f, clarity = 0.25f,
                sharpness = 0.35f, temperature = -0.08f, vignette = 0.1f
            )
        ),
        FilterPreset("metalheart_titanium", "Black Titanium", FilterCategory.METALHEART,
            params = ImageProcessor.ProcessingParams(
                contrast = 1.25f, saturation = 0.55f, brightness = -0.2f,
                clarity = 0.3f, grain = 0.08f
            )
        ),
        FilterPreset("metalheart_polished", "Polished Steel", FilterCategory.METALHEART,
            params = ImageProcessor.ProcessingParams(
                contrast = 1.3f, saturation = 0.65f, brightness = 0.1f,
                sharpness = 0.45f
            )
        ),

        // CINEMATIC
        FilterPreset("cinematic_teal", "Teal & Orange", FilterCategory.CINEMATIC,
            params = ImageProcessor.ProcessingParams(
                contrast = 1.2f, saturation = 1.1f, temperature = 0.15f,
                vibrance = 0.2f, vignette = 0.2f, highlights = -0.1f
            )
        ),
        FilterPreset("cinematic_moody", "Moody Film", FilterCategory.CINEMATIC,
            params = ImageProcessor.ProcessingParams(
                contrast = 1.15f, saturation = 0.85f, brightness = -0.1f,
                temperature = 0.08f, grain = 0.12f, vignette = 0.25f
            )
        ),
        FilterPreset("cinematic_blockbuster", "Blockbuster", FilterCategory.CINEMATIC,
            params = ImageProcessor.ProcessingParams(
                contrast = 1.25f, saturation = 1.05f, clarity = 0.15f,
                temperature = 0.1f, vignette = 0.3f, shadows = 0.1f
            )
        ),

        // FILM EMULATION
        FilterPreset("film_kodak_gold", "Kodak Gold", FilterCategory.FILM,
            params = ImageProcessor.ProcessingParams(
                contrast = 1.1f, saturation = 1.15f, temperature = 0.2f,
                grain = 0.18f, highlights = -0.05f, vibrance = 0.1f
            )
        ),
        FilterPreset("film_fuji_pro", "Fuji Pro 400H", FilterCategory.FILM,
            params = ImageProcessor.ProcessingParams(
                contrast = 1.05f, saturation = 0.95f, temperature = -0.05f,
                tint = 0.08f, grain = 0.15f, highlights = 0.1f
            )
        ),
        FilterPreset("film_ilford", "Ilford HP5", FilterCategory.FILM,
            params = ImageProcessor.ProcessingParams(
                contrast = 1.15f, saturation = 0.8f, grain = 0.22f,
                vignette = 0.1f, clarity = 0.1f
            )
        ),
        FilterPreset("film_cinestill", "CineStill 800T", FilterCategory.FILM,
            params = ImageProcessor.ProcessingParams(
                contrast = 1.1f, saturation = 0.9f, temperature = -0.25f,
                grain = 0.2f, highlights = -0.15f, tint = 0.05f
            )
        ),

        // RETRO
        FilterPreset("retro_vhs", "VHS Tape", FilterCategory.RETRO,
            params = ImageProcessor.ProcessingParams(
                contrast = 1.05f, saturation = 1.1f, grain = 0.15f,
                chromaticAberration = 0.2f, brightness = -0.05f
            )
        ),
        FilterPreset("retro_disposable", "Disposable Camera", FilterCategory.RETRO,
            params = ImageProcessor.ProcessingParams(
                contrast = 1.15f, saturation = 0.9f, grain = 0.2f,
                brightness = 0.05f, sharpness = -0.1f
            )
        ),
        FilterPreset("retro_y2k", "Y2K Digital", FilterCategory.RETRO,
            params = ImageProcessor.ProcessingParams(
                contrast = 1.1f, saturation = 1.05f, brightness = 0.1f,
                sharpness = 0.2f
            )
        ),

        // GLITCH
        FilterPreset("glitch_rgb_split", "RGB Split", FilterCategory.GLITCH,
            customApplier = { bitmap, intensity ->
                applyRGBSplit(bitmap, intensity)
            }
        ),
        FilterPreset("glitch_scanlines", "Scanlines", FilterCategory.GLITCH,
            customApplier = { bitmap, intensity ->
                applyScanlines(bitmap, intensity)
            }
        ),
        FilterPreset("glitch_pixel_sort", "Pixel Sort", FilterCategory.GLITCH,
            customApplier = { bitmap, intensity ->
                applyPixelSort(bitmap, intensity)
            }
        ),

        // PIXEL ART
        FilterPreset("pixel_8bit", "8-Bit", FilterCategory.PIXEL,
            customApplier = { bitmap, intensity ->
                applyPixelation(bitmap, intensity, 8)
            }
        ),
        FilterPreset("pixel_16bit", "16-Bit", FilterCategory.PIXEL,
            customApplier = { bitmap, intensity ->
                applyPixelation(bitmap, intensity, 4)
            }
        ),
        FilterPreset("pixel_dither", "Dithered", FilterCategory.PIXEL,
            customApplier = { bitmap, intensity ->
                applyDithering(bitmap, intensity)
            }
        ),

        // PORTRAIT
        FilterPreset("portrait_soft", "Soft Portrait", FilterCategory.PORTRAIT,
            params = ImageProcessor.ProcessingParams(
                brightness = 0.05f, contrast = 1.05f, saturation = 1.05f,
                clarity = -0.1f, sharpness = 0.1f
            )
        ),
        FilterPreset("portrait_dramatic", "Dramatic", FilterCategory.PORTRAIT,
            params = ImageProcessor.ProcessingParams(
                contrast = 1.3f, saturation = 0.9f, shadows = -0.2f,
                clarity = 0.2f, vignette = 0.3f
            )
        ),

        // COLOR GRADE
        FilterPreset("grade_golden", "Golden Hour", FilterCategory.COLOR_GRADE,
            params = ImageProcessor.ProcessingParams(
                temperature = 0.25f, saturation = 1.1f, brightness = 0.05f,
                vibrance = 0.15f, highlights = -0.1f
            )
        ),
        FilterPreset("grade_blue_hour", "Blue Hour", FilterCategory.COLOR_GRADE,
            params = ImageProcessor.ProcessingParams(
                temperature = -0.2f, tint = -0.05f, saturation = 0.9f,
                brightness = -0.05f, shadows = 0.1f
            )
        ),

        // MONOCHROME
        FilterPreset("mono_bw_classic", "Classic B&W", FilterCategory.MONOCHROME,
            customApplier = { bitmap, intensity ->
                applyMonochrome(bitmap, intensity, 1f, 1f, 1f)
            }
        ),
        FilterPreset("mono_sepia", "Sepia", FilterCategory.MONOCHROME,
            customApplier = { bitmap, intensity ->
                applyMonochrome(bitmap, intensity, 1.2f, 1.0f, 0.8f)
            }
        ),
        FilterPreset("mono_cyanotype", "Cyanotype", FilterCategory.MONOCHROME,
            customApplier = { bitmap, intensity ->
                applyMonochrome(bitmap, intensity, 0.4f, 0.7f, 1.3f)
            }
        ),

        // NIGHT
        FilterPreset("night_neon", "Neon Night", FilterCategory.NIGHT,
            params = ImageProcessor.ProcessingParams(
                exposure = 0.5f, saturation = 1.3f, contrast = 1.15f,
                clarity = 0.2f, vibrance = 0.25f
            )
        ),
        FilterPreset("night_city", "City Lights", FilterCategory.NIGHT,
            params = ImageProcessor.ProcessingParams(
                exposure = 0.3f, contrast = 1.2f, saturation = 1.1f,
                highlights = -0.2f, shadows = 0.3f
            )
        ),

        // CREATIVE
        FilterPreset("creative_dream", "Dream", FilterCategory.CREATIVE,
            params = ImageProcessor.ProcessingParams(
                brightness = 0.1f, contrast = 0.9f, saturation = 1.1f,
                blur = 0.0f, vignette = 0.1f
            )
        ),
        FilterPreset("creative_neon_glow", "Neon Glow", FilterCategory.CREATIVE,
            params = ImageProcessor.ProcessingParams(
                contrast = 1.2f, saturation = 1.4f, vibrance = 0.3f,
                sharpness = 0.3f, brightness = 0.05f
            )
        )
    )

    // --- Custom filter implementations ---

    private fun applyRGBSplit(bitmap: Bitmap, intensity: Float) {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val offset = (intensity * 8).toInt().coerceAtLeast(1)
        val result = IntArray(w * h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                val rX = (x + offset).coerceIn(0, w - 1)
                val bX = (x - offset).coerceIn(0, w - 1)
                val r = Color.red(pixels[y * w + rX])
                val g = Color.green(pixels[idx])
                val b = Color.blue(pixels[y * w + bX])
                result[idx] = Color.argb(Color.alpha(pixels[idx]), r, g, b)
            }
        }
        bitmap.setPixels(result, 0, w, 0, 0, w, h)
    }

    private fun applyScanlines(bitmap: Bitmap, intensity: Float) {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val gap = 3
        for (y in 0 until h) {
            if (y % gap == 0) {
                val darken = 1f - intensity * 0.3f
                for (x in 0 until w) {
                    val idx = y * w + x
                    val r = (Color.red(pixels[idx]) * darken).toInt().coerceIn(0, 255)
                    val g = (Color.green(pixels[idx]) * darken).toInt().coerceIn(0, 255)
                    val b = (Color.blue(pixels[idx]) * darken).toInt().coerceIn(0, 255)
                    pixels[idx] = Color.argb(Color.alpha(pixels[idx]), r, g, b)
                }
            }
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun applyPixelSort(bitmap: Bitmap, intensity: Float) {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val threshold = (1f - intensity) * 128
        val sortLength = (w * intensity * 0.3f).toInt().coerceAtLeast(1)

        for (y in 0 until h step 2) {
            for (x in 0 until w - sortLength step sortLength + 1) {
                val start = y * w + x
                val end = (start + sortLength).coerceAtMost(y * w + w - 1)
                val segment = pixels.sliceArray(start until end)
                segment.sortBy { Color.luminance(it) }
                for (i in segment.indices) {
                    if (Color.luminance(pixels[start + i]) > threshold / 255f) {
                        pixels[start + i] = segment[i]
                    }
                }
            }
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun applyPixelation(bitmap: Bitmap, intensity: Float, baseBlockSize: Int) {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val blockSize = (baseBlockSize * intensity).toInt().coerceAtLeast(1)

        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                // Average color in block
                var r = 0; var g = 0; var b = 0; var count = 0
                val yEnd = (y + blockSize).coerceAtMost(h)
                val xEnd = (x + blockSize).coerceAtMost(w)
                for (by in y until yEnd) {
                    for (bx in x until xEnd) {
                        val p = pixels[by * w + bx]
                        r += Color.red(p)
                        g += Color.green(p)
                        b += Color.blue(p)
                        count++
                    }
                }
                if (count > 0) {
                    val avg = Color.argb(255, r / count, g / count, b / count)
                    for (by in y until yEnd) {
                        for (bx in x until xEnd) {
                            pixels[by * w + bx] = avg
                        }
                    }
                }
                x += blockSize
            }
            y += blockSize
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun applyDithering(bitmap: Bitmap, intensity: Float) {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val levels = (8 - intensity * 6).toInt().coerceAtLeast(2)
        val step = 255 / (levels - 1)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                val r = Color.red(pixels[idx])
                val g = Color.green(pixels[idx])
                val b = Color.blue(pixels[idx])

                val nr = ((r / step) * step).toInt().coerceIn(0, 255)
                val ng = ((g / step) * step).toInt().coerceIn(0, 255)
                val nb = ((b / step) * step).toInt().coerceIn(0, 255)

                // Floyd-Steinberg error diffusion
                val errR = r - nr
                val errG = g - ng
                val errB = b - nb

                pixels[idx] = Color.argb(Color.alpha(pixels[idx]), nr, ng, nb)

                if (x + 1 < w) {
                    val ni = y * w + x + 1
                    val pr = Color.red(pixels[ni])
                    val pg = Color.green(pixels[ni])
                    val pb = Color.blue(pixels[ni])
                    pixels[ni] = Color.argb(Color.alpha(pixels[ni]),
                        (pr + errR * 7f / 16f).toInt().coerceIn(0, 255),
                        (pg + errG * 7f / 16f).toInt().coerceIn(0, 255),
                        (pb + errB * 7f / 16f).toInt().coerceIn(0, 255))
                }
                if (y + 1 < h) {
                    if (x > 0) {
                        val ni = (y + 1) * w + x - 1
                        val pr = Color.red(pixels[ni])
                        val pg = Color.green(pixels[ni])
                        val pb = Color.blue(pixels[ni])
                        pixels[ni] = Color.argb(Color.alpha(pixels[ni]),
                            (pr + errR * 3f / 16f).toInt().coerceIn(0, 255),
                            (pg + errG * 3f / 16f).toInt().coerceIn(0, 255),
                            (pb + errB * 3f / 16f).toInt().coerceIn(0, 255))
                    }
                    val ni = (y + 1) * w + x
                    val pr = Color.red(pixels[ni])
                    val pg = Color.green(pixels[ni])
                    val pb = Color.blue(pixels[ni])
                    pixels[ni] = Color.argb(Color.alpha(pixels[ni]),
                        (pr + errR * 5f / 16f).toInt().coerceIn(0, 255),
                        (pg + errG * 5f / 16f).toInt().coerceIn(0, 255),
                        (pb + errB * 5f / 16f).toInt().coerceIn(0, 255))
                    if (x + 1 < w) {
                        val ni2 = (y + 1) * w + x + 1
                        val pr2 = Color.red(pixels[ni2])
                        val pg2 = Color.green(pixels[ni2])
                        val pb2 = Color.blue(pixels[ni2])
                        pixels[ni2] = Color.argb(Color.alpha(pixels[ni2]),
                            (pr2 + errR * 1f / 16f).toInt().coerceIn(0, 255),
                            (pg2 + errG * 1f / 16f).toInt().coerceIn(0, 255),
                            (pb2 + errB * 1f / 16f).toInt().coerceIn(0, 255))
                    }
                }
            }
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun applyMonochrome(bitmap: Bitmap, intensity: Float, rMult: Float, gMult: Float, bMult: Float) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val r = Color.red(pixels[i])
            val g = Color.green(pixels[i])
            val b = Color.blue(pixels[i])

            val lum = 0.299f * r + 0.587f * g + 0.114f * b

            val nr = (lum * rMult).toInt().coerceIn(0, 255)
            val ng = (lum * gMult).toInt().coerceIn(0, 255)
            val nb = (lum * bMult).toInt().coerceIn(0, 255)

            val finalR = (r + (nr - r) * intensity).toInt().coerceIn(0, 255)
            val finalG = (g + (ng - g) * intensity).toInt().coerceIn(0, 255)
            val finalB = (b + (nb - b) * intensity).toInt().coerceIn(0, 255)

            pixels[i] = Color.argb(Color.alpha(pixels[i]), finalR, finalG, finalB)
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }
}
