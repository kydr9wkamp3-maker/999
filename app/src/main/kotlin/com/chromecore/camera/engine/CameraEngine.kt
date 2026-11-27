package com.chromecore.camera.engine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core camera engine built on Camera2 API.
 * Handles camera lifecycle, preview, capture, and parameter control.
 */
@Singleton
class CameraEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "CameraEngine"
        private const val MAX_PREVIEW_WIDTH = 1920
        private const val MAX_PREVIEW_HEIGHT = 1080
        private const val CAMERA_OPEN_TIMEOUT_MS = 2500L
    }

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var imageReader: ImageReader? = null

    private val cameraOpenCloseLock = Semaphore(1)
    private val cameraManager: CameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private val _cameraState = MutableStateFlow(CameraState())
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private val _availableCameras = MutableStateFlow<List<CameraInfo>>(emptyList())
    val availableCameras: StateFlow<List<CameraInfo>> = _availableCameras.asStateFlow()

    private var currentCameraId: String = "0"
    private var currentFacing = CameraCharacteristics.LENS_FACING_BACK
    private var previewSize: Size = Size(1920, 1080)
    private var sensorOrientation = 0

    data class CameraState(
        val isOpen: Boolean = false,
        val isCapturing: Boolean = false,
        val isRecording: Boolean = false,
        val flashMode: FlashMode = FlashMode.AUTO,
        val zoomLevel: Float = 1.0f,
        val focusMode: FocusMode = FocusMode.AUTO,
        val exposureCompensation: Int = 0,
        val iso: Int? = null,
        val shutterSpeed: Long? = null,
        val whiteBalance: Int? = null,
        val focusDistance: Float? = null,
        val isHDRAvailable: Boolean = false,
        val isRAWAvailable: Boolean = false,
        val cameraName: String = ""
    )

    enum class FlashMode { ON, OFF, AUTO, TORCH }
    enum class FocusMode { AUTO, MANUAL, CONTINUOUS, LOCKED }

    data class CameraInfo(
        val id: String,
        val facing: Int,
        val name: String,
        val supportedSizes: List<Size>,
        val isRAWCapable: Boolean,
        val isHDRCapable: Boolean,
        val hardwareLevel: Int,
        val focalLengths: FloatArray,
        val sensorSize: android.util.SizeF?,
        val pixelArraySize: Size?
    )

    fun startBackgroundThread() {
        backgroundThread = HandlerThread("ChromecoreCamera").also {
            it.start()
            backgroundHandler = Handler(it.looper)
        }
    }

    fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join(2000)
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.w(TAG, "Background thread interrupted", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera(cameraId: String? = null, surfaceTexture: SurfaceTexture? = null) {
        val id = cameraId ?: chooseOptimalCamera()
        currentCameraId = id

        try {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0

            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val outputSizes = map?.getOutputSizes(SurfaceTexture::class.java) ?: arrayOf()
            previewSize = chooseOptimalSize(outputSizes, MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT)

            currentFacing = characteristics.get(CameraCharacteristics.LENS_FACING) ?: CameraCharacteristics.LENS_FACING_BACK

            val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
            val isRAW = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
            val hwLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) ?: CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY

            _cameraState.value = _cameraState.value.copy(
                isHDRAvailable = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_HDR),
                isRAWAvailable = isRAW,
                cameraName = getCameraName(id, currentFacing)
            )

            if (!cameraOpenCloseLock.tryAcquire(CAMERA_OPEN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Camera open timeout")
            }

            cameraManager.openCamera(id, cameraStateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Camera access error", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "Camera permission not granted", e)
        }
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice = camera
            _cameraState.value = _cameraState.value.copy(isOpen = true)
            Log.i(TAG, "Camera opened: ${camera.id}")
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
            _cameraState.value = _cameraState.value.copy(isOpen = false)
            Log.w(TAG, "Camera disconnected: ${camera.id}")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
            _cameraState.value = _cameraState.value.copy(isOpen = false)
            Log.e(TAG, "Camera error: ${camera.id}, error=$error")
        }
    }

    fun startPreview(surface: Surface) {
        val device = cameraDevice ?: return
        try {
            previewRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                set(CaptureRequest.CONTROL_MODE, CameraCharacteristics.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
            }

            device.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return
                        captureSession = session
                        previewRequestBuilder?.let {
                            session.setRepeatingRequest(it.build())
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Preview configuration failed")
                    }
                },
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Preview start failed", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun capturePhoto(surface: Surface, onImageAvailable: (Bitmap) -> Unit) {
        val device = cameraDevice ?: return
        _cameraState.value = _cameraState.value.copy(isCapturing = true)

        try {
            val reader = setupImageReader(onImageAvailable)
            val captureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(surface)
                addTarget(reader.surface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.JPEG_QUALITY, 95.toByte())

                // Apply current settings
                _cameraState.value.iso?.let {
                    set(CaptureRequest.SENSOR_SENSITIVITY, it)
                }
                _cameraState.value.shutterSpeed?.let {
                    set(CaptureRequest.SENSOR_EXPOSURE_TIME, it)
                }
                _cameraState.value.whiteBalance?.let {
                    set(CaptureRequest.CONTROL_AWB_MODE, CameraCharacteristics.CONTROL_AWB_MODE_OFF)
                    set(CaptureRequest.COLOR_CORRECTION_MODE, CameraCharacteristics.COLOR_CORRECTION_MODE_TRANSFORM)
                }
            }

            captureSession?.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    _cameraState.value = _cameraState.value.copy(isCapturing = false)
                    Log.d(TAG, "Capture completed")
                }

                override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
                    _cameraState.value = _cameraState.value.copy(isCapturing = false)
                    Log.e(TAG, "Capture failed: ${failure.reason}")
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Capture error", e)
            _cameraState.value = _cameraState.value.copy(isCapturing = false)
        }
    }

    fun setFlashMode(mode: FlashMode) {
        _cameraState.value = _cameraState.value.copy(flashMode = mode)
        updatePreviewRequest {
            when (mode) {
                FlashMode.ON -> set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE)
                FlashMode.OFF -> set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                FlashMode.AUTO -> set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                FlashMode.TORCH -> set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
            }
        }
    }

    fun setZoomLevel(level: Float) {
        val clamped = level.coerceIn(1.0f, getMaxZoom())
        _cameraState.value = _cameraState.value.copy(zoomLevel = clamped)
        updatePreviewRequest {
            try {
                val characteristics = cameraManager.getCameraCharacteristics(currentCameraId)
                val maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
                val zoom = clamped.coerceIn(1.0f, maxZoom)
                val cropRegion = computeCropRegion(zoom)
                set(CaptureRequest.SCALER_CROP_REGION, cropRegion)
            } catch (e: CameraAccessException) {
                Log.w(TAG, "Zoom set failed", e)
            }
        }
    }

    fun setExposureCompensation(compensation: Int) {
        _cameraState.value = _cameraState.value.copy(exposureCompensation = compensation)
        updatePreviewRequest {
            set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, compensation)
        }
    }

    fun setISO(iso: Int?) {
        _cameraState.value = _cameraState.value.copy(iso = iso)
        updatePreviewRequest {
            if (iso != null) {
                set(CaptureRequest.CONTROL_MODE, CameraCharacteristics.CONTROL_MODE_OFF)
                set(CaptureRequest.SENSOR_SENSITIVITY, iso)
            } else {
                set(CaptureRequest.CONTROL_MODE, CameraCharacteristics.CONTROL_MODE_AUTO)
            }
        }
    }

    fun setShutterSpeed(speedNs: Long?) {
        _cameraState.value = _cameraState.value.copy(shutterSpeed = speedNs)
        updatePreviewRequest {
            if (speedNs != null) {
                set(CaptureRequest.CONTROL_MODE, CameraCharacteristics.CONTROL_MODE_OFF)
                set(CaptureRequest.SENSOR_EXPOSURE_TIME, speedNs)
            } else {
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            }
        }
    }

    fun setFocusDistance(distance: Float?) {
        _cameraState.value = _cameraState.value.copy(focusDistance = distance)
        updatePreviewRequest {
            if (distance != null) {
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                set(CaptureRequest.LENS_FOCUS_DISTANCE, distance)
            } else {
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            }
        }
    }

    private fun updatePreviewRequest(block: CaptureRequest.Builder.() -> Unit) {
        previewRequestBuilder?.apply {
            block()
            captureSession?.setRepeatingRequest(build())
        }
    }

    private fun getMaxZoom(): Float {
        return try {
            val chars = cameraManager.getCameraCharacteristics(currentCameraId)
            chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
        } catch (e: CameraAccessException) {
            1.0f
        }
    }

    private fun computeCropRegion(zoom: Float): android.graphics.Rect {
        val chars = cameraManager.getCameraCharacteristics(currentCameraId)
        val sensorRect = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            ?: return android.graphics.Rect(0, 0, previewSize.width, previewSize.height)

        val centerW = sensorRect.width() / 2f
        val centerH = sensorRect.height() / 2f
        val halfW = (centerW / zoom).toInt()
        val halfH = (centerH / zoom).toInt()

        return android.graphics.Rect(
            (centerW - halfW).toInt(),
            (centerH - halfH).toInt(),
            (centerW + halfW).toInt(),
            (centerH + halfH).toInt()
        )
    }

    private fun setupImageReader(onImageAvailable: (Bitmap) -> Unit): ImageReader {
        imageReader?.close()
        val reader = ImageReader.newInstance(
            previewSize.width, previewSize.height,
            ImageFormat.JPEG, 2
        )
        reader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            image?.let {
                val bitmap = imageToBitmap(it)
                val rotated = rotateBitmap(bitmap, sensorOrientation.toFloat())
                onImageAvailable(rotated)
                it.close()
            }
        }, backgroundHandler)
        imageReader = reader
        return reader
    }

    private fun imageToBitmap(image: android.media.Image): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun chooseOptimalCamera(): String {
        val ids = cameraManager.cameraIdList
        return ids.firstOrNull { id ->
            val chars = cameraManager.getCameraCharacteristics(id)
            val facing = chars.get(CameraCharacteristics.LENS_FACING)
            facing == CameraCharacteristics.LENS_FACING_BACK
        } ?: ids.firstOrNull() ?: "0"
    }

    private fun chooseOptimalSize(sizes: Array<Size>, maxWidth: Int, maxHeight: Int): Size {
        return sizes.filter {
            it.width <= maxWidth && it.height <= maxHeight
        }.maxByOrNull { it.width * it.height } ?: Size(1920, 1080)
    }

    private fun getCameraName(id: String, facing: Int): String {
        return when (facing) {
            CameraCharacteristics.LENS_FACING_BACK -> "Rear Camera"
            CameraCharacteristics.LENS_FACING_FRONT -> "Front Camera"
            else -> "Camera $id"
        }
    }

    fun switchCamera() {
        closeCamera()
        val targetFacing = if (currentFacing == CameraCharacteristics.LENS_FACING_BACK) {
            CameraCharacteristics.LENS_FACING_FRONT
        } else {
            CameraCharacteristics.LENS_FACING_BACK
        }
        val ids = cameraManager.cameraIdList
        val newId = ids.firstOrNull { id ->
            val chars = cameraManager.getCameraCharacteristics(id)
            chars.get(CameraCharacteristics.LENS_FACING) == targetFacing
        } ?: currentCameraId
        openCamera(newId)
    }

    fun closeCamera() {
        try {
            cameraOpenCloseLock.tryAcquire(CAMERA_OPEN_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
            _cameraState.value = _cameraState.value.copy(isOpen = false)
        } catch (e: InterruptedException) {
            Log.w(TAG, "Camera close interrupted", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    fun release() {
        closeCamera()
        stopBackgroundThread()
    }
}
