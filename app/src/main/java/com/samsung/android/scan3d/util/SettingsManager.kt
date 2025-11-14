package com.samsung.android.scan3d.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.samsung.android.scan3d.ViewState

object SettingsManager {

    private const val PREFS_NAME = "RemoteCamSettings"

// =========================================================
// ============== PREFERENCE KEYS ==========================
// =========================================================

    // --- ViewState Keys ---
    private const val KEY_PREVIEW = "preview"
    private const val KEY_STREAM = "stream"
    private const val KEY_CAMERA_ID = "cameraId"
    private const val KEY_RESOLUTION_INDEX = "resolutionIndex"
    private const val KEY_QUALITY = "quality"
    private const val KEY_FLASH = "flash_enabled"
    private const val KEY_ZOOM_RATIO = "zoom_ratio"

    // --- General Keys ---
    private const val KEY_THEME = "theme_mode"
    private const val KEY_MONET = "monet_enabled"
    private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    private const val KEY_LANGUAGE = "language_code"

    // --- Remember Settings Keys ---
    private const val KEY_REMEMBER_SETTINGS_ENABLED = "remember_settings"
    private const val KEY_REMEMBER_FLASH = "remember_flash"
    private const val KEY_REMEMBER_ZOOM = "remember_zoom"
    private const val KEY_REMEMBER_SENSOR = "remember_sensor"
    private const val KEY_REMEMBER_RESOLUTION = "remember_resolution"
    private const val KEY_REMEMBER_QUALITY = "remember_quality"

    // --- Advanced Keys ---
    private const val KEY_TARGET_FPS = "target_fps"
    private const val KEY_DOUBLE_TAP_ACTION = "double_tap_action"
    private const val KEY_STABILIZATION_OFF = "stabilization_off"
    private const val KEY_ANTI_FLICKER_MODE = "anti_flicker_mode"
    private const val KEY_NOISE_REDUCTION_MODE = "noise_reduction_mode"

    // --- Controls Key ---
    private const val KEY_VOLUME_ACTION = "volume_action"

    // --- (NEW) Power and Screen Keys ---
    // KEY_IGNORE_OPTIMIZATIONS removed
    private const val KEY_AUTO_DIM_DELAY = "auto_dim_delay"
    private const val KEY_LOCK_INPUT_ON_DIM = "lock_input_on_dim"
    private const val KEY_BACKGROUND_STREAMING = "background_streaming"
    private const val KEY_ALLOW_RECONNECTS = "allow_reconnects"

    // --- (NEW) Zoom Smoothing Key ---
    private const val KEY_ZOOM_SMOOTHING_DELAY = "zoom_smoothing_delay"

    // --- (NEW) Network Key ---
    private const val KEY_HTTP_PORT = "http_port"


// =========================================================
// ============== CONSTANTS ================================
// =========================================================

    // --- Theme Constants ---
    const val THEME_AUTO = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2

    // --- Language Constants ---
    const val LANG_AUTO = "auto"
    const val LANG_EN = "en"
    const val LANG_FR = "fr"
    const val LANG_HU = "hu"

    // --- Action Constants ---
    const val DOUBLE_TAP_OFF = 0
    const val DOUBLE_TAP_SWITCH_CAM = 1
    const val DOUBLE_TAP_TOGGLE_ZOOM = 2

    // --- Advanced Constants ---
    const val ANTI_FLICKER_AUTO = 0
    const val ANTI_FLICKER_OFF = 1
    const val ANTI_FLICKER_50HZ = 2
    const val ANTI_FLICKER_60HZ = 3

    const val NR_AUTO = 0
    const val NR_OFF = 1
    const val NR_LOW = 2
    const val NR_HIGH = 3

    // --- NEW CONSTANTS (Controls) ---
    const val VOL_ACTION_OFF = 0
    const val VOL_ACTION_ZOOM = 1
    const val VOL_ACTION_SWITCH_CAM = 2
    const val VOL_ACTION_TOGGLE_FLASH = 3

    const val DIM_DELAY_OFF = 0
    const val DIM_DELAY_45S = 45000
    const val DIM_DELAY_1M = 60000
    const val DIM_DELAY_90S = 90000
    const val DIM_DELAY_2M = 120000
    const val DIM_DELAY_3M = 180000
    const val DIM_DELAY_5M = 300000

    const val SMOOTH_DELAY_NONE = 0
    const val SMOOTH_DELAY_5 = 5
    const val SMOOTH_DELAY_8 = 8
    const val SMOOTH_DELAY_10 = 10
    const val SMOOTH_DELAY_15 = 15
    const val SMOOTH_DELAY_20 = 20
    const val SMOOTH_DELAY_25 = 25
    const val SMOOTH_DELAY_30 = 30
    const val SMOOTH_DELAY_40 = 40
    const val SMOOTH_DELAY_50 = 50

