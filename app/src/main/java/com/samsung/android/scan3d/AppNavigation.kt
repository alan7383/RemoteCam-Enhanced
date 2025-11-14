package com.samsung.android.scan3d

import androidx.compose.animation.AnimatedContentTransitionScope
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

        composable(
            route = Screen.Permissions.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
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
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
        ) {
            CameraScreen(
                cameraActivity = cameraActivity,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                isInputLocked = isInputLocked
            )
        }

        composable(
            route = Screen.Settings.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
        ) {
            SettingsScreen(
                onBackClicked = { navController.popBackStack() },
                onNavigateToAdditionalSettings = { navController.navigate(Screen.AdditionalSettings.route) },
                onNavigateToPowerSettings = { navController.navigate(Screen.PowerSettings.route) }
            )
        }

        composable(
            route = Screen.AdditionalSettings.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
        ) {
            AdditionalSettingsScreen(
                onBackClicked = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PowerSettings.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
        ) {
            PowerSettingsScreen(
                onBackClicked = { navController.popBackStack() }
            )
        }
    }
}