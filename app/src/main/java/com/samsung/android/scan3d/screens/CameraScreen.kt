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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.samsung.android.scan3d.CameraActivity
import com.samsung.android.scan3d.R
import com.samsung.android.scan3d.ViewState
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

    val initialViewState = remember {
        SettingsManager.loadViewState(context, defaultCameraId)
    }

    var viewState by remember { mutableStateOf(initialViewState) }
    var zoomSliderPosition by remember { mutableFloatStateOf(0.0f) }
    var camData by remember { mutableStateOf<CamEngine.Companion.Data?>(null) }
    var quickData by remember { mutableStateOf<CamEngine.Companion.DataQuick?>(null) }
    var localIp by remember { mutableStateOf("0.0.0.0:8080/cam.mjpeg") }

    val textureView = remember {
        TextureView(context)
    }

    var previewSurface by remember { mutableStateOf<Surface?>(null) }

    LaunchedEffect(Unit) {
        IpUtil.getLocalIpAddress()?.let {
            val port = SettingsManager.loadPort(context)
            localIp = "$it:$port/cam.mjpeg"
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val window = cameraActivity.window

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                Lifecycle.Event.ON_RESUME -> {
                    val keepScreenOn = SettingsManager.loadKeepScreenOn(context)
                    if (keepScreenOn) {
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

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            @SuppressLint("UnsafeParcelable")
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == "PORT_UPDATED") {
                    IpUtil.getLocalIpAddress()?.let {
                        val port = SettingsManager.loadPort(context)
                        localIp = "$it:$port/cam.mjpeg"
                    }
                    return
                }

                val extras = intent.extras ?: return
                quickData = extras.getParcelable("dataQuick", CamEngine.Companion.DataQuick::class.java)
                val data = extras.getParcelable("data", CamEngine.Companion.Data::class.java)

                data?.let { d ->
                    camData = d

                    val engineQuality = d.quality
                    val engineResolutionIndex = d.resolutionSelected
                    val engineCameraId = d.sensorSelected.cameraId
                    val engineFlashState = d.flashState

                    var stateChanged = false
                    var newViewState = viewState

                    if (viewState.cameraId != engineCameraId) {
                        newViewState = newViewState.copy(
                            cameraId = engineCameraId,
                            flash = engineFlashState,
                            resolutionIndex = engineResolutionIndex
                        )
                        stateChanged = true
                    }

                    if (viewState.resolutionIndex != engineResolutionIndex) {
                        newViewState = newViewState.copy(resolutionIndex = engineResolutionIndex)
                        stateChanged = true
                    }

                    if (viewState.quality != engineQuality) {
                        newViewState = newViewState.copy(quality = engineQuality)
                        stateChanged = true
                    }

                    if (viewState.flash != engineFlashState) {
                        newViewState = newViewState.copy(flash = engineFlashState)
                        stateChanged = true
                    }

                    if (stateChanged) viewState = newViewState

                    if (d.maxZoom > d.minZoom) {
                        val ratio = (d.currentZoom - d.minZoom) / (d.maxZoom - d.minZoom)
                        zoomSliderPosition = ratio.coerceIn(0.0f, 1.0f)
                    } else {
                        zoomSliderPosition = 0.0f
                    }
                }
            }
        }
        val filter = IntentFilter("UpdateFromCameraEngine")
        filter.addAction("PORT_UPDATED")
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

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
        onPreviewToggled = { isChecked -> sendViewState(viewState.copy(preview = isChecked)) },
        onStreamToggled = { isChecked -> sendViewState(viewState.copy(stream = isChecked)) },
        onFlashToggled = { isChecked -> sendViewState(viewState.copy(flash = isChecked)) },
        onSensorSelected = { index ->
            camData?.sensors?.getOrNull(index)?.let { sensor ->
                val newCamId = sensor.cameraId
                val oldCamId = viewState.cameraId

                // --- FIX: RESET ZOOM ON CAMERA CHANGE ---
                if (oldCamId != newCamId) {
                    zoomSliderPosition = 0.0f
                    // On force la sauvegarde du zoom à 1.0f pour que le nouveau moteur charge cette valeur
                    SettingsManager.saveZoomRatio(context, 1.0f)
                }
                // --- END FIX ---

                sendViewState(
                    viewState.copy(
                        cameraId = newCamId,
                        resolutionIndex = if (oldCamId != newCamId) null else viewState.resolutionIndex,
                        flash = false
                    )
                )
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
            camData?.let { data ->
                if (data.maxZoom > data.minZoom) {
                    val currentZoom = data.currentZoom
                    val newZoom = (currentZoom * scaleFactor).coerceIn(data.minZoom, data.maxZoom)

                    val newRatio = (newZoom - data.minZoom) / (data.maxZoom - data.minZoom)
                    zoomSliderPosition = newRatio.coerceIn(0.0f, 1.0f)
                }
            }

            cameraActivity.sendCam {
                it.action = "scale_zoom"
                it.putExtra("scale_factor", scaleFactor)
            }
        },
        onZoomRatioChanged = { newPosition ->
            zoomSliderPosition = newPosition

            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)

            cameraActivity.sendCam {
                it.action = "set_zoom_ratio"
                it.putExtra("ratio", newPosition)
            }
        },
        onSettingsClicked = onNavigateToSettings,
        onDoubleTapped = { handleDoubleTap() }
    )
}