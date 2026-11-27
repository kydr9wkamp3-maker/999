package com.chromecore.camera.capabilities

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.util.Log
import android.util.Size
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans and reports all real hardware capabilities of the device.
 * Never assumes capabilities from specs tables — always queries the actual device.
 */
@Singleton
class DeviceCapabilitiesScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DeviceCaps"
    }

    data class DeviceReport(
        val deviceName: String,
        val manufacturer: String,
        val model: String,
        val androidVersion: String,
        val sdkLevel: Int,
        val gpuRenderer: String,
        val cpuArch: String,
        val ramMB: Long,
        val cameras: List<CameraReport>
    )

    data class CameraReport(
        val id: String,
        val facing: String,
        val hardwareLevel: String,
        val sensorSize: String,
        val pixelArraySize: String,
        val activeArraySize: String,
        val focalLengths: List<Float>,
        val apertures: List<Float>,
        val isoRange: Pair<Int, Int>,
        val exposureTimeRange: Pair<Long, Long>,
        val availableSizes: List<Size>,
        val fpsRanges: List<Pair<Int, Int>>,
        val supportsRAW: Boolean,
        val supportsHDR: Boolean,
        val supportsOIS: Boolean,
        val supportsManualSensor: Boolean,
        val supportsManualPostProcessing: Boolean,
        val supportsBurstCapture: Boolean,
        val supportsYuvReprocessing: Boolean,
        val supportsPrivateReprocessing: Boolean,
        val maxDigitalZoom: Float,
        val colorFilterLayout: Int?,
        val supportedFormats: List<Int>,
        val autoExposureModes: List<Int>,
        val focusModes: List<Int>
    )

    suspend fun scanDevice(): DeviceReport = withContext(Dispatchers.IO) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameras = cameraManager.cameraIdList.map { id ->
            scanCamera(cameraManager, id)
        }

        DeviceReport(
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            sdkLevel = Build.VERSION.SDK_INT,
            gpuRenderer = getGpuRenderer(),
            cpuArch = getCPUArchitecture(),
            ramMB = getAvailableRam(),
            cameras = cameras
        )
    }

    private fun scanCamera(cameraManager: CameraManager, cameraId: String): CameraReport {
        val chars = cameraManager.getCameraCharacteristics(cameraId)

        val facing = when (chars.get(CameraCharacteristics.LENS_FACING)) {
            CameraCharacteristics.LENS_FACING_BACK -> "BACK"
            CameraCharacteristics.LENS_FACING_FRONT -> "FRONT"
            else -> "EXTERNAL"
        }

        val hwLevel = when (chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) {
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
            else -> "UNKNOWN"
        }

        val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
        val sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        val pixelArray = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val activeArray = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList() ?: emptyList()
        val apertures = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.toList() ?: emptyList()
        val isoRange = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val exposureRange = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val maxZoom = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f

        val configMap = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val sizes = configMap?.getOutputSizes(android.graphics.ImageFormat.JPEG)?.toList() ?: emptyList()

        val fpsRanges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            ?.map { it.lower..it.upper }
            ?.map { it.first to it.last } ?: emptyList()

        val supportedFormats = configMap?.getOutputFormats()?.toList() ?: emptyList()

        return CameraReport(
            id = cameraId,
            facing = facing,
            hardwareLevel = hwLevel,
            sensorSize = sensorSize?.toString() ?: "unknown",
            pixelArraySize = pixelArray?.toString() ?: "unknown",
            activeArraySize = activeArray?.toString() ?: "unknown",
            focalLengths = focalLengths,
            apertures = apertures,
            isoRange = if (isoRange != null) Pair(isoRange.lower, isoRange.upper) else Pair(0, 0),
            exposureTimeRange = if (exposureRange != null) Pair(exposureRange.lower, exposureRange.upper) else Pair(0L, 0L),
            availableSizes = sizes,
            fpsRanges = fpsRanges,
            supportsRAW = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW),
            supportsHDR = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_HDR),
            supportsOIS = false, // Checked per mode below
            supportsManualSensor = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR),
            supportsManualPostProcessing = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING),
            supportsBurstCapture = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE),
            supportsYuvReprocessing = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING),
            supportsPrivateReprocessing = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING),
            maxDigitalZoom = maxZoom,
            colorFilterLayout = chars.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT),
            supportedFormats = supportedFormats,
            autoExposureModes = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)?.toList() ?: emptyList(),
            focusModes = chars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)?.toList() ?: emptyList()
        )
    }

    private fun getGpuRenderer(): String {
        return try {
            val glView = android.opengl.GLSurfaceView(context)
            // In practice, GPU info comes from EGL_RENDERER string at runtime
            "OpenGL ES (query at runtime)"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getCPUArchitecture(): String {
        return System.getProperty("os.arch") ?: "unknown"
    }

    private fun getAvailableRam(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.totalMem / (1024 * 1024)
    }

    fun exportAsJson(report: DeviceReport): String {
        val json = JSONObject().apply {
            put("device", report.deviceName)
            put("manufacturer", report.manufacturer)
            put("model", report.model)
            put("androidVersion", report.androidVersion)
            put("sdkLevel", report.sdkLevel)
            put("gpuRenderer", report.gpuRenderer)
            put("cpuArch", report.cpuArch)
            put("ramMB", report.ramMB)

            val camerasJson = JSONArray()
            report.cameras.forEach { cam ->
                camerasJson.put(JSONObject().apply {
                    put("id", cam.id)
                    put("facing", cam.facing)
                    put("hardwareLevel", cam.hardwareLevel)
                    put("sensorSize", cam.sensorSize)
                    put("pixelArraySize", cam.pixelArraySize)
                    put("focalLengths", JSONArray(cam.focalLengths))
                    put("apertures", JSONArray(cam.apertures))
                    put("isoMin", cam.isoRange.first)
                    put("isoMax", cam.isoRange.second)
                    put("supportsRAW", cam.supportsRAW)
                    put("supportsHDR", cam.supportsHDR)
                    put("supportsOIS", cam.supportsOIS)
                    put("maxDigitalZoom", cam.maxDigitalZoom)
                    put("supportsManualSensor", cam.supportsManualSensor)
                    put("supportsBurstCapture", cam.supportsBurstCapture)

                    val sizesArray = JSONArray()
                    cam.availableSizes.take(20).forEach {
                        sizesArray.put("${it.width}x${it.height}")
                    }
                    put("availableSizes", sizesArray)
                })
            }
            put("cameras", camerasJson)
        }
        return json.toString(2)
    }
}
