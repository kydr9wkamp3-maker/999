package com.chromecore.camera.ui.screens.devicelab

import androidx.compose.runtime.Composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromecore.camera.ui.theme.ChromecoreColors

/**
 * CHROMECORE X — Device Camera Laboratory
 * Shows real hardware capabilities of the device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Composable
fun DeviceLabScreen(onBack: () -> Unit) {
    var report by remember { mutableStateOf<DeviceReportData?>(null) }
    var isScanning by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isScanning = true
        // In production, call DeviceCapabilitiesScanner
        report = DeviceReportData.sample()
        isScanning = false
    }

    Scaffold(
        containerColor = ChromecoreColors.ObsidianBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Device Laboratory",
                        fontWeight = FontWeight.Light,
                        color = ChromecoreColors.ColdWhite,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = ChromecoreColors.ColdWhite)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Export JSON */ }) {
                        Icon(Icons.Filled.Share, "Export", tint = ChromecoreColors.CyanAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isScanning) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ChromecoreColors.CyanAccent)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Scanning device capabilities...", color = ChromecoreColors.ChromeSilver)
                    }
                }
            }

            report?.let { data ->
                // Device info card
                InfoCard("Device") {
                    InfoRow("Name", data.deviceName)
                    InfoRow("Android", data.androidVersion)
                    InfoRow("SDK Level", data.sdkLevel.toString())
                    InfoRow("CPU Architecture", data.cpuArch)
                    InfoRow("Total RAM", "${data.ramMB} MB")
                    InfoRow("GPU", data.gpuRenderer)
                }

                // Camera info cards
                data.cameras.forEach { cam ->
                    InfoCard("Camera ${cam.id} — ${cam.facing}") {
                        InfoRow("Hardware Level", cam.hardwareLevel)
                        InfoRow("Sensor Size", cam.sensorSize)
                        InfoRow("Pixel Array", cam.pixelArraySize)
                        InfoRow("Focal Length", cam.focalLengths.joinToString(", ") { "${it}mm" })
                        InfoRow("Max Digital Zoom", "${"%.1f".format(cam.maxDigitalZoom)}x")
                        InfoRow("ISO Range", "${cam.isoRange.first} – ${cam.isoRange.second}")
                        InfoRow("RAW Support", if (cam.supportsRAW) "✅ Yes" else "❌ No")
                        InfoRow("HDR Support", if (cam.supportsHDR) "✅ Yes" else "❌ No")
                        InfoRow("OIS Support", if (cam.supportsOIS) "✅ Yes" else "❌ No")
                        InfoRow("Manual Sensor", if (cam.supportsManualSensor) "✅ Yes" else "❌ No")
                        InfoRow("Burst Capture", if (cam.supportsBurstCapture) "✅ Yes" else "❌ No")

                        InfoRow("Available Sizes", "")
                        cam.availableSizes.take(10).forEach { size ->
                            Text(
                                "  • ${size}",
                                fontSize = 11.sp,
                                color = ChromecoreColors.TitaniumGrey,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ChromecoreColors.SurfaceVariant)
            .padding(16.dp)
    ) {
        Text(
            title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ChromecoreColors.CyanAccent,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

@Composable
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = ChromecoreColors.ChromeSilver)
        Text(
            value,
            fontSize = 13.sp,
            color = ChromecoreColors.ColdWhite,
            fontWeight = FontWeight.Medium,
            maxLines = 2
        )
    }
}

/**
 * Sample data for preview — replaced by real DeviceCapabilitiesScanner output.
 */
data class DeviceReportData(
    val deviceName: String,
    val androidVersion: String,
    val sdkLevel: Int,
    val cpuArch: String,
    val ramMB: Long,
    val gpuRenderer: String,
    val cameras: List<CameraReportData>
) {
    companion object {
        fun sample() = DeviceReportData(
            deviceName = "motorola moto g85 5G",
            androidVersion = "15",
            sdkLevel = 35,
            cpuArch = "aarch64",
            ramMB = 8192,
            gpuRenderer = "Adreno 619",
            cameras = listOf(
                CameraReportData(
                    id = "0", facing = "BACK", hardwareLevel = "LEVEL_3",
                    sensorSize = "6.45 x 4.85 mm", pixelArraySize = "8160 x 6120",
                    focalLengths = listOf(4.56f), maxDigitalZoom = 10.0f,
                    isoRange = 50 to 3200, supportsRAW = true, supportsHDR = true,
                    supportsOIS = true, supportsManualSensor = true,
                    supportsBurstCapture = true,
                    availableSizes = listOf("4000x3000", "3840x2160", "1920x1080", "1280x720")
                ),
                CameraReportData(
                    id = "1", facing = "FRONT", hardwareLevel = "LIMITED",
                    sensorSize = "4.50 x 3.38 mm", pixelArraySize = "6528 x 4896",
                    focalLengths = listOf(2.65f), maxDigitalZoom = 4.0f,
                    isoRange = 100 to 1600, supportsRAW = false, supportsHDR = false,
                    supportsOIS = false, supportsManualSensor = false,
                    supportsBurstCapture = false,
                    availableSizes = listOf("3264x2448", "1920x1080", "1280x720")
                )
            )
        )
    }
}

data class CameraReportData(
    val id: String, val facing: String, val hardwareLevel: String,
    val sensorSize: String, val pixelArraySize: String,
    val focalLengths: List<Float>, val maxDigitalZoom: Float,
    val isoRange: Pair<Int, Int>,
    val supportsRAW: Boolean, val supportsHDR: Boolean,
    val supportsOIS: Boolean, val supportsManualSensor: Boolean,
    val supportsBurstCapture: Boolean,
    val availableSizes: List<String>
)
