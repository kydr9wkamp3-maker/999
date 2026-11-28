package com.chromecore.camera.ui.screens.devicelab

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromecore.camera.ui.theme.ChromecoreColors
import org.json.JSONArray
import org.json.JSONObject

data class CameraInfo(
    val id: String,
    val facing: String,
    val hardwareLevel: String,
    val focalLengths: List<Float>,
    val isoRange: Pair<Int, Int>?,
    val exposureTimeRange: Pair<Long, Long>?,
    val sensorSize: Size?,
    val pixelArraySize: Size?,
    val minFocusDistance: Float?,
    val capabilities: List<Int>,
    val physicalCameraIds: Set<String>
)

@Composable
fun DeviceLabScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val cameras = remember { getCameraInfoList(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChromecoreColors.ObsidianBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "Device Lab",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.weight(1f))
                // Export JSON button
                TextButton(onClick = { exportToJSON(context, cameras) }) {
                    Text("Export JSON", color = ChromecoreColors.CyanAccent)
                }
            }

            Divider(color = ChromecoreColors.GraphiteMid, thickness = 1.dp)

            // Camera list
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                cameras.forEach { camera ->
                    CameraCard(camera)
                }
            }
        }
    }
}

@Composable
private fun CameraCard(info: CameraInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ChromecoreColors.Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Camera ${info.id} — ${info.facing}",
                color = ChromecoreColors.CyanAccent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            InfoRow("Hardware Level", info.hardwareLevel)
            info.sensorSize?.let { InfoRow("Sensor Size", "${it.width}×${it.height}") }
            info.pixelArraySize?.let { InfoRow("Pixel Array", "${it.width}×${it.height}") }

            if (info.focalLengths.isNotEmpty()) {
                InfoRow("Focal Lengths", info.focalLengths.joinToString(", ") { "${it}mm" })
            }

            info.isoRange?.let { (min, max) ->
                InfoRow("ISO Range", "$min – $max")
            }

            info.exposureTimeRange?.let { (min, max) ->
                InfoRow("Exposure Time", "${min}ns – ${max}ns")
            }

            info.minFocusDistance?.let {
                InfoRow("Min Focus Distance", "${it}m")
            }

            if (info.physicalCameraIds.isNotEmpty()) {
                InfoRow("Physical IDs", info.physicalCameraIds.joinToString(", "))
            }

            if (info.capabilities.isNotEmpty()) {
                InfoRow("Capabilities", info.capabilities.joinToString(", ") { "CAP_$it" })
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            "$label: ",
            color = ChromecoreColors.ChromeSilver,
            fontSize = 13.sp,
            modifier = Modifier.width(140.dp)
        )
        Text(
            value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getCameraInfoList(context: Context): List<CameraInfo> {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val result = mutableListOf<CameraInfo>()

    for (cameraId in cameraManager.cameraIdList) {
        try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)

            val facing = when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
                CameraCharacteristics.LENS_FACING_BACK -> "Back"
                CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                else -> "Unknown"
            }

            val hardwareLevel = when (characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) {
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
                else -> "UNKNOWN"
            }

            val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList() ?: emptyList()
            val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
            val exposureTimeRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
            val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
            val pixelArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
            val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
            val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.toList() ?: emptyList()
            val physicalCameraIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                characteristics.physicalCameraIds
            } else {
                emptySet()
            }

            result.add(
                CameraInfo(
                    id = cameraId,
                    facing = facing,
                    hardwareLevel = hardwareLevel,
                    focalLengths = focalLengths,
                    isoRange = isoRange?.let { it.lower to it.upper },
                    exposureTimeRange = exposureTimeRange?.let { it.lower to it.upper },
                    sensorSize = sensorSize?.let { Size(it.width.toInt(), it.height.toInt()) },
                    pixelArraySize = pixelArraySize,
                    minFocusDistance = minFocusDistance,
                    capabilities = capabilities,
                    physicalCameraIds = physicalCameraIds
                )
            )
        } catch (e: Exception) {
            // Skip cameras that can't be read
        }
    }

    return result
}

private fun exportToJSON(context: Context, cameras: List<CameraInfo>) {
    val jsonArray = JSONArray()
    cameras.forEach { camera ->
        val json = JSONObject().apply {
            put("id", camera.id)
            put("facing", camera.facing)
            put("hardwareLevel", camera.hardwareLevel)
            put("focalLengths", JSONArray(camera.focalLengths))
            camera.isoRange?.let { (min, max) ->
                put("isoMin", min)
                put("isoMax", max)
            }
            camera.exposureTimeRange?.let { (min, max) ->
                put("exposureTimeMin", min)
                put("exposureTimeMax", max)
            }
            put("capabilities", JSONArray(camera.capabilities.map { it.toString() }))
            put("physicalCameraIds", JSONArray(camera.physicalCameraIds.toList()))
        }
        jsonArray.put(json)
    }

    // Save to file
    val file = java.io.File(context.getExternalFilesDir(null), "chromecore_device_lab.json")
    file.writeText(jsonArray.toString(2))

    // In a real app, we would show a toast or use the share intent
    // For now, just log it
    android.util.Log.i("DeviceLab", "Exported to: ${file.absolutePath}")
}
