package com.samsung.android.scan3d.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
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
    var showThemeDialog by remember { mutableStateOf(false) }

    var currentTheme: Int by remember { mutableIntStateOf(SettingsManager.loadThemeMode(context)) }
    var dynamicColor: Boolean by remember { mutableStateOf(SettingsManager.loadMonetEnabled(context)) }
    var keepScreenOn: Boolean by remember { mutableStateOf(SettingsManager.loadKeepScreenOn(context)) }
    var currentLanguage: String by remember { mutableStateOf(SettingsManager.loadLanguage(context)) }

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

    if (showThemeDialog) {
        val themeOptions = mapOf(
            SettingsManager.THEME_AUTO to stringResource(R.string.settings_theme_auto),
            SettingsManager.THEME_LIGHT to stringResource(R.string.settings_theme_light),
            SettingsManager.THEME_DARK to stringResource(R.string.settings_theme_dark)
        )
        SettingsRadioDialog(
            title = stringResource(R.string.settings_theme),
            options = themeOptions,
            selected = currentTheme,
            onDismiss = { showThemeDialog = false },
            onSelected = {
                currentTheme = it
                updateAppTheme(context, it)
                showThemeDialog = false
            }
        )
    }

    SettingsScaffold(
        title = stringResource(R.string.settings_title),
        onBackClick = onBackClicked
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 32.dp)
        ) {
            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_appearance),
                    items = listOf(
                        { shape ->
                            SettingsItem(
                                shape = shape,
                                title = stringResource(R.string.settings_theme),
                                subtitle = when (currentTheme) {
                                    SettingsManager.THEME_AUTO -> stringResource(R.string.settings_theme_auto)
                                    SettingsManager.THEME_LIGHT -> stringResource(R.string.settings_theme_light)
                                    else -> stringResource(R.string.settings_theme_dark)
                                },
                                icon = Icons.Rounded.DarkMode,
                                onClick = { showThemeDialog = true }
                            )
                        },
                        { shape ->
                            SettingsItem(
                                shape = shape,
                                title = stringResource(R.string.settings_monet),
                                icon = Icons.Rounded.Palette,
                                hasSwitch = true,
                                switchState = dynamicColor,
                                onSwitchChange = {
                                    dynamicColor = it
                                    updateAppMonet(context, it)
                                }
                            )
                        }
                    )
                )
            }

            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_behavior_title),
                    items = listOf(
                        { shape ->
                            SettingsItem(
                                shape = shape,
                                title = stringResource(R.string.settings_keep_screen_on),
                                icon = Icons.Rounded.Visibility,
                                hasSwitch = true,
                                switchState = keepScreenOn,
                                onSwitchChange = {
                                    keepScreenOn = it
                                    SettingsManager.saveKeepScreenOn(context, it)
                                }
                            )
                        },
                        { shape ->
                            SettingsItem(
                                shape = shape,
                                title = stringResource(R.string.settings_language),
                                subtitle = when (currentLanguage) {
                                    SettingsManager.LANG_EN -> stringResource(R.string.settings_lang_en)
                                    SettingsManager.LANG_FR -> stringResource(R.string.settings_lang_fr)
                                    SettingsManager.LANG_HU -> stringResource(R.string.settings_lang_hu)
                                    SettingsManager.LANG_PT -> stringResource(R.string.settings_lang_pt_br)
                                    else -> stringResource(R.string.settings_lang_auto)
                                },
                                icon = Icons.Rounded.Language,
                                onClick = { showLanguageDialog = true }
                            )
                        }
                    )
                )
            }

            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_additional_title),
                    items = listOf(
                        { shape ->
                            SettingsItem(
                                shape = shape,
                                title = stringResource(R.string.settings_camera_title),
                                icon = Icons.Rounded.CameraAlt,
                                onClick = onNavigateToAdditionalSettings
                            )
                        },
                        { shape ->
                            SettingsItem(
                                shape = shape,
                                title = stringResource(R.string.settings_power_title),
                                icon = Icons.Rounded.Power,
                                onClick = onNavigateToPowerSettings
                            )
                        }
                    )
                )
            }

            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_app_title),
                    items = listOf(
                        { shape ->
                            SettingsItem(
                                shape = shape,
                                title = stringResource(R.string.settings_about_title),
                                icon = Icons.Default.Info,
                                onClick = { showAboutDialog = true }
                            )
                        }
                    )
                )
            }
        }
    }
}