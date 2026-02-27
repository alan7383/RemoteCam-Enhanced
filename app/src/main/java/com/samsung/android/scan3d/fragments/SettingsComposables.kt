package com.samsung.android.scan3d.fragments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PhotoSizeSelectActual
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samsung.android.scan3d.R
import com.samsung.android.scan3d.util.SettingsManager

fun getSettingsShape(groupSize: Int, index: Int): Shape {
    if (groupSize <= 1) return RoundedCornerShape(24.dp)
    val large = 24.dp
    val small = 4.dp
    return when (index) {
        0 -> RoundedCornerShape(topStart = large, topEnd = large, bottomEnd = small, bottomStart = small)
        groupSize - 1 -> RoundedCornerShape(topStart = small, topEnd = small, bottomEnd = large, bottomStart = large)
        else -> RoundedCornerShape(small)
    }
}

@Composable
fun SettingsGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp, top = 16.dp)
    )
}

@Composable
fun SettingsGroup(
    title: String? = null,
    items: List<@Composable (Shape) -> Unit>
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (title != null) {
            SettingsGroupTitle(title)
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items.forEachIndexed { index, itemContent ->
                val shape = getSettingsShape(items.size, index)
                itemContent(shape)
            }
        }
    }
}

@Composable
fun SettingsItem(
    shape: Shape,
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    hasSwitch: Boolean = false,
    switchState: Boolean = false,
    onSwitchChange: ((Boolean) -> Unit)? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val interactionSource = remember { MutableInteractionSource() }

    val onToggleOrClick = {
        if (hasSwitch && onSwitchChange != null) {
            onSwitchChange(!switchState)
        } else {
            onClick?.invoke()
        }
    }

    Card(
        onClick = { onToggleOrClick() },
        enabled = onClick != null || hasSwitch,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = shape,
        modifier = Modifier.fillMaxWidth(),
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = titleColor
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (hasSwitch && onSwitchChange != null) {
                Switch(
                    checked = switchState,
                    onCheckedChange = { onSwitchChange(it) },
                    interactionSource = interactionSource,
                    thumbContent = {
                        if (switchState) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                tint = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScaffold(
    title: String,
    onBackClick: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(title, fontWeight = FontWeight.Bold, maxLines = 1)
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBackClick,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        content(padding)
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
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun DialogRadioRow(
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
            .padding(vertical = 12.dp, horizontal = 16.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
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
                    DialogRadioRow(
                        text = text,
                        selected = (key == selected),
                        onClick = { onSelected(key) }
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_close)) }
        },
        title = { Text(stringResource(R.string.settings_about_title)) },
        icon = { Icon(Icons.Rounded.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
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
                    Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(18.dp))
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.egg_button_alan)) }
        },
        icon = { Icon(Icons.Rounded.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.egg_title_alan)) },
        text = { Text(text = stringResource(R.string.egg_message_alan), style = MaterialTheme.typography.bodyMedium) },
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
    val languages = mapOf(
        SettingsManager.LANG_AUTO to stringResource(R.string.settings_lang_auto),
        SettingsManager.LANG_EN to stringResource(R.string.settings_lang_en),
        SettingsManager.LANG_FR to stringResource(R.string.settings_lang_fr),
        SettingsManager.LANG_HU to stringResource(R.string.settings_lang_hu),
        SettingsManager.LANG_PT to stringResource(R.string.settings_lang_pt_br)
    )
    SettingsRadioDialog(
        title = stringResource(R.string.settings_language),
        options = languages,
        selected = currentLanguage,
        onDismiss = onDismiss,
        onSelected = { newLang ->
            onLanguageSelected(newLang)
            onDismiss()
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
        icon = { Icon(Icons.Rounded.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.settings_remember_dialog_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                CheckboxRow(stringResource(R.string.settings_remember_sensor), Icons.Rounded.CameraAlt, tempRememberSensor, { tempRememberSensor = it })
                CheckboxRow(stringResource(R.string.settings_remember_resolution), Icons.Rounded.PhotoSizeSelectActual, tempRememberResolution, { tempRememberResolution = it })
                CheckboxRow(stringResource(R.string.settings_remember_quality), Icons.Rounded.HighQuality, tempRememberQuality, { tempRememberQuality = it })
                CheckboxRow(stringResource(R.string.settings_remember_flash), Icons.Rounded.FlashOn, tempRememberFlash, { tempRememberFlash = it })
                CheckboxRow(stringResource(R.string.settings_remember_zoom), Icons.Rounded.ZoomIn, tempRememberZoom, { tempRememberZoom = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(tempRememberFlash, tempRememberZoom, tempRememberSensor, tempRememberResolution, tempRememberQuality)
            }) { Text(stringResource(R.string.settings_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_close)) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetFpsDialog(currentFps: Int, onDismiss: () -> Unit, onFpsSelected: (Int) -> Unit) {
    val fpsOptions = mapOf(15 to "15 FPS", 24 to "24 FPS", 30 to "30 FPS", 60 to "60 FPS")
    SettingsRadioDialog(
        title = stringResource(R.string.settings_fps_title),
        options = fpsOptions,
        selected = currentFps,
        onDismiss = onDismiss,
        onSelected = { newFps -> onFpsSelected(newFps); onDismiss() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoubleTapDialog(currentAction: Int, onDismiss: () -> Unit, onActionSelected: (Int) -> Unit) {
    val actions = mapOf(
        SettingsManager.DOUBLE_TAP_OFF to stringResource(R.string.settings_double_tap_off),
        SettingsManager.DOUBLE_TAP_SWITCH_CAM to stringResource(R.string.settings_double_tap_switch_cam),
        SettingsManager.DOUBLE_TAP_TOGGLE_ZOOM to stringResource(R.string.settings_double_tap_toggle_zoom)
    )
    SettingsRadioDialog(title = stringResource(R.string.settings_double_tap_title), options = actions, selected = currentAction, onDismiss = onDismiss, onSelected = { newAction -> onActionSelected(newAction); onDismiss() })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntiFlickerDialog(currentMode: Int, onDismiss: () -> Unit, onModeSelected: (Int) -> Unit) {
    val modes = mapOf(
        SettingsManager.ANTI_FLICKER_AUTO to stringResource(R.string.settings_flicker_auto),
        SettingsManager.ANTI_FLICKER_OFF to stringResource(R.string.settings_flicker_off),
        SettingsManager.ANTI_FLICKER_50HZ to stringResource(R.string.settings_flicker_50hz),
        SettingsManager.ANTI_FLICKER_60HZ to stringResource(R.string.settings_flicker_60hz)
    )
    SettingsRadioDialog(title = stringResource(R.string.settings_flicker_title), options = modes, selected = currentMode, onDismiss = onDismiss, onSelected = { newMode -> onModeSelected(newMode); onDismiss() })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoiseReductionDialog(currentMode: Int, onDismiss: () -> Unit, onModeSelected: (Int) -> Unit) {
    val modes = mapOf(
        SettingsManager.NR_AUTO to stringResource(R.string.settings_noise_reduction_auto),
        SettingsManager.NR_OFF to stringResource(R.string.settings_noise_reduction_off),
        SettingsManager.NR_LOW to stringResource(R.string.settings_noise_reduction_low),
        SettingsManager.NR_HIGH to stringResource(R.string.settings_noise_reduction_high)
    )
    SettingsRadioDialog(title = stringResource(R.string.settings_noise_reduction_title), options = modes, selected = currentMode, onDismiss = onDismiss, onSelected = { newMode -> onModeSelected(newMode); onDismiss() })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeActionDialog(currentAction: Int, onDismiss: () -> Unit, onActionSelected: (Int) -> Unit) {
    val actions = mapOf(
        SettingsManager.VOL_ACTION_OFF to stringResource(R.string.settings_volume_action_off),
        SettingsManager.VOL_ACTION_ZOOM to stringResource(R.string.settings_volume_action_zoom),
        SettingsManager.VOL_ACTION_SWITCH_CAM to stringResource(R.string.settings_volume_action_switch_cam),
        SettingsManager.VOL_ACTION_TOGGLE_FLASH to stringResource(R.string.settings_volume_action_toggle_flash)
    )
    SettingsRadioDialog(title = stringResource(R.string.settings_volume_action_title), options = actions, selected = currentAction, onDismiss = onDismiss, onSelected = { newAction -> onActionSelected(newAction); onDismiss() })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoDimDialog(currentDelay: Int, onDismiss: () -> Unit, onDelaySelected: (Int) -> Unit) {
    val options = mapOf(
        SettingsManager.DIM_DELAY_OFF to stringResource(R.string.settings_power_auto_dim_off),
        SettingsManager.DIM_DELAY_45S to stringResource(R.string.settings_power_auto_dim_45s),
        SettingsManager.DIM_DELAY_1M to stringResource(R.string.settings_power_auto_dim_1m),
        SettingsManager.DIM_DELAY_90S to stringResource(R.string.settings_power_auto_dim_90s),
        SettingsManager.DIM_DELAY_2M to stringResource(R.string.settings_power_auto_dim_2m),
        SettingsManager.DIM_DELAY_3M to stringResource(R.string.settings_power_auto_dim_3m),
        SettingsManager.DIM_DELAY_5M to stringResource(R.string.settings_power_auto_dim_5m)
    )
    SettingsRadioDialog(title = stringResource(R.string.settings_power_auto_dim_dialog_title), options = options, selected = currentDelay, onDismiss = onDismiss, onSelected = { newDelay -> onDelaySelected(newDelay); onDismiss() })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoomSmoothingDialog(currentDelay: Int, onDismiss: () -> Unit, onDelaySelected: (Int) -> Unit) {
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
    SettingsRadioDialog(title = stringResource(R.string.settings_zoom_smoothing_dialog_title), options = options, selected = currentDelay, onDismiss = onDismiss, onSelected = { newDelay -> onDelaySelected(newDelay); onDismiss() })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortSettingDialog(currentPort: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by rememberSaveable { mutableStateOf(currentPort.toString()) }
    var isError by rememberSaveable { mutableStateOf(false) }

    fun validate(portStr: String): Int? {
        val port = portStr.toIntOrNull()
        return if (port != null && port in 1025..65535) port else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.SettingsEthernet, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
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
            ) { Text(stringResource(R.string.settings_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_close)) }
        }
    )
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
                imageVector = if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
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