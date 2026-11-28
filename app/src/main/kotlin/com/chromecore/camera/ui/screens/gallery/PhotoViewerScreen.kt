package com.chromecore.camera.ui.screens.gallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chromecore.camera.ui.theme.ChromecoreColors

@Composable
fun PhotoViewerScreen(
    photo: PhotoItem,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    // Zoom state
    var scale by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val transformableState = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        rotation += rotationChange
        offset += offsetChange
    }

    // Photo details
    val photoDetails = remember(photo.uri) { getPhotoDetails(context, photo) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    // Reset zoom on double tap
                    scale = 1f
                    rotation = 0f
                    offset = androidx.compose.ui.geometry.Offset.Zero
                })
            }
    ) {
        // Image with zoom/pan
        AsyncImage(
            model = photo.uri,
            contentDescription = photo.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    rotationZ = rotation,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(state = transformableState)
        )

        // Top bar
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White
                )
            }
            Text(
                photo.displayName,
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
            )
            IconButton(onClick = { sharePhoto(context, photo.uri) }) {
                Icon(Icons.Default.Share, "Поделиться", tint = Color.White)
            }
            IconButton(onClick = {
                deletePhoto(context, photo.uri)
                onDelete()
            }) {
                Icon(Icons.Default.Delete, "Удалить", tint = Color.White)
            }
        }

        // Bottom info panel
        if (scale == 1f) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    photo.displayName,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                photoDetails.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Text(
                            "$label: ",
                            color = ChromecoreColors.ChromeSilver,
                            fontSize = 12.sp,
                            modifier = Modifier.width(100.dp)
                        )
                        Text(
                            value,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

private fun getPhotoDetails(context: Context, photo: PhotoItem): List<Pair<String, String>> {
    val details = mutableListOf<Pair<String, String>>()

    try {
        context.contentResolver.query(
            photo.uri,
            null,
            null,
            null,
            null
        )?.use { cursor ->
            val widthIdx = cursor.getColumnIndex("width")
            val heightIdx = cursor.getColumnIndex("height")
            val sizeIdx = cursor.getColumnIndex("_size")
            val dateIdx = cursor.getColumnIndex("date_added")

            if (cursor.moveToFirst()) {
                if (widthIdx >= 0 && heightIdx >= 0) {
                    details.add("Resolution" to "${cursor.getInt(widthIdx)}×${cursor.getInt(heightIdx)}")
                }
                if (sizeIdx >= 0) {
                    val sizeBytes = cursor.getLong(sizeIdx)
                    val sizeMB = sizeBytes / 1024.0 / 1024.0
                    details.add("Size" to "%.1f MB".format(sizeMB))
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("PhotoViewer", "Failed to get photo details", e)
    }

    return details
}

private fun sharePhoto(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Поделиться фото"))
}

private fun deletePhoto(context: Context, uri: Uri) {
    try {
        context.contentResolver.delete(uri, null, null)
    } catch (e: Exception) {
        android.util.Log.e("PhotoViewer", "Failed to delete photo", e)
    }
}
