package com.chromecore.camera.ui.screens.camera

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.chromecore.camera.engine.CameraEngine
import com.chromecore.camera.ui.theme.ChromecoreColors
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executors
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.ui.platform.LocalContext
import android.content.Context

/**
 * CHROMECORE X — Camera Screen
 *
 * Main shooting interface with:
 * - Full-bleed camera preview
 * - Large metallic shutter button
 * - Mode selector (Photo, Video, Portrait, Night, Pro, Effects)
 * - Filter quick-access strip
 * - Camera controls (flash, switch, zoom, timer)
 * - Last photo thumbnail
 * - Focus indicator
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onNavigateToGallery: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEditor: (String) -> Unit
) {
    val context = LocalContext.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    if (cameraPermission.status.isGranted) {
        CameraContent(
            onNavigateToGallery = onNavigateToGallery,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToEditor = onNavigateToEditor
        )
    } else {
        PermissionRequestScreen(onRequestPermission = { cameraPermission.launchPermissionRequest() })
    }
}

@Composable
private fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChromecoreColors.ObsidianBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Filled.CameraAlt,
                contentDescription = null,
                tint = ChromecoreColors.ChromeSilver,
                modifier = Modifier.size(64.dp)
            )
            Text(
                "Camera Access Required",
                style = MaterialTheme.typography.headlineMedium,
                color = ChromecoreColors.ColdWhite
            )
            Text(
                "CHROMECORE X needs camera access to capture photos and videos",
                style = MaterialTheme.typography.bodyMedium,
                color = ChromecoreColors.ChromeSilver,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChromecoreColors.CyanAccent,
                    contentColor = ChromecoreColors.ObsidianBlack
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Grant Permission", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CameraContent(
    onNavigateToGallery: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEditor: (String) -> Unit
) {
    var selectedMode by remember { mutableStateOf(CameraModeEnum.PHOTO) }
    var flashMode by remember { mutableStateOf(FlashMode.AUTO) }
    var isRecording by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableFloatStateOf(1f) }
    var showFilters by remember { mutableStateOf(false) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChromecoreColors.ObsidianBlack)
    ) {
        // Camera Preview
        CameraPreview(
            onTapToFocus = { offset -> focusPoint = offset }
        )

        // Focus indicator
        focusPoint?.let { point ->
            FocusIndicator(point)
            LaunchedEffect(point) {
                kotlinx.coroutines.delay(1500)
                focusPoint = null
            }
        }

        // Top HUD overlay
        TopControls(
            flashMode = flashMode,
            onFlashClick = { 
                flashMode = when (flashMode) {
                    FlashMode.AUTO -> FlashMode.ON
                    FlashMode.ON -> FlashMode.OFF
                    FlashMode.OFF -> FlashMode.TORCH
                    FlashMode.TORCH -> FlashMode.AUTO
                }
            },
            onSettingsClick = onNavigateToSettings,
            onTimerClick = { /* Timer logic */ }
        )

        // Right side - Zoom slider
        ZoomSlider(
            zoomLevel = zoomLevel,
            onZoomChange = { zoomLevel = it }
        )

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, ChromecoreColors.ObsidianBlack),
                        startY = 0f,
                        endY = 300f
                    )
                )
                .padding(bottom = 24.dp)
        ) {
            // Filter strip (when showing)
            AnimatedVisibility(
                visible = showFilters,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                FilterStrip()
            }

            // Mode selector
            ModeSelector(
                selectedMode = selectedMode,
                onModeSelected = { selectedMode = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main capture controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery thumbnail
                GalleryThumbnail(
                    onClick = onNavigateToGallery
                )

                // Shutter button
                ShutterButton(
                    isRecording = isRecording,
                    onClick = {
                        if (selectedMode == CameraModeEnum.VIDEO) {
                            isRecording = !isRecording
                        }
                        // Capture logic
                    }
                )

                // Switch camera
                IconButton(
                    onClick = { /* Switch camera */ },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ChromecoreColors.GraphiteMid.copy(alpha = 0.6f))
                ) {
                    Icon(
                        Icons.Filled.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = ChromecoreColors.ColdWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick toggle: Effects button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = { showFilters = !showFilters }
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = ChromecoreColors.CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Effects",
                        color = ChromecoreColors.CyanAccent,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(onTapToFocus: (Offset) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onTapToFocus(offset)
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(surfaceProvider)
                        }

                        val imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("ChromecoreX", "Camera bind failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun FocusIndicator(position: Offset) {
    Box(
        modifier = Modifier
            .offset(x = position.x.dp - 30.dp, y = position.y.dp - 30.dp)
            .size(60.dp)
            .clip(CircleShape)
            .border(
                width = 1.5.dp,
                color = ChromecoreColors.CyanAccent,
                shape = CircleShape
            )
            .animateColorAsState(
                targetValue = ChromecoreColors.CyanAccent,
                animationSpec = tween(300)
            )
    )
}

@Composable
private fun TopControls(
    flashMode: FlashMode,
    onFlashClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTimerClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left - Settings
        CameraControlButton(
            icon = Icons.Filled.Settings,
            onClick = onSettingsClick
        )

        // Center - Timer
        CameraControlButton(
            icon = Icons.Filled.Timer,
            onClick = onTimerClick
        )

        // Right - Flash
        val flashIcon = when (flashMode) {
            FlashMode.AUTO -> Icons.Filled.FlashAuto
            FlashMode.ON -> Icons.Filled.FlashOn
            FlashMode.OFF -> Icons.Filled.FlashOff
            FlashMode.TORCH -> Icons.Filled.FlashlightOn
        }
        CameraControlButton(
            icon = flashIcon,
            onClick = onFlashClick
        )
    }
}

@Composable
private fun CameraControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(ChromecoreColors.GraphiteDark.copy(alpha = 0.7f))
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = ChromecoreColors.ColdWhite,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ZoomSlider(zoomLevel: Float, onZoomChange: (Float) -> Unit) {
    Column(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(end = 12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Zoom level indicator pills
        listOf(1f, 2f, 3f, 5f).forEach { level ->
            val isSelected = abs(zoomLevel - level) < 0.5f
            TextButton(
                onClick = { onZoomChange(level) },
                modifier = Modifier
                    .size(32.dp)
                    .padding(2.dp)
            ) {
                Text(
                    "${level.toInt()}x",
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) ChromecoreColors.CyanAccent else ChromecoreColors.ChromeSilver
                )
            }
        }
    }
}

@Composable
private fun ModeSelector(selectedMode: CameraModeEnum, onModeSelected: (CameraModeEnum) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        items(CameraModeEnum.entries.toList()) { mode ->
            val isSelected = mode == selectedMode
            Text(
                text = mode.label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) ChromecoreColors.CyanAccent else ChromecoreColors.ChromeSilver,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .clickable { onModeSelected(mode) }
            )
        }
    }
}

@Composable
private fun ShutterButton(isRecording: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .drawBehind {
                drawCircle(
                    color = ChromecoreColors.GraphiteMid,
                    radius = size.minDimension / 2,
                    style = Stroke(width = 4.dp.toPx())
                )
            }
            .clickable(onClick = onClick)
            .padding(6.dp)
            .clip(CircleShape)
            .background(
                if (isRecording) ChromecoreColors.ErrorRed 
                else Brush.verticalGradient(
                    listOf(ChromecoreColors.ColdWhite, ChromecoreColors.ChromeSilver)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun GalleryThumbnail(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ChromecoreColors.GraphiteMid)
            .border(1.dp, ChromecoreColors.GraphiteLight, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.PhotoLibrary,
            contentDescription = "Gallery",
            tint = ChromecoreColors.ChromeSilver,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun FilterStrip() {
    val filters = listOf(
        "Original" to "📷",
        "Liquid Chrome" to "🪞",
        "Metalheart" to "⚙️",
        "Teal & Orange" to "🎬",
        "Moody Film" to "🎞️",
        "Kodak Gold" to "📸",
        "Neon Night" to "💜",
        "VHS Tape" to "📼",
        "8-Bit" to "👾",
        "B&W" to "◼️",
        "Pixel Sort" to "🌊",
        "Scanlines" to "📺"
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(filters) { (name, emoji) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(64.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ChromecoreColors.GraphiteMid)
                        .border(1.dp, ChromecoreColors.GraphiteLight, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    name,
                    fontSize = 10.sp,
                    color = ChromecoreColors.ChromeSilver,
                    maxLines = 1
                )
            }
        }
    }
}

enum class CameraModeEnum(val label: String) {
    PHOTO("PHOTO"),
    VIDEO("VIDEO"),
    PORTRAIT("PORTRAIT"),
    NIGHT("NIGHT"),
    PRO("PRO"),
    EFFECTS("FX")
}

enum class FlashMode { AUTO, ON, OFF, TORCH }

private fun abs(value: Float): Float = kotlin.math.abs(value)
