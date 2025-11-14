package com.samsung.android.scan3d.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.samsung.android.scan3d.R
import com.samsung.android.scan3d.fragments.*
import com.samsung.android.scan3d.ui.theme.updateAppMonet
import com.samsung.android.scan3d.ui.theme.updateAppTheme
import com.samsung.android.scan3d.util.SettingsManager
import com.samsung.android.scan3d.CameraActivity
import androidx.compose.runtime.mutableIntStateOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClicked: () -> Unit,
    onNavigateToAdditionalSettings: () -> Unit,
    onNavigateToPowerSettings: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? CameraActivity

    var showAboutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    var currentTheme: Int by remember { mutableIntStateOf(SettingsManager.loadThemeMode(context)) }
    var dynamicColor: Boolean by remember { mutableStateOf(SettingsManager.loadMonetEnabled(context)) }
    var keepScreenOn: Boolean by remember { mutableStateOf(SettingsManager.loadKeepScreenOn(context)) }
    var currentLanguage: String by remember { mutableStateOf(SettingsManager.loadLanguage(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsGroup(title = stringResource(R.string.settings_appearance)) {
                ThemeOptionRow(
                    text = stringResource(R.string.settings_theme_auto),
                    icon = Icons.Rounded.SettingsSystemDaydream,
                    selected = currentTheme == SettingsManager.THEME_AUTO,
                    onClick = {
                        if (currentTheme != SettingsManager.THEME_AUTO) {
                            currentTheme = SettingsManager.THEME_AUTO
                            updateAppTheme(context, SettingsManager.THEME_AUTO)
                        }
                    }
                )
                ThemeOptionRow(
                    text = stringResource(R.string.settings_theme_light),
                    icon = Icons.Default.WbSunny,
                    selected = currentTheme == SettingsManager.THEME_LIGHT,
                    onClick = {
                        if (currentTheme != SettingsManager.THEME_LIGHT) {
                            currentTheme = SettingsManager.THEME_LIGHT
                            updateAppTheme(context, SettingsManager.THEME_LIGHT)
                        }
                    }
                )
                ThemeOptionRow(
                    text = stringResource(R.string.settings_theme_dark),
                    icon = Icons.Rounded.DarkMode,
                    selected = currentTheme == SettingsManager.THEME_DARK,
                    onClick = {
                        if (currentTheme != SettingsManager.THEME_DARK) {
                            currentTheme = SettingsManager.THEME_DARK
                            updateAppTheme(context, SettingsManager.THEME_DARK)
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                SettingsToggleRow(
                    text = stringResource(R.string.settings_monet),
                    icon = Icons.Rounded.Palette,
                    checked = dynamicColor,
                    onCheckedChange = {
                        dynamicColor = it
                        updateAppMonet(context, it)
                    }
                )
            }

            SettingsGroup(title = stringResource(R.string.settings_behavior_title)) {
                SettingsToggleRow(
                    text = stringResource(R.string.settings_keep_screen_on),
                    icon = Icons.Rounded.Visibility,
                    checked = keepScreenOn,
                    onCheckedChange = {
                        keepScreenOn = it
                        SettingsManager.saveKeepScreenOn(context, it)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                val langSummary = when (currentLanguage) {
                    SettingsManager.LANG_EN -> stringResource(R.string.settings_lang_en)
                    SettingsManager.LANG_FR -> stringResource(R.string.settings_lang_fr)
                    else -> stringResource(R.string.settings_lang_auto)
                }
                SettingsClickableRow(
                    text = stringResource(R.string.settings_language),
                    icon = Icons.Rounded.Language,
                    summary = langSummary,
                    onClick = { showLanguageDialog = true }
                )
            }

            SettingsGroup(title = stringResource(R.string.settings_additional_title)) {
                SettingsClickableRow(
                    text = stringResource(R.string.settings_camera_title),
                    icon = Icons.Rounded.CameraAlt,
                    onClick = onNavigateToAdditionalSettings
                )

                SettingsClickableRow(
                    text = stringResource(R.string.settings_power_title),
                    icon = Icons.Rounded.Power,
                    onClick = onNavigateToPowerSettings
                )
            }

            SettingsGroup(title = stringResource(R.string.settings_app_title)) {
                SettingsClickableRow(
                    text = stringResource(R.string.settings_about_title),
                    icon = Icons.Default.Info,
                    onClick = { showAboutDialog = true }
                )
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showLanguageDialog) {
        LanguageDialog(
            currentLanguage = currentLanguage,
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { newLang ->
                showLanguageDialog = false
                if (currentLanguage != newLang) {
                    SettingsManager.saveLanguage(context, newLang)
                    val appLocale = LocaleListCompat.forLanguageTags(if (newLang == SettingsManager.LANG_AUTO) "" else newLang)
                    activity?.isRestarting = true
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
            }
        )
    }
}
