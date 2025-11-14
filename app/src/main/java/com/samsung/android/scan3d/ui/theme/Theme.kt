package com.samsung.android.scan3d.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.samsung.android.scan3d.util.SettingsManager
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

private var AppThemeMode by mutableIntStateOf(SettingsManager.THEME_AUTO)
private var AppMonetEnabled by mutableStateOf(true)

fun updateAppTheme(context: Context, mode: Int) {
    AppThemeMode = mode
    SettingsManager.saveThemeMode(context, mode)
}

fun updateAppMonet(context: Context, enabled: Boolean) {
    AppMonetEnabled = enabled
    SettingsManager.saveMonetEnabled(context, enabled)
}

private val ExpressiveDarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFD0BCFF),
    secondary = androidx.compose.ui.graphics.Color(0xFFCCC2DC),
    tertiary = androidx.compose.ui.graphics.Color(0xFFEFB8C8),
    background = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    surface = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF381E72),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF332D41),
    onTertiary = androidx.compose.ui.graphics.Color(0xFF492532),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE6E1E5)
)

private val ExpressiveLightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF6750A4),
    secondary = androidx.compose.ui.graphics.Color(0xFF625B71),
    tertiary = androidx.compose.ui.graphics.Color(0xFF7D5260),
    background = androidx.compose.ui.graphics.Color(0xFFFFFBFE),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFBFE),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onBackground = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1C1B1F)
)

@Composable
fun RemoteCamM3Theme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        AppThemeMode = SettingsManager.loadThemeMode(context)
        AppMonetEnabled = SettingsManager.loadMonetEnabled(context)
    }
    val darkTheme = when (AppThemeMode) {
        SettingsManager.THEME_LIGHT -> false
        SettingsManager.THEME_DARK -> true
        else -> isSystemInDarkTheme() // THEME_AUTO
    }

    val dynamicColor = AppMonetEnabled

    val colorScheme = when {
        dynamicColor -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ExpressiveDarkColors
        else -> ExpressiveLightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
