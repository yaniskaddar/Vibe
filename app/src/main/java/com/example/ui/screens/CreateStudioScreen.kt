package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.FilterPreset
import com.example.data.model.PublicProfile
import com.example.ui.theme.VibeAiViolet
import com.example.ui.theme.VibeSocialBlue
import com.example.ui.viewmodel.AiArtState
import com.example.ui.viewmodel.AiCaptionState
import com.example.ui.viewmodel.CameraCaptureTarget
import com.example.ui.viewmodel.CreateAiViewModel
import com.example.ui.viewmodel.CreateMode
import com.example.ui.viewmodel.PublishState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateStudioScreen(
    viewModel: CreateAiViewModel,
    myProfile: PublicProfile?,
    onOpenCamera: (CameraCaptureTarget) -> Unit,
    onPublishedSuccess: (CreateMode) -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val multiPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(4),
        onResult = { uris -> viewModel.addPickedImages(context, uris) }
    )
    val singlePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { viewModel.addPickedImages(context, listOf(it)) } }
    )
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { viewModel.setPickedVideo(context, it) } }
    )

    LaunchedEffect(state.publishState) {
        val pub = state.publishState
        if (pub is PublishState.Published) {
            onPublishedSuccess(pub.mode)
            viewModel.resetPublishStatus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 620.dp)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Create Studio",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Mode Tabs
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CreateMode.entries) { mode ->
                    FilterChip(
                        selected = state.mode == mode,
                        onClick = { viewModel.setMode(mode) },
                        label = { Text(mode.title) },
                        leadingIcon = if (mode == CreateMode.AI_ART) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = VibeAiViolet
                                )
                            }
                        } else null,
                        modifier = Modifier.testTag("create_mode_${mode.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Media Source Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.mode == CreateMode.REEL) {
                    OutlinedButton(
                        onClick = {
                            videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("pick_video_button")
                    ) {
                        Icon(Icons.Filled.Videocam, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Video")
                    }
                }
                OutlinedButton(
                    onClick = {
                        if (state.mode == CreateMode.POST) {
                            multiPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        } else {
                            singlePhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("pick_photo_button")
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (state.mode == CreateMode.POST) "Gallery (1-4)" else "Gallery")
                }

                OutlinedButton(
                    onClick = {
                        val target = if (state.mode == CreateMode.STORY) {
                            CameraCaptureTarget.STORY
                        } else {
                            CameraCaptureTarget.POST
                        }
                        onOpenCamera(target)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("open_camera_button")
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Camera")
                }
            }

            // AI Art Generator / Remix Section
            if (state.mode == CreateMode.AI_ART) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = VibeAiViolet.copy(alpha = 0.08f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = VibeAiViolet
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Vibe AI Image Studio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.aiArtPrompt,
                            onValueChange = viewModel::updateAiArtPrompt,
                            label = { Text("Describe artwork or remix style") },
                            placeholder = { Text("e.g. Neon Tokyo rooftop rain, analog 35mm film grain") },
                            minLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_art_prompt_input")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.generateAiArt(remixCurrent = false) },
                                enabled = state.aiArtState !is AiArtState.Loading,
                                colors = ButtonDefaults.buttonColors(containerColor = VibeAiViolet),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("ai_art_generate_button")
                            ) {
                                Text(stringResource(R.string.ai_art_generate_cta))
                            }
                            if (state.selectedImages.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { viewModel.generateAiArt(remixCurrent = true) },
                                    enabled = state.aiArtState !is AiArtState.Loading,
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("ai_art_remix_button")
                                ) {
                                    Text(stringResource(R.string.ai_art_remix_cta))
                                }
                            }
                        }

                        when (val artState = state.aiArtState) {
                            is AiArtState.Loading -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = VibeAiViolet
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Synthesizing with Gemini Image Studio (30-60s)...",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            is AiArtState.Error -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = artState.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            else -> Unit
                        }
                    }
                }
            }

            // Selected Media Preview + Color Filters
            val previewBitmap = state.selectedImages.firstOrNull() ?: state.videoThumbnail
            if (previewBitmap != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Selected media preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = state.selectedFilter.toColorFilter()
                    )
                    if (state.localVideoPath != null && state.mode == CreateMode.REEL) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Video Reel Ready",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (state.selectedImages.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(state.selectedImages) { idx, bmp ->
                            Box(modifier = Modifier.size(64.dp)) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Slide ${idx + 1}",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop,
                                    colorFilter = state.selectedFilter.toColorFilter()
                                )
                                IconButton(
                                    onClick = { viewModel.removeImageAt(idx) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(22.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove image",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Color Filter Presets
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Color Grade",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(FilterPreset.entries) { preset ->
                        val isSelected = state.selectedFilter == preset
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { viewModel.setFilter(preset) }
                        ) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = preset.label,
                                contentScale = ContentScale.Crop,
                                colorFilter = preset.toColorFilter(),
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .then(
                                        if (isSelected) Modifier.border(2.5.dp, VibeSocialBlue, RoundedCornerShape(10.dp))
                                        else Modifier
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) VibeSocialBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Poll Builder Fields
            if (state.mode == CreateMode.POLL) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = state.pollQuestion,
                    onValueChange = viewModel::updatePollQuestion,
                    label = { Text("Poll Question") },
                    placeholder = { Text("Which aesthetic fits tonight's set?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("poll_question_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                state.pollOptions.forEachIndexed { index, opt ->
                    OutlinedTextField(
                        value = opt,
                        onValueChange = { viewModel.updatePollOption(index, it) },
                        label = { Text("Option ${index + 1}") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
                if (state.pollOptions.size < 4) {
                    TextButton(onClick = viewModel::addPollOption) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Option")
                    }
                }
            }

            // Story Mood Tag
            if (state.mode == CreateMode.STORY) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.storyMoodTag,
                    onValueChange = viewModel::updateStoryMood,
                    label = { Text("Story Mood Badge") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Caption + Magic AI Captions
            OutlinedTextField(
                value = state.caption,
                onValueChange = viewModel::updateCaption,
                label = { Text(if (state.mode == CreateMode.STORY) "Story Overlay Text" else "Caption") },
                placeholder = { Text("Write a caption or tap Magic AI Captions...") },
                minLines = 2,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("caption_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = viewModel::generateSmartCaptions,
                    enabled = state.aiCaptionState !is AiCaptionState.Loading,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("magic_caption_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = VibeAiViolet,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.magic_caption_cta))
                }
            }

            when (val capState = state.aiCaptionState) {
                is AiCaptionState.Loading -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Crafting captions & hashtags with Gemini...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                is AiCaptionState.Success -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Vibe: ${capState.result.vibeSummary} (tap to apply)",
                                style = MaterialTheme.typography.labelLarge,
                                color = VibeAiViolet
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            capState.result.captions.forEach { option ->
                                Surface(
                                    color = MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clickable {
                                            viewModel.applyGeneratedCaption(option, capState.result.hashtags)
                                        }
                                ) {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                            if (capState.result.hashtags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    capState.result.hashtags.forEach { tag ->
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VibeSocialBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is AiCaptionState.Error -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = capState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                else -> Unit
            }

            if (state.mode != CreateMode.STORY) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.hashtagsText,
                    onValueChange = viewModel::updateHashtags,
                    label = { Text("Hashtags") },
                    placeholder = { Text("#vibe #photography #aesthetic") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.publishState is PublishState.Error) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = (state.publishState as PublishState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    viewModel.publish(
                        authorHandle = myProfile?.handle ?: "creator",
                        authorAvatar = myProfile?.avatarData.orEmpty()
                    )
                },
                enabled = state.publishState !is PublishState.Publishing,
                colors = ButtonDefaults.buttonColors(containerColor = VibeSocialBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("publish_button")
            ) {
                if (state.publishState is PublishState.Publishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.publish_cta),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
