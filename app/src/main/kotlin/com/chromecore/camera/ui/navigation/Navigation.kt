package com.chromecore.camera.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Camera : Screen("camera")
    data object Gallery : Screen("gallery")
    data object Editor : Screen("editor/{photoUri}") {
        fun createRoute(uri: String) = "editor/$uri"
    }
    data object Settings : Screen("settings")
    data object DeviceLab : Screen("device_lab")
}

enum class CameraMode(val label: String, val icon: ImageVector) {
    PHOTO("Photo", Icons.Filled.CameraAlt),
    VIDEO("Video", Icons.Filled.Videocam),
    PORTRAIT("Portrait", Icons.Filled.Person),
    NIGHT("Night", Icons.Filled.Nightlight),
    PRO("Pro", Icons.Filled.Tune),
    EFFECTS("Effects", Icons.Filled.AutoAwesome)
}

enum class BottomTab(val label: String, val icon: ImageVector) {
    CAMERA("Camera", Icons.Filled.CameraAlt),
    EFFECTS("Effects", Icons.Filled.AutoAwesome),
    GALLERY("Gallery", Icons.Filled.PhotoLibrary),
    SETTINGS("Settings", Icons.Filled.Settings)
}
