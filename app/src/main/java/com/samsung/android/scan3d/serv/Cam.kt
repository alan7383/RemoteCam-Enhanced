package com.samsung.android.scan3d.serv

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.view.Surface
import androidx.core.app.NotificationCompat
import com.samsung.android.scan3d.CameraActivity
import com.samsung.android.scan3d.R
import com.samsung.android.scan3d.ViewState
import com.samsung.android.scan3d.http.HttpService
import com.samsung.android.scan3d.util.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class Cam : Service(), CoroutineScope {
    var engine: CamEngine? = null
    var http: HttpService? = null
    // Renamed to follow Kotlin property naming conventions.
    private val channelId = "REMOTE_CAM"
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("CAM", "onStartCommand " + intent?.action)

        if (intent == null) return START_STICKY

        if (http == null && intent.action != "start") {
            Log.w("CAM", "Service not yet initialized (http=null). Command '${intent.action}' ignored.")
            return START_STICKY
        }

        when (intent.action) {
            "start" -> {
                if (http == null) {
                    val channel = NotificationChannel(
                        channelId,
                        channelId,
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                    channel.description = "RemoteCam run"
                    val notificationManager = getSystemService(NotificationManager::class.java)
                    notificationManager.createNotificationChannel(channel)

                    val notificationIntent = Intent(this, CameraActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(
                        this,
                        System.currentTimeMillis().toInt(),
                        notificationIntent,
                        FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
                    )

                    val intentKill = Intent(this, Cam::class.java)
                    intentKill.action = "KILL"

                    val pendingIntentKill = PendingIntent.getService(
                        this,
                        System.currentTimeMillis().toInt(),
                        intentKill,
                        FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
                    )

                    val builder =
                        NotificationCompat.Builder(this, channelId)
                            .setContentTitle(getString(R.string.notif_title))
                            .setContentText(getString(R.string.notif_text))
                            .setOngoing(true)
                            .setSmallIcon(R.drawable.ic_linked_camera)
                            .addAction(R.drawable.ic_close, getString(R.string.notif_kill), pendingIntentKill)
                            .setContentIntent(pendingIntent)

                    val notification: Notification = builder.build()

                    startForeground(123, notification)

                    http = HttpService(this)
                    engine?.http = http

                    launch(Dispatchers.IO) {
                        // Correct method to start the http service is start(), not main().
                        http?.start()
                    }
                }
            }

            "onPause" -> {
                val allowBackground = SettingsManager.loadBackgroundStreaming(this)

                if (!allowBackground) {
                    engine?.insidePause = true
                    Log.i("CAM", "onPause: Background streaming disabled, pausing.")
                } else {
                    engine?.insidePause = false
                    Log.i("CAM", "onPause: Background streaming enabled, continuing.")
                }
            }

            "onResume" -> {
                engine?.insidePause = false
                Log.i("CAM", "onResume: Resuming.")
            }

            "start_engine_with_surface" -> {
                // Using modern, type-safe getParcelable since minSdk is >= 33.
                val surface: Surface? = intent.extras?.getParcelable("surface", Surface::class.java)

                engine?.let {
                    if (it.insidePause) {
                        Log.i("CAM", "New surface received, forcing insidePause to false.")
                        it.insidePause = false
                    }
                }

                if (engine == null) {
                    Log.i("CAM", "CamEngine does not exist, creating...")
                    val targetFps = SettingsManager.loadTargetFps(this)
                    val antiFlicker = SettingsManager.loadAntiFlickerMode(this)
                    val noiseReduction = SettingsManager.loadNoiseReductionMode(this)
                    val stabilizationOff = SettingsManager.loadStabilizationOff(this)

                    engine = CamEngine(
                        context = this,
                        targetFps = targetFps,
                        currentAntiFlickerMode = antiFlicker,
                        currentNoiseReductionMode = noiseReduction,
                        isStabilizationOff = stabilizationOff
                    )
                    engine?.http = http
                }

                engine?.previewSurface = surface
                engine?.initializeCamera()
            }

            "new_view_state" -> {
                engine?.let { eng ->
                    val old = eng.viewState
                    // Using modern, type-safe getParcelable. The non-null assertion is kept from original code.
                    val new: ViewState = intent.extras?.getParcelable("data", ViewState::class.java)!!
                    eng.viewState = new

                    if (old.cameraId != new.cameraId ||
                        old.resolutionIndex != new.resolutionIndex ||
                        old.preview != new.preview)
                    {
                        Log.i("CAM", "Major change detected, restarting CamEngine.")
                        eng.restart()
                    }
                    else if (old.flash != new.flash || old.quality != new.quality) {
                        Log.i("CAM", "Minor change detected, updating request.")
                        eng.updateRepeatingRequest()
                    }
                }
            }

            "preview_surface_destroyed" -> {
                engine?.previewSurface = null
                if (engine?.isShowingPreview == true) {
                    engine?.restart()
                }
            }

            "scale_zoom" -> {
                val scale = intent.getFloatExtra("scale_factor", 1.0f)
                engine?.scaleZoom(scale)
            }

            "set_zoom_ratio" -> {
                val ratio = intent.getFloatExtra("ratio", 0.0f)
                engine?.setZoomRatio(ratio)
            }

            "volume_zoom_in" -> {
                engine?.stepZoomIn()
            }
            "volume_zoom_out" -> {
                engine?.stepZoomOut()
            }
            "volume_action_switch_cam" -> {
                engine?.switchToNextCamera()
            }
            "volume_action_toggle_flash" -> {
                engine?.toggleFlash()
            }

            "set_target_fps" -> {
                val fps = intent.getIntExtra("fps", 30)
                engine?.setTargetFps(fps)
            }

            "double_tap_action_switch_camera" -> {
                engine?.switchToNextCamera()
            }

            "double_tap_action_toggle_zoom" -> {
                engine?.toggleZoom()
            }

            "set_anti_flicker" -> {
                val mode = intent.getIntExtra("mode", SettingsManager.ANTI_FLICKER_AUTO)
                engine?.setAntiFlickerMode(mode)
            }

            "set_noise_reduction" -> {
                val mode = intent.getIntExtra("mode", SettingsManager.NR_AUTO)
                engine?.setNoiseReductionMode(mode)
            }

            "set_stabilization" -> {
                val isOff = intent.getBooleanExtra("is_off", false)
                engine?.setStabilizationOff(isOff)
            }

            "set_zoom_smoothing" -> {
                val delay = intent.getIntExtra("delay", SettingsManager.SMOOTH_DELAY_NONE)
                engine?.setZoomSmoothingDelay(delay)
            }

            "set_http_port" -> {
                val newPort = intent.getIntExtra("port", SettingsManager.DEFAULT_PORT)
                http?.restartServer(newPort)

                val uiIntent = Intent("PORT_UPDATED").setPackage(packageName)
                sendBroadcast(uiIntent)
                engine?.restart()
            }

            "KILL", "stop" -> {
                kill()
            }

            else -> {
                Log.w("CAM", "Unknown or unhandled action: ${intent.action}")
            }
        }

        return START_STICKY
    }

    fun kill(){
        engine?.destroy()
        engine = null
        // Call the new public stop() method on the http service.
        http?.stop()
        http = null

        val intent = Intent("KILL_ACTIVITY").setPackage(packageName)
        sendBroadcast(intent)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("CAM", "OnDestroy")
        job.cancel()
        kill()
    }

    // Removed unused companion object.
}
