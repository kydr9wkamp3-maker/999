package com.chromecore.camera.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.chromecore.camera.ui.navigation.ChromecoreNavHost
import com.chromecore.camera.ui.theme.ChromecoreColors
import com.chromecore.camera.ui.theme.ChromecoreTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ChromecoreTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ChromecoreColors.ObsidianBlack
                ) {
                    val navController = rememberNavController()
                    ChromecoreNavHost(navController = navController)
                }
            }
        }
    }
}
