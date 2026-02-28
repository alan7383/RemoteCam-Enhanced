package com.samsung.android.scan3d.fragments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.FilterCenterFocus
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
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
    onSendFormatIntent: (Int) -> Unit,
    onSendAntiFlickerIntent: (Int) -> Unit,
    onSendNoiseReductionIntent: (Int) -> Unit,
    onSendStabilizationIntent: (Boolean) -> Unit,
    onSendZoomSmoothingIntent: (Int) -> Unit
) {
    val context = LocalContext.current

    // Dialog States
    var showRememberDialog by remember { mutableStateOf(false) }
    var showFpsDialog by remember { mutableStateOf(false) }
    var showFormatDialog by remember { mutableStateOf(false) }
    var showDoubleTapDialog by remember { mutableStateOf(false) }
    var showFlickerDialog by remember { mutableStateOf(false) }
    var showNoiseReductionDialog by remember { mutableStateOf(false) }
    var showVolumeActionDialog by remember { mutableStateOf(false) }
    var showZoomSmoothingDialog by remember { mutableStateOf(false) }

    // Settings states loaded from SettingsManager
    var currentFormat by remember { mutableIntStateOf(SettingsManager.loadStreamFormat(context)) }
    var currentSmoothingDelay by remember { mutableIntStateOf(SettingsManager.loadZoomSmoothingDelay(context)) }
    var rememberSettingsEnabled by remember { mutableStateOf(SettingsManager.loadRememberSettings(context)) }
    var rememberFlash by remember { mutableStateOf(SettingsManager.loadRememberFlash(context)) }
    var rememberZoom by remember { mutableStateOf(SettingsManager.loadRememberZoom(context)) }
    var rememberSensor by remember { mutableStateOf(SettingsManager.loadRememberSensor(context)) }
    var rememberResolution by remember { mutableStateOf(SettingsManager.loadRememberResolution(context)) }
    var rememberQuality by remember { mutableStateOf(SettingsManager.loadRememberQuality(context)) }
    var rememberH264 by remember { mutableStateOf(SettingsManager.loadRememberH264(context)) }
    var currentFps by remember { mutableIntStateOf(SettingsManager.loadTargetFps(context)) }
    var currentDoubleTap by remember { mutableIntStateOf(SettingsManager.loadDoubleTapAction(context)) }
    var stabilizationOff by remember { mutableStateOf(SettingsManager.loadStabilizationOff(context)) }
    var currentFlicker by remember { mutableIntStateOf(SettingsManager.loadAntiFlickerMode(context)) }
    var currentNoiseReduction by remember { mutableIntStateOf(SettingsManager.loadNoiseReductionMode(context)) }
    var currentVolumeAction by remember { mutableIntStateOf(SettingsManager.loadVolumeAction(context)) }

    // Format Dialog Logic (H.264 / MJPEG)
    if (showFormatDialog) {
        val formatOptions = mapOf(
            SettingsManager.FORMAT_MJPEG to stringResource(R.string.settings_format_mjpeg),
            SettingsManager.FORMAT_H264 to stringResource(R.string.settings_format_h264)
        )
        val formatSubtitles = mapOf(
            SettingsManager.FORMAT_MJPEG to stringResource(R.string.settings_format_mjpeg_desc),
            SettingsManager.FORMAT_H264 to stringResource(R.string.settings_format_h264_desc)
        )
        SettingsRadioDialog(
            title = stringResource(R.string.settings_format_title),
            options = formatOptions,
            selected = currentFormat,
            onDismiss = { showFormatDialog = false },
            onSelected = { newFormat ->
                currentFormat = newFormat
                SettingsManager.saveStreamFormat(context, newFormat)
                onSendFormatIntent(newFormat)
                showFormatDialog = false
            },
            showBetaBadgeForItem = { it == SettingsManager.FORMAT_H264 },
            subtitles = formatSubtitles
        )
    }

    if (showRememberDialog) {
        RememberSettingsDialog(
            rememberFlash = rememberFlash,
            rememberZoom = rememberZoom,
            rememberSensor = rememberSensor,
            rememberResolution = rememberResolution,
            rememberQuality = rememberQuality,
            rememberH264 = rememberH264,
            onDismiss = { showRememberDialog = false },
            onSave = { newFlash, newZoom, newSensor, newRes, newQual, newH264 ->
                rememberFlash = newFlash; SettingsManager.saveRememberFlash(context, newFlash)
                rememberZoom = newZoom; SettingsManager.saveRememberZoom(context, newZoom)
                rememberSensor = newSensor; SettingsManager.saveRememberSensor(context, newSensor)
                rememberResolution = newRes; SettingsManager.saveRememberResolution(context, newRes)
                rememberQuality = newQual; SettingsManager.saveRememberQuality(context, newQual)
                rememberH264 = newH264; SettingsManager.saveRememberH264(context, newH264)
                showRememberDialog = false
            }
        )
    }

    if (showFpsDialog) {
        TargetFpsDialog(currentFps, onDismiss = { showFpsDialog = false }) { newFps ->
            currentFps = newFps; SettingsManager.saveTargetFps(context, newFps); onSendFpsIntent(newFps)
        }
    }

    if (showDoubleTapDialog) {
        DoubleTapDialog(currentDoubleTap, onDismiss = { showDoubleTapDialog = false }) { newAction ->
            currentDoubleTap = newAction; SettingsManager.saveDoubleTapAction(context, newAction)
        }
    }

    if (showFlickerDialog) {
        AntiFlickerDialog(currentFlicker, onDismiss = { showFlickerDialog = false }) { newMode ->
            currentFlicker = newMode; SettingsManager.saveAntiFlickerMode(context, newMode); onSendAntiFlickerIntent(newMode)
        }
    }

    if (showNoiseReductionDialog) {
        NoiseReductionDialog(currentNoiseReduction, onDismiss = { showNoiseReductionDialog = false }) { newMode ->
            currentNoiseReduction = newMode; SettingsManager.saveNoiseReductionMode(context, newMode); onSendNoiseReductionIntent(newMode)
        }
    }

    if (showVolumeActionDialog) {
        VolumeActionDialog(currentVolumeAction, onDismiss = { showVolumeActionDialog = false }) { newAction ->
            currentVolumeAction = newAction; SettingsManager.saveVolumeAction(context, newAction)
        }
    }

    if (showZoomSmoothingDialog) {
        ZoomSmoothingDialog(currentSmoothingDelay, onDismiss = { showZoomSmoothingDialog = false }) { newDelay ->
            currentSmoothingDelay = newDelay; SettingsManager.saveZoomSmoothingDelay(context, newDelay); onSendZoomSmoothingIntent(newDelay)
        }
    }

    SettingsScaffold(
        title = stringResource(R.string.settings_camera_title),
        onBackClick = onBackClicked
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 32.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    SettingsGroupTitle(stringResource(R.string.settings_camera_title))

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {

                        SettingsItem(
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                            title = stringResource(R.string.settings_remember_title),
                            subtitle = stringResource(R.string.settings_remember_desc),
                            icon = Icons.Rounded.Memory,
                            hasSwitch = true,
                            switchState = rememberSettingsEnabled,
                            onSwitchChange = {
                                rememberSettingsEnabled = it
                                SettingsManager.saveRememberSettings(context, it)
                                if (it) {
                                    rememberFlash = true; SettingsManager.saveRememberFlash(context, true)
                                    rememberZoom = true; SettingsManager.saveRememberZoom(context, true)
                                    rememberSensor = true; SettingsManager.saveRememberSensor(context, true)
                                    rememberResolution = true; SettingsManager.saveRememberResolution(context, true)
                                    rememberQuality = true; SettingsManager.saveRememberQuality(context, true)
                                    rememberH264 = true; SettingsManager.saveRememberH264(context, true)
                                }
                            }
                        )

                        AnimatedVisibility(
                            visible = rememberSettingsEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            SettingsItem(
                                shape = RoundedCornerShape(4.dp),
                                title = stringResource(R.string.settings_remember_dialog_title),
                                subtitle = null,
                                icon = Icons.Rounded.Memory,
                                onClick = { showRememberDialog = true }
                            )
                        }

                        SettingsItem(
                            shape = RoundedCornerShape(4.dp),
                            title = stringResource(R.string.settings_format_title),
                            subtitle = if (currentFormat == SettingsManager.FORMAT_H264)
                                stringResource(R.string.settings_format_h264)
                            else
                                stringResource(R.string.settings_format_mjpeg),
                            icon = Icons.Rounded.Videocam,
                            onClick = { showFormatDialog = true },
                            showBetaBadge = true
                        )

                        SettingsItem(
                            shape = RoundedCornerShape(4.dp),
                            title = stringResource(R.string.settings_fps_title),
                            subtitle = "$currentFps FPS",
                            icon = Icons.Rounded.Speed,
                            onClick = { showFpsDialog = true }
                        )

                        SettingsItem(
                            shape = RoundedCornerShape(4.dp),
                            title = stringResource(R.string.settings_double_tap_title),
                            subtitle = when (currentDoubleTap) {
                                SettingsManager.DOUBLE_TAP_SWITCH_CAM -> stringResource(R.string.settings_double_tap_switch_cam)
                                SettingsManager.DOUBLE_TAP_TOGGLE_ZOOM -> stringResource(R.string.settings_double_tap_toggle_zoom)
                                else -> stringResource(R.string.settings_double_tap_off)
                            },
                            icon = Icons.Rounded.TouchApp,
                            onClick = { showDoubleTapDialog = true }
                        )

                        SettingsItem(
                            shape = RoundedCornerShape(4.dp),
                            title = stringResource(R.string.settings_stabilization_title),
                            subtitle = stringResource(R.string.settings_stabilization_desc),
                            icon = Icons.Rounded.FilterCenterFocus,
                            hasSwitch = true,
                            switchState = stabilizationOff,
                            onSwitchChange = {
                                stabilizationOff = it
                                SettingsManager.saveStabilizationOff(context, it)
                                onSendStabilizationIntent(it)
                            }
                        )

                        SettingsItem(
                            shape = RoundedCornerShape(4.dp),
                            title = stringResource(R.string.settings_flicker_title),
                            subtitle = when (currentFlicker) {
                                SettingsManager.ANTI_FLICKER_50HZ -> stringResource(R.string.settings_flicker_50hz)
                                SettingsManager.ANTI_FLICKER_60HZ -> stringResource(R.string.settings_flicker_60hz)
                                SettingsManager.ANTI_FLICKER_OFF -> stringResource(R.string.settings_flicker_off)
                                else -> stringResource(R.string.settings_flicker_auto)
                            },
                            icon = Icons.Rounded.Block,
                            onClick = { showFlickerDialog = true }
                        )

                        SettingsItem(
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                            title = stringResource(R.string.settings_noise_reduction_title),
                            subtitle = when (currentNoiseReduction) {
                                SettingsManager.NR_OFF -> stringResource(R.string.settings_noise_reduction_off)
                                SettingsManager.NR_LOW -> stringResource(R.string.settings_noise_reduction_low)
                                SettingsManager.NR_HIGH -> stringResource(R.string.settings_noise_reduction_high)
                                else -> stringResource(R.string.settings_noise_reduction_auto)
                            },
                            icon = Icons.Rounded.Grain,
                            onClick = { showNoiseReductionDialog = true }
                        )
                    }
                }
            }

            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_controls_title),
                    items = listOf(
                        { shape ->
                            SettingsItem(
                                shape = shape,
                                title = stringResource(R.string.settings_volume_action_title),
                                subtitle = when (currentVolumeAction) {
                                    SettingsManager.VOL_ACTION_ZOOM -> stringResource(R.string.settings_volume_action_zoom)
                                    SettingsManager.VOL_ACTION_SWITCH_CAM -> stringResource(R.string.settings_volume_action_switch_cam)
                                    SettingsManager.VOL_ACTION_TOGGLE_FLASH -> stringResource(R.string.settings_volume_action_toggle_flash)
                                    else -> stringResource(R.string.settings_volume_action_off)
                                },
                                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                                onClick = { showVolumeActionDialog = true }
                            )
                        }
                    )
                )
            }

            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_remote_control_title),
                    items = listOf(
                        { shape ->
                            SettingsItem(
                                shape = shape,
                                title = stringResource(R.string.settings_zoom_smoothing_title),
                                subtitle = if (currentSmoothingDelay == 0) stringResource(R.string.settings_zoom_smoothing_none) else "$currentSmoothingDelay ms",
                                icon = Icons.Rounded.Movie,
                                onClick = { showZoomSmoothingDialog = true }
                            )
                        }
                    )
                )
            }
        }
    }
}