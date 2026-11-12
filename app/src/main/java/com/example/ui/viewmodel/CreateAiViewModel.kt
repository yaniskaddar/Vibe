package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.FilterPreset
import com.example.core.ImageUtils
import com.example.core.unwrapFirestoreMessage
import com.example.data.repository.AiCaptionResult
import com.example.data.repository.FirebaseAiRepository
import com.example.data.repository.SocialRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class CreateMode(val title: String) {
    POST("Post"),
    STORY("Story"),
    REEL("Reel"),
    POLL("Poll"),
    AI_ART("AI Studio")
}

sealed interface AiCaptionState {
    data object Idle : AiCaptionState
    data object Loading : AiCaptionState
    data class Success(val result: AiCaptionResult) : AiCaptionState
    data class Error(val message: String) : AiCaptionState
}

sealed interface AiArtState {
    data object Idle : AiArtState
    data object Loading : AiArtState
    data class Success(val bitmap: Bitmap) : AiArtState
    data class Error(val message: String) : AiArtState
}

sealed interface PublishState {
    data object Idle : PublishState
    data object Publishing : PublishState
    data class Published(val mode: CreateMode) : PublishState
    data class Error(val message: String) : PublishState
}

data class CreateStudioUiState(
    val mode: CreateMode = CreateMode.POST,
    val selectedImages: List<Bitmap> = emptyList(),
    val selectedFilter: FilterPreset = FilterPreset.ORIGINAL,
    val localVideoPath: String? = null,
    val videoThumbnail: Bitmap? = null,
    val caption: String = "",
    val hashtagsText: String = "",
    val storyMoodTag: String = "Golden Hour",
    val pollQuestion: String = "",
    val pollOptions: List<String> = listOf("", ""),
    val aiArtPrompt: String = "",
    val aiCaptionState: AiCaptionState = AiCaptionState.Idle,
    val aiArtState: AiArtState = AiArtState.Idle,
    val publishState: PublishState = PublishState.Idle
)

