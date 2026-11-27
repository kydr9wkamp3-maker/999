package org.adamant.lab

import android.content.Context
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Null metrics are exported as empty CSV fields, never as invented measurements. */
data class ExperimentRecord(
    val timestamp: String,
    val device: String,
    val androidVersion: String,
    val imageName: String,
    val width: Int,
    val height: Int,
    val codec: String,
    val mode: String,
    val originalBytes: Long,
    val compressedBytes: Long,
    val ratio: Double?,
    val encodeMs: Double,
    val decodeMs: Double,
    val psnr: Double?,
    val ssim: Double?,
    val maxSampleError: Int?,
    val shaMatch: Boolean,
    val notes: String,
    val sourceFileSha256: String,
    val sourceSha256: String,
    val decodedSha256: String,
    val bitExact: Boolean,
    val peakMemoryBytes: Long? = null,
    val ssimulacra2: Double? = null,
    val ssimulacra2Status: String = "not measured in MVP"
) {
    companion object {
        fun from(input: ImageInput, mode: Mode, result: CompressionResult) = ExperimentRecord(
            timestamp = Instant.now().toString(),
            device = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            imageName = input.name,
            width = input.bitmap.width,
            height = input.bitmap.height,
            codec = result.codec.display,
            mode = mode.display,
            originalBytes = input.bytes.size.toLong(),
            compressedBytes = result.encoded.size.toLong(),
            ratio = if (result.encoded.isEmpty()) null else input.bytes.size.toDouble() / result.encoded.size,
            encodeMs = result.encodeMs,
            decodeMs = result.decodeMs,
            psnr = result.quality.psnr,
            ssim = result.quality.ssim,
            maxSampleError = result.quality.maxSampleError,
            shaMatch = result.bitExact,
            notes = listOf("SHA match uses normalized RGBA pixel bytes.", input.notes, result.note).filter { it.isNotBlank() }.joinToString(" "),
            sourceFileSha256 = result.sourceFileSha,
            sourceSha256 = result.sourceSha,
            decodedSha256 = result.decodedSha,
            bitExact = result.bitExact
        )
    }
}

object ExperimentLog {
    private const val PREFS = "adamant_experiments"
    private const val KEY = "records"

    fun read(context: Context): List<ExperimentRecord> {
        val array = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"))
        return (0 until array.length()).map { i -> array.getJSONObject(i).toRecord() }
    }

    fun append(context: Context, record: ExperimentRecord) {
        val records = read(context).toMutableList().apply { add(record) }
        save(context, records)
    }

    fun clear(context: Context) { save(context, emptyList()) }

    private fun save(context: Context, records: List<ExperimentRecord>) {
        val array = JSONArray()
        records.forEach { r ->
            array.put(JSONObject().apply {
                put("timestamp", r.timestamp); put("device", r.device); put("androidVersion", r.androidVersion)
                put("imageName", r.imageName); put("width", r.width); put("height", r.height)
                put("codec", r.codec); put("mode", r.mode); put("originalBytes", r.originalBytes)
                put("compressedBytes", r.compressedBytes); put("ratio", r.ratio ?: JSONObject.NULL)
                put("encodeMs", r.encodeMs); put("decodeMs", r.decodeMs)
                put("psnr", r.psnr ?: JSONObject.NULL); put("ssim", r.ssim ?: JSONObject.NULL)
                put("maxSampleError", r.maxSampleError ?: JSONObject.NULL)
                put("shaMatch", r.shaMatch); put("notes", r.notes); put("sourceFileSha256", r.sourceFileSha256)
                put("sourceSha256", r.sourceSha256); put("decodedSha256", r.decodedSha256); put("bitExact", r.bitExact)
                put("peakMemoryBytes", r.peakMemoryBytes ?: JSONObject.NULL)
                put("ssimulacra2", r.ssimulacra2 ?: JSONObject.NULL); put("ssimulacra2Status", r.ssimulacra2Status)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }

    private fun JSONObject.nullableDouble(key: String): Double? = if (isNull(key)) null else getDouble(key)
    private fun JSONObject.nullableLong(key: String): Long? = if (isNull(key)) null else getLong(key)
    private fun JSONObject.nullableInt(key: String): Int? = if (isNull(key)) null else getInt(key)
    private fun JSONObject.optionalString(key: String): String = if (has(key) && !isNull(key)) optString(key, "") else ""

    private fun JSONObject.toRecord() = ExperimentRecord(
        timestamp = getString("timestamp"), device = getString("device"), androidVersion = getString("androidVersion"),
        imageName = getString("imageName"), width = getInt("width"), height = getInt("height"),
        codec = getString("codec"), mode = getString("mode"), originalBytes = getLong("originalBytes"),
        compressedBytes = getLong("compressedBytes"), ratio = nullableDouble("ratio"),
        encodeMs = getDouble("encodeMs"), decodeMs = getDouble("decodeMs"), psnr = nullableDouble("psnr"),
        ssim = nullableDouble("ssim"), maxSampleError = nullableInt("maxSampleError"),
        shaMatch = getBoolean("shaMatch"), notes = optionalString("notes"),
        sourceFileSha256 = optionalString("sourceFileSha256"), sourceSha256 = optionalString("sourceSha256"),
        decodedSha256 = optionalString("decodedSha256"), bitExact = getBoolean("bitExact"),
        peakMemoryBytes = nullableLong("peakMemoryBytes"), ssimulacra2 = nullableDouble("ssimulacra2"),
        ssimulacra2Status = optionalString("ssimulacra2Status").ifEmpty { "not measured in MVP" }
    )

    fun toCsv(records: List<ExperimentRecord>): String {
        val header = listOf(
            "timestamp", "device", "android_version", "image_name", "width", "height", "codec", "mode",
            "original_bytes", "compressed_bytes", "ratio", "encode_ms", "decode_ms", "psnr", "ssim", "max_sample_error",
            "sha_match", "notes", "peak_memory_bytes", "source_file_sha256", "source_rgba_sha256", "decoded_rgba_sha256", "bit_exact",
            "ssimulacra2", "ssimulacra2_status"
        )
        val lines = mutableListOf(header.joinToString(","))
        records.forEach { r ->
            val values = listOf(
                r.timestamp, r.device, r.androidVersion, r.imageName, r.width.toString(), r.height.toString(),
                r.codec, r.mode, r.originalBytes.toString(), r.compressedBytes.toString(), r.ratio?.toString().orEmpty(),
                r.encodeMs.toString(), r.decodeMs.toString(), r.psnr?.toString().orEmpty(), r.ssim?.toString().orEmpty(),
                r.maxSampleError?.toString().orEmpty(), r.shaMatch.toString(), r.notes, r.peakMemoryBytes?.toString().orEmpty(), r.sourceFileSha256,
                r.sourceSha256, r.decodedSha256, r.bitExact.toString(), r.ssimulacra2?.toString().orEmpty(), r.ssimulacra2Status
            )
            lines += values.joinToString(",") { csv(it) }
        }
        return lines.joinToString("\r\n", postfix = "\r\n")
    }

    private fun csv(value: String): String = if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' })
        "\"${value.replace("\"", "\"\"")}\"" else value
}
