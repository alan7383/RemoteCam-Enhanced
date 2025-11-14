// File: app/src/main/java/com/samsung/android/scan3d/CameraActivity.kt

package com.samsung.android.scan3d

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import android.view.KeyEvent
import com.samsung.android.scan3d.serv.Cam
import com.samsung.android.scan3d.ui.theme.RemoteCamM3Theme
import com.samsung.android.scan3d.util.SettingsManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.BroadcastReceiver
import android.content.IntentFilter

class CameraActivity : AppCompatActivity() {

    @Volatile
    var isRestarting: Boolean = false

    // --- (NEW) Variables for Power and Screen ---
    private var isInputLocked by mutableStateOf(false)
    private var isScreenDimmed = false
    private val dimHandler = Handler(Looper.getMainLooper())
    private var dimRunnable: Runnable? = null
    private var originalBrightness: Float = -1.0f // -1.0f is the system default value

    private val killReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "KILL_ACTIVITY") {
                finishAndRemoveTask()
            }
        }
    }

    /**
     * Checks if essential permissions are already granted.
     */
    private fun arePermissionsGranted(): Boolean {
        val permissions =
            listOf(Manifest.permission.CAMERA, Manifest.permission.POST_NOTIFICATIONS)

        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Starts the main camera service.
     * This function is called by AppNavigation
     */
    fun startCamService() {
        sendCam { it.action = "start" }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        // --- START OF LANGUAGE FIX ---
        val langCode = SettingsManager.loadLanguage(this)
        val appLocale: LocaleListCompat = if (langCode == SettingsManager.LANG_AUTO) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(langCode)
        }
        AppCompatDelegate.setApplicationLocales(appLocale)
        // --- END OF LANGUAGE FIX ---

        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        originalBrightness = window.attributes.screenBrightness

        val startDestination = if (arePermissionsGranted()) {
            startCamService()
            Screen.Camera.route
        } else {
            Screen.Permissions.route
        }

        val filter = IntentFilter("KILL_ACTIVITY")
        registerReceiver(killReceiver, filter, RECEIVER_NOT_EXPORTED)

        setContent {
            RemoteCamM3Theme {
                Box(Modifier.fillMaxSize()) {
                    AppNavigation(
                        cameraActivity = this@CameraActivity,
                        startDestination = startDestination,
                        isInputLocked = isInputLocked
                    )

                    // --- (NEW) Lock Overlay ---
                    if (isInputLocked) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable(
                                    onClick = { resetDimTimer() }, // Unlocks on click
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.cam_overlay_locked),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    fun sendCam(callback: (intent: Intent) -> Unit) {
        val intent = Intent(this, Cam::class.java)
        callback(intent)
        startService(intent)
    }

    // --- (NEW) Dimming/Locking management functions ---
    private fun setScreenBrightness(value: Float) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = value
        window.attributes = layoutParams
    }

    private fun dimScreen() {
        setScreenBrightness(0.01f) // 1% brightness
        isScreenDimmed = true
        if (SettingsManager.loadLockInputOnDim(this)) {
            isInputLocked = true // Triggers the Compose overlay
        }
    }

    fun resetDimTimer() {
        stopDimTimer()

        if (isScreenDimmed) {
            setScreenBrightness(originalBrightness) // Restore brightness
            isScreenDimmed = false
            isInputLocked = false // Disables the overlay
        }

        val delay = SettingsManager.loadAutoDimDelay(this)
        if (delay > 0) {
            dimRunnable = Runnable { dimScreen() }
            dimHandler.postDelayed(dimRunnable!!, delay.toLong())
        }
    }

    private fun stopDimTimer() {
        dimRunnable?.let { dimHandler.removeCallbacks(it) }
        dimRunnable = null
    }

    // --- (NEW) Trigger the timer when the user touches the screen ---
    override fun onUserInteraction() {
        super.onUserInteraction()
        if (!isInputLocked) {
            resetDimTimer()
        }
    }

    // --- (NEW) Lifecycle for timers and WakeLock ---
    override fun onResume() {
        super.onResume()
        resetDimTimer()
        sendCam { it.action = "onResume" }
    }

    override fun onPause() {
        super.onPause()
        stopDimTimer()
        sendCam { it.action = "onPause" }
        // Don't release the WakeLock here, the service handles it
    }

    // --- onKeyDown FUNCTION (UPDATED) ---
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isInputLocked) {
            return true
        }
        // Load the setting
        val action = SettingsManager.loadVolumeAction(this)

        // If the action is disabled, let the system handle it
        if (action == SettingsManager.VOL_ACTION_OFF) {
            return super.onKeyDown(keyCode, event)
        }

        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                when (action) {
                    SettingsManager.VOL_ACTION_ZOOM -> sendCam { it.action = "volume_zoom_in" }
                    SettingsManager.VOL_ACTION_SWITCH_CAM -> sendCam { it.action = "volume_action_switch_cam" }
                    SettingsManager.VOL_ACTION_TOGGLE_FLASH -> sendCam { it.action = "volume_action_toggle_flash" }
                }
                return true // Consume the event
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                when (action) {
                    SettingsManager.VOL_ACTION_ZOOM -> sendCam { it.action = "volume_zoom_out" }
                    // For "Switch Camera" and "Flash", do the same action for both buttons
                    SettingsManager.VOL_ACTION_SWITCH_CAM -> sendCam { it.action = "volume_action_switch_cam" }
                    SettingsManager.VOL_ACTION_TOGGLE_FLASH -> sendCam { it.action = "volume_action_toggle_flash" }
                }
                return true // Consume the event
            }
        }

        // If it's not a handled key, let the system handle it
        return super.onKeyDown(keyCode, event)
    }
    // --- END OF UPDATE ---


    override fun onDestroy() {
        unregisterReceiver(killReceiver)
        if (!isRestarting) {
            sendCam { it.action = "KILL" }
        }
        stopDimTimer()
        super.onDestroy()
    }
}
