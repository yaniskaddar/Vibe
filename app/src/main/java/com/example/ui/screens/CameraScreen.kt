package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.BasicImageFilter
import com.example.ui.theme.VibeSocialBlue
import com.example.ui.theme.VibeStoryGradient
import com.example.ui.viewmodel.CameraCaptureTarget
import com.example.ui.viewmodel.CameraViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onClose: () -> Unit,
    onPhotoConfirmed: (Bitmap, CameraCaptureTarget) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            viewModel.updatePermission(granted)
        }
    )

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.updatePermission(granted)
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    BackHandler {
        if (state.capturedBitmap != null) {
            viewModel.retakePhoto()
        } else {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val captured = state.capturedBitmap
        val originalCaptured = state.originalCapturedBitmap ?: captured
        when {
            !state.hasPermission -> {
                CameraPermissionGate(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onBack = onClose
                )
            }
            captured != null && originalCaptured != null -> {
                CapturedPhotoReview(
                    filteredBitmap = captured,
                    originalBitmap = originalCaptured,
                    captureTarget = state.captureTarget,
                    selectedFilter = state.selectedFilter,
                    isApplyingFilter = state.isApplyingFilter,
                    onSelectFilter = viewModel::setFilter,
                    onSelectTarget = viewModel::setCaptureTarget,
                    onRetake = viewModel::retakePhoto,
                    onConfirm = {
                        val target = state.captureTarget
                        viewModel.resetAfterUse()
                        onPhotoConfirmed(captured, target)
                    }
                )
            }
            else -> {
                LiveCameraViewfinder(
                    captureTarget = state.captureTarget,
                    flashMode = state.flashMode,
                    isCameraReady = state.isCameraReady,
                    isCapturing = state.isCapturing,
                    zoomRatio = state.zoomRatio,
                    maxZoomRatio = state.maxZoomRatio,
                    selectedFilter = state.selectedFilter,
                    errorMessage = state.errorMessage,
                    onBindPreview = { previewView ->
                        viewModel.bindCamera(context, lifecycleOwner, previewView.surfaceProvider)
                    },
                    onTapToFocus = { previewView, offset ->
                        viewModel.focusAtPoint(previewView.meteringPointFactory, offset.x, offset.y)
                    },
                    onFlipCamera = { previewView ->
                        viewModel.toggleLens(context, lifecycleOwner, previewView.surfaceProvider)
                    },
                    onCycleFlash = viewModel::cycleFlashMode,
                    onSelectZoom = viewModel::setZoomRatio,
                    onSelectFilter = viewModel::setFilter,
                    onSelectTarget = viewModel::setCaptureTarget,
                    onCapture = { viewModel.capturePhoto(context) },
                    onClose = onClose
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionGate(
    onRequestPermission: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = Color.White.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(84.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Vibe Camera Studio",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Allow camera access to shoot crisp Stories and Posts directly inside Vibe.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFD4D4D8),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = VibeSocialBlue),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("camera_grant_permission_button")
        ) {
            Text(
                text = "Enable Camera",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("camera_back_button")
        ) {
            Text(text = "Back", color = Color.White)
        }
    }
}

@Composable
private fun LiveCameraViewfinder(
    captureTarget: CameraCaptureTarget,
    flashMode: Int,
    isCameraReady: Boolean,
    isCapturing: Boolean,
    zoomRatio: Float,
    maxZoomRatio: Float,
    selectedFilter: BasicImageFilter,
    errorMessage: String?,
    onBindPreview: (PreviewView) -> Unit,
    onTapToFocus: (PreviewView, Offset) -> Unit,
    onFlipCamera: (PreviewView) -> Unit,
    onCycleFlash: () -> Unit,
    onSelectZoom: (Float) -> Unit,
    onSelectFilter: (BasicImageFilter) -> Unit,
    onSelectTarget: (CameraCaptureTarget) -> Unit,
    onCapture: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            delay(900)
            focusPoint = null
        }
    }

    LaunchedEffect(previewView) {
        onBindPreview(previewView)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(previewView) {
                    detectTapGestures { offset ->
                        focusPoint = offset
                        onTapToFocus(previewView, offset)
                    }
                }
        )

        // Subtle live tint preview matching selected filter preset
        val filterTintOverlay = when (selectedFilter) {
            BasicImageFilter.ORIGINAL -> Color.Transparent
            BasicImageFilter.GRAYSCALE -> Color(0x30121214)
            BasicImageFilter.SEPIA -> Color(0x2E8B5A2B)
            BasicImageFilter.WARM_FILM -> Color(0x22FFB347)
            BasicImageFilter.GOLDEN -> Color(0x24F59E0B)
            BasicImageFilter.VIVID -> Color(0x1AE11D48)
            BasicImageFilter.CYBER -> Color(0x228B5CF6)
        }
        if (filterTintOverlay != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(filterTintOverlay)
            )
        }

        // Tap-to-focus ring indicator
        AnimatedVisibility(
            visible = focusPoint != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            focusPoint?.let { pt ->
                val ringPx = with(density) { 64.dp.toPx() }
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (pt.x - ringPx / 2f).roundToInt(),
                                (pt.y - ringPx / 2f).roundToInt()
                            )
                        }
                        .size(64.dp)
                        .border(2.dp, Color.White, CircleShape)
                )
            }
        }

        // Top Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .background(Color.Black.copy(alpha = 0.48f), CircleShape)
                    .testTag("camera_close_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close camera",
                    tint = Color.White
                )
            }

            Surface(
                color = Color.Black.copy(alpha = 0.48f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "${captureTarget.label} Camera",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onCycleFlash,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .background(Color.Black.copy(alpha = 0.48f), CircleShape)
                        .testTag("camera_flash_button")
                ) {
                    val flashIcon = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Filled.FlashOn
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Filled.FlashAuto
                        else -> Icons.Filled.FlashOff
                    }
                    Icon(
                        imageVector = flashIcon,
                        contentDescription = "Cycle flash mode",
                        tint = if (flashMode == ImageCapture.FLASH_MODE_OFF) Color.White else Color(0xFFFACC15)
                    )
                }

                IconButton(
                    onClick = { onFlipCamera(previewView) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .background(Color.Black.copy(alpha = 0.48f), CircleShape)
                        .testTag("camera_flip_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.FlipCameraAndroid,
                        contentDescription = "Switch front or back camera",
                        tint = Color.White
                    )
                }
            }
        }

        if (errorMessage != null) {
            Surface(
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFFFB4AB),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Bottom Controls Deck
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .navigationBarsPadding()
                .padding(top = 12.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zoom Quick Controls
            if (maxZoomRatio > 1.2f) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    listOf(1f, 2f).filter { it <= maxZoomRatio }.forEach { presetZoom ->
                        val active = kotlin.math.abs(zoomRatio - presetZoom) < 0.25f
                        Surface(
                            color = if (active) VibeSocialBlue else Color.Black.copy(alpha = 0.55f),
                            shape = CircleShape,
                            modifier = Modifier
                                .clickable { onSelectZoom(presetZoom) }
                        ) {
                            Text(
                                text = "${presetZoom.toInt()}x",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Live Filter Strip
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(BasicImageFilter.entries) { filter ->
                    val selected = filter == selectedFilter
                    Surface(
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .clickable { onSelectFilter(filter) }
                            .testTag("camera_live_filter_${filter.name.lowercase()}")
                    ) {
                        Text(
                            text = filter.label,
                            color = if (selected) Color.Black else Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Shutter Button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .border(4.dp, VibeStoryGradient, CircleShape)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(enabled = !isCapturing) { onCapture() }
                    .testTag("camera_shutter_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isCapturing || !isCameraReady) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = VibeSocialBlue,
                        strokeWidth = 2.5.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Story vs Post Target Selector
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CameraCaptureTarget.entries.forEach { target ->
                    val selected = target == captureTarget
                    Surface(
                        color = if (selected) VibeSocialBlue else Color.Transparent,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .clickable { onSelectTarget(target) }
                            .testTag("camera_target_${target.name.lowercase()}")
                    ) {
                        Text(
                            text = target.label.uppercase(),
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CapturedPhotoReview(
    filteredBitmap: Bitmap,
    originalBitmap: Bitmap,
    captureTarget: CameraCaptureTarget,
    selectedFilter: BasicImageFilter,
    isApplyingFilter: Boolean,
    onSelectFilter: (BasicImageFilter) -> Unit,
    onSelectTarget: (CameraCaptureTarget) -> Unit,
    onRetake: () -> Unit,
    onConfirm: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = filteredBitmap.asImageBitmap(),
            contentDescription = "Captured photo preview with ${selectedFilter.label} filter",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (isApplyingFilter) {
            CircularProgressIndicator(
                color = VibeSocialBlue,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onRetake,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    .testTag("camera_retake_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Retake photo",
                    tint = Color.White
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CameraCaptureTarget.entries.forEach { target ->
                    val selected = target == captureTarget
                    Surface(
                        color = if (selected) VibeSocialBlue else Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.clickable { onSelectTarget(target) }
                    ) {
                        Text(
                            text = target.label,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .navigationBarsPadding()
                .padding(vertical = 16.dp)
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(BasicImageFilter.entries) { filter ->
                    val selected = filter == selectedFilter
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onSelectFilter(filter) }
                            .testTag("camera_review_filter_${filter.name.lowercase()}")
                    ) {
                        Image(
                            bitmap = originalBitmap.asImageBitmap(),
                            contentDescription = filter.label,
                            contentScale = ContentScale.Crop,
                            colorFilter = filter.toComposeColorFilter(),
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .then(
                                    if (selected) Modifier.border(2.dp, VibeSocialBlue, RoundedCornerShape(10.dp))
                                    else Modifier
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = filter.label,
                            color = if (selected) VibeSocialBlue else Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRetake,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Retake", color = Color.White)
                }
                Button(
                    onClick = onConfirm,
                    enabled = !isApplyingFilter,
                    colors = ButtonDefaults.buttonColors(containerColor = VibeSocialBlue),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .semantics(mergeDescendants = true) {}
                        .testTag("camera_confirm_button")
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Use for ${captureTarget.label}",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
