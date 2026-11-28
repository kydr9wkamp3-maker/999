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
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.chromecore.camera.ui.theme.ChromecoreColors
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

enum class FlashMode { OFF, ON, AUTO }

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
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        com.chromecore.camera.ui.screens.settings.SettingsScreen(
            onBack = { showSettings = false }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(ChromecoreColors.ObsidianBlack)) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        bindCamera(previewView, lifecycleOwner, useFrontCamera, imageCapture) { cam ->
                            currentCamera = cam
                        }
                    }
                },
                update = { previewView ->
                    bindCamera(previewView, lifecycleOwner, useFrontCamera, imageCapture) { cam ->
                        currentCamera = cam
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .transformable(
                        state = rememberTransformableState { zoomChange, _, _ ->
                            val maxZoom = getMaxZoom(currentCamera)
                            val newZoom = (zoomLevel * zoomChange).coerceIn(1f, maxZoom)
                            zoomLevel = newZoom
                            applyZoom(currentCamera, newZoom)
                        }
                    )
            )

            // Top controls row
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Settings button
                IconButton(
                    onClick = { showSettings = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Flash toggle
                IconButton(
                    onClick = {
                        flashMode = flashMode.next()
                        applyFlash(currentCamera, flashMode)
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
            val maxZoom = getMaxZoom(currentCamera)
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

            // Bottom controls
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                        takePhotoToGallery(context, imageCapture, cameraExecutor) { uri ->
                            lastSavedMessage = if (uri != null) "Saved to gallery" else "Capture failed"
                        }
                    }
                )
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

private fun applyFlash(camera: Camera?, mode: FlashMode) {
    when (mode) {
        FlashMode.OFF -> camera?.cameraControl?.enableTorch(false)
        FlashMode.ON -> camera?.cameraControl?.enableTorch(true)
        FlashMode.AUTO -> camera?.cameraControl?.enableTorch(false)
    }
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