class CreateAiViewModel(
    private val socialRepository: SocialRepository,
    private val aiRepository: FirebaseAiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateStudioUiState())
    val uiState: StateFlow<CreateStudioUiState> = _uiState.asStateFlow()

    fun setMode(mode: CreateMode) {
        _uiState.update { it.copy(mode = mode, publishState = PublishState.Idle) }
    }

    fun setFilter(preset: FilterPreset) {
        _uiState.update { it.copy(selectedFilter = preset) }
    }

    fun updateCaption(value: String) {
        _uiState.update { it.copy(caption = value) }
    }

    fun updateHashtags(value: String) {
        _uiState.update { it.copy(hashtagsText = value) }
    }

    fun updateStoryMood(value: String) {
        _uiState.update { it.copy(storyMoodTag = value) }
    }

    fun updatePollQuestion(value: String) {
        _uiState.update { it.copy(pollQuestion = value) }
    }

    fun updatePollOption(index: Int, value: String) {
        _uiState.update { state ->
            val updated = state.pollOptions.toMutableList()
            if (index in updated.indices) updated[index] = value
            state.copy(pollOptions = updated)
        }
    }

    fun addPollOption() {
        _uiState.update { state ->
            if (state.pollOptions.size < 4) state.copy(pollOptions = state.pollOptions + "") else state
        }
    }

    fun updateAiArtPrompt(value: String) {
        _uiState.update { it.copy(aiArtPrompt = value) }
    }

    fun addPickedImages(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val decoded = uris.take(4).mapNotNull { uri ->
                ImageUtils.decodeUriToBitmap(context, uri)
            }
            _uiState.update { state ->
                val merged = (state.selectedImages + decoded).take(4)
                state.copy(selectedImages = merged, publishState = PublishState.Idle)
            }
        }
    }

    fun addCapturedBitmap(bitmap: Bitmap) {
        _uiState.update { state ->
            val merged = (state.selectedImages + bitmap).take(4)
            state.copy(selectedImages = merged, publishState = PublishState.Idle)
        }
    }

    fun applyCameraCapture(
        bitmap: Bitmap,
        target: CameraCaptureTarget,
        filter: FilterPreset
    ) {
        val targetMode = when (target) {
            CameraCaptureTarget.STORY -> CreateMode.STORY
            CameraCaptureTarget.POST -> CreateMode.POST
        }
        _uiState.update { state ->
            val merged = if (targetMode == CreateMode.STORY) {
                listOf(bitmap)
            } else {
                (state.selectedImages + bitmap).take(4)
            }
            state.copy(
                mode = targetMode,
                selectedImages = merged,
                selectedFilter = filter,
                publishState = PublishState.Idle
            )
        }
    }

    fun removeImageAt(index: Int) {
        _uiState.update { state ->
            val remaining = state.selectedImages.filterIndexed { i, _ -> i != index }
            state.copy(selectedImages = remaining)
        }
    }

    fun setPickedVideo(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = ImageUtils.cacheVideoAndExtractThumbnail(context, uri)
            if (result != null) {
                val (path, thumbUri) = result
                val thumbBitmap = thumbUri?.let { ImageUtils.decodeDataUriToBitmap(it) }
                _uiState.update {
                    it.copy(
                        localVideoPath = path,
                        videoThumbnail = thumbBitmap,
                        publishState = PublishState.Idle
                    )
                }
            } else {
                _uiState.update {
                    it.copy(publishState = PublishState.Error("Could not import selected video."))
                }
            }
        }
    }

    fun generateSmartCaptions() {
        val current = _uiState.value
        val activeBitmap = current.selectedImages.firstOrNull() ?: current.videoThumbnail
        val hint = current.caption.ifBlank { current.pollQuestion }
        viewModelScope.launch {
            _uiState.update { it.copy(aiCaptionState = AiCaptionState.Loading) }
            val result = aiRepository.generateSmartCaptions(activeBitmap, hint)
            result.onSuccess { captionRes ->
                _uiState.update { it.copy(aiCaptionState = AiCaptionState.Success(captionRes)) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        aiCaptionState = AiCaptionState.Error(
                            e.localizedMessage ?: "Gemini caption generation failed."
                        )
                    )
                }
            }
        }
    }

    fun applyGeneratedCaption(caption: String, hashtags: List<String>) {
        _uiState.update {
            it.copy(
                caption = caption,
                hashtagsText = hashtags.joinToString(" ")
            )
        }
    }

    fun generateAiArt(remixCurrent: Boolean) {
        val current = _uiState.value
        val prompt = current.aiArtPrompt.trim()
        if (prompt.isEmpty()) {
            _uiState.update { it.copy(aiArtState = AiArtState.Error("Describe what you want Vibe AI Studio to create.")) }
            return
        }
        val source = if (remixCurrent) current.selectedImages.firstOrNull() else null
        viewModelScope.launch {
            _uiState.update { it.copy(aiArtState = AiArtState.Loading) }
            val result = aiRepository.generateOrRemixImage(prompt, source)
            result.onSuccess { generatedBitmap ->
                _uiState.update { state ->
                    val updatedList = (listOf(generatedBitmap) + state.selectedImages).take(4)
                    state.copy(
                        aiArtState = AiArtState.Success(generatedBitmap),
                        selectedImages = updatedList
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        aiArtState = AiArtState.Error(
                            e.localizedMessage ?: "AI image synthesis failed."
                        )
                    )
                }
            }
        }
    }

    fun publish(authorHandle: String, authorAvatar: String) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(publishState = PublishState.Publishing) }
            try {
                val parsedTags = state.hashtagsText
                    .split(Regex("[\\s,]+"))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .map { if (it.startsWith("#")) it else "#$it" }
                    .take(10)

                val result: Result<String> = when (state.mode) {
                    CreateMode.STORY -> {
                        val bmp = state.selectedImages.firstOrNull()
                            ?: throw IllegalStateException("Select or capture a photo for your Story.")
                        val payload = withContext(Dispatchers.Default) {
                            ImageUtils.compressBitmapToDataUri(bmp, maxDimension = 720, maxBytes = 180_000)
                        }
                        socialRepository.createStory(
                            authorHandle = authorHandle,
                            authorAvatar = authorAvatar,
                            mediaPayload = payload,
                            captionOverlay = state.caption,
                            moodTag = state.storyMoodTag
                        )
                    }
                    CreateMode.REEL -> {
                        val thumb = state.videoThumbnail ?: state.selectedImages.firstOrNull()
                        val mediaPayloads = if (thumb != null) {
                            listOf(
                                withContext(Dispatchers.Default) {
                                    ImageUtils.compressBitmapToDataUri(thumb, maxDimension = 640, maxBytes = 150_000)
                                }
                            )
                        } else emptyList()
                        if (state.localVideoPath.isNullOrBlank() && mediaPayloads.isEmpty()) {
                            throw IllegalStateException("Select a video or cover image for your Reel.")
                        }
                        socialRepository.createPost(
                            authorHandle = authorHandle,
                            authorAvatar = authorAvatar,
                            mediaType = "reel",
                            mediaPayloads = mediaPayloads,
                            localVideoPath = state.localVideoPath.orEmpty(),
                            filterPreset = state.selectedFilter.name,
                            caption = state.caption.ifBlank { "New Reel on Vibe" },
                            hashtags = parsedTags
                        )
                    }
                    CreateMode.POLL -> {
                        val cleanQuestion = state.pollQuestion.trim()
                        val cleanOptions = state.pollOptions.map { it.trim() }.filter { it.isNotBlank() }
                        if (cleanQuestion.isEmpty() || cleanOptions.size < 2) {
                            throw IllegalStateException("Enter a poll question and at least 2 options.")
                        }
                        val mediaPayloads = withContext(Dispatchers.Default) {
                            state.selectedImages.take(1).map {
                                ImageUtils.compressBitmapToDataUri(it, maxDimension = 640, maxBytes = 140_000)
                            }
                        }
                        socialRepository.createPost(
                            authorHandle = authorHandle,
                            authorAvatar = authorAvatar,
                            mediaType = "poll",
                            mediaPayloads = mediaPayloads,
                            filterPreset = state.selectedFilter.name,
                            caption = state.caption.ifBlank { cleanQuestion },
                            hashtags = parsedTags,
                            pollQuestion = cleanQuestion,
                            pollOptions = cleanOptions
                        )
                    }
                    CreateMode.POST, CreateMode.AI_ART -> {
                        if (state.selectedImages.isEmpty()) {
                            throw IllegalStateException("Pick, capture, or generate at least one photo.")
                        }
                        val maxPerImage = if (state.selectedImages.size > 1) 115_000 else 185_000
                        val mediaPayloads = withContext(Dispatchers.Default) {
                            state.selectedImages.map {
                                ImageUtils.compressBitmapToDataUri(it, maxDimension = 720, maxBytes = maxPerImage)
                            }
                        }
                        val type = when {
                            state.mode == CreateMode.AI_ART -> "ai_art"
                            mediaPayloads.size > 1 -> "carousel"
                            else -> "photo"
                        }
                        socialRepository.createPost(
                            authorHandle = authorHandle,
                            authorAvatar = authorAvatar,
                            mediaType = type,
                            mediaPayloads = mediaPayloads,
                            filterPreset = state.selectedFilter.name,
                            caption = state.caption,
                            hashtags = parsedTags
                        )
                    }
                }

                result.onSuccess {
                    _uiState.value = CreateStudioUiState(publishState = PublishState.Published(state.mode))
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(publishState = PublishState.Error(e.unwrapFirestoreMessage("Publish failed.")))
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(publishState = PublishState.Error(e.localizedMessage ?: "Cannot publish yet."))
                }
            }
        }
    }

    fun resetPublishStatus() {
        _uiState.update { it.copy(publishState = PublishState.Idle) }
    }
}
