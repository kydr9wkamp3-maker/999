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
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.chromecore.camera.ui.theme.ChromecoreColors
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }
    var lastSavedMessage by remember { mutableStateOf<String?>(null) }

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
    var useFrontCamera by remember { mutableStateOf(false) }

    // ImageCapture instance — recreated when camera switches
    val imageCapture = remember(useFrontCamera) {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }

    // PreviewView reference holder
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Bind camera when permission granted or camera selection changes
    LaunchedEffect(hasCameraPermission, useFrontCamera) {
        if (!hasCameraPermission) return@LaunchedEffect
        val pv = previewView ?: return@LaunchedEffect

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(pv.surfaceProvider)
                }
                val cameraSelector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageCapture
                )
                Log.i("CameraScreen", "Camera bound: front=$useFrontCamera")
            } catch (e: Exception) {
                Log.e("CameraScreen", "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize().background(ChromecoreColors.ObsidianBlack)) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { pv ->
                        previewView = pv
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Switch camera button
            IconButton(
                onClick = { useFrontCamera = !useFrontCamera },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .semantics { contentDescription = "Switch camera" }
            ) {
                Icon(
                    Icons.Default.Cameraswitch,
                    "Switch Camera",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Column for bottom controls
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Save confirmation
                lastSavedMessage?.let { msg ->
                    Text(
                        msg,
                        color = ChromecoreColors.SuccessGreen,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Shutter button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(ChromecoreColors.CyanAccent)
                        .semantics { contentDescription = "Take photo" },
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            takePhotoToGallery(context, imageCapture, cameraExecutor) { uri ->
                                lastSavedMessage = if (uri != null) "Saved to gallery" else "Capture failed"
                            }
                        },
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            Icons.Default.Cameraswitch,
                            "Capture",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
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

/**
 * Takes a photo and saves it to the device gallery via MediaStore.
 * On Android 10+ (API 29+) no WRITE_EXTERNAL_STORAGE needed.
 * Photo appears in Pictures/ChromecoreX/ directory.
 */
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
                // Clean up the orphaned MediaStore entry on failure
                try {
                    resolver.delete(imageUri, null, null)
                } catch (e: Exception) {
                    Log.w("CameraScreen", "Failed to clean up orphaned URI", e)
                }
                onSaved(null)
            }
        })
}
