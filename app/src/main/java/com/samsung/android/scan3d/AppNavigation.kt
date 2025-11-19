package com.samsung.android.scan3d

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.samsung.android.scan3d.screens.AdditionalSettingsScreen
import com.samsung.android.scan3d.screens.CameraScreen
import com.samsung.android.scan3d.screens.PermissionScreen
import com.samsung.android.scan3d.screens.SettingsScreen
import com.samsung.android.scan3d.screens.PowerSettingsScreen

sealed class Screen(val route: String) {
    object Permissions : Screen("permissions_screen")
    object Camera : Screen("camera_screen")
    object Settings : Screen("settings_screen")
    object AdditionalSettings : Screen("additional_settings_screen")
    object PowerSettings : Screen("power_settings_screen")
}

@Composable
fun AppNavigation(
    cameraActivity: CameraActivity,
    startDestination: String,
    isInputLocked: Boolean
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        // Application des animations style "SukiSU Ultra" pour chaque écran

        composable(
            route = Screen.Permissions.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn() },
            popExitTransition = { scaleOut(targetScale = 0.9f) + fadeOut() }
        ) {
            PermissionScreen(
                onNavigateToCamera = {
                    navController.navigate(Screen.Camera.route) {
                        popUpTo(Screen.Permissions.route) { inclusive = true }
                    }
                },
                onServiceStart = { cameraActivity.startCamService() }
            )
        }

        composable(
            route = Screen.Camera.route,
            // La caméra est souvent l'écran principal, on garde les mêmes anims pour la cohérence
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn() },
            popExitTransition = { scaleOut(targetScale = 0.9f) + fadeOut() }
        ) {
            CameraScreen(
                cameraActivity = cameraActivity,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                isInputLocked = isInputLocked
            )
        }

        composable(
            route = Screen.Settings.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn() },
            popExitTransition = { scaleOut(targetScale = 0.9f) + fadeOut() }
        ) {
            SettingsScreen(
                onBackClicked = { navController.popBackStack() },
                onNavigateToAdditionalSettings = { navController.navigate(Screen.AdditionalSettings.route) },
                onNavigateToPowerSettings = { navController.navigate(Screen.PowerSettings.route) }
            )
        }

        composable(
            route = Screen.AdditionalSettings.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn() },
            popExitTransition = { scaleOut(targetScale = 0.9f) + fadeOut() }
        ) {
            AdditionalSettingsScreen(
                onBackClicked = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PowerSettings.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn() },
            popExitTransition = { scaleOut(targetScale = 0.9f) + fadeOut() }
        ) {
            PowerSettingsScreen(
                onBackClicked = { navController.popBackStack() }
            )
        }
    }
}
