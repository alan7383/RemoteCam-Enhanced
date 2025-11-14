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

    private var lastQuickUpdateTime = 0L
    private val quickUpdateIntervalMs = 500L

    private var currentSmoothingDelay: Int = SettingsManager.loadZoomSmoothingDelay(context)
    private var zoomAnimatorJob: Job? = null
    private val zoomAnimationSteps = 10 // Number of steps for the animation

    // --- CAMERA VARIABLES (will be initialized later) ---
    private var maxZoom: Float = 1.0f
    private var currentZoomRatio: Float = 1.0f
    private lateinit var activeArraySize: Rect
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var sessionCallback: CameraCaptureSession.CaptureCallback? = null
    private var hasFlash: Boolean = false
    private lateinit var fpsRanges: Array<Range<Int>>
    private var zoomToggleState: Boolean = false
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
        if (session != null) {
            Log.i("CAMERA", "close")
            try {
                session!!.stopRepeating()
                session!!.close()
            } catch (e: Exception) {
                Log.e("CamEngine", "Error while closing session", e)
            }
            session = null
            try {
                if (::camera.isInitialized) camera.close()
                if (::imageReader.isInitialized) imageReader.close()
            } catch (e: Exception) {
                Log.e("CamEngine", "Error closing camera/imageReader", e)
            }
        }
    }

    fun restart() {
        cameraScope.launch {
            if (isInitializing) {
                Log.w("CamEngine", "Initialization in progress, queuing restart.")
                restartPending = true
                return@launch
            }
            isInitializing = true
            try {
                stopRunning()
                initializeCameraInternal()
            } catch (e: Exception) {
                Log.e("CamEngine", "Failed to restart camera", e)
            } finally {
                isInitializing = false
                if (restartPending) {
                    Log.i("CamEngine", "Executing queued restart.")
                    restartPending = false
                    restart()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun openCamera(manager: CameraManager, cameraId: String, handler: Handler? = null): CameraDevice = suspendCancellableCoroutine { cont ->
        try {
            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    // --- FIXED ---
                    if (cont.context.isActive) {
                        cont.resume(device)
                    } else {
                        device.close()
                    }
                }

                override fun onDisconnected(device: CameraDevice) {
                    Log.w("CamEngine", "Camera $cameraId has been disconnected")
                }

                override fun onError(device: CameraDevice, error: Int) {
                    val msg = when(error) { else -> "Unknown" }
                    val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                    Log.e("CamEngine", exc.message, exc)
                    // --- FIXED ---
                    if (cont.context.isActive) {
                        cont.resumeWithException(exc)
                    }
                }
            }, handler)
        } catch (e: Exception) {
            Log.e("CamEngine", "Synchronous error during openCamera", e)
            // --- FIXED ---
            if (cont.context.isActive) {
                cont.resumeWithException(e)
            }
        }
    }

    private suspend fun createCaptureSession(device: CameraDevice, targets: List<Surface>, handler: Handler? = null): CameraCaptureSession = suspendCoroutine { cont ->
        val stateCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                if (cont.context.isActive) {
                    cont.resume(session)
                } else {
                    session.close()
                }
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                val exc = RuntimeException("Camera ${device.id} session configuration failed")
                Log.e("CamEngine", exc.message, exc)
                // --- FIXED ---
                if (cont.context.isActive) {
                    cont.resumeWithException(exc)
                }
            }
        }

        try {
            // No if/else needed, assuming minSdk >= 28 (P)
            val outputConfigs = targets.map { OutputConfiguration(it) }
            // Convert Handler to Executor
            val executor = Executor { runnable -> handler?.post(runnable) ?: runnable.run() }
            val sessionConfig = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                outputConfigs,
                executor,
                stateCallback
            )
            device.createCaptureSession(sessionConfig)

        } catch (e: Exception) {
            Log.e("CamEngine", "Synchronous error during createCaptureSession", e)
            // --- FIXED ---
            if (cont.context.isActive) {
                cont.resumeWithException(e)
            }
        }
    }

    private suspend fun initializeCameraInternal() {
        Log.i("CAMERA", "initializeCameraInternal")
        stopRunning()

        if (cameraList.none { it.cameraId == viewState.cameraId }) {
            Log.w("CamEngine", "Saved camera (${viewState.cameraId}) not found. Reverting to default camera.")
            viewState = viewState.copy(cameraId = defaultCameraId)
            SettingsManager.saveSettings(context, viewState)
        }

        try {
            characteristics = cameraManager.getCameraCharacteristics(viewState.cameraId)
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
            sizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(camOutPutFormat).reversed()
            hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
            fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) ?: arrayOf()
            availableAntiFlickerModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES) ?: intArrayOf()
            availableNoiseReductionModes = characteristics.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES) ?: intArrayOf()
            availableOisModes = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION) ?: intArrayOf()
            availableEisModes = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES) ?: intArrayOf()
            maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
            activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)!!
        } catch (e: Exception) {
            Log.e("CamEngine", "Could not get characteristics for ${viewState.cameraId}. Aborting.", e)
            return
        }

        if (sizes.isEmpty()) {
            Log.e("CamEngine", "No JPEG sizes found for ${viewState.cameraId}")
            return
        }

        val isFirstSetup = (viewState.resolutionIndex == null)
        val isIndexInvalid = !isFirstSetup && viewState.resolutionIndex!! >= sizes.size

        if (isFirstSetup || isIndexInvalid) {
            if (isFirstSetup) {
                Log.i("CamEngine", "First run detected. Applying default settings (1280x720, 1% quality).")
                val targetWidth = 1280
                val targetHeight = 720
                var targetIndex = sizes.indexOfFirst { it.width == targetWidth && it.height == targetHeight }
                if (targetIndex == -1) {
                    Log.w("CamEngine", "Resolution 1280x720 does not exist, falling back to <= 720p.")
                    targetIndex = sizes.indexOfLast { it.height <= 720 }.takeIf { it != -1 } ?: (sizes.size - 1)
                }
                viewState = viewState.copy(resolutionIndex = targetIndex, quality = 1)
            } else {
                Log.w("CamEngine", "Invalid resolution index. Resetting to a size <= 720p.")
                viewState = viewState.copy(resolutionIndex = sizes.indexOfLast { it.height <= 720 }.takeIf { it != -1 } ?: (sizes.size - 1))
            }
            SettingsManager.saveSettings(context, viewState)
        }

        if (!hasFlash && viewState.flash) {
            viewState = viewState.copy(flash = false)
            SettingsManager.saveSettings(context, viewState)
        }

        val selectedSize = sizes[viewState.resolutionIndex!!]
        resW = selectedSize.width
        resH = selectedSize.height

        val showLiveSurface = viewState.preview && !insidePause && previewSurface != null
        isShowingPreview = showLiveSurface

        try {
            camera = openCamera(cameraManager, viewState.cameraId, cameraHandler)
        } catch (e: Exception) {
            Log.e("CamEngine", "Could not open camera ${viewState.cameraId}", e)
            return
        }

        imageReader = ImageReader.newInstance(resW, resH, camOutPutFormat, 4)
        var targets = listOf(imageReader.surface)
        if (showLiveSurface) {
            if (previewSurface?.isValid == true) {
                targets = targets.plus(previewSurface!!)
            } else {
                Log.w("CamEngine", "Preview surface is invalid, continuing without it.")
                isShowingPreview = false
            }
        }

        try {
            session = createCaptureSession(camera, targets, cameraHandler)
        } catch (e: Exception) {
            Log.e("CamEngine", "Could not create capture session", e)
            return
        }

        captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        if (showLiveSurface && previewSurface?.isValid == true) {
            captureRequestBuilder!!.addTarget(previewSurface!!)
        }
        captureRequestBuilder!!.addTarget(imageReader.surface)

        currentZoomRatio = SettingsManager.loadZoomRatio(context).coerceIn(1.0f, maxZoom)
        zoomToggleState = (currentZoomRatio > 1.01f)

        applyFlash(captureRequestBuilder!!)
        applyZoom(captureRequestBuilder!!)
        applyFps(captureRequestBuilder!!)
        applyStabilization(captureRequestBuilder!!)
        applyNoiseReduction(captureRequestBuilder!!)
        applyAntiFlicker(captureRequestBuilder!!)
        captureRequestBuilder!!.set(CaptureRequest.JPEG_QUALITY, viewState.quality.toByte())

        var lastTime = System.currentTimeMillis()
        val aquired = AtomicInteger(0)

        sessionCallback = object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                super.onCaptureCompleted(session, request, result)
                var lastImg: android.media.Image? = try {
                    imageReader.acquireNextImage()
                } catch (e: Exception) {
                    Log.w("CamEngine", "Failed to acquire next image", e)
                    null
                }
                if (aquired.get() > 1 && lastImg != null) {
                    lastImg.close()
                    lastImg = null
                }
                val img = lastImg ?: return
                aquired.incrementAndGet()

                val acquisitionTime = System.currentTimeMillis()
                val delta = acquisitionTime - lastTime
                lastTime = acquisitionTime

                val buffer = img.planes[0].buffer
                val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }
                if (viewState.stream && !insidePause) {
                    http?.sendFrame(bytes)
                }
                img.close()
                aquired.decrementAndGet()

                if (delta > 0) {
                    val rate = (bytes.size.toLong() * 1000) / (delta * 1024)
                    val quickData = DataQuick(ms = delta.toInt(), rateKbs = rate.toInt())
                    if (acquisitionTime - lastQuickUpdateTime > quickUpdateIntervalMs) {
                        updateViewQuick(quickData)
                        lastQuickUpdateTime = acquisitionTime
                    }
                }
            }
        }

        session?.setRepeatingRequest(captureRequestBuilder!!.build(), sessionCallback!!, cameraHandler)
        updateView()
    }

    fun initializeCamera() {
        restart()
    }

    fun destroy() {
        stopRunning()
        cameraThread.quitSafely()
    }

    fun updateView() {
        if (!::sizes.isInitialized) return
        val resIndex = viewState.resolutionIndex ?: return
        val sensor = cameraList.find { it.cameraId == viewState.cameraId } ?: return
        val parcelableSizes = sizes.map { ParcelableSize(it.width, it.height) }

        val dataToSend = Data(
            sensors = cameraList,
            sensorSelected = sensor,
            resolutions = parcelableSizes,
            resolutionSelected = resIndex,
            currentZoom = currentZoomRatio,
            maxZoom = maxZoom,
            hasFlash = hasFlash,
            quality = viewState.quality,
            flashState = viewState.flash,
            sensorOrientation = sensorOrientation
        )

        val intent = Intent("UpdateFromCameraEngine").setPackage(context.packageName)
        intent.putExtra("data", dataToSend)
        context.sendBroadcast(intent)
    }

    fun updateViewQuick(dq: DataQuick) {
        val intent = Intent("UpdateFromCameraEngine").setPackage(context.packageName)
        intent.putExtra("dataQuick", dq)
        context.sendBroadcast(intent)
    }

    private fun applyZoom(builder: CaptureRequest.Builder) {
        if (!::activeArraySize.isInitialized) return
        val zoomWidth = (activeArraySize.width() / currentZoomRatio).toInt()
        val zoomHeight = (activeArraySize.height() / currentZoomRatio).toInt()
        val centerX = activeArraySize.width() / 2
        val centerY = activeArraySize.height() / 2
        val cropRect = Rect(centerX - zoomWidth / 2, centerY - zoomHeight / 2, centerX + zoomWidth / 2, centerY + zoomHeight / 2)
        builder.set(CaptureRequest.SCALER_CROP_REGION, cropRect)
    }

    fun scaleZoom(scaleFactor: Float) {
        setZoom(currentZoomRatio * scaleFactor)
    }

    fun setZoomRatio(ratio: Float) {
        if (!::activeArraySize.isInitialized) return
        setZoom(1.0f + (maxZoom - 1.0f) * ratio.coerceIn(0.0f, 1.0f))
    }

    private fun setZoom(zoomRatio: Float) {
        if (session == null || captureRequestBuilder == null || !::camera.isInitialized) return
        val newZoom = zoomRatio.coerceIn(1.0f, maxZoom)
        if (newZoom == currentZoomRatio) return
        currentZoomRatio = newZoom
        zoomToggleState = (currentZoomRatio > 1.01f)
        applyZoom(captureRequestBuilder!!)
        try {
            session?.setRepeatingRequest(captureRequestBuilder!!.build(), sessionCallback!!, cameraHandler)
            if (SettingsManager.loadRememberSettings(context) && SettingsManager.loadRememberZoom(context)) {
                SettingsManager.saveZoomRatio(context, currentZoomRatio)
            }
            updateView()
        } catch (e: Exception) {
            Log.e("CamEngine", "Failed to update zoom", e)
        }
    }

    private fun getZoomStep(): Float {
        return (maxZoom - 1.0f) * 0.05f
    }

    fun stepZoomIn() {
        val step = getZoomStep()
        animateZoom((currentZoomRatio + step).coerceIn(1.0f, maxZoom))
    }

    fun stepZoomOut() {
        val step = getZoomStep()
        animateZoom((currentZoomRatio - step).coerceIn(1.0f, maxZoom))
    }

    private fun applyFlash(builder: CaptureRequest.Builder) {
        if (!hasFlash) return
        builder.set(CaptureRequest.FLASH_MODE, if (viewState.flash) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF)
    }

    private fun applyFps(builder: CaptureRequest.Builder) {
        if (fpsRanges.isEmpty()) return
        val selectedRange = fpsRanges.find { it.lower == targetFps && it.upper == targetFps }
            ?: fpsRanges.filter { it.upper >= targetFps && it.lower <= targetFps }.minByOrNull { it.upper }
            ?: fpsRanges.maxByOrNull { it.upper }
        if (selectedRange != null) {
            builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, selectedRange)
        }
    }

    private fun applyStabilization(builder: CaptureRequest.Builder) {
        if (isStabilizationOff) {
            if (availableEisModes.contains(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF)) builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF)
            if (availableOisModes.contains(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF)) builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF)
        } else {
            if (availableEisModes.contains(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON)) builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON)
            if (availableOisModes.contains(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON)) builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON)
        }
    }

    private fun applyNoiseReduction(builder: CaptureRequest.Builder) {
        val mode = when (currentNoiseReductionMode) {
            SettingsManager.NR_OFF -> CaptureRequest.NOISE_REDUCTION_MODE_OFF
            SettingsManager.NR_LOW -> CaptureRequest.NOISE_REDUCTION_MODE_FAST
            SettingsManager.NR_HIGH -> CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY
            else -> CaptureRequest.NOISE_REDUCTION_MODE_FAST
        }
        if (availableNoiseReductionModes.contains(mode)) builder.set(CaptureRequest.NOISE_REDUCTION_MODE, mode)
    }

    private fun applyAntiFlicker(builder: CaptureRequest.Builder) {
        val mode = when (currentAntiFlickerMode) {
            SettingsManager.ANTI_FLICKER_OFF -> CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_OFF
            SettingsManager.ANTI_FLICKER_50HZ -> CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_50HZ
            SettingsManager.ANTI_FLICKER_60HZ -> CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_60HZ
            else -> CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO
        }
        if (availableAntiFlickerModes.contains(mode)) builder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, mode)
    }

    fun updateRepeatingRequest() {
        if (session == null || captureRequestBuilder == null || !::camera.isInitialized) return
        applyFlash(captureRequestBuilder!!)
        applyNoiseReduction(captureRequestBuilder!!)
        applyAntiFlicker(captureRequestBuilder!!)
        captureRequestBuilder!!.set(CaptureRequest.JPEG_QUALITY, viewState.quality.toByte())
        try {
            session?.setRepeatingRequest(captureRequestBuilder!!.build(), sessionCallback!!, cameraHandler)
        } catch (e: Exception) {
            Log.e("CamEngine", "Failed 'light' update", e)
        }
    }

    fun setTargetFps(fps: Int) {
        if (fps == this.targetFps) return
        this.targetFps = fps
        SettingsManager.saveTargetFps(context, fps)
        restart()
    }

    fun setAntiFlickerMode(mode: Int) {
        if (mode == this.currentAntiFlickerMode) return
        this.currentAntiFlickerMode = mode
        SettingsManager.saveAntiFlickerMode(context, mode)
        updateRepeatingRequest()
    }

    fun setNoiseReductionMode(mode: Int) {
        if (mode == this.currentNoiseReductionMode) return
        this.currentNoiseReductionMode = mode
        SettingsManager.saveNoiseReductionMode(context, mode)
        updateRepeatingRequest()
    }

    fun setStabilizationOff(isOff: Boolean) {
        if (isOff == this.isStabilizationOff) return
        this.isStabilizationOff = isOff
        SettingsManager.saveStabilizationOff(context, isOff)
        restart()
    }

    fun switchToNextCamera() {
        val currentIndex = cameraList.indexOfFirst { it.cameraId == viewState.cameraId }
        val nextCameraId = cameraList[(currentIndex + 1) % cameraList.size].cameraId
        viewState = viewState.copy(cameraId = nextCameraId, flash = false, resolutionIndex = null)
        SettingsManager.saveSettings(context, viewState)
        restart()
    }

    fun toggleFlash() {
        if (session == null || !hasFlash) return
        val newFlashState = !viewState.flash
        viewState = viewState.copy(flash = newFlashState)
        SettingsManager.saveSettings(context, viewState)
        updateRepeatingRequest()
        updateView()
    }

    fun toggleZoom() {
        val newZoom = if (zoomToggleState) 1.0f else 2.0f.coerceAtMost(maxZoom)
        animateZoom(newZoom)
    }

    fun setZoomSmoothingDelay(delayMs: Int) {
        currentSmoothingDelay = delayMs
        SettingsManager.saveZoomSmoothingDelay(context, delayMs)
    }

    private fun animateZoom(targetZoom: Float) {
        zoomAnimatorJob?.cancel()

        if (currentSmoothingDelay == 0) {
            setZoom(targetZoom)
            return
        }

        zoomAnimatorJob = cameraScope.launch {
            val startZoom = currentZoomRatio
            val delayMs = currentSmoothingDelay.toLong()

            for (i in 1..zoomAnimationSteps) {
                val progress = i.toFloat() / zoomAnimationSteps.toFloat()
                val newZoom = startZoom + (targetZoom - startZoom) * progress

                setZoom(newZoom)

                delay(delayMs)
            }
            setZoom(targetZoom)
        }
    }

    companion object {
        @Parcelize data class ParcelableSize(val width: Int, val height: Int) : Parcelable {
            override fun toString(): String = "$width x $height"
        }
        @Parcelize data class Data(
            val sensors: List<Selector.SensorDesc>,
            val sensorSelected: Selector.SensorDesc,
            val resolutions: List<ParcelableSize>,
            val resolutionSelected: Int,
            val currentZoom: Float,
            val maxZoom: Float,
            val hasFlash: Boolean,
            val quality: Int,
            val flashState: Boolean,
            val sensorOrientation: Int
        ) : Parcelable
        @Parcelize data class DataQuick(val ms: Int, val rateKbs: Int) : Parcelable
    }
}
