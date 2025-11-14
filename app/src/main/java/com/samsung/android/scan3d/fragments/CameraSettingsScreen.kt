package com.samsung.android.scan3d.fragments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.FilterCenterFocus
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.samsung.android.scan3d.R
import com.samsung.android.scan3d.util.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSettingsScreen(
    onBackClicked: () -> Unit,
    onSendFpsIntent: (Int) -> Unit,
    onSendAntiFlickerIntent: (Int) -> Unit,
    onSendNoiseReductionIntent: (Int) -> Unit,
    onSendStabilizationIntent: (Boolean) -> Unit,
    onSendZoomSmoothingIntent: (Int) -> Unit
) {
    val context = LocalContext.current

    var showRememberDialog by remember { mutableStateOf(false) }
    val dismissRememberDialog = { showRememberDialog = false }
    var showFpsDialog by remember { mutableStateOf(false) }
    val dismissFpsDialog = { showFpsDialog = false }
    var showDoubleTapDialog by remember { mutableStateOf(false) }
    val dismissDoubleTapDialog = { showDoubleTapDialog = false }
    var showFlickerDialog by remember { mutableStateOf(false) }
    val dismissFlickerDialog = { showFlickerDialog = false }
    var showNoiseReductionDialog by remember { mutableStateOf(false) }
    val dismissNoiseReductionDialog = { showNoiseReductionDialog = false }
    var showVolumeActionDialog by remember { mutableStateOf(false) }
    val dismissVolumeActionDialog = { showVolumeActionDialog = false }
    var showZoomSmoothingDialog by remember { mutableStateOf(false) }
    val dismissZoomSmoothingDialog = { showZoomSmoothingDialog = false }

    var currentSmoothingDelay by remember { mutableIntStateOf(SettingsManager.loadZoomSmoothingDelay(context)) }
    var rememberSettingsEnabled by remember { mutableStateOf(SettingsManager.loadRememberSettings(context)) }
    var rememberFlash by remember { mutableStateOf(SettingsManager.loadRememberFlash(context)) }
    var rememberZoom by remember { mutableStateOf(SettingsManager.loadRememberZoom(context)) }
    var rememberSensor by remember { mutableStateOf(SettingsManager.loadRememberSensor(context)) }
    var rememberResolution by remember { mutableStateOf(SettingsManager.loadRememberResolution(context)) }
    var rememberQuality by remember { mutableStateOf(SettingsManager.loadRememberQuality(context)) }
    var currentFps by remember { mutableIntStateOf(SettingsManager.loadTargetFps(context)) }
    var currentDoubleTap by remember { mutableIntStateOf(SettingsManager.loadDoubleTapAction(context)) }
    var stabilizationOff by remember { mutableStateOf(SettingsManager.loadStabilizationOff(context)) }
    var currentFlicker by remember { mutableIntStateOf(SettingsManager.loadAntiFlickerMode(context)) }
    var currentNoiseReduction by remember { mutableIntStateOf(SettingsManager.loadNoiseReductionMode(context)) }
    var currentVolumeAction by remember { mutableIntStateOf(SettingsManager.loadVolumeAction(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_camera_title)) },
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
            SettingsGroup(title = stringResource(R.string.settings_camera_title)) {
                SettingsClickableToggleRow(
                    text = stringResource(R.string.settings_remember_title),
                    summary = stringResource(R.string.settings_remember_desc),
                    icon = Icons.Rounded.Memory,
                    checked = rememberSettingsEnabled,
                    onCheckedChange = {
                        rememberSettingsEnabled = it
                        SettingsManager.saveRememberSettings(context, it)
                        if (it) {
                            rememberFlash = true
                            SettingsManager.saveRememberFlash(context, true)
                            rememberZoom = true
                            SettingsManager.saveRememberZoom(context, true)
                            rememberSensor = true
                            SettingsManager.saveRememberSensor(context, true)
                            rememberResolution = true
                            SettingsManager.saveRememberResolution(context, true)
                            rememberQuality = true
                            SettingsManager.saveRememberQuality(context, true)
                        }
                    },
                    onClick = { showRememberDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                val fpsSummary = "$currentFps FPS"
                SettingsClickableRow(
                    text = stringResource(R.string.settings_fps_title),
                    icon = Icons.Rounded.Speed,
                    summary = fpsSummary,
                    onClick = { showFpsDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                val doubleTapSummary = when (currentDoubleTap) {
                    SettingsManager.DOUBLE_TAP_SWITCH_CAM -> stringResource(R.string.settings_double_tap_switch_cam)
                    SettingsManager.DOUBLE_TAP_TOGGLE_ZOOM -> stringResource(R.string.settings_double_tap_toggle_zoom)
                    else -> stringResource(R.string.settings_double_tap_off)
                }
                SettingsClickableRow(
                    text = stringResource(R.string.settings_double_tap_title),
                    icon = Icons.Rounded.TouchApp,
                    summary = doubleTapSummary,
                    onClick = { showDoubleTapDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                SettingsToggleRow(
                    text = stringResource(R.string.settings_stabilization_title),
                    icon = Icons.Rounded.FilterCenterFocus,
                    checked = stabilizationOff,
                    onCheckedChange = {
                        stabilizationOff = it
                        SettingsManager.saveStabilizationOff(context, it)
                        onSendStabilizationIntent(it)
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                val flickerSummary = when (currentFlicker) {
                    SettingsManager.ANTI_FLICKER_50HZ -> stringResource(R.string.settings_flicker_50hz)
                    SettingsManager.ANTI_FLICKER_60HZ -> stringResource(R.string.settings_flicker_60hz)
                    SettingsManager.ANTI_FLICKER_OFF -> stringResource(R.string.settings_flicker_off)
                    else -> stringResource(R.string.settings_flicker_auto)
                }
                SettingsClickableRow(
                    text = stringResource(R.string.settings_flicker_title),
                    icon = Icons.Rounded.Block,
                    summary = flickerSummary,
                    onClick = { showFlickerDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                val noiseReductionSummary = when (currentNoiseReduction) {
                    SettingsManager.NR_OFF -> stringResource(R.string.settings_noise_reduction_off)
                    SettingsManager.NR_LOW -> stringResource(R.string.settings_noise_reduction_low)
                    SettingsManager.NR_HIGH -> stringResource(R.string.settings_noise_reduction_high)
                    else -> stringResource(R.string.settings_noise_reduction_auto)
                }
                SettingsClickableRow(
                    text = stringResource(R.string.settings_noise_reduction_title),
                    icon = Icons.Rounded.Grain,
                    summary = noiseReductionSummary,
                    onClick = { showNoiseReductionDialog = true }
                )
            }

            SettingsGroup(title = stringResource(R.string.settings_controls_title)) {
                val volumeSummary = when (currentVolumeAction) {
                    SettingsManager.VOL_ACTION_ZOOM -> stringResource(R.string.settings_volume_action_zoom)
                    SettingsManager.VOL_ACTION_SWITCH_CAM -> stringResource(R.string.settings_volume_action_switch_cam)
                    SettingsManager.VOL_ACTION_TOGGLE_FLASH -> stringResource(R.string.settings_volume_action_toggle_flash)
                    else -> stringResource(R.string.settings_volume_action_off)
                }
                SettingsClickableRow(
                    text = stringResource(R.string.settings_volume_action_title),
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    summary = volumeSummary,
                    onClick = { showVolumeActionDialog = true }
                )
            }

            SettingsGroup(title = stringResource(R.string.settings_remote_control_title)) {
                val smoothingSummary = if (currentSmoothingDelay == 0) {
                    stringResource(R.string.settings_zoom_smoothing_none)
                } else {
                    "$currentSmoothingDelay ms"
                }
                SettingsClickableRow(
                    text = stringResource(R.string.settings_zoom_smoothing_title),
                    summary = stringResource(R.string.settings_zoom_smoothing_desc),
                    icon = Icons.Rounded.Movie,
                    subSummary = smoothingSummary,
                    onClick = { showZoomSmoothingDialog = true }
                )
            }
        }
    }

    if (showRememberDialog) {
        RememberSettingsDialog(
            rememberFlash = rememberFlash,
            rememberZoom = rememberZoom,
            rememberSensor = rememberSensor,
            rememberResolution = rememberResolution,
            rememberQuality = rememberQuality,
            onDismiss = dismissRememberDialog,
            onSave = { newFlash, newZoom, newSensor, newRes, newQuality ->
                rememberFlash = newFlash
                SettingsManager.saveRememberFlash(context, rememberFlash)
                rememberZoom = newZoom
                SettingsManager.saveRememberZoom(context, rememberZoom)
                rememberSensor = newSensor
                SettingsManager.saveRememberSensor(context, rememberSensor)
                rememberResolution = newRes
                SettingsManager.saveRememberResolution(context, rememberResolution)
                rememberQuality = newQuality
                SettingsManager.saveRememberQuality(context, rememberQuality)
                dismissRememberDialog()
            }
        )
    }

    if (showFpsDialog) {
        TargetFpsDialog(
            currentFps = currentFps,
            onDismiss = dismissFpsDialog,
            onFpsSelected = { newFps ->
                currentFps = newFps
                SettingsManager.saveTargetFps(context, newFps)
                onSendFpsIntent(newFps)
                dismissFpsDialog()
            }
        )
    }

    if (showDoubleTapDialog) {
        DoubleTapDialog(
            currentAction = currentDoubleTap,
            onDismiss = dismissDoubleTapDialog,
            onActionSelected = { newAction ->
                currentDoubleTap = newAction
                SettingsManager.saveDoubleTapAction(context, newAction)
                dismissDoubleTapDialog()
            }
        )
    }

    if (showFlickerDialog) {
        AntiFlickerDialog(
            currentMode = currentFlicker,
            onDismiss = dismissFlickerDialog,
            onModeSelected = { newMode ->
                currentFlicker = newMode
                SettingsManager.saveAntiFlickerMode(context, newMode)
                onSendAntiFlickerIntent(newMode)
                dismissFlickerDialog()
            }
        )
    }

    if (showNoiseReductionDialog) {
        NoiseReductionDialog(
            currentMode = currentNoiseReduction,
            onDismiss = dismissNoiseReductionDialog,
            onModeSelected = { newMode ->
                currentNoiseReduction = newMode
                SettingsManager.saveNoiseReductionMode(context, newMode)
                onSendNoiseReductionIntent(newMode)
                dismissNoiseReductionDialog()
            }
        )
    }

    if (showVolumeActionDialog) {
        VolumeActionDialog(
            currentAction = currentVolumeAction,
            onDismiss = dismissVolumeActionDialog,
            onActionSelected = { newAction ->
                currentVolumeAction = newAction
                SettingsManager.saveVolumeAction(context, newAction)
                dismissVolumeActionDialog()
            }
        )
    }

    if (showZoomSmoothingDialog) {
        ZoomSmoothingDialog(
            currentDelay = currentSmoothingDelay,
            onDismiss = dismissZoomSmoothingDialog,
            onDelaySelected = { newDelay ->
                currentSmoothingDelay = newDelay
                SettingsManager.saveZoomSmoothingDelay(context, newDelay)
                onSendZoomSmoothingIntent(newDelay)
                dismissZoomSmoothingDialog()
            }
        )
    }
}
