package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.BasicImageFilter
import com.example.core.PhotoFilterProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

enum class CameraCaptureTarget(val label: String) {
    POST("Post"),
    STORY("Story")
}

data class CameraUiState(
    val captureTarget: CameraCaptureTarget = CameraCaptureTarget.POST,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val hasPermission: Boolean = false,
    val isCameraReady: Boolean = false,
    val isCapturing: Boolean = false,
    val isApplyingFilter: Boolean = false,
    val zoomRatio: Float = 1f,
    val minZoomRatio: Float = 1f,
    val maxZoomRatio: Float = 1f,
    val selectedFilter: BasicImageFilter = BasicImageFilter.ORIGINAL,
    val originalCapturedBitmap: Bitmap? = null,
    val capturedBitmap: Bitmap? = null,
    val errorMessage: String? = null
)

class CameraViewModel(
    private val filterProcessor: PhotoFilterProcessor = PhotoFilterProcessor()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var boundCamera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var filterJob: Job? = null

    fun setCaptureTarget(target: CameraCaptureTarget) {
        _uiState.update { it.copy(captureTarget = target) }
    }

    fun setFilter(filter: BasicImageFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
        val source = _uiState.value.originalCapturedBitmap ?: return

        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            _uiState.update { it.copy(isApplyingFilter = true) }
            val filtered = withContext(Dispatchers.Default) {
                filterProcessor.applyFilter(source, filter)
            }
            _uiState.update {
                it.copy(
                    capturedBitmap = filtered,
                    isApplyingFilter = false
                )
            }
        }
    }

    fun updatePermission(granted: Boolean) {
        _uiState.update { it.copy(hasPermission = granted) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun bindCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider
    ) {
        val currentState = _uiState.value
        if (!currentState.hasPermission || currentState.capturedBitmap != null) return

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                cameraProvider = provider
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = surfaceProvider
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setFlashMode(_uiState.value.flashMode)
                    .build()

                val selector = CameraSelector.Builder()
                    .requireLensFacing(_uiState.value.lensFacing)
                    .build()

                provider.unbindAll()
                val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                boundCamera = camera
                imageCapture = capture

                val zoomState = camera.cameraInfo.zoomState.value
                _uiState.update {
                    it.copy(
                        isCameraReady = true,
                        zoomRatio = zoomState?.zoomRatio ?: 1f,
                        minZoomRatio = zoomState?.minZoomRatio ?: 1f,
                        maxZoomRatio = zoomState?.maxZoomRatio ?: 1f,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCameraReady = false,
                        errorMessage = e.localizedMessage ?: "Camera preview unavailable on this device."
                    )
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun toggleLens(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider
    ) {
        val nextLens = if (_uiState.value.lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        _uiState.update {
            it.copy(
                lensFacing = nextLens,
                isCameraReady = false,
                zoomRatio = 1f
            )
        }
        bindCamera(context, lifecycleOwner, surfaceProvider)
    }

    fun cycleFlashMode() {
        val nextMode = when (_uiState.value.flashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
            else -> ImageCapture.FLASH_MODE_OFF
        }
        imageCapture?.flashMode = nextMode
        _uiState.update { it.copy(flashMode = nextMode) }
    }

    fun setZoomRatio(ratio: Float) {
        val state = _uiState.value
        val clamped = ratio.coerceIn(state.minZoomRatio, state.maxZoomRatio)
        boundCamera?.cameraControl?.setZoomRatio(clamped)
        _uiState.update { it.copy(zoomRatio = clamped) }
    }

    fun focusAtPoint(factory: MeteringPointFactory, x: Float, y: Float) {
        val camera = boundCamera ?: return
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        camera.cameraControl.startFocusAndMetering(action)
    }

    fun capturePhoto(context: Context) {
        val capture = imageCapture
        if (capture == null || _uiState.value.isCapturing) {
            if (capture == null) {
                _uiState.update { it.copy(errorMessage = "Camera is still warming up.") }
            }
            return
        }

        val mirrorHorizontally = _uiState.value.lensFacing == CameraSelector.LENS_FACING_FRONT
        val activeFilter = _uiState.value.selectedFilter
        _uiState.update { it.copy(isCapturing = true, errorMessage = null) }

        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val rotation = image.imageInfo.rotationDegrees.toFloat()
                    val rawBitmap = image.toBitmap()
                    image.close()

                    viewModelScope.launch {
                        val (oriented, filtered) = withContext(Dispatchers.Default) {
                            val orientedBmp = transformCapturedBitmap(rawBitmap, rotation, mirrorHorizontally)
                            val filteredBmp = filterProcessor.applyFilter(orientedBmp, activeFilter)
                            orientedBmp to filteredBmp
                        }
                        _uiState.update {
                            it.copy(
                                isCapturing = false,
                                originalCapturedBitmap = oriented,
                                capturedBitmap = filtered
                            )
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    _uiState.update {
                        it.copy(
                            isCapturing = false,
                            errorMessage = exception.localizedMessage ?: "Photo capture failed."
                        )
                    }
                }
            }
        )
    }

    fun retakePhoto() {
        filterJob?.cancel()
        _uiState.update {
            it.copy(
                originalCapturedBitmap = null,
                capturedBitmap = null,
                isApplyingFilter = false,
                errorMessage = null
            )
        }
    }

    fun resetAfterUse() {
        filterJob?.cancel()
        _uiState.update {
            it.copy(
                originalCapturedBitmap = null,
                capturedBitmap = null,
                isCapturing = false,
                isApplyingFilter = false,
                errorMessage = null
            )
        }
    }

    private fun transformCapturedBitmap(
        source: Bitmap,
        rotationDegrees: Float,
        mirrorHorizontally: Boolean
    ): Bitmap {
        if (rotationDegrees == 0f && !mirrorHorizontally) return source
        val matrix = Matrix().apply {
            if (rotationDegrees != 0f) postRotate(rotationDegrees)
            if (mirrorHorizontally) postScale(-1f, 1f)
        }
        val transformed = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (transformed !== source) {
            source.recycle()
        }
        return transformed
    }

    override fun onCleared() {
        super.onCleared()
        filterJob?.cancel()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }
}
