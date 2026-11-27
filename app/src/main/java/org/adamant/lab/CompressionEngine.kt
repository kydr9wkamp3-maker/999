package org.adamant.lab

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.SystemClock
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.MessageDigest
import kotlin.math.ln
import kotlin.math.max

const val ADAMANT_LABEL = "ADAMANT-1 prototype"
const val CHIMERA_TOLERANCE = 6

enum class Codec(val display: String) {
    ADAMANT(ADAMANT_LABEL), CHIMERA("CHIMERA-1 research prototype"), PNG("PNG"), JPEG("JPEG quality 95"), WEBP_LOSSLESS("WebP lossless"), WEBP("WebP quality 95")
}
enum class Mode(val display: String) {
    EXACT("Exact"), BOUNDED_ERROR("Bounded-error (lossy, max 6/255)"), VISUAL_LOSSLESS("Visual-lossless"), PROGRESSIVE("Progressive")
}

data class ImageInput(
    val bitmap: Bitmap,
    val bytes: ByteArray,
    val name: String,
    val mime: String,
    val notes: String = "",
    val sourceByteSize: Long = bytes.size.toLong(),
    val sourceFileSha256: String? = null,
    val downsampledForAnalysis: Boolean = false
)
data class Quality(val psnr: Double?, val ssim: Double?, val maxSampleError: Int?)
data class CompressionResult(
    val codec: Codec, val mode: Mode, val encoded: ByteArray, val decoded: Bitmap,
    val encodeMs: Double, val decodeMs: Double, val quality: Quality,
    val sourceFileSha: String, val sourceSha: String, val decodedSha: String, val bitExact: Boolean, val note: String = ""
)

