package com.samsung.android.scan3d.serv

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Parcelable
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import com.samsung.android.scan3d.ViewState
import com.samsung.android.scan3d.http.HttpService
import com.samsung.android.scan3d.util.Selector
import com.samsung.android.scan3d.util.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.parcelize.Parcelize
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.Executor

class CamEngine(
    val context: Context,
    private var targetFps: Int,
    private var currentAntiFlickerMode: Int,
    private var currentNoiseReductionMode: Int,
    private var isStabilizationOff: Boolean
) {

    var http: HttpService? = null
    var resW = 1280
    var resH = 720

    var insidePause = false
    var isShowingPreview: Boolean = false

    private var cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraList: List<Selector.SensorDesc> = Selector.enumerateCameras(cameraManager)

    val camOutPutFormat = ImageFormat.JPEG

    @Volatile
    private var isInitializing = false
    @Volatile
    private var restartPending = false
    @Volatile
    private var isShuttingDown = false

    private var lastQuickUpdateTime = 0L
    private val quickUpdateIntervalMs = 1200L

    private var currentSmoothingDelay: Int = SettingsManager.loadZoomSmoothingDelay(context)
    private var zoomAnimatorJob: Job? = null
    private val zoomAnimationSteps = 10

    // --- H.264 / Codec Variables ---
    private var mediaCodec: MediaCodec? = null
    private var codecInputSurface: Surface? = null
    private val codecBufferInfo = MediaCodec.BufferInfo()
    private var codecJob: Job? = null
    private var h264ConfigData: ByteArray? = null

    private var minZoom: Float = 1.0f
    private var maxZoom: Float = 1.0f
    private var currentZoomRatio: Float = 1.0f
    private var useZoomRatioApi: Boolean = false

    private lateinit var activeArraySize: Rect
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var sessionCallback: CameraCaptureSession.CaptureCallback? = null
    private var hasFlash: Boolean = false
    private var maxFlashLevel: Int = 1
    private lateinit var fpsRanges: Array<Range<Int>>
    private var availableAntiFlickerModes: IntArray = intArrayOf()
    private var availableNoiseReductionModes: IntArray = intArrayOf()
    private var availableOisModes: IntArray = intArrayOf()
    private var availableEisModes: IntArray = intArrayOf()
    private lateinit var characteristics: CameraCharacteristics
    private lateinit var sizes: List<Size>
    private var sensorOrientation: Int = 0

    private val defaultCameraId = cameraList.firstOrNull()?.cameraId ?: "0"
    var viewState: ViewState = SettingsManager.loadViewState(context, defaultCameraId)

    private lateinit var imageReader: ImageReader
    private val cameraThread = HandlerThread("CameraThread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)
    private val cameraScope = CoroutineScope(cameraHandler.asCoroutineDispatcher())
    private lateinit var camera: CameraDevice
    var previewSurface: Surface? = null
    private var session: CameraCaptureSession? = null

    private fun stopRunning() {
        zoomAnimatorJob?.cancel()
        codecJob?.cancel()

        try {
            mediaCodec?.stop()
            mediaCodec?.release()
            mediaCodec = null
            codecInputSurface = null
        } catch (e: Exception) {}

        if (session != null) {
            try {
                session!!.stopRepeating()
                session!!.close()
            } catch (e: Exception) {}
            session = null
        }
        try {
            if (::camera.isInitialized) camera.close()
            if (::imageReader.isInitialized) imageReader.close()
        } catch (e: Exception) {}
    }

    fun restart(disconnectClients: Boolean = false) {
        if (isShuttingDown) return
        cameraScope.launch {
            if (isInitializing) {
                restartPending = true
                return@launch
            }
            isInitializing = true
            try {
                if (disconnectClients) http?.disconnectClients()
                stopRunning()
                initializeCameraInternal()
            } catch (e: Exception) {
                Log.e("camengine", "failed to restart camera", e)
            } finally {
                isInitializing = false
                if (restartPending) {
                    restartPending = false
                    restart(disconnectClients)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun openCamera(manager: CameraManager, cameraId: String, handler: Handler? = null): CameraDevice = suspendCancellableCoroutine { cont ->
        try {
            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    if (isShuttingDown || !cont.context.isActive) {
                        device.close()
                        return
                    }
                    if (cont.context.isActive) cont.resume(device)
                }
                override fun onDisconnected(device: CameraDevice) {
                    try { device.close() } catch(_: Exception){}
                }
                override fun onError(device: CameraDevice, error: Int) {
                    val exc = RuntimeException("camera $cameraId error: $error")
                    if (cont.context.isActive) cont.resumeWithException(exc)
                }
            }, handler)
        } catch (e: Exception) {
            if (cont.context.isActive) cont.resumeWithException(e)
        }
    }

    private suspend fun createCaptureSession(device: CameraDevice, targets: List<Surface>, handler: Handler? = null): CameraCaptureSession = suspendCoroutine { cont ->
        val stateCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                if (isShuttingDown || !cont.context.isActive) {
                    session.close()
                    return
                }
                if (cont.context.isActive) cont.resume(session)
            }
            override fun onConfigureFailed(session: CameraCaptureSession) {
                val exc = RuntimeException("camera session configuration failed")
                if (cont.context.isActive) cont.resumeWithException(exc)
            }
        }
        try {
            val outputConfigs = targets.map { OutputConfiguration(it) }
            val executor = Executor { runnable -> handler?.post(runnable) ?: runnable.run() }
            val sessionConfig = SessionConfiguration(SessionConfiguration.SESSION_REGULAR, outputConfigs, executor, stateCallback)
            device.createCaptureSession(sessionConfig)
        } catch (e: Exception) {
            if (cont.context.isActive) cont.resumeWithException(e)
        }
    }

    private suspend fun initializeCameraInternal() {
        if (isShuttingDown) return
        stopRunning()

        if (cameraList.none { it.cameraId == viewState.cameraId }) {
            viewState = viewState.copy(cameraId = defaultCameraId)
            SettingsManager.saveSettings(context, viewState)
        }

        try {
            characteristics = cameraManager.getCameraCharacteristics(viewState.cameraId)
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
            sizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(camOutPutFormat).reversed()
            hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

            // --- RESTAURATION LOGIQUE VERSION 1 / FLASHDIM ---
            // Ensure that if API >= 33, we request max power from the system.
            maxFlashLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
            } else {
                1
            }

            fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) ?: arrayOf()
            availableAntiFlickerModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES) ?: intArrayOf()
            availableNoiseReductionModes = characteristics.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES) ?: intArrayOf()
            availableOisModes = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION) ?: intArrayOf()
            availableEisModes = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES) ?: intArrayOf()
            activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)!!

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val zoomRatioRange = characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
                minZoom = zoomRatioRange?.lower ?: 1.0f
                maxZoom = zoomRatioRange?.upper ?: (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f)
                useZoomRatioApi = (zoomRatioRange != null)
            } else {
                useZoomRatioApi = false
                minZoom = 1.0f
                maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
            }
        } catch (e: Exception) { return }

        if (sizes.isEmpty()) return
        val selectedSize = sizes[viewState.resolutionIndex ?: (sizes.size - 1)]
        resW = selectedSize.width
        resH = selectedSize.height

        val showLiveSurface = viewState.preview && !insidePause && previewSurface != null
        isShowingPreview = showLiveSurface

        try { camera = openCamera(cameraManager, viewState.cameraId, cameraHandler) } catch (e: Exception) { return }

        var targets = mutableListOf<Surface>()
        if (showLiveSurface && previewSurface?.isValid == true) targets.add(previewSurface!!)

        if (viewState.streamFormat == SettingsManager.FORMAT_H264) {
            try {
                val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, resW, resH)
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                format.setInteger(MediaFormat.KEY_BIT_RATE, viewState.h264Bitrate * 1_000_000)
                format.setInteger(MediaFormat.KEY_FRAME_RATE, targetFps)
                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

                val mode = if (viewState.h264Mode == SettingsManager.H264_MODE_VBR) MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR else MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR
                format.setInteger(MediaFormat.KEY_BITRATE_MODE, mode)

                format.setInteger(MediaFormat.KEY_PRIORITY, 0)
                format.setInteger(MediaFormat.KEY_OPERATING_RATE, Short.MAX_VALUE.toInt())
                format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / targetFps)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) format.setInteger(MediaFormat.KEY_LATENCY, 1)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) format.setInteger(MediaFormat.KEY_MAX_B_FRAMES, 0)

                mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                codecInputSurface = mediaCodec?.createInputSurface()
                mediaCodec?.start()
                targets.add(codecInputSurface!!)
            } catch (e: Exception) {
                mediaCodec?.release(); mediaCodec = null; codecInputSurface = null
            }
        }

        if (viewState.streamFormat != SettingsManager.FORMAT_H264 || codecInputSurface == null) {
            imageReader = ImageReader.newInstance(resW, resH, camOutPutFormat, 4)
            targets.add(imageReader.surface)
        }

        try { session = createCaptureSession(camera, targets, cameraHandler) } catch (e: Exception) { return }

        captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        targets.forEach { captureRequestBuilder!!.addTarget(it) }

        applyFlash(captureRequestBuilder!!)
        applyZoom(captureRequestBuilder!!)
        applyFps(captureRequestBuilder!!)
        applyStabilization(captureRequestBuilder!!)
        applyNoiseReduction(captureRequestBuilder!!)
        applyAntiFlicker(captureRequestBuilder!!)
        captureRequestBuilder!!.set(CaptureRequest.JPEG_QUALITY, viewState.quality.toByte())

        var lastTimeMjpeg = System.currentTimeMillis()
        val acquired = AtomicInteger(0)
        sessionCallback = object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                if (viewState.streamFormat == SettingsManager.FORMAT_H264) return
                var img: android.media.Image? = try { imageReader.acquireNextImage() } catch (e: Exception) { null }
                if (acquired.get() > 1 && img != null) { img.close(); img = null }
                val image = img ?: return
                acquired.incrementAndGet()
                val now = System.currentTimeMillis()
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }
                if (viewState.stream && !insidePause) http?.sendFrame(bytes)
                image.close(); acquired.decrementAndGet()
                if (now - lastQuickUpdateTime > quickUpdateIntervalMs) {
                    val rate = (bytes.size.toLong() * 1000) / ((now - lastTimeMjpeg).coerceAtLeast(1) * 1024)
                    updateViewQuick(DataQuick(ms = (now - lastTimeMjpeg).toInt(), rateKbs = rate.toInt()))
                    lastQuickUpdateTime = now; lastTimeMjpeg = now
                }
            }
        }

        session?.setRepeatingRequest(captureRequestBuilder!!.build(), sessionCallback!!, cameraHandler)
        if (viewState.streamFormat == SettingsManager.FORMAT_H264) startH264EncoderLoop()
        updateView()
    }

    private fun startH264EncoderLoop() {
        var lastTimeH264 = System.currentTimeMillis()
        var bytesAccumulated = 0
        codecJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && mediaCodec != null) {
                try {
                    val index = mediaCodec?.dequeueOutputBuffer(codecBufferInfo, 10000) ?: -1
                    if (index >= 0) {
                        val buffer = mediaCodec?.getOutputBuffer(index)
                        if (buffer != null) {
                            val bytes = ByteArray(codecBufferInfo.size)
                            buffer.get(bytes)
                            if ((codecBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                h264ConfigData = bytes
                            } else {
                                val isKey = (codecBufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
                                val data = if (isKey && h264ConfigData != null) h264ConfigData!! + bytes else bytes
                                if (viewState.stream && !insidePause) http?.sendH264Frame(data, isKey)
                                bytesAccumulated += bytes.size
                                val now = System.currentTimeMillis()
                                if (now - lastTimeH264 > quickUpdateIntervalMs) {
                                    updateViewQuick(DataQuick(ms = 0, rateKbs = (bytesAccumulated * 1000L / ((now - lastTimeH264) * 1024L)).toInt()))
                                    lastTimeH264 = now; bytesAccumulated = 0
                                }
                            }
                        }
                        mediaCodec?.releaseOutputBuffer(index, false)
                    }
                } catch (e: Exception) { }
            }
        }
    }

    fun setStreamFormat(f: Int) { if (viewState.streamFormat != f) { viewState = viewState.copy(streamFormat = f); SettingsManager.saveStreamFormat(context, f); restart(true) } }
    fun setH264Bitrate(b: Int) { if (viewState.h264Bitrate != b) { viewState = viewState.copy(h264Bitrate = b); SettingsManager.saveH264Bitrate(context, b); restart(false) } }
    fun setH264Mode(m: Int) { if (viewState.h264Mode != m) { viewState = viewState.copy(h264Mode = m); SettingsManager.saveH264Mode(context, m); restart(false) } }

    private fun applyZoom(builder: CaptureRequest.Builder) {
        if (useZoomRatioApi && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.set(CaptureRequest.CONTROL_ZOOM_RATIO, currentZoomRatio)
        } else {
            if (!::activeArraySize.isInitialized) return
            val zW = (activeArraySize.width() / currentZoomRatio.coerceAtLeast(1f)).toInt()
            val zH = (activeArraySize.height() / currentZoomRatio.coerceAtLeast(1f)).toInt()
            builder.set(CaptureRequest.SCALER_CROP_REGION, Rect(activeArraySize.centerX() - zW/2, activeArraySize.centerY() - zH/2, activeArraySize.centerX() + zW/2, activeArraySize.centerY() + zH/2))
        }
    }

    fun setZoomRatio(r: Float) {
        val t = minZoom + (maxZoom - minZoom) * r.coerceIn(0f, 1f)
        if (t == currentZoomRatio || isShuttingDown) return
        currentZoomRatio = t; applyZoom(captureRequestBuilder ?: return)
        try { session?.setRepeatingRequest(captureRequestBuilder!!.build(), sessionCallback!!, cameraHandler); SettingsManager.saveZoomRatio(context, currentZoomRatio); updateView() } catch (e: Exception) {}
    }

    fun scaleZoom(s: Float) {
        // Calculate new theoretical zoom
        var nextZoom = (currentZoomRatio * s).coerceIn(minZoom, maxZoom)

        // --- LOGIQUE MAGNETIQUE ---
        // If approaching 1.0x with fingers (threshold of 0.08x)
        if (kotlin.math.abs(nextZoom - 1.0f) < 0.08f) {
            nextZoom = 1.0f
        }

        if (nextZoom != currentZoomRatio) {
            currentZoomRatio = nextZoom
            applyZoom(captureRequestBuilder ?: return)
            try {
                session?.setRepeatingRequest(captureRequestBuilder!!.build(), sessionCallback!!, cameraHandler)
                updateView()
            } catch (e: Exception) {
                Log.e("CamEngine", "Failed to update zoom on scale: ${e.message}")
            }
        }
    }
    fun stepZoomIn() { animateZoom((currentZoomRatio + (maxZoom - minZoom) * 0.1f).coerceIn(minZoom, maxZoom)) }
    fun stepZoomOut() { animateZoom((currentZoomRatio - (maxZoom - minZoom) * 0.1f).coerceIn(minZoom, maxZoom)) }

    private fun animateZoom(t: Float) {
        zoomAnimatorJob?.cancel()
        if (currentSmoothingDelay == 0) { currentZoomRatio = t; applyZoom(captureRequestBuilder ?: return); session?.setRepeatingRequest(captureRequestBuilder!!.build(), sessionCallback!!, cameraHandler); updateView(); return }
        zoomAnimatorJob = cameraScope.launch {
            val s = currentZoomRatio
            for (i in 1..zoomAnimationSteps) {
                if (!isActive) break
                currentZoomRatio = s + (t - s) * (i.toFloat() / zoomAnimationSteps)
                applyZoom(captureRequestBuilder ?: break)
                session?.setRepeatingRequest(captureRequestBuilder!!.build(), sessionCallback!!, cameraHandler)
                delay(currentSmoothingDelay.toLong())
            }
            updateView()
        }
    }

    private fun applyFlash(b: CaptureRequest.Builder) {
        if (!hasFlash) return
        // On active le mode TORCH classique
        b.set(CaptureRequest.FLASH_MODE, if (viewState.flash) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF)

        // If API 35+ (Android 15), use the new key.
        // For lower versions, rely on setFlashLevel() which will call CameraManager.
        if (viewState.flash && Build.VERSION.SDK_INT >= 35 && maxFlashLevel > 1) {
            try {
                val k = CaptureRequest::class.java.getDeclaredField("FLASH_STRENGTH_LEVEL").get(null) as CaptureRequest.Key<Int>
                b.set(k, viewState.flashLevel)
            } catch (e: Exception) {}
        }
    }

    fun setFlashLevel(level: Int) {
        if (level == viewState.flashLevel || isShuttingDown) return

        viewState = viewState.copy(flashLevel = level)
        SettingsManager.saveSettings(context, viewState)

        if (Build.VERSION.SDK_INT >= 35) {
            // API 35+: FLASH_STRENGTH_LEVEL key is in applyFlash()
            updateRepeatingRequest()
        } else if (Build.VERSION.SDK_INT >= 33) {
            // API 33-34: direct control via CameraManager
            // NO updateRepeatingRequest() otherwise it overwrites the level!
            try {
                cameraManager.turnOnTorchWithStrengthLevel(viewState.cameraId, level)
            } catch (e: Exception) {
                Log.e("CamEngine", "Failed to set torch strength: ${e.message}")
            }
        }
        updateView()
    }

    private fun applyFps(b: CaptureRequest.Builder) { val s = fpsRanges.find { it.lower == targetFps && it.upper == targetFps } ?: fpsRanges.filter { it.upper >= targetFps && it.lower <= targetFps }.minByOrNull { it.upper }; if (s != null) b.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, s) }
    private fun applyStabilization(b: CaptureRequest.Builder) { val m = if (isStabilizationOff) CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF else CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON; if (availableEisModes.contains(m)) b.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, m) }
    private fun applyNoiseReduction(b: CaptureRequest.Builder) { val m = when(currentNoiseReductionMode) { SettingsManager.NR_OFF -> CaptureRequest.NOISE_REDUCTION_MODE_OFF; SettingsManager.NR_LOW -> CaptureRequest.NOISE_REDUCTION_MODE_FAST; SettingsManager.NR_HIGH -> CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY; else -> CaptureRequest.NOISE_REDUCTION_MODE_FAST }; if (availableNoiseReductionModes.contains(m)) b.set(CaptureRequest.NOISE_REDUCTION_MODE, m) }
    private fun applyAntiFlicker(b: CaptureRequest.Builder) { val m = when(currentAntiFlickerMode) { SettingsManager.ANTI_FLICKER_OFF -> CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_OFF; SettingsManager.ANTI_FLICKER_50HZ -> CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_50HZ; SettingsManager.ANTI_FLICKER_60HZ -> CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_60HZ; else -> CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO }; if (availableAntiFlickerModes.contains(m)) b.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, m) }

    fun updateRepeatingRequest() {
        if (session == null || captureRequestBuilder == null || !::camera.isInitialized || isShuttingDown) return
        applyFlash(captureRequestBuilder!!); applyNoiseReduction(captureRequestBuilder!!); applyAntiFlicker(captureRequestBuilder!!)
        captureRequestBuilder!!.set(CaptureRequest.JPEG_QUALITY, viewState.quality.toByte())
        try { session?.setRepeatingRequest(captureRequestBuilder!!.build(), sessionCallback!!, cameraHandler) } catch (e: Exception) {}
    }

    fun setTargetFps(f: Int) { targetFps = f; SettingsManager.saveTargetFps(context, f); restart(false) }
    fun setAntiFlickerMode(m: Int) { currentAntiFlickerMode = m; SettingsManager.saveAntiFlickerMode(context, m); updateRepeatingRequest() }
    fun setNoiseReductionMode(m: Int) { currentNoiseReductionMode = m; SettingsManager.saveNoiseReductionMode(context, m); updateRepeatingRequest() }
    fun setStabilizationOff(o: Boolean) { isStabilizationOff = o; SettingsManager.saveStabilizationOff(context, o); restart(false) }
    fun setZoomSmoothingDelay(d: Int) { currentSmoothingDelay = d; SettingsManager.saveZoomSmoothingDelay(context, d) }

    fun switchToNextCamera() {
        val idx = cameraList.indexOfFirst { it.cameraId == viewState.cameraId }
        viewState = viewState.copy(cameraId = cameraList[(idx + 1) % cameraList.size].cameraId, flash = false, resolutionIndex = null)
        currentZoomRatio = 1.0f; SettingsManager.saveZoomRatio(context, 1.0f); SettingsManager.saveSettings(context, viewState); restart(true)
    }

    fun toggleFlash() { viewState = viewState.copy(flash = !viewState.flash); SettingsManager.saveSettings(context, viewState); updateRepeatingRequest(); updateView() }
    fun toggleZoom() { animateZoom(if (currentZoomRatio in 0.9f..1.1f) 2.0f.coerceAtMost(maxZoom) else 1.0f) }

    fun updateView() {
        if (!::sizes.isInitialized || isShuttingDown) return
        val sensor = cameraList.find { it.cameraId == viewState.cameraId } ?: return
        val data = Data(cameraList, sensor, sizes.map { ParcelableSize(it.width, it.height) }, viewState.resolutionIndex ?: (sizes.size - 1), currentZoomRatio, minZoom, maxZoom, hasFlash, maxFlashLevel, viewState.quality, viewState.flash, viewState.flashLevel, sensorOrientation)
        context.sendBroadcast(Intent("UpdateFromCameraEngine").setPackage(context.packageName).putExtra("data", data))
    }

    fun updateViewQuick(dq: DataQuick) {
        if (isShuttingDown) return
        context.sendBroadcast(Intent("UpdateFromCameraEngine").setPackage(context.packageName).putExtra("dataQuick", dq))
    }

    fun initializeCamera() { restart(false) }
    fun destroy() { isShuttingDown = true; cameraScope.launch { stopRunning(); cameraThread.quitSafely() } }

    companion object {
        @Parcelize data class ParcelableSize(val width: Int, val height: Int) : Parcelable { override fun toString() = "$width x $height" }
        @Parcelize data class Data(val sensors: List<Selector.SensorDesc>, val sensorSelected: Selector.SensorDesc, val resolutions: List<ParcelableSize>, val resolutionSelected: Int, val currentZoom: Float, val minZoom: Float, val maxZoom: Float, val hasFlash: Boolean, val maxFlashLevel: Int, val quality: Int, val flashState: Boolean, val flashLevel: Int, val sensorOrientation: Int) : Parcelable
        @Parcelize data class DataQuick(val ms: Int, val rateKbs: Int) : Parcelable
    }
}