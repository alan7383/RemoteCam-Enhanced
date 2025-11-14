package com.samsung.android.scan3d.fragments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhotoSizeSelectActual
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samsung.android.scan3d.R
import com.samsung.android.scan3d.util.SettingsManager
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material3.TextField
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.rounded.EditNote


@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content()
            }
        }
    }
}
@Composable
fun ThemeOptionRow(text: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = text, modifier = Modifier.padding(end = 16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        RadioButton(selected = selected, onClick = null)
    }
}
@Composable
fun SettingsToggleRow(
    text: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .clip(RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = text, modifier = Modifier.padding(end = 16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)

        AnimatedSystemSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsClickableToggleRow(
    text: String,
    summary: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = text, modifier = Modifier.padding(end = 16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(modifier = Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedSystemSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
@Composable
fun SettingsClickableRow(
    text: String,
    icon: ImageVector,
    summary: String? = null,
    subSummary: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(8.dp))
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = text, modifier = Modifier.padding(end = 16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(modifier = Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (subSummary != null) {
            Text(
                text = subSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
@Composable
fun CheckboxRow(
    text: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.padding(end = 16.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun AnimatedSystemSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val checkedThumbColor = MaterialTheme.colorScheme.onPrimary
    val checkedTrackColor = MaterialTheme.colorScheme.primary
    val checkedIconTint = MaterialTheme.colorScheme.primary

    val uncheckedThumbColor = MaterialTheme.colorScheme.outline
    val uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
    val uncheckedIconTint = MaterialTheme.colorScheme.onSurfaceVariant

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        thumbContent = {
            Icon(
                imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
                tint = if (checked) checkedIconTint else uncheckedIconTint
            )
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = checkedThumbColor,
            checkedTrackColor = checkedTrackColor,

            uncheckedThumbColor = uncheckedThumbColor,
            uncheckedTrackColor = uncheckedTrackColor
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val githubRepoUrl = "https://github.com/alan7383/RemoteCam-Enhanced"

    val context = LocalContext.current
    var clickCount by remember { mutableIntStateOf(0) }
    var showEasterEgg by remember { mutableStateOf(false) }

    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (_: Exception) {
        "?.?.?"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_close))
            }
        },
        title = { Text(stringResource(R.string.settings_about_title)) },
        icon = { Icon(Icons.Rounded.Sync, contentDescription = null) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.about_fork),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { uriHandler.openUri(githubRepoUrl) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.about_star))
                }

                Text(
                    text = stringResource(R.string.about_telegram),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.about_version, versionName ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            clickCount++
                            if (clickCount >= 7) {
                                showEasterEgg = true
                                clickCount = 0
                            }
                        }
                )

            }
        },
        shape = RoundedCornerShape(28.dp)
    )

    if (showEasterEgg) {
        EasterEggDialog(onDismiss = { showEasterEgg = false })
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EasterEggDialog(onDismiss: () -> Unit) {

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.egg_button_alan))
            }
        },
        icon = { Icon(Icons.Rounded.EditNote, contentDescription = null) },
        title = { Text(stringResource(R.string.egg_title_alan)) },
        text = {
            Text(
                text = stringResource(R.string.egg_message_alan),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        shape = RoundedCornerShape(28.dp)
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val languages = listOf(
        SettingsManager.LANG_AUTO to stringResource(R.string.settings_lang_auto),
        SettingsManager.LANG_EN to stringResource(R.string.settings_lang_en),
        SettingsManager.LANG_FR to stringResource(R.string.settings_lang_fr),
        SettingsManager.LANG_HU to stringResource(R.string.settings_lang_hu)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Language, contentDescription = null) },
        title = { Text(stringResource(R.string.settings_language)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                languages.forEach { (langCode, langName) ->
                    DialogRadioRow(
                        text = langName,
                        selected = (currentLanguage == langCode),
                        onClick = { onLanguageSelected(langCode) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_close))
            }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RememberSettingsDialog(
    rememberFlash: Boolean,
    rememberZoom: Boolean,
    rememberSensor: Boolean,
    rememberResolution: Boolean,
    rememberQuality: Boolean,
    onDismiss: () -> Unit,
    onSave: (Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    var tempRememberFlash by remember { mutableStateOf(rememberFlash) }
    var tempRememberZoom by remember { mutableStateOf(rememberZoom) }
    var tempRememberSensor by remember { mutableStateOf(rememberSensor) }
    var tempRememberResolution by remember { mutableStateOf(rememberResolution) }
    var tempRememberQuality by remember { mutableStateOf(rememberQuality) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Memory, contentDescription = null) },
        title = { Text(stringResource(R.string.settings_remember_dialog_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                CheckboxRow(
                    text = stringResource(R.string.settings_remember_sensor),
                    icon = Icons.Rounded.CameraAlt,
                    checked = tempRememberSensor,
                    onCheckedChange = { tempRememberSensor = it }
                )
                CheckboxRow(
                    text = stringResource(R.string.settings_remember_resolution),
                    icon = Icons.Rounded.PhotoSizeSelectActual,
                    checked = tempRememberResolution,
                    onCheckedChange = { tempRememberResolution = it }
                )
                CheckboxRow(
                    text = stringResource(R.string.settings_remember_quality),
                    icon = Icons.Rounded.HighQuality,
                    checked = tempRememberQuality,
                    onCheckedChange = { tempRememberQuality = it }
                )
                CheckboxRow(
                    text = stringResource(R.string.settings_remember_flash),
                    icon = Icons.Rounded.FlashOn,
                    checked = tempRememberFlash,
                    onCheckedChange = { tempRememberFlash = it }
                )
                CheckboxRow(
                    text = stringResource(R.string.settings_remember_zoom),
                    icon = Icons.Rounded.ZoomIn,
                    checked = tempRememberZoom,
                    onCheckedChange = { tempRememberZoom = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    tempRememberFlash,
                    tempRememberZoom,
                    tempRememberSensor,
                    tempRememberResolution,
                    tempRememberQuality
                )
            }) {
                Text(stringResource(R.string.settings_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_close))
            }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetFpsDialog(
    currentFps: Int,
    onDismiss: () -> Unit,
    onFpsSelected: (Int) -> Unit
) {
    val fpsOptions = listOf(15, 24, 30, 60)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Speed, contentDescription = null) },
        title = { Text(stringResource(R.string.settings_fps_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                fpsOptions.forEach { fps ->
                    DialogRadioRow(
                        text = "$fps FPS",
                        selected = (currentFps == fps),
                        onClick = { onFpsSelected(fps) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_close))
            }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoubleTapDialog(
    currentAction: Int,
    onDismiss: () -> Unit,
    onActionSelected: (Int) -> Unit
) {
    val actions = listOf(
        SettingsManager.DOUBLE_TAP_OFF to stringResource(R.string.settings_double_tap_off),
        SettingsManager.DOUBLE_TAP_SWITCH_CAM to stringResource(R.string.settings_double_tap_switch_cam),
        SettingsManager.DOUBLE_TAP_TOGGLE_ZOOM to stringResource(R.string.settings_double_tap_toggle_zoom)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.TouchApp, contentDescription = null) },
        title = { Text(stringResource(R.string.settings_double_tap_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                actions.forEach { (actionCode, actionName) ->
                    DialogRadioRow(
                        text = actionName,
                        selected = (currentAction == actionCode),
                        onClick = { onActionSelected(actionCode) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_close))
            }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntiFlickerDialog(
    currentMode: Int,
    onDismiss: () -> Unit,
    onModeSelected: (Int) -> Unit
) {
    val modes = listOf(
        SettingsManager.ANTI_FLICKER_AUTO to stringResource(R.string.settings_flicker_auto),
        SettingsManager.ANTI_FLICKER_OFF to stringResource(R.string.settings_flicker_off),
        SettingsManager.ANTI_FLICKER_50HZ to stringResource(R.string.settings_flicker_50hz),
        SettingsManager.ANTI_FLICKER_60HZ to stringResource(R.string.settings_flicker_60hz)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Block, contentDescription = null) },
        title = { Text(stringResource(R.string.settings_flicker_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                modes.forEach { (modeCode, modeName) ->
                    DialogRadioRow(
                        text = modeName,
                        selected = (currentMode == modeCode),
                        onClick = { onModeSelected(modeCode) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_close))
            }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoiseReductionDialog(
    currentMode: Int,
    onDismiss: () -> Unit,
    onModeSelected: (Int) -> Unit
) {
    val modes = listOf(
        SettingsManager.NR_AUTO to stringResource(R.string.settings_noise_reduction_auto),
        SettingsManager.NR_OFF to stringResource(R.string.settings_noise_reduction_off),
        SettingsManager.NR_LOW to stringResource(R.string.settings_noise_reduction_low),
        SettingsManager.NR_HIGH to stringResource(R.string.settings_noise_reduction_high)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Grain, contentDescription = null) },
        title = { Text(stringResource(R.string.settings_noise_reduction_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                modes.forEach { (modeCode, modeName) ->
                    DialogRadioRow(
                        text = modeName,
                        selected = (currentMode == modeCode),
                        onClick = { onModeSelected(modeCode) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_close))
            }
        }
    )
}

@Composable
private fun DialogRadioRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
@Composable
fun <T> SettingsRadioDialog(
    title: String,
    options: Map<T, String>,
    selected: T,
    onDismiss: () -> Unit,
    onSelected: (T) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (key, text) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (key == selected),
                                onClick = { onSelected(key) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (key == selected),
                            onClick = null
                        )
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_close))
            }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeActionDialog(
    currentAction: Int,
    onDismiss: () -> Unit,
    onActionSelected: (Int) -> Unit
) {
    val actions = mapOf(
        SettingsManager.VOL_ACTION_OFF to stringResource(R.string.settings_volume_action_off),
        SettingsManager.VOL_ACTION_ZOOM to stringResource(R.string.settings_volume_action_zoom),
        SettingsManager.VOL_ACTION_SWITCH_CAM to stringResource(R.string.settings_volume_action_switch_cam),
        SettingsManager.VOL_ACTION_TOGGLE_FLASH to stringResource(R.string.settings_volume_action_toggle_flash)
    )

    SettingsRadioDialog(
        title = stringResource(R.string.settings_volume_action_title),
        options = actions,
        selected = currentAction,
        onDismiss = onDismiss,
        onSelected = { newAction ->
            onActionSelected(newAction)
            onDismiss()
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoDimDialog(
    currentDelay: Int,
    onDismiss: () -> Unit,
    onDelaySelected: (Int) -> Unit
) {
    val options = mapOf(
        SettingsManager.DIM_DELAY_OFF to stringResource(R.string.settings_power_auto_dim_off),
        SettingsManager.DIM_DELAY_45S to stringResource(R.string.settings_power_auto_dim_45s),
        SettingsManager.DIM_DELAY_1M to stringResource(R.string.settings_power_auto_dim_1m),
        SettingsManager.DIM_DELAY_90S to stringResource(R.string.settings_power_auto_dim_90s),
        SettingsManager.DIM_DELAY_2M to stringResource(R.string.settings_power_auto_dim_2m),
        SettingsManager.DIM_DELAY_3M to stringResource(R.string.settings_power_auto_dim_3m),
        SettingsManager.DIM_DELAY_5M to stringResource(R.string.settings_power_auto_dim_5m)
    )

    SettingsRadioDialog(
        title = stringResource(R.string.settings_power_auto_dim_dialog_title),
        options = options,
        selected = currentDelay,
        onDismiss = onDismiss,
        onSelected = { newDelay ->
            onDelaySelected(newDelay)
            onDismiss()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoomSmoothingDialog(
    currentDelay: Int,
    onDismiss: () -> Unit,
    onDelaySelected: (Int) -> Unit
) {
    val options = mapOf(
        SettingsManager.SMOOTH_DELAY_NONE to stringResource(R.string.settings_zoom_smoothing_none),
        SettingsManager.SMOOTH_DELAY_5 to "5 ms",
        SettingsManager.SMOOTH_DELAY_8 to "8 ms",
        SettingsManager.SMOOTH_DELAY_10 to "10 ms",
        SettingsManager.SMOOTH_DELAY_15 to "15 ms",
        SettingsManager.SMOOTH_DELAY_20 to "20 ms",
        SettingsManager.SMOOTH_DELAY_25 to "25 ms",
        SettingsManager.SMOOTH_DELAY_30 to "30 ms",
        SettingsManager.SMOOTH_DELAY_40 to "40 ms",
        SettingsManager.SMOOTH_DELAY_50 to "50 ms"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Movie, contentDescription = null) },
        title = { Text(stringResource(R.string.settings_zoom_smoothing_dialog_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (delayMs, text) ->
                    DialogRadioRow(
                        text = text,
                        selected = (currentDelay == delayMs),
                        onClick = { onDelaySelected(delayMs) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_close))
            }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortSettingDialog(
    currentPort: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var text by rememberSaveable { mutableStateOf(currentPort.toString()) }
    var isError by rememberSaveable { mutableStateOf(false) }

    fun validate(portStr: String): Int? {
        val port = portStr.toIntOrNull()
        return if (port != null && port in 1025..65535) {
            port
        } else {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.SettingsEthernet, contentDescription = null) },
        title = { Text(stringResource(R.string.settings_port_dialog_title)) },
        text = {
            Column {
                TextField(
                    value = text,
                    onValueChange = {
                        text = it
                        isError = validate(it) == null
                    },
                    label = { Text(stringResource(R.string.settings_port_dialog_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError
                )
                if (isError) {
                    Text(
                        text = stringResource(R.string.settings_port_dialog_invalid),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val validPort = validate(text)
                    if (validPort != null) {
                        onSave(validPort)
                        onDismiss()
                    }
                }
            ) {
                Text(stringResource(R.string.settings_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_close))
            }
        }
    )
}
