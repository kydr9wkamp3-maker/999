package com.chromecore.camera.ui.screens.editor

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chromecore.camera.filters.FilterEngine
import com.chromecore.camera.ui.theme.ChromecoreColors

/**
 * CHROMECORE X — Editor Screen
 * Non-destructive photo editing with filters and adjustments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    photoUri: String,
    onBack: () -> Unit
) {
    val filterEngine = remember { FilterEngine() }
    val presets = remember { filterEngine.getPresets() }
    var selectedFilterIndex by remember { mutableIntStateOf(-1) }
    var filterIntensity by remember { mutableFloatStateOf(1f) }
    var showAdjustments by remember { mutableStateOf(false) }

    // Adjustment values
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var temperature by remember { mutableFloatStateOf(0f) }
    var sharpness by remember { mutableFloatStateOf(0f) }
    var vignette by remember { mutableFloatStateOf(0f) }

    Scaffold(
        containerColor = ChromecoreColors.ObsidianBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Editor",
                        fontWeight = FontWeight.Light,
                        color = ChromecoreColors.ColdWhite,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = ChromecoreColors.ColdWhite
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Save */ }) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Save",
                            tint = ChromecoreColors.CyanAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            EditorBottomBar(
                showAdjustments = showAdjustments,
                onToggleAdjustments = { showAdjustments = !showAdjustments }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Photo preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Editing photo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            // Filter intensity slider
            if (selectedFilterIndex >= 0) {
                IntensitySlider(
                    value = filterIntensity,
                    onValueChange = { filterIntensity = it },
                    label = "Intensity"
                )
            }

            // Adjustments panel
            if (showAdjustments) {
                AdjustmentsPanel(
                    brightness = brightness,
                    onBrightnessChange = { brightness = it },
                    contrast = contrast,
                    onContrastChange = { contrast = it },
                    saturation = saturation,
                    onSaturationChange = { saturation = it },
                    temperature = temperature,
                    onTemperatureChange = { temperature = it },
                    sharpness = sharpness,
                    onSharpnessChange = { sharpness = it },
                    vignette = vignette,
                    onVignetteChange = { vignette = it }
                )
            } else {
                // Filter strip
                FilterCarousel(
                    presets = presets,
                    selectedIndex = selectedFilterIndex,
                    onFilterSelected = { index ->
                        selectedFilterIndex = index
                        filterIntensity = 1f
                    }
                )
            }
        }
    }
}

@Composable
private fun IntensitySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = ChromecoreColors.ChromeSilver, fontSize = 12.sp)
            Text("${(value * 100).toInt()}%", color = ChromecoreColors.CyanAccent, fontSize = 12.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = ChromecoreColors.CyanAccent,
                activeTrackColor = ChromecoreColors.CyanAccent,
                inactiveTrackColor = ChromecoreColors.GraphiteMid
            )
        )
    }
}

@Composable
private fun FilterCarousel(
    presets: List<FilterEngine.FilterPreset>,
    selectedIndex: Int,
    onFilterSelected: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        item {
            FilterThumbnail(
                name = "Original",
                emoji = "📷",
                isSelected = selectedIndex == -1,
                onClick = { onFilterSelected(-1) }
            )
        }
        items(presets.withIndex().toList()) { (index, preset) ->
            FilterThumbnail(
                name = preset.name,
                emoji = categoryEmoji(preset.category),
                isSelected = selectedIndex == index,
                onClick = { onFilterSelected(index) }
            )
        }
    }
}

@Composable
private fun FilterThumbnail(
    name: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ChromecoreColors.GraphiteMid)
                .then(
                    if (isSelected) Modifier.border(
                        2.dp,
                        ChromecoreColors.CyanAccent,
                        RoundedCornerShape(10.dp)
                    ) else Modifier
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            name,
            fontSize = 10.sp,
            color = if (isSelected) ChromecoreColors.CyanAccent else ChromecoreColors.ChromeSilver,
            maxLines = 1
        )
    }
}

@Composable
private fun AdjustmentsPanel(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    contrast: Float,
    onContrastChange: (Float) -> Unit,
    saturation: Float,
    onSaturationChange: (Float) -> Unit,
    temperature: Float,
    onTemperatureChange: (Float) -> Unit,
    sharpness: Float,
    onSharpnessChange: (Float) -> Unit,
    vignette: Float,
    onVignetteChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(ChromecoreColors.GraphiteDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AdjustmentSlider("Brightness", brightness, -1f, 1f, onBrightnessChange, "☀️")
        AdjustmentSlider("Contrast", contrast, 0f, 2f, onContrastChange, "◐")
        AdjustmentSlider("Saturation", saturation, 0f, 2f, onSaturationChange, "🎨")
        AdjustmentSlider("Temperature", temperature, -1f, 1f, onTemperatureChange, "🌡️")
        AdjustmentSlider("Sharpness", sharpness, 0f, 1f, onSharpnessChange, "💎")
        AdjustmentSlider("Vignette", vignette, 0f, 1f, onVignetteChange, "⭕")
    }
}

@Composable
private fun AdjustmentSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onValueChange: (Float) -> Unit,
    icon: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 16.sp, modifier = Modifier.width(28.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = ChromecoreColors.ChromeSilver,
            modifier = Modifier.width(80.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = ChromecoreColors.CyanAccent,
                activeTrackColor = ChromecoreColors.CyanAccent,
                inactiveTrackColor = ChromecoreColors.GraphiteMid
            )
        )
    }
}

@Composable
private fun EditorBottomBar(
    showAdjustments: Boolean,
    onToggleAdjustments: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChromecoreColors.ObsidianBlack)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        EditorToolButton(Icons.Filled.Tune, "Filters", !showAdjustments, onToggleAdjustments)
        EditorToolButton(Icons.Filled.Crop, "Crop", false) { }
        EditorToolButton(Icons.Filled.AutoAwesome, "Adjust", showAdjustments, onToggleAdjustments)
        EditorToolButton(Icons.Filled.Compare, "Compare", false) { }
        EditorToolButton(Icons.Filled.Undo, "Undo", false) { }
    }
}

@Composable
private fun EditorToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isActive) ChromecoreColors.CyanAccent else ChromecoreColors.ChromeSilver,
            modifier = Modifier.size(22.dp)
        )
        Text(
            label,
            fontSize = 10.sp,
            color = if (isActive) ChromecoreColors.CyanAccent else ChromecoreColors.TitaniumGrey,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

private fun categoryEmoji(category: FilterEngine.FilterCategory): String = when (category) {
    FilterEngine.FilterCategory.CHROME -> "🪞"
    FilterEngine.FilterCategory.METALHEART -> "⚙️"
    FilterEngine.FilterCategory.CINEMATIC -> "🎬"
    FilterEngine.FilterCategory.FILM -> "🎞️"
    FilterEngine.FilterCategory.RETRO -> "📼"
    FilterEngine.FilterCategory.GLITCH -> "⚡"
    FilterEngine.FilterCategory.PIXEL -> "👾"
    FilterEngine.FilterCategory.PORTRAIT -> "👤"
    FilterEngine.FilterCategory.COLOR_GRADE -> "🎨"
    FilterEngine.FilterCategory.MONOCHROME -> "◼️"
    FilterEngine.FilterCategory.CREATIVE -> "✨"
    FilterEngine.FilterCategory.NIGHT -> "🌙"
}
