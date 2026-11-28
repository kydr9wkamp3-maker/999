package com.chromecore.camera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromecore.camera.ui.theme.ChromecoreColors

enum class CameraMode {
    PHOTO, VIDEO, PORTRAIT, NIGHT, PRO
}

@Composable
fun ModeSelector(
    selectedMode: CameraMode,
    onModeSelected: (CameraMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CameraMode.entries.forEach { mode ->
            ModeChip(
                mode = mode,
                isSelected = mode == selectedMode,
                onClick = { onModeSelected(mode) }
            )
        }
    }
}

@Composable
private fun ModeChip(
    mode: CameraMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        ChromecoreColors.CyanAccent
    } else {
        Color.Transparent
    }

    val textColor = if (isSelected) {
        Color.Black
    } else {
        ChromecoreColors.ChromeSilver
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { contentDescription = "Режим: ${mode.name}" },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = mode.label(),
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun CameraMode.label(): String = when (this) {
    CameraMode.PHOTO -> "Фото"
    CameraMode.VIDEO -> "Видео"
    CameraMode.PORTRAIT -> "Портрет"
    CameraMode.NIGHT -> "Ночь"
    CameraMode.PRO -> "Pro"
}