    // --- (NEW) Network Constant ---
    const val DEFAULT_PORT = 8080

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Saves the camera settings state (ViewState)
     * Called on every change in CameraScreen (via sendViewState)
     */
    fun saveSettings(context: Context, viewState: ViewState) {
        val rememberSettings = loadRememberSettings(context)

        getPrefs(context).edit {
            // Only saves if "Remember Settings" is enabled
            if (rememberSettings) {
                putBoolean(KEY_PREVIEW, viewState.preview)
                putBoolean(KEY_STREAM, viewState.stream)

                if (loadRememberSensor(context)) {
                    putString(KEY_CAMERA_ID, viewState.cameraId)
                }
                if (loadRememberResolution(context)) {
                    putInt(KEY_RESOLUTION_INDEX, viewState.resolutionIndex ?: -1)
                }
                if (loadRememberQuality(context)) {
                    putInt(KEY_QUALITY, viewState.quality)
                }
                if (loadRememberFlash(context)) {
                    putBoolean(KEY_FLASH, viewState.flash)
                }
                // Zoom is handled by its own saveZoomRatio function
            }
        }
    }

    /**
     * Loads the camera settings state (ViewState)
     * Called when CameraScreen starts.
     */
    fun loadViewState(context: Context, defaultCameraId: String): ViewState {
        val prefs = getPrefs(context)
        val rememberSettings = loadRememberSettings(context)

        // Default values (used if "rememberSettings" is disabled)
        val defaultPreview = true
        val defaultStream = false
        val defaultQuality = 80
        val defaultFlash = false

        val preview = if (rememberSettings) prefs.getBoolean(KEY_PREVIEW, defaultPreview) else defaultPreview
        val stream = if (rememberSettings) prefs.getBoolean(KEY_STREAM, defaultStream) else defaultStream

        val cameraId = if (rememberSettings && loadRememberSensor(context)) {
            prefs.getString(KEY_CAMERA_ID, defaultCameraId)!!
        } else {
            defaultCameraId
        }

        val quality = if (rememberSettings && loadRememberQuality(context)) {
            prefs.getInt(KEY_QUALITY, defaultQuality)
        } else {
            defaultQuality
        }

        val resolutionIndexInt = if (rememberSettings && loadRememberResolution(context)) {
            prefs.getInt(KEY_RESOLUTION_INDEX, -1)
        } else {
            -1
        }
        val resolutionIndex = if (resolutionIndexInt == -1) null else resolutionIndexInt

        val flash = if (rememberSettings && loadRememberFlash(context)) {
            prefs.getBoolean(KEY_FLASH, defaultFlash)
        } else {
            defaultFlash
        }

        return ViewState(
            preview = preview,
            stream = stream,
            cameraId = cameraId,
            resolutionIndex = resolutionIndex,
            quality = quality,
            flash = flash
        )
    }

    // --- Theme & Appearance Functions ---
    fun saveThemeMode(context: Context, mode: Int) {
        getPrefs(context).edit { putInt(KEY_THEME, mode) }
    }
    fun loadThemeMode(context: Context): Int {
        return getPrefs(context).getInt(KEY_THEME, THEME_AUTO)
    }
    fun saveMonetEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_MONET, enabled) }
    }
    fun loadMonetEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_MONET, true)
    }
    fun saveKeepScreenOn(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_KEEP_SCREEN_ON, enabled) }
    }
    fun loadKeepScreenOn(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_KEEP_SCREEN_ON, false)
    }

    // --- Language Functions ---
    fun saveLanguage(context: Context, languageCode: String) {
        getPrefs(context).edit { putString(KEY_LANGUAGE, languageCode) }
    }
    fun loadLanguage(context: Context): String {
        return getPrefs(context).getString(KEY_LANGUAGE, LANG_AUTO) ?: LANG_AUTO
    }

    // --- Camera Functions (Remember Flags) ---
    fun saveRememberSettings(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_REMEMBER_SETTINGS_ENABLED, enabled) }
    }
    fun loadRememberSettings(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_REMEMBER_SETTINGS_ENABLED, true)
    }
    fun saveRememberFlash(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_REMEMBER_FLASH, enabled) }
    }
    fun loadRememberFlash(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_REMEMBER_FLASH, true)
    }
    fun saveRememberZoom(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_REMEMBER_ZOOM, enabled) }
    }
    fun loadRememberZoom(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_REMEMBER_ZOOM, true)
    }
    fun saveRememberSensor(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_REMEMBER_SENSOR, enabled) }
    }
    fun loadRememberSensor(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_REMEMBER_SENSOR, true)
    }
    fun saveRememberResolution(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_REMEMBER_RESOLUTION, enabled) }
    }
    fun loadRememberResolution(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_REMEMBER_RESOLUTION, true)
    }
    fun saveRememberQuality(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_REMEMBER_QUALITY, enabled) }
    }
    fun loadRememberQuality(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_REMEMBER_QUALITY, true)
    }

    // --- Functions for current values (Zoom/Exposure) ---
    fun saveZoomRatio(context: Context, ratio: Float) {
        if(loadRememberSettings(context) && loadRememberZoom(context)) {
            getPrefs(context).edit { putFloat(KEY_ZOOM_RATIO, ratio) }
        }
    }
    fun loadZoomRatio(context: Context): Float {
        if(loadRememberSettings(context) && loadRememberZoom(context)) {
            return getPrefs(context).getFloat(KEY_ZOOM_RATIO, 1.0f)
        }
        return 1.0f
    }
