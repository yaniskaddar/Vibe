package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.unwrapFirestoreMessage
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.data.repository.MessagingRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesViewModel(
    private val repository: MessagingRepository,
    val currentUserId: String
) : ViewModel() {

    val conversations: StateFlow<UiState<List<Conversation>>> =
        repository.observeConversations(currentUserId)
            .map<List<Conversation>, UiState<List<Conversation>>> { UiState.Success(it) }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                Log.w(TAG, "Failed to observe conversations", fsErr ?: error)
                emit(UiState.Error(error.unwrapFirestoreMessage("Unable to load messages.")))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    private val _activeConversation = MutableStateFlow<Conversation?>(null)
    val activeConversation: StateFlow<Conversation?> = _activeConversation.asStateFlow()

    val activeMessages: StateFlow<List<DirectMessage>> = _activeConversation
        .flatMapLatest { conv ->
            if (conv == null) flowOf(emptyList())
            else repository.observeMessages(conv.id, currentUserId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _errorFeedback = MutableStateFlow<String?>(null)
    val errorFeedback: StateFlow<String?> = _errorFeedback.asStateFlow()

    fun openConversation(conversation: Conversation) {
        _activeConversation.value = conversation
    }

    fun closeConversation() {
        _activeConversation.value = null
    }

    fun startDirectChat(myHandle: String, target: PublicProfile, onOpened: () -> Unit = {}) {
        if (target.userId == currentUserId) return
        viewModelScope.launch {
            repository.getOrCreateDirectConversation(
                currentHandle = myHandle,
                targetUserId = target.userId,
                targetHandle = target.handle
            ).onSuccess { conv ->
                _activeConversation.value = conv
                onOpened()
            }.onFailure { e ->
                _errorFeedback.value = e.unwrapFirestoreMessage("Could not open conversation.")
            }
        }
    }

    fun sendMessage(
        myHandle: String,
        text: String,
        mediaPayload: String = "",
        sharedPostId: String = ""
    ) {
        val conv = _activeConversation.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                conversationId = conv.id,
                participantIds = conv.participantIds,
                senderHandle = myHandle,
                text = text,
                mediaPayload = mediaPayload,
                sharedPostId = sharedPostId
            ).onFailure { e ->
                _errorFeedback.value = e.unwrapFirestoreMessage("Failed to send message.")
            }
        }
    }

    fun sharePostToUser(
        myHandle: String,
        target: PublicProfile,
        postId: String,
        postCaption: String
    ) {
        if (target.userId == currentUserId) return
        viewModelScope.launch {
            repository.getOrCreateDirectConversation(
                currentHandle = myHandle,
                targetUserId = target.userId,
                targetHandle = target.handle
            ).onSuccess { conv ->
                repository.sendMessage(
                    conversationId = conv.id,
                    participantIds = conv.participantIds,
                    senderHandle = myHandle,
                    text = if (postCaption.isNotBlank()) "Shared a post: ${postCaption.take(80)}" else "Check out this Vibe post!",
                    sharedPostId = postId
                )
            }.onFailure { e ->
                _errorFeedback.value = e.unwrapFirestoreMessage("Failed to share post.")
            }
        }
    }

    fun clearError() {
        _errorFeedback.value = null
    }

    private companion object {
        const val TAG = "MessagesVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
