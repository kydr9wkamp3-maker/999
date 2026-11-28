package com.chromecore.camera.ui.screens.camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import com.chromecore.camera.ui.components.ShutterButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.chromecore.camera.ui.theme.ChromecoreColors
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

enum class FlashMode { OFF, ON, AUTO }

private fun FlashMode.toImageCaptureMode(): Int = when (this) {
    FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
    FlashMode.ON -> ImageCapture.FLASH_MODE_ON
    FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
}

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }
    var lastSavedMessage by remember { mutableStateOf<String?>(null) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var flashMode by remember { mutableStateOf(FlashMode.OFF) }
    var zoomLevel by remember { mutableFloatStateOf(1f) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionRequested = true
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        hasCameraPermission = granted
        if (!granted && !permissionRequested) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }

    var currentCamera by remember { mutableStateOf<Camera?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var focusRingPosition by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    var exposureIndex by remember { mutableIntStateOf(0) }
    var showGrid by remember { mutableStateOf(true) }
    var timerSeconds by remember { mutableIntStateOf(0) }
    var countdown by remember { mutableIntStateOf(0) }
    var isCountingDown by remember { mutableStateOf(false) }
    var showDeviceLab by remember { mutableStateOf(false) }
    var showGallery by remember { mutableStateOf(false) }

    // Get max zoom from camera (re-reads on recomposition)
    val maxZoom = currentCamera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1f

    // Get exposure compensation range
    val exposureState = currentCamera?.cameraInfo?.exposureState
    val minExposure = exposureState?.exposureCompensationRange?.lower ?: -10
    val maxExposure = exposureState?.exposureCompensationRange?.upper ?: 10
    val exposureStep = exposureState?.exposureCompensationStep?.toDouble() ?: 1.0

    // Reset zoom when camera switches
    LaunchedEffect(useFrontCamera) {
        zoomLevel = 1f
        applyZoom(currentCamera, 1f)
    }

    // Hide focus ring after 2 seconds
    LaunchedEffect(focusRingPosition) {
        if (focusRingPosition != null) {
            kotlinx.coroutines.delay(2000)
            focusRingPosition = null
        }
    }

    // Countdown timer
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            isCountingDown = true
            kotlinx.coroutines.delay(1000)
            countdown--
        } else if (isCountingDown) {
            isCountingDown = false
            // Take photo when countdown reaches 0
            takePhotoToGallery(context, imageCapture, cameraExecutor) { uri ->
                lastSavedMessage = if (uri != null) "Saved to gallery" else "Capture failed"
            }
        }
    }

    // Device Lab
    if (showDeviceLab) {
        com.chromecore.camera.ui.screens.devicelab.DeviceLabScreen(
            onBack = { showDeviceLab = false }
        )
        return
    }

    // Gallery
    if (showGallery) {
        com.chromecore.camera.ui.screens.gallery.GalleryScreen(
            onBack = { showGallery = false }
        )
        return
    }

    // Settings screen removed until items have real logic (AGENTS.md §2.4)

    Box(modifier = Modifier.fillMaxSize().background(ChromecoreColors.ObsidianBlack)) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewViewRef = it }
                },
                update = { previewView ->
                    previewViewRef = previewView
                    bindCamera(previewView, lifecycleOwner, useFrontCamera, imageCapture) { cam ->
                        currentCamera = cam
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .transformable(
                        state = rememberTransformableState { zoomChange, _, _ ->
                            val newZoom = (zoomLevel * zoomChange).coerceIn(1f, maxZoom)
                            zoomLevel = newZoom
                            applyZoom(currentCamera, newZoom)
                        }
                    )
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            focusRingPosition = offset
                            val pv = previewViewRef
                            if (pv != null && pv.width > 0 && pv.height > 0) {
                                val factory = SurfaceOrientedMeteringPointFactory(
                                    pv.width.toFloat(),
                                    pv.height.toFloat()
                                )
                                val point = factory.createPoint(offset.x, offset.y)
                                val action = FocusMeteringAction.Builder(
                                    point,
                                    FocusMeteringAction.FLAG_AF
                                ).setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS).build()
                                currentCamera?.cameraControl?.startFocusAndMetering(action)
                            }
                        }
                    }
            )

            // Focus ring indicator
            focusRingPosition?.let { pos ->
                Box(
                    modifier = Modifier
                        .offset { IntOffset(pos.x.toInt() - 40, pos.y.toInt() - 40) }
                        .size(80.dp)
                        .border(2.dp, ChromecoreColors.CyanAccent, CircleShape)
                )
            }

            // Countdown display
            if (countdown > 0) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$countdown",
                        color = Color.White,
                        fontSize = 120.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Light
                    )
                }
            }

            // Rule of thirds grid overlay
            if (showGrid) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val lineColor = Color.White.copy(alpha = 0.3f)
                    val strokeWidth = 1.dp.toPx()

                    // Vertical lines at 1/3 and 2/3
                    drawLine(lineColor, androidx.compose.ui.geometry.Offset(w / 3f, 0f), androidx.compose.ui.geometry.Offset(w / 3f, h), strokeWidth)
                    drawLine(lineColor, androidx.compose.ui.geometry.Offset(2f * w / 3f, 0f), androidx.compose.ui.geometry.Offset(2f * w / 3f, h), strokeWidth)

                    // Horizontal lines at 1/3 and 2/3
                    drawLine(lineColor, androidx.compose.ui.geometry.Offset(0f, h / 3f), androidx.compose.ui.geometry.Offset(w, h / 3f), strokeWidth)
                    drawLine(lineColor, androidx.compose.ui.geometry.Offset(0f, 2f * h / 3f), androidx.compose.ui.geometry.Offset(w, 2f * h / 3f), strokeWidth)
                }
            }

            // Top controls row
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Flash toggle
                IconButton(
                    onClick = {
                        flashMode = flashMode.next()
                        applyFlash(imageCapture, flashMode)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = when (flashMode) {
                            FlashMode.OFF -> Icons.Default.FlashOff
                            FlashMode.ON -> Icons.Default.FlashOn
                            FlashMode.AUTO -> Icons.Default.FlashAuto
                        },
                        contentDescription = "Flash mode: $flashMode",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Grid toggle
                IconButton(
                    onClick = { showGrid = !showGrid },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.GridOn,
                        "Toggle grid",
                        tint = if (showGrid) ChromecoreColors.CyanAccent else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Timer toggle
                IconButton(
                    onClick = {
                        timerSeconds = when (timerSeconds) {
                            0 -> 3
                            3 -> 5
                            5 -> 10
                            else -> 0
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Text(
                        if (timerSeconds > 0) "${timerSeconds}s" else "Off",
                        color = if (timerSeconds > 0) ChromecoreColors.CyanAccent else Color.White,
                        fontSize = 12.sp
                    )
                }

                // Device Lab button
                IconButton(
                    onClick = { showDeviceLab = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        "Device Lab",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Zoom level indicator
                Text(
                    "${"%.1f".format(zoomLevel)}x",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Switch camera button
                IconButton(
                    onClick = { useFrontCamera = !useFrontCamera },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Cameraswitch,
                        "Switch Camera",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Zoom slider
            if (maxZoom > 1f) {
                Slider(
                    value = zoomLevel,
                    onValueChange = { newLevel ->
                        zoomLevel = newLevel
                        applyZoom(currentCamera, newLevel)
                    },
                    valueRange = 1f..maxZoom,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.7f)
                        .padding(bottom = 120.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = ChromecoreColors.CyanAccent,
                        activeTrackColor = ChromecoreColors.CyanAccent,
                        inactiveTrackColor = ChromecoreColors.GraphiteLight
                    )
                )
            }

            // Exposure compensation slider (vertical, right side)
            if (maxExposure > minExposure) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .height(200.dp)
                ) {
                    Text(
                        "${(exposureIndex * exposureStep).toString().take(4)} EV",
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Slider(
                        value = exposureIndex.toFloat(),
                        onValueChange = { newIndex ->
                            exposureIndex = newIndex.toInt()
                            currentCamera?.cameraControl?.setExposureCompensationIndex(exposureIndex)
                        },
                        valueRange = minExposure.toFloat()..maxExposure.toFloat(),
                        modifier = Modifier
                            .weight(1f)
                            .width(40.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = ChromecoreColors.CyanAccent,
                            activeTrackColor = ChromecoreColors.CyanAccent,
                            inactiveTrackColor = ChromecoreColors.GraphiteLight
                        )
                    )
                }
            }

            // Bottom controls
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery button (left)
                IconButton(
                    onClick = { showGallery = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Info, // Using Info as placeholder, Gallery icon not in default set
                        "Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Shutter button (center)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    lastSavedMessage?.let { msg ->
                        Text(
                            msg,
                            color = ChromecoreColors.SuccessGreen,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    ShutterButton(
                        onClick = {
                            if (timerSeconds > 0) {
                                countdown = timerSeconds
                            } else {
                                takePhotoToGallery(context, imageCapture, cameraExecutor) { uri ->
                                    lastSavedMessage = if (uri != null) "Saved to gallery" else "Capture failed"
                                }
                            }
                        }
                    )
                }

                // Spacer (right) - for symmetry
                Spacer(modifier = Modifier.size(48.dp))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "CHROMECORE X",
                    color = ChromecoreColors.CyanAccent,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Camera permission is required to take photos",
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = ChromecoreColors.CyanAccent)
                ) {
                    Text("Grant Permission", color = Color.Black)
                }
            }
        }
    }
}

private fun getMaxZoom(camera: Camera?): Float =
    camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1f

private fun applyZoom(camera: Camera?, level: Float) {
    camera?.cameraControl?.setZoomRatio(level.coerceIn(1f, getMaxZoom(camera)))
}

private fun FlashMode.next(): FlashMode = when (this) {
    FlashMode.OFF -> FlashMode.AUTO
    FlashMode.AUTO -> FlashMode.ON
    FlashMode.ON -> FlashMode.OFF
}

private fun applyFlash(imageCapture: ImageCapture, mode: FlashMode) {
    imageCapture.flashMode = mode.toImageCaptureMode()
}

private fun bindCamera(
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    useFrontCamera: Boolean,
    imageCapture: ImageCapture,
    onCameraReady: (Camera) -> Unit
) {
    val context = previewView.context
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            cameraProvider.unbindAll()
            val cam = cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageCapture
            )
            onCameraReady(cam)
            Log.i("CameraScreen", "Camera bound: front=$useFrontCamera")
        } catch (e: Exception) {
            Log.e("CameraScreen", "Camera bind failed", e)
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun takePhotoToGallery(
    context: Context,
    imageCapture: ImageCapture,
    executor: ExecutorService,
    onSaved: (Uri?) -> Unit
) {
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "CCX_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ChromecoreX")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val imageUri = resolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
    )

    if (imageUri == null) {
        Log.e("CameraScreen", "MediaStore insert returned null URI")
        onSaved(null)
        return
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        resolver, imageUri, contentValues
    ).build()

    imageCapture.takePicture(outputOptions, executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val updateValues = ContentValues().apply {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                    }
                    resolver.update(imageUri, updateValues, null, null)
                }
                Log.i("CameraScreen", "Photo saved to gallery: $imageUri")
                onSaved(imageUri)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraScreen", "Photo capture failed: ${exception.imageCaptureError}", exception)
                try {
                    resolver.delete(imageUri, null, null)
                } catch (e: Exception) {
                    Log.w("CameraScreen", "Failed to clean up orphaned URI", e)
                }
                onSaved(null)
            }
        })
}
