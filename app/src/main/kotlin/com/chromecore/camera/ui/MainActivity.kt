package com.chromecore.camera.ui

import android.content.Context
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
import com.chromecore.camera.ui.screens.onboarding.OnboardingScreen
import com.chromecore.camera.ui.theme.ChromecoreColors
import com.chromecore.camera.ui.theme.ChromecoreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("chromecore", Context.MODE_PRIVATE)
        val hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false)

        setContent {
            ChromecoreTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = ChromecoreColors.ObsidianBlack) {
                    var showOnboarding by remember { mutableStateOf(!hasSeenOnboarding) }

                    if (showOnboarding) {
                        OnboardingScreen(
                            onComplete = {
                                prefs.edit().putBoolean("has_seen_onboarding", true).apply()
                                showOnboarding = false
                            }
                        )
                    } else {
                        CameraScreen()
                    }
                }
            }
        }
    }
}
