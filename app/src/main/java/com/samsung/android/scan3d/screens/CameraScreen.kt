package com.samsung.android.scan3d.screens

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.samsung.android.scan3d.CameraActivity
import com.samsung.android.scan3d.R
import com.samsung.android.scan3d.ViewState
import com.samsung.android.scan3d.fragments.PortSettingDialog
import com.samsung.android.scan3d.serv.Cam
import com.samsung.android.scan3d.serv.CamEngine
import com.samsung.android.scan3d.util.ClipboardUtil
import com.samsung.android.scan3d.util.IpUtil
import com.samsung.android.scan3d.util.Selector
import com.samsung.android.scan3d.util.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    cameraActivity: CameraActivity,
    onNavigateToSettings: () -> Unit,
    isInputLocked: Boolean
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    val defaultCameraId = remember { Selector.enumerateCameras(cameraManager).firstOrNull()?.cameraId ?: "0" }

    // Initial state loaded from preferences
    val initialViewState = remember {
        SettingsManager.loadViewState(context, defaultCameraId)
    }

    // États de l'UI
    var viewState by remember { mutableStateOf(initialViewState) }
    var zoomSliderPosition by remember { mutableFloatStateOf(0.0f) }
    var camData by remember { mutableStateOf<CamEngine.Companion.Data?>(null) }
    var quickData by remember { mutableStateOf<CamEngine.Companion.DataQuick?>(null) }
    var localIp by remember { mutableStateOf("0.0.0.0:8080/cam.mjpeg") }

    // States for port error handling
    var showBindErrorDialog by remember { mutableStateOf(false) }
    var showPortChangeDialog by remember { mutableStateOf(false) }
    var failedPort by remember { mutableIntStateOf(8080) }

    val textureView = remember { TextureView(context) }
    var previewSurface by remember { mutableStateOf<Surface?>(null) }

    // Update the displayed IP (format and port)
    fun updateIpDisplay(format: Int) {
        IpUtil.getLocalIpAddress()?.let {
            val port = SettingsManager.loadPort(context)
            val extension = if (format == SettingsManager.FORMAT_H264) "cam.h264" else "cam.mjpeg"
            localIp = "$it:$port/$extension"
        }
    }

    // Perform the update when the format or IP changes at startup
    LaunchedEffect(viewState.streamFormat) {
        updateIpDisplay(viewState.streamFormat)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val window = cameraActivity.window
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                Lifecycle.Event.ON_RESUME -> {
                    if (SettingsManager.loadKeepScreenOn(context)) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Receiving updates from CamEngine and HttpService
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            @SuppressLint("UnsafeParcelable")
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    "PORT_UPDATED" -> {
                        updateIpDisplay(viewState.streamFormat)
                    }
                    "PORT_BIND_ERROR" -> {
                        failedPort = intent.getIntExtra("failed_port", 8080)
                        showBindErrorDialog = true
                    }
                    "UpdateFromCameraEngine" -> {
                        val extras = intent.extras ?: return

                        // Smooth data (FPS / Bitrate)
                        val newQuickData = extras.getParcelable("dataQuick", CamEngine.Companion.DataQuick::class.java)
                        if (newQuickData != null) quickData = newQuickData

                        // Configuration data (Sensors / Zoom / Flash)
                        val data = extras.getParcelable("data", CamEngine.Companion.Data::class.java)
                        data?.let { d ->
                            camData = d
                            // Update UI state if the engine changed something
                            viewState = viewState.copy(
                                cameraId = d.sensorSelected.cameraId,
                                flash = d.flashState,
                                flashLevel = d.flashLevel,
                                resolutionIndex = d.resolutionSelected,
                                quality = d.quality
                            )
                            // Update zoom slider
                            if (d.maxZoom > d.minZoom) {
                                val ratio = (d.currentZoom - d.minZoom) / (d.maxZoom - d.minZoom)
                                zoomSliderPosition = ratio.coerceIn(0.0f, 1.0f)
                            }
                        }
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction("UpdateFromCameraEngine")
            addAction("PORT_UPDATED")
            addAction("PORT_BIND_ERROR")
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Port error handling (Dialogs)
    if (showBindErrorDialog) {
        AlertDialog(
            onDismissRequest = { showBindErrorDialog = false },
            title = { Text(stringResource(R.string.error_port_title)) },
            text = { Text(stringResource(R.string.error_port_message, failedPort)) },
            confirmButton = {
                TextButton(onClick = {
                    showBindErrorDialog = false
                    showPortChangeDialog = true
                }) {
                    Text(stringResource(R.string.btn_change_port))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBindErrorDialog = false }) {
                    Text(stringResource(R.string.settings_close))
                }
            }
        )
    }

    if (showPortChangeDialog) {
        PortSettingDialog(
            currentPort = SettingsManager.loadPort(context),
            onDismiss = { showPortChangeDialog = false },
            onSave = { newPort ->
                SettingsManager.savePort(context, newPort)
                cameraActivity.sendCam {
                    it.action = "set_http_port"
                    it.putExtra("port", newPort)
                }
                showPortChangeDialog = false
            }
        )
    }

    // Sending ViewState changes to the Service
    fun sendViewState(newState: ViewState) {
        viewState = newState
        SettingsManager.saveSettings(context, viewState)
        cameraActivity.sendCam {
            it.action = "new_view_state"
            it.putExtra("data", viewState)
        }
    }

    fun handleDoubleTap() {
        val action = SettingsManager.loadDoubleTapAction(context)
        val intentAction = when (action) {
            SettingsManager.DOUBLE_TAP_SWITCH_CAM -> "double_tap_action_switch_camera"
            SettingsManager.DOUBLE_TAP_TOGGLE_ZOOM -> "double_tap_action_toggle_zoom"
            else -> null
        }
        intentAction?.let { cameraActivity.sendCam { intent -> intent.action = it } }
    }

    CameraScreenUI(
        textureView = textureView,
        camData = camData,
        quickData = quickData,
        viewState = viewState,
        localIp = localIp,
        zoomSliderPosition = zoomSliderPosition,
        isInputLocked = isInputLocked,
        onSurfaceAvailable = { surface ->
            previewSurface?.release()
            previewSurface = surface
            cameraActivity.sendCam {
                it.action = "start_engine_with_surface"
                it.putExtra("surface", surface)
            }
        },
        onSurfaceDestroyed = {
            cameraActivity.sendCam { it.action = "preview_surface_destroyed" }
            previewSurface?.release()
            previewSurface = null
        },
        onStopClicked = {
            cameraActivity.sendCam { it.action = "KILL" }
            cameraActivity.finishAndRemoveTask()
        },
        onPreviewToggled = { sendViewState(viewState.copy(preview = it)) },
        onStreamToggled = { sendViewState(viewState.copy(stream = it)) },
        onFlashToggled = { sendViewState(viewState.copy(flash = it)) },
        onFlashLevelChanged = { level ->
            sendViewState(viewState.copy(flashLevel = level))
            cameraActivity.sendCam {
                it.action = "set_flash_level"
                it.putExtra("level", level)
            }
        },
        onSensorSelected = { index ->
            camData?.sensors?.getOrNull(index)?.let { sensor ->
                val newCamId = sensor.cameraId
                if (viewState.cameraId != newCamId) {
                    zoomSliderPosition = 0.0f
                    SettingsManager.saveZoomRatio(context, 1.0f)
                }
                sendViewState(viewState.copy(cameraId = newCamId, resolutionIndex = null, flash = false))
            }
        },
        onResolutionSelected = { index ->
            if (camData?.resolutions?.indices?.contains(index) == true) {
                sendViewState(viewState.copy(resolutionIndex = index))
            }
        },
        onQualitySelected = { quality -> sendViewState(viewState.copy(quality = quality)) },
        onIpClicked = { ip ->
            ClipboardUtil.copyToClipboard(context, "ip", ip)
            Toast.makeText(context, R.string.cam_clipboard_copied, Toast.LENGTH_SHORT).show()
        },
        onZoomScaleChanged = { scaleFactor ->
            cameraActivity.sendCam {
                it.action = "scale_zoom"
                it.putExtra("scale_factor", scaleFactor)
            }
        },
        onZoomRatioChanged = { newPosition ->
            // --- LOGIQUE DE CLIP (SNAP) 1.0x ---
            var finalPos = newPosition
            val data = camData
            if (data != null) {
                val realZoom = data.minZoom + (data.maxZoom - data.minZoom) * newPosition
                // If we are within +/- 0.12x of 1.0x, snap to it
                if (kotlin.math.abs(realZoom - 1.0f) < 0.12f) {
                    finalPos = ((1.0f - data.minZoom) / (data.maxZoom - data.minZoom)).coerceIn(0f, 1f)
                    // Stronger vibration when "clicking" on 1.0x
                    if (kotlin.math.abs(zoomSliderPosition - finalPos) > 0.001f) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                } else {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            } else {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }

            zoomSliderPosition = finalPos
            cameraActivity.sendCam {
                it.action = "set_zoom_ratio"
                it.putExtra("ratio", finalPos)
            }
        },
        onH264BitrateChanged = { mbps ->
            viewState = viewState.copy(h264Bitrate = mbps)
            cameraActivity.sendCam {
                it.action = "set_h264_bitrate"
                it.putExtra("bitrate", mbps)
            }
        },
        onH264ModeChanged = { mode ->
            viewState = viewState.copy(h264Mode = mode)
            cameraActivity.sendCam {
                it.action = "set_h264_mode"
                it.putExtra("mode", mode)
            }
        },
        onSettingsClicked = onNavigateToSettings,
        onDoubleTapped = { handleDoubleTap() }
    )
}