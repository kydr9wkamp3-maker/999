package com.chromecore.camera.ui.screens.gallery

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chromecore.camera.ui.theme.ChromecoreColors

/**
 * CHROMECORE X — Gallery Screen
 * Shows captured photos in a chrome-styled grid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onPhotoClick: (String) -> Unit,
    onBack: () -> Unit
) {
    var photos by remember { mutableStateOf(listOf<String>()) }

    // In production, load from MediaStore
    // For now, show empty state

    Scaffold(
        containerColor = ChromecoreColors.ObsidianBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Gallery",
                        fontWeight = FontWeight.Light,
                        color = ChromecoreColors.ColdWhite,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ChromecoreColors.ColdWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (photos.isEmpty()) {
                EmptyGallery()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(photos) { uri ->
                        PhotoThumbnail(
                            uri = uri,
                            onClick = { onPhotoClick(uri) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGallery() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.PhotoLibrary,
                contentDescription = null,
                tint = ChromecoreColors.TitaniumGrey,
                modifier = Modifier.size(72.dp)
            )
            Text(
                "No photos yet",
                style = MaterialTheme.typography.headlineMedium,
                color = ChromecoreColors.ChromeSilver
            )
            Text(
                "Capture your first shot to see it here",
                style = MaterialTheme.typography.bodyMedium,
                color = ChromecoreColors.TitaniumGrey
            )
        }
    }
}

@Composable
private fun PhotoThumbnail(uri: String, onClick: () -> Unit) {
    AsyncImage(
        model = uri,
        contentDescription = "Photo",
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentScale = ContentScale.Crop
    )
}