// --- End Zoom/Exposure ---

    fun saveTargetFps(context: Context, fps: Int) {
        getPrefs(context).edit { putInt(KEY_TARGET_FPS, fps) }
    }
    fun loadTargetFps(context: Context): Int {
        return getPrefs(context).getInt(KEY_TARGET_FPS, 30)
    }
    fun saveDoubleTapAction(context: Context, action: Int) {
        getPrefs(context).edit { putInt(KEY_DOUBLE_TAP_ACTION, action) }
    }
    fun loadDoubleTapAction(context: Context): Int {
        return getPrefs(context).getInt(KEY_DOUBLE_TAP_ACTION, DOUBLE_TAP_OFF)
    }

    // --- Advanced Functions ---
    fun saveStabilizationOff(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_STABILIZATION_OFF, enabled) }
    }
    fun loadStabilizationOff(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_STABILIZATION_OFF, false)
    }
    fun saveAntiFlickerMode(context: Context, mode: Int) {
        getPrefs(context).edit { putInt(KEY_ANTI_FLICKER_MODE, mode) }
    }
    fun loadAntiFlickerMode(context: Context): Int {
        return getPrefs(context).getInt(KEY_ANTI_FLICKER_MODE, ANTI_FLICKER_AUTO)
    }
    fun saveNoiseReductionMode(context: Context, mode: Int) {
        getPrefs(context).edit { putInt(KEY_NOISE_REDUCTION_MODE, mode) }
    }
    fun loadNoiseReductionMode(context: Context): Int {
        return getPrefs(context).getInt(KEY_NOISE_REDUCTION_MODE, NR_AUTO)
    }

    // --- Control Functions (UPDATED) ---
    fun saveVolumeAction(context: Context, action: Int) {
        getPrefs(context).edit { putInt(KEY_VOLUME_ACTION, action) }
    }
    fun loadVolumeAction(context: Context): Int {
        // Defaults to "OFF" (Disabled)
        return getPrefs(context).getInt(KEY_VOLUME_ACTION, VOL_ACTION_OFF)
    }

// --- (NEW) Power and Screen Functions ---

    fun saveAutoDimDelay(context: Context, delayMs: Int) {
        getPrefs(context).edit { putInt(KEY_AUTO_DIM_DELAY, delayMs) }
    }
    fun loadAutoDimDelay(context: Context): Int {
        // Defaults to "Off" (0)
        return getPrefs(context).getInt(KEY_AUTO_DIM_DELAY, DIM_DELAY_OFF)
    }

    fun saveLockInputOnDim(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_LOCK_INPUT_ON_DIM, enabled) }
    }
    fun loadLockInputOnDim(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_LOCK_INPUT_ON_DIM, false)
    }

    fun saveBackgroundStreaming(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_BACKGROUND_STREAMING, enabled) }
    }
    fun loadBackgroundStreaming(context:Context): Boolean {
        // Enabled by default
        return getPrefs(context).getBoolean(KEY_BACKGROUND_STREAMING, true)
    }

    fun saveAllowReconnects(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_ALLOW_RECONNECTS, enabled) }
    }
    fun loadAllowReconnects(context: Context): Boolean {
        // Enabled by default (as the fix will be applied)
        return getPrefs(context).getBoolean(KEY_ALLOW_RECONNECTS, true)
    }
    // --- (NEW) Zoom Smoothing Functions ---
    fun saveZoomSmoothingDelay(context: Context, delayMs: Int) {
        getPrefs(context).edit { putInt(KEY_ZOOM_SMOOTHING_DELAY, delayMs) }
    }
    fun loadZoomSmoothingDelay(context: Context): Int {
        // Defaults to "None" (0ms)
        return getPrefs(context).getInt(KEY_ZOOM_SMOOTHING_DELAY, SMOOTH_DELAY_NONE)
    }

    // --- (NEW) Network Functions ---
    fun savePort(context: Context, port: Int) {
        getPrefs(context).edit { putInt(KEY_HTTP_PORT, port) }
    }
    fun loadPort(context: Context): Int {
        return getPrefs(context).getInt(KEY_HTTP_PORT, DEFAULT_PORT)
    }
}
