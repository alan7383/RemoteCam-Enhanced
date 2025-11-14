package com.samsung.android.scan3d.screens

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.samsung.android.scan3d.fragments.CameraSettingsScreen
import com.samsung.android.scan3d.serv.Cam

@Composable
fun AdditionalSettingsScreen(
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current

    fun sendIntentToCamService(action: String, extras: ((Intent) -> Unit) = {}) {
        val intent = Intent(context, Cam::class.java).apply {
            this.action = action
            extras(this)
        }
        context.startService(intent)
    }

    CameraSettingsScreen(
        onBackClicked = onBackClicked,
        onSendFpsIntent = { fps ->
            sendIntentToCamService("set_target_fps") { it.putExtra("fps", fps) }
        },
        onSendAntiFlickerIntent = { mode ->
            sendIntentToCamService("set_anti_flicker") { it.putExtra("mode", mode) }
        },
        onSendNoiseReductionIntent = { mode ->
            sendIntentToCamService("set_noise_reduction") { it.putExtra("mode", mode) }
        },
        onSendStabilizationIntent = { isOff ->
            sendIntentToCamService("set_stabilization") { it.putExtra("is_off", isOff) }
        },
        onSendZoomSmoothingIntent = { delay ->
            sendIntentToCamService("set_zoom_smoothing") { it.putExtra("delay", delay) }
        }
    )
}
