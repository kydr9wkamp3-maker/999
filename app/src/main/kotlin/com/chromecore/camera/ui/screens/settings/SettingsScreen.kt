package com.chromecore.camera.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HdrOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromecore.camera.ui.theme.ChromecoreColors

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChromecoreColors.ObsidianBlack)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "Настройки",
                    color = Color.White,
                    fontSize = 20.sp
                )
            }

            Divider(color = ChromecoreColors.GraphiteMid, thickness = 1.dp)

            // Settings items
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingItem(
                    icon = Icons.Default.Camera,
                    title = "Качество фото",
                    subtitle = "Максимальное",
                    onClick = { /* TODO */ }
                )
                SettingItem(
                    icon = Icons.Default.FlashOn,
                    title = "Вспышка",
                    subtitle = "Авто",
                    onClick = { /* TODO */ }
                )
                SettingItem(
                    icon = Icons.Default.GridOn,
                    title = "Сетка",
                    subtitle = "Правило третей",
                    onClick = { /* TODO */ }
                )
                SettingItem(
                    icon = Icons.Default.HdrOn,
                    title = "HDR",
                    subtitle = "Включён",
                    onClick = { /* TODO */ }
                )
                SettingItem(
                    icon = Icons.Default.Info,
                    title = "О приложении",
                    subtitle = "CHROMECORE X v1.0",
                    onClick = { /* TODO */ }
                )
            }
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = ChromecoreColors.CyanAccent,
            modifier = Modifier.height(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp
            )
            Text(
                text = subtitle,
                color = ChromecoreColors.ChromeSilver,
                fontSize = 12.sp
            )
        }
    }
}
