package com.chromecore.camera.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.chromecore.camera.ui.screens.camera.CameraScreen
import com.chromecore.camera.ui.screens.editor.EditorScreen
import com.chromecore.camera.ui.screens.gallery.GalleryScreen
import com.chromecore.camera.ui.screens.settings.SettingsScreen
import com.chromecore.camera.ui.screens.devicelab.DeviceLabScreen

@Composable
fun ChromecoreNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Camera.route
    ) {
        composable(Screen.Camera.route) {
            CameraScreen(
                onNavigateToGallery = { navController.navigate(Screen.Gallery.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToEditor = { uri ->
                    navController.navigate(Screen.Editor.createRoute(uri))
                }
            )
        }

        composable(Screen.Gallery.route) {
            GalleryScreen(
                onPhotoClick = { uri ->
                    navController.navigate(Screen.Editor.createRoute(uri))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(navArgument("photoUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val photoUri = backStackEntry.arguments?.getString("photoUri") ?: ""
            EditorScreen(
                photoUri = photoUri,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DeviceLab.route) {
            DeviceLabScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
