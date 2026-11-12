package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.FollowRelation
import com.example.data.model.MoodboardCollection
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.PublicProfile
import com.example.data.model.Story
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

class SocialRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun ensureUserBootstrapped(
        uid: String,
        displayName: String?,
        email: String?,
        photoUrl: String?
    ): Result<PublicProfile> = runCatching {
        val accountRef = db.collection("users").document(uid)
        val profileRef = db.collection("public_profiles").document(uid)
        val existingProfileSnap = profileRef.get().await()

        val accountSnap = accountRef.get().await()
        if (!accountSnap.exists()) {
            val accountPayload = buildMap<String, Any> {
                put("userId", uid)
                if (!email.isNullOrBlank()) put("email", email.take(254))
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            accountRef.set(accountPayload).await()
        }

        if (!existingProfileSnap.exists()) {
            val safeName = displayName?.takeIf { it.isNotBlank() }?.take(50) ?: "Vibe Creator"
            val rawHandle = (email?.substringBefore("@") ?: safeName)
                .lowercase()
                .replace(Regex("[^a-z0-9_.]"), "")
                .take(20)
                .ifBlank { "vibe_${uid.take(6).lowercase()}" }
                .let { if (it.length < 2) "vibe_$it" else it }

            val profilePayload = mapOf(
                "userId" to uid,
                "handle" to rawHandle,
                "displayName" to safeName,
                "bio" to "Creating visual moments on Vibe.",
                "avatarData" to (photoUrl?.take(400_000) ?: ""),
                "isDiscoverable" to true,
                "followersCount" to 0L,
                "followingCount" to 0L,
                "postsCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            profileRef.set(profilePayload).await()

            val defaultBoardRef = accountRef.collection("collections").document("saved_inspo")
            if (!defaultBoardRef.get().await().exists()) {
                defaultBoardRef.set(
                    mapOf(
                        "userId" to uid,
                        "name" to "Saved Inspo",
                        "postIds" to emptyList<String>(),
                        "coverPayload" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }

        profileRef.get().await()
            .toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: PublicProfile(id = uid, userId = uid, handle = "creator", displayName = displayName ?: "Vibe Creator")
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "public_profiles/$uid")
    }

    fun observePublicProfiles(): Flow<List<PublicProfile>> =
        db.collection("public_profiles")
            .whereEqualTo("isDiscoverable", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (auth.currentUser != null) {
                    handleFirestoreError(error, OperationType.LIST, "public_profiles")
                }
                throw error
            }

    fun observeProfile(userId: String): Flow<PublicProfile?> =
        db.collection("public_profiles").document(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (auth.currentUser != null) {
                    handleFirestoreError(error, OperationType.GET, "public_profiles/$userId")
                }
                throw error
            }

    suspend fun updateProfile(
        handle: String,
        displayName: String,
        bio: String,
        avatarData: String?
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val cleanHandle = handle.trim().removePrefix("@").take(30)
        require(cleanHandle.length >= 2) { "Handle must be at least 2 characters" }
        val cleanName = displayName.trim().take(50)
        require(cleanName.isNotEmpty()) { "Display name cannot be empty" }

        val updates = buildMap<String, Any> {
            put("handle", cleanHandle)
            put("displayName", cleanName)
            put("bio", bio.trim().take(200))
            if (avatarData != null) put("avatarData", avatarData.take(400_000))
            put("updatedAt", FieldValue.serverTimestamp())
        }
        db.collection("public_profiles").document(uid).update(updates).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "public_profiles/${auth.currentUser?.uid}")
    }

    fun observePublicPosts(): Flow<List<Post>> =
        db.collection("posts")
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (auth.currentUser != null) {
                    handleFirestoreError(error, OperationType.LIST, "posts")
                }
                throw error
            }

    fun observeUserPosts(userId: String): Flow<List<Post>> =
        db.collection("posts")
            .whereEqualTo("authorId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (auth.currentUser != null) {
                    handleFirestoreError(error, OperationType.LIST, "posts?authorId=$userId")
                }
                throw error
            }

    suspend fun createPost(
        authorHandle: String,
        authorAvatar: String,
        mediaType: String,
        mediaPayloads: List<String>,
        localVideoPath: String = "",
        filterPreset: String = "ORIGINAL",
        caption: String,
        hashtags: List<String>,
        pollQuestion: String = "",
        pollOptions: List<String> = emptyList()
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val postRef = db.collection("posts").document()
        val profileRef = db.collection("public_profiles").document(uid)

        val boundedMedia = mediaPayloads.take(4).map { it.take(400_000) }
        val boundedTags = hashtags.take(10).map { it.trim().take(40) }.filter { it.isNotBlank() }
        val boundedPollOpts = pollOptions.take(4).map { it.trim().take(80) }.filter { it.isNotBlank() }
        val zeroCounts = List(boundedPollOpts.size) { 0L }

        val postPayload = buildMap<String, Any> {
            put("authorId", uid)
            put("authorHandle", authorHandle.take(30).ifBlank { "creator" })
            put("authorAvatar", authorAvatar.take(400_000))
            put("visibility", "public")
            put("mediaType", mediaType)
            put("mediaPayloads", boundedMedia)
            if (localVideoPath.isNotBlank()) put("localVideoPath", localVideoPath.take(300))
            put("filterPreset", filterPreset.take(40))
            put("caption", caption.trim().take(1000))
            put("hashtags", boundedTags)
            if (pollQuestion.isNotBlank()) put("pollQuestion", pollQuestion.trim().take(200))
            if (boundedPollOpts.isNotEmpty()) {
                put("pollOptions", boundedPollOpts)
                put("pollCounts", zeroCounts)
            }
            put("likesCount", 0L)
            put("commentsCount", 0L)
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }

        val batch = db.batch()
        batch.set(postRef, postPayload)
        batch.update(
            profileRef,
            mapOf(
                "postsCount" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        postRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "posts")
    }

    suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(postId)
        val profileRef = db.collection("public_profiles").document(uid)
        val batch = db.batch()
        batch.delete(postRef)
        batch.update(
            profileRef,
            mapOf(
                "postsCount" to FieldValue.increment(-1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "posts/$postId")
    }

    suspend fun toggleLike(postId: String, currentlyLiked: Boolean): Result<Boolean> = runCatching {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(postId)
        val likeRef = postRef.collection("likes").document(uid)
        val batch = db.batch()
        if (currentlyLiked) {
            batch.delete(likeRef)
            batch.update(
                postRef,
                mapOf(
                    "likesCount" to FieldValue.increment(-1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        } else {
            batch.set(
                likeRef,
                mapOf(
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                postRef,
                mapOf(
                    "likesCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
        !currentlyLiked
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "posts/$postId/likes")
    }

    suspend fun isPostLikedByMe(postId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            db.collection("posts").document(postId)
                .collection("likes").document(uid)
                .get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    fun observeComments(postId: String): Flow<List<PostComment>> =
        db.collection("posts").document(postId).collection("comments")
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PostComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (auth.currentUser != null) {
                    handleFirestoreError(error, OperationType.LIST, "posts/$postId/comments")
                }
                throw error
            }

    suspend fun addComment(
        postId: String,
        authorHandle: String,
        authorAvatar: String,
        text: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val trimmed = text.trim().take(500)
        require(trimmed.isNotEmpty()) { "Comment cannot be empty" }
        val postRef = db.collection("posts").document(postId)
        val commentRef = postRef.collection("comments").document()

        val batch = db.batch()
        batch.set(
            commentRef,
            mapOf(
                "authorId" to uid,
                "authorHandle" to authorHandle.take(30).ifBlank { "creator" },
                "authorAvatar" to authorAvatar.take(400_000),
                "visibility" to "public",
                "text" to trimmed,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            postRef,
            mapOf(
                "commentsCount" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        commentRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "posts/$postId/comments")
    }

    suspend fun castPollVote(postId: String, optionIndex: Int): Result<Unit> = runCatching {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(postId)
        val voteRef = postRef.collection("pollVotes").document(uid)

        db.runTransaction { tx ->
            val existingVote = tx.get(voteRef)
            if (existingVote.exists()) return@runTransaction
            val postSnap = tx.get(postRef)
            val currentCounts = (postSnap.get("pollCounts") as? List<*>)
                ?.map { (it as? Number)?.toLong() ?: 0L }
                ?.toMutableList()
                ?: mutableListOf()
            if (optionIndex in currentCounts.indices) {
                currentCounts[optionIndex] = currentCounts[optionIndex] + 1L
                tx.set(
                    voteRef,
                    mapOf(
                        "userId" to uid,
                        "optionIndex" to optionIndex.toLong(),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    postRef,
                    mapOf(
                        "pollCounts" to currentCounts,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }.await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "posts/$postId/pollVotes")
    }

    suspend fun getMyPollVote(postId: String): Int? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snap = db.collection("posts").document(postId)
                .collection("pollVotes").document(uid).get().await()
            if (snap.exists()) snap.getLong("optionIndex")?.toInt() else null
        } catch (_: Exception) {
            null
        }
    }

    fun observeActiveStories(): Flow<List<Story>> =
        db.collection("stories")
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                val now = Timestamp.now()
                snapshot.toObjects(Story::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { story ->
                        val exp = story.expiresAt
                        exp == null || exp.seconds >= now.seconds
                    }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (auth.currentUser != null) {
                    handleFirestoreError(error, OperationType.LIST, "stories")
                }
                throw error
            }

    suspend fun createStory(
        authorHandle: String,
        authorAvatar: String,
        mediaPayload: String,
        captionOverlay: String = "",
        moodTag: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val storyRef = db.collection("stories").document()
        val expiresAt = Timestamp(Date(System.currentTimeMillis() + 24L * 60L * 60L * 1000L))
        val payload = buildMap<String, Any> {
            put("authorId", uid)
            put("authorHandle", authorHandle.take(30).ifBlank { "creator" })
            put("authorAvatar", authorAvatar.take(400_000))
            put("visibility", "public")
            put("mediaPayload", mediaPayload.take(400_000))
            if (captionOverlay.isNotBlank()) put("captionOverlay", captionOverlay.trim().take(150))
            if (moodTag.isNotBlank()) put("moodTag", moodTag.trim().take(40))
            put("createdAt", FieldValue.serverTimestamp())
            put("expiresAt", expiresAt)
        }
        storyRef.set(payload).await()
        storyRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "stories")
    }

    fun observeFollowingIds(userId: String): Flow<Set<String>> =
        db.collection("follows")
            .whereEqualTo("followerId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FollowRelation::class.java).map { it.targetId }.toSet()
            }
            .catch { error ->
                if (auth.currentUser != null) {
                    handleFirestoreError(error, OperationType.LIST, "follows")
                }
                throw error
            }

    suspend fun toggleFollow(targetUserId: String, currentlyFollowing: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(targetUserId != uid) { "Cannot follow yourself" }
        val followId = "${uid}_$targetUserId"
        val followRef = db.collection("follows").document(followId)
        val myProfileRef = db.collection("public_profiles").document(uid)
        val targetProfileRef = db.collection("public_profiles").document(targetUserId)

        val batch = db.batch()
        if (currentlyFollowing) {
            batch.delete(followRef)
            batch.update(
                myProfileRef,
                mapOf(
                    "followingCount" to FieldValue.increment(-1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                targetProfileRef,
                mapOf(
                    "followersCount" to FieldValue.increment(-1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        } else {
            batch.set(
                followRef,
                mapOf(
                    "followerId" to uid,
                    "targetId" to targetUserId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                myProfileRef,
                mapOf(
                    "followingCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                targetProfileRef,
                mapOf(
                    "followersCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "follows")
    }

    fun observeCollections(userId: String): Flow<List<MoodboardCollection>> =
        db.collection("users").document(userId).collection("collections")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(MoodboardCollection::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (auth.currentUser != null) {
                    handleFirestoreError(error, OperationType.LIST, "users/$userId/collections")
                }
                throw error
            }

    suspend fun createCollection(
        name: String,
        initialPostId: String? = null,
        coverPayload: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val cleanName = name.trim().take(40)
        require(cleanName.isNotEmpty()) { "Collection name cannot be empty" }
        val colRef = db.collection("users").document(uid).collection("collections").document()
        val postIds = if (!initialPostId.isNullOrBlank()) listOf(initialPostId) else emptyList()
        colRef.set(
            mapOf(
                "userId" to uid,
                "name" to cleanName,
                "postIds" to postIds,
                "coverPayload" to coverPayload.take(400_000),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        colRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "users/${auth.currentUser?.uid}/collections")
    }

    suspend fun togglePostInCollection(
        collectionId: String,
        postId: String,
        coverPayload: String = ""
    ): Result<Boolean> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("collections").document(collectionId)
        val snap = colRef.get().await()
        val currentIds = (snap.get("postIds") as? List<*>)?.mapNotNull { it as? String }?.toMutableList()
            ?: mutableListOf()
        val existingCover = snap.getString("coverPayload").orEmpty()
        val added: Boolean
        if (postId in currentIds) {
            currentIds.remove(postId)
            added = false
        } else {
            if (currentIds.size < 100) {
                currentIds.add(postId)
            }
            added = true
        }
        val updates = buildMap<String, Any> {
            put("postIds", currentIds)
            if (existingCover.isBlank() && coverPayload.isNotBlank() && added) {
                put("coverPayload", coverPayload.take(400_000))
            }
            put("updatedAt", FieldValue.serverTimestamp())
        }
        colRef.update(updates).await()
        added
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/collections/$collectionId")
    }
}