object ImageBytes {
    /** Converts one row at a time to avoid a second full-image IntArray allocation. */
    fun rgba(bitmap: Bitmap): ByteArray {
        val out = ByteArray(Math.multiplyExact(Math.multiplyExact(bitmap.width, bitmap.height), 4))
        val row = IntArray(bitmap.width)
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            var p = y * bitmap.width * 4
            for (c in row) {
                out[p++] = Color.red(c).toByte()
                out[p++] = Color.green(c).toByte()
                out[p++] = Color.blue(c).toByte()
                out[p++] = Color.alpha(c).toByte()
            }
        }
        return out
    }

    /** Reconstructs the bitmap row-wise, again avoiding a full-image IntArray. */
    fun bitmap(rgba: ByteArray, width: Int, height: Int): Bitmap {
        require(rgba.size.toLong() == width.toLong() * height * 4L) { "Decoded pixel length does not match image dimensions" }
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val row = IntArray(width)
        for (y in 0 until height) {
            var p = y * width * 4
            for (x in row.indices) {
                val r = rgba[p++].toInt() and 255
                val g = rgba[p++].toInt() and 255
                val b = rgba[p++].toInt() and 255
                val a = rgba[p++].toInt() and 255
                row[x] = Color.argb(a, r, g, b)
            }
            result.setPixels(row, 0, width, 0, y, width, 1)
        }
        return result
    }

    fun sha256(data: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(data).joinToString("") { "%02x".format(it) }

    fun sha256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

object QualityMetrics {
    /** PSNR across normalized RGBA samples; null means mathematically identical. */
    fun psnr(a: ByteArray, b: ByteArray): Double? {
        require(a.size == b.size)
        var sum = 0.0
        for (i in a.indices) {
            val d = (a[i].toInt() and 255) - (b[i].toInt() and 255)
            sum += d.toDouble() * d
        }
        if (sum == 0.0) return null
        val mse = sum / a.size
        return 20.0 * ln(255.0) / ln(10.0) - 10.0 * ln(mse) / ln(10.0)
    }

    /** Global luminance SSIM approximation (not windowed SSIM). */
    fun globalSsim(a: ByteArray, b: ByteArray): Double {
        require(a.size == b.size && a.isNotEmpty() && a.size % 4 == 0)
        val n = a.size / 4
        var meanA = 0.0; var meanB = 0.0
        for (i in 0 until n) {
            val p = i * 4
            meanA += .2126 * (a[p].toInt() and 255) + .7152 * (a[p + 1].toInt() and 255) + .0722 * (a[p + 2].toInt() and 255)
            meanB += .2126 * (b[p].toInt() and 255) + .7152 * (b[p + 1].toInt() and 255) + .0722 * (b[p + 2].toInt() and 255)
        }
        meanA /= n; meanB /= n
        var va = 0.0; var vb = 0.0; var covariance = 0.0
        for (i in 0 until n) {
            val p = i * 4
            val lumA = .2126 * (a[p].toInt() and 255) + .7152 * (a[p + 1].toInt() and 255) + .0722 * (a[p + 2].toInt() and 255)
            val lumB = .2126 * (b[p].toInt() and 255) + .7152 * (b[p + 1].toInt() and 255) + .0722 * (b[p + 2].toInt() and 255)
            val da = lumA - meanA; val db = lumB - meanB
            va += da * da; vb += db * db; covariance += da * db
        }
        val divisor = max(1, n - 1).toDouble()
        va /= divisor; vb /= divisor; covariance /= divisor
        val c1 = (0.01 * 255) * (0.01 * 255)
        val c2 = (0.03 * 255) * (0.03 * 255)
        return ((2 * meanA * meanB + c1) * (2 * covariance + c2)) /
            ((meanA * meanA + meanB * meanB + c1) * (va + vb + c2))
    }

    fun maxSampleError(a: ByteArray, b: ByteArray): Int {
        require(a.size == b.size)
        var maximum = 0
        for (i in a.indices) maximum = max(maximum, kotlin.math.abs((a[i].toInt() and 255) - (b[i].toInt() and 255)))
        return maximum
    }

    fun compare(a: ByteArray, b: ByteArray): Quality {
        if (a.size != b.size) return Quality(null, null, null)
        return Quality(psnr(a, b), globalSsim(a, b), maxSampleError(a, b))
    }
}

object CompressionEngine {
    fun compress(input: ImageInput, codec: Codec, mode: Mode): CompressionResult {
        when (codec) {
            Codec.ADAMANT -> require(mode == Mode.EXACT) { "ADAMANT-1 prototype in MVP supports Exact only" }
            Codec.CHIMERA -> require(mode == Mode.EXACT || mode == Mode.BOUNDED_ERROR) {
                "CHIMERA-1 research prototype supports Exact residual or Bounded-error only"
            }
            else -> require(mode == Mode.EXACT) { "$mode is not implemented for ${codec.display}; refusing to mislabel output" }
        }
        val originalRgba = ImageBytes.rgba(input.bitmap)
        val sourceSha = ImageBytes.sha256(originalRgba)
        val encodeStart = SystemClock.elapsedRealtimeNanos()
        val encoded = when (codec) {
            Codec.ADAMANT -> AdamantNative.nativeEncode(originalRgba, input.bitmap.width, input.bitmap.height)
            Codec.CHIMERA -> AdamantNative.nativeChimeraEncode(
                originalRgba, input.bitmap.width, input.bitmap.height, mode == Mode.EXACT, CHIMERA_TOLERANCE
            )
            Codec.PNG -> encodeBitmap(input.bitmap, Bitmap.CompressFormat.PNG, 100)
            Codec.JPEG -> encodeBitmap(input.bitmap, Bitmap.CompressFormat.JPEG, 95)
            Codec.WEBP -> encodeBitmap(input.bitmap, webpLossyFormat(), 95)
            Codec.WEBP_LOSSLESS -> {
                if (Build.VERSION.SDK_INT < 30) throw UnsupportedOperationException("WebP lossless compression API is unavailable before Android 11")
                encodeBitmap(input.bitmap, Bitmap.CompressFormat.WEBP_LOSSLESS, 100)
            }
        }
        val encodeMs = (SystemClock.elapsedRealtimeNanos() - encodeStart) / 1_000_000.0
        val decodeStart = SystemClock.elapsedRealtimeNanos()
        val decodedAndPixels = if (codec == Codec.ADAMANT || codec == Codec.CHIMERA) {
            val raw = if (codec == Codec.ADAMANT) AdamantNative.nativeDecode(encoded) else AdamantNative.nativeChimeraDecode(encoded)
            ImageBytes.bitmap(raw, input.bitmap.width, input.bitmap.height) to raw
        } else {
            val bitmap = BitmapFactory.decodeByteArray(encoded, 0, encoded.size)
                ?: throw IllegalStateException("Android could not decode ${codec.display} output")
            bitmap to ImageBytes.rgba(bitmap)
        }
        val decodeMs = (SystemClock.elapsedRealtimeNanos() - decodeStart) / 1_000_000.0
        val decoded = decodedAndPixels.first
        val decodedRgba = decodedAndPixels.second
        val exact = originalRgba.contentEquals(decodedRgba)
        val quality = QualityMetrics.compare(originalRgba, decodedRgba)
        if (codec == Codec.CHIMERA && mode == Mode.BOUNDED_ERROR) {
            require(quality.maxSampleError != null && quality.maxSampleError <= CHIMERA_TOLERANCE) {
                "CHIMERA bounded-error postcondition failed; refusing to report result"
            }
        }
        val note = when {
            codec == Codec.ADAMANT -> "Prototype codec; round-trip checks normalized RGBA pixel bytes, not original file bytes."
            codec == Codec.CHIMERA && mode == Mode.EXACT -> "Research prototype: quantized/routed base plus XOR residual. Exactness is checked on normalized RGBA bytes; no rate advantage is claimed."
            codec == Codec.CHIMERA -> "Lossy bounded-error research mode; maximum per-channel RGBA error checked at <= $CHIMERA_TOLERANCE/255. This is not visual-lossless and not a JND guarantee."
            codec == Codec.JPEG -> "JPEG is lossy; quality is requested as 95."
            codec == Codec.WEBP && Build.VERSION.SDK_INT < 30 -> "Legacy Android WEBP encoder selected; quality setting is requested as 95."
            else -> ""
        }
        return CompressionResult(
            codec, mode, encoded, decoded, encodeMs, decodeMs, quality, input.sourceFileSha256 ?: ImageBytes.sha256(input.bytes), sourceSha,
            ImageBytes.sha256(decodedRgba), exact, note
        )
    }

    private fun encodeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        check(bitmap.compress(format, quality, stream)) { "Bitmap encoder failed for $format" }
        return stream.toByteArray()
    }

    @Suppress("DEPRECATION")
    private fun webpLossyFormat(): Bitmap.CompressFormat = if (Build.VERSION.SDK_INT >= 30)
        Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
}
