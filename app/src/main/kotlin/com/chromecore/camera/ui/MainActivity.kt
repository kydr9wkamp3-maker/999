package com.chromecore.camera.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.chromecore.camera.ui.screens.camera.CameraScreen
import com.chromecore.camera.ui.screens.settings.SettingsScreen
import com.chromecore.camera.ui.theme.ChromecoreColors
import com.chromecore.camera.ui.theme.ChromecoreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChromecoreTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = ChromecoreColors.ObsidianBlack) {
                    var showSettings by remember { mutableStateOf(false) }
                    if (showSettings) {
                        SettingsScreen(onBack = { showSettings = false })
                    } else {
                        CameraScreen()
                    }
                }
            }
        }
    }
}
