package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val handle: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarData: String = "",
    val isDiscoverable: Boolean = true,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val postsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MoodboardCollection(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val postIds: List<String> = emptyList(),
    val coverPayload: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FollowRelation(
    @DocumentId val id: String = "",
    val followerId: String = "",
    val targetId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Post(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorHandle: String = "",
    val authorAvatar: String = "",
    val visibility: String = "public",
    val mediaType: String = "photo",
    val mediaPayloads: List<String> = emptyList(),
    val localVideoPath: String = "",
    val filterPreset: String = "ORIGINAL",
    val caption: String = "",
    val hashtags: List<String> = emptyList(),
    val pollQuestion: String = "",
    val pollOptions: List<String> = emptyList(),
    val pollCounts: List<Long> = emptyList(),
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorHandle: String = "",
    val authorAvatar: String = "",
    val visibility: String = "public",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PollVote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val optionIndex: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Story(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorHandle: String = "",
    val authorAvatar: String = "",
    val visibility: String = "public",
    val mediaPayload: String = "",
    val captionOverlay: String = "",
    val moodTag: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantHandles: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DirectMessage(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val senderId: String = "",
    val senderHandle: String = "",
    val text: String = "",
    val mediaPayload: String = "",
    val sharedPostId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
