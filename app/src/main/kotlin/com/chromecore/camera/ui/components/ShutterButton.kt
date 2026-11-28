package com.chromecore.camera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chromecore.camera.ui.theme.ChromecoreColors

/**
 * Хромированная кнопка затвора с градиентной рамкой.
 */
@Composable
fun ShutterButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val chromeBrush = Brush.linearGradient(
        0.0f to Color(0xFFF4F6FA),
        0.35f to Color(0xFF8E96A3),
        0.55f to Color(0xFFE6E9EE),
        1.0f to Color(0xFF4A4F58)
    )

    Box(
        modifier = modifier
            .size(84.dp)
            .border(4.dp, chromeBrush, CircleShape)
            .padding(8.dp)
            .background(Color(0xFFE6E9EE), CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Сделать фото" },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(ChromecoreColors.CyanAccent)
        )
    }
}
