package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MessagingRepository(
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
            ?: throw IllegalStateException("User must be signed in with Google before accessing messages.")
    }

    fun observeConversations(userId: String): Flow<List<Conversation>> =
        db.collection("conversations")
            .whereArrayContains("participantIds", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (auth.currentUser != null) {
                    handleFirestoreError(error, OperationType.LIST, "conversations")
                }
                throw error
            }

    suspend fun getOrCreateDirectConversation(
        currentHandle: String,
        targetUserId: String,
        targetHandle: String
    ): Result<Conversation> = runCatching {
        val uid = requireUserId()
        require(targetUserId != uid) { "Cannot start a conversation with yourself" }
        val sortedPair = listOf(uid, targetUserId).sorted()
        val convId = "dm_${sortedPair[0]}_${sortedPair[1]}"
        val convRef = db.collection("conversations").document(convId)
        val snap = convRef.get().await()
        if (!snap.exists()) {
            val handles = if (sortedPair[0] == uid) {
                listOf(currentHandle.take(30).ifBlank { "creator" }, targetHandle.take(30).ifBlank { "creator" })
            } else {
                listOf(targetHandle.take(30).ifBlank { "creator" }, currentHandle.take(30).ifBlank { "creator" })
            }
            convRef.set(
                mapOf(
                    "participantIds" to sortedPair,
                    "participantHandles" to handles,
                    "lastMessage" to "Started a conversation",
                    "lastSenderId" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        convRef.get().await()
            .toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: Conversation(
                id = convId,
                participantIds = sortedPair,
                participantHandles = listOf(currentHandle, targetHandle),
                lastMessage = "Started a conversation",
                lastSenderId = uid
            )
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "conversations")
    }

    fun observeMessages(conversationId: String, currentUserId: String): Flow<List<DirectMessage>> =
        db.collection("conversations").document(conversationId).collection("messages")
            .whereArrayContains("participantIds", currentUserId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DirectMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (auth.currentUser != null) {
                    handleFirestoreError(error, OperationType.LIST, "conversations/$conversationId/messages")
                }
                throw error
            }

    suspend fun sendMessage(
        conversationId: String,
        participantIds: List<String>,
        senderHandle: String,
        text: String,
        mediaPayload: String = "",
        sharedPostId: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val cleanText = text.trim().take(1000)
        require(cleanText.isNotEmpty() || mediaPayload.isNotBlank() || sharedPostId.isNotBlank()) {
            "Message cannot be empty"
        }

        val convRef = db.collection("conversations").document(conversationId)
        val msgRef = convRef.collection("messages").document()
        val msgPayload = buildMap<String, Any> {
            put("participantIds", participantIds.take(10))
            put("senderId", uid)
            put("senderHandle", senderHandle.take(30).ifBlank { "creator" })
            put("text", cleanText)
            if (mediaPayload.isNotBlank()) put("mediaPayload", mediaPayload.take(400_000))
            if (sharedPostId.isNotBlank()) put("sharedPostId", sharedPostId.take(128))
            put("createdAt", FieldValue.serverTimestamp())
        }

        val preview = when {
            cleanText.isNotBlank() -> cleanText.take(500)
            mediaPayload.isNotBlank() -> "Sent a photo"
            else -> "Shared a post"
        }

        val batch = db.batch()
        batch.set(msgRef, msgPayload)
        batch.update(
            convRef,
            mapOf(
                "lastMessage" to preview,
                "lastSenderId" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        msgRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "conversations/$conversationId/messages")
    }
}
