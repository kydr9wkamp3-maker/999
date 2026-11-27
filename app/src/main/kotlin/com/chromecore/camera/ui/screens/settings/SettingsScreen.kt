package com.chromecore.camera.ui.screens.settings

import androidx.compose.runtime.Composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromecore.camera.ui.theme.ChromecoreColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = ChromecoreColors.ObsidianBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Light,
                        color = ChromecoreColors.ColdWhite,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = ChromecoreColors.ColdWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection("Camera") {
                SettingsToggle("Save Original", "Keep unprocessed copy alongside edited photo", true)
                SettingsToggle("Grid Lines", "Show composition grid on camera preview", false)
                SettingsToggle("Location Tagging", "Include GPS data in photos", false)
                SettingsToggleItem("Photo Quality", "Maximum")
                SettingsToggleItem("Video Resolution", "1080p 30fps")
            }

            SettingsSection("Performance") {
                SettingsToggleItem("Render Quality", "Balanced")
                SettingsToggle("Adaptive Quality", "Automatically adjust effects based on device load", true)
                SettingsToggle("Hardware Acceleration", "Use GPU for image processing", true)
            }

            SettingsSection("Appearance") {
                SettingsToggleItem("Theme", "Obsidian Chrome")
                SettingsToggle("Haptic Feedback", "Vibrate on capture and mode change", true)
                SettingsToggle("Sound Effects", "Play shutter and focus sounds", true)
            }

            SettingsSection("Storage") {
                SettingsToggleItem("Storage Location", "Device")
                SettingsItem("Clear Cache", "Free up temporary storage space")
                SettingsItem("Manage Storage", "View and manage saved photos")
            }

            SettingsSection("Advanced") {
                SettingsItem("Device Laboratory", "View camera sensor capabilities")
                SettingsItem("Performance Monitor", "Show FPS and resource usage overlay")
                SettingsItem("Export Diagnostic Report", "Generate JSON report of device capabilities")
            }

            SettingsSection("About") {
                SettingsItem("Version", "1.0.0-alpha01")
                SettingsItem("Open Source Licenses", "Third-party library licenses")
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ChromecoreColors.CyanAccent,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
        content()
    }
}

@Composable
private fun SettingsToggle(title: String, subtitle: String, checked: Boolean) {
    var isChecked by remember { mutableStateOf(checked) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isChecked = !isChecked }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = ChromecoreColors.ColdWhite, fontSize = 14.sp)
            Text(subtitle, color = ChromecoreColors.TitaniumGrey, fontSize = 12.sp)
        }
        Switch(
            checked = isChecked,
            onCheckedChange = { isChecked = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = ChromecoreColors.CyanAccent,
                checkedTrackColor = CyanDeep,
                uncheckedThumbColor = ChromecoreColors.TitaniumGrey,
                uncheckedTrackColor = ChromecoreColors.GraphiteMid
            )
        )
    }
}

@Composable
private fun SettingsToggleItem(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = ChromecoreColors.ColdWhite, fontSize = 14.sp)
        Text(value, color = ChromecoreColors.CyanAccent, fontSize = 14.sp)
    }
}

@Composable
private fun SettingsItem(title: String, subtitle: String = "") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, color = ChromecoreColors.ColdWhite, fontSize = 14.sp)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = ChromecoreColors.TitaniumGrey, fontSize = 12.sp)
            }
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = ChromecoreColors.GraphiteLight
        )
    }
}

private val CyanDeep = Color(0xFF00838F)
