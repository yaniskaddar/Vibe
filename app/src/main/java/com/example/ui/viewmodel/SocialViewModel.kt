package com.example.ui.viewmodel

import android.util.Log
import com.example.core.unwrapFirestoreMessage
import com.example.data.model.MoodboardCollection
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.PublicProfile
import com.example.data.model.Story
import com.example.data.model.UiState
import com.example.data.repository.SocialRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class SocialViewModel(
    private val repository: SocialRepository,
    val currentUserId: String,
    initialDisplayName: String?,
    initialEmail: String?,
    initialPhotoUrl: String?
) : ViewModel() {

    val myProfile: StateFlow<PublicProfile?> = repository.observeProfile(currentUserId)
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            Log.w(TAG, "Failed to observe myProfile", e)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val allProfiles: StateFlow<List<PublicProfile>> = repository.observePublicProfiles()
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            Log.w(TAG, "Failed to observe public profiles", e)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val feedPosts: StateFlow<UiState<List<Post>>> = repository.observePublicPosts()
        .map<List<Post>, UiState<List<Post>>> { posts ->
            hydrateLikesAndPolls(posts)
            UiState.Success(posts)
        }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing feed posts", fsErr ?: error)
            emit(UiState.Error(error.unwrapFirestoreMessage("Unable to load Vibe feed.")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val activeStories: StateFlow<List<Story>> = repository.observeActiveStories()
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            Log.w(TAG, "Failed to observe stories", e)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val followingIds: StateFlow<Set<String>> = repository.observeFollowingIds(currentUserId)
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            Log.w(TAG, "Failed to observe following IDs", e)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptySet())

    val myCollections: StateFlow<List<MoodboardCollection>> = repository.observeCollections(currentUserId)
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            Log.w(TAG, "Failed to observe collections", e)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _likedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val likedPostIds: StateFlow<Set<String>> = _likedPostIds.asStateFlow()

    private val _myPollVotes = MutableStateFlow<Map<String, Int>>(emptyMap())
    val myPollVotes: StateFlow<Map<String, Int>> = _myPollVotes.asStateFlow()

    private val _selectedCommentsPostId = MutableStateFlow<String?>(null)
    val selectedCommentsPostId: StateFlow<String?> = _selectedCommentsPostId.asStateFlow()

    val commentsForSelectedPost: StateFlow<List<PostComment>> = _selectedCommentsPostId
        .flatMapLatest { postId ->
            if (postId.isNullOrBlank()) flowOf(emptyList())
            else repository.observeComments(postId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _actionFeedback = MutableStateFlow<String?>(null)
    val actionFeedback: StateFlow<String?> = _actionFeedback.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserBootstrapped(
                uid = currentUserId,
                displayName = initialDisplayName,
                email = initialEmail,
                photoUrl = initialPhotoUrl
            ).onFailure { e ->
                _actionFeedback.value = e.unwrapFirestoreMessage("Could not initialize Vibe profile.")
            }
        }
    }

    private fun hydrateLikesAndPolls(posts: List<Post>) {
        viewModelScope.launch {
            val liked = mutableSetOf<String>()
            val pollVotes = mutableMapOf<String, Int>()
            posts.take(30).forEach { post ->
                if (repository.isPostLikedByMe(post.id)) {
                    liked.add(post.id)
                }
                if (post.mediaType == "poll") {
                    repository.getMyPollVote(post.id)?.let { choice ->
                        pollVotes[post.id] = choice
                    }
                }
            }
            _likedPostIds.value = _likedPostIds.value + liked
            _myPollVotes.value = _myPollVotes.value + pollVotes
        }
    }

    fun toggleLike(post: Post) {
        val currentlyLiked = post.id in _likedPostIds.value
        _likedPostIds.value = if (currentlyLiked) {
            _likedPostIds.value - post.id
        } else {
            _likedPostIds.value + post.id
        }
        viewModelScope.launch {
            repository.toggleLike(post.id, currentlyLiked).onFailure { e ->
                // Revert optimistic toggle on failure
                _likedPostIds.value = if (currentlyLiked) {
                    _likedPostIds.value + post.id
                } else {
                    _likedPostIds.value - post.id
                }
                _actionFeedback.value = e.unwrapFirestoreMessage("Failed to update like.")
            }
        }
    }

    fun openComments(postId: String) {
        _selectedCommentsPostId.value = postId
    }

    fun closeComments() {
        _selectedCommentsPostId.value = null
    }

    fun submitComment(postId: String, text: String) {
        if (text.isBlank()) return
        val profile = myProfile.value
        val handle = profile?.handle?.ifBlank { "creator" } ?: "creator"
        val avatar = profile?.avatarData.orEmpty()
        viewModelScope.launch {
            repository.addComment(postId, handle, avatar, text).onFailure { e ->
                _actionFeedback.value = e.unwrapFirestoreMessage("Failed to post comment.")
            }
        }
    }

    fun castPollVote(postId: String, optionIndex: Int) {
        if (_myPollVotes.value.containsKey(postId)) return
        _myPollVotes.value = _myPollVotes.value + (postId to optionIndex)
        viewModelScope.launch {
            repository.castPollVote(postId, optionIndex).onFailure { e ->
                _myPollVotes.value = _myPollVotes.value - postId
                _actionFeedback.value = e.unwrapFirestoreMessage("Vote could not be recorded.")
            }
        }
    }

    fun toggleFollow(targetUserId: String) {
        if (targetUserId == currentUserId) return
        val currentlyFollowing = targetUserId in followingIds.value
        viewModelScope.launch {
            repository.toggleFollow(targetUserId, currentlyFollowing).onFailure { e ->
                _actionFeedback.value = e.unwrapFirestoreMessage("Could not update follow state.")
            }
        }
    }

    fun togglePostInCollection(collectionId: String, post: Post) {
        viewModelScope.launch {
            val cover = post.mediaPayloads.firstOrNull().orEmpty()
            repository.togglePostInCollection(collectionId, post.id, cover)
                .onSuccess { added ->
                    _actionFeedback.value = if (added) "Saved to collection" else "Removed from collection"
                }
                .onFailure { e ->
                    _actionFeedback.value = e.unwrapFirestoreMessage("Failed to update collection.")
                }
        }
    }

    fun createCollectionAndSave(name: String, post: Post?) {
        viewModelScope.launch {
            val cover = post?.mediaPayloads?.firstOrNull().orEmpty()
            repository.createCollection(name, post?.id, cover)
                .onSuccess {
                    _actionFeedback.value = "Created \"$name\" moodboard"
                }
                .onFailure { e ->
                    _actionFeedback.value = e.unwrapFirestoreMessage("Could not create moodboard.")
                }
        }
    }

    fun updateProfile(handle: String, displayName: String, bio: String, avatarData: String?) {
        viewModelScope.launch {
            repository.updateProfile(handle, displayName, bio, avatarData)
                .onSuccess {
                    _actionFeedback.value = "Profile updated"
                }
                .onFailure { e ->
                    _actionFeedback.value = e.unwrapFirestoreMessage("Could not save profile.")
                }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId)
                .onSuccess { _actionFeedback.value = "Post deleted" }
                .onFailure { e -> _actionFeedback.value = e.unwrapFirestoreMessage("Failed to delete post.") }
        }
    }

    fun clearFeedback() {
        _actionFeedback.value = null
    }

    private companion object {
        const val TAG = "SocialViewModel"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
