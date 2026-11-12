package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class VibeRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun bootstrapProfileAndCreatePost_validFlow_succeedsAndEmits(): Unit = runBlocking {
        val aliceUid = signInTestUser("alice_${UUID.randomUUID()}@vibe.test")
        val repo = SocialRepository(firestore, auth)

        val profileRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserBootstrapped(aliceUid, "Alice Vibe", "alice@vibe.test", null)
        }
        assertTrue(profileRes.isSuccess)

        val postRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createPost(
                authorHandle = "alice_vibe",
                authorAvatar = "",
                mediaType = "photo",
                mediaPayloads = listOf("data:image/jpeg;base64,ZmFrZQ=="),
                caption = "Golden hour capture",
                hashtags = listOf("#vibe", "#goldenhour")
            )
        }
        assertTrue(postRes.isSuccess)
        val postId = postRes.getOrThrow()

        val posts = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePublicPosts().first { list -> list.any { it.id == postId } }
        }
        assertTrue(posts.any { it.id == postId && it.caption == "Golden hour capture" })
    }

    @Test
    fun likeCommentAndPoll_crossUserInteraction_succeeds(): Unit = runBlocking {
        val aliceUid = signInTestUser("alice2_${UUID.randomUUID()}@vibe.test")
        val aliceRepo = SocialRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureUserBootstrapped(aliceUid, "Alice", null, null).getOrThrow()
        }
        val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createPost(
                authorHandle = "alice",
                authorAvatar = "",
                mediaType = "poll",
                mediaPayloads = emptyList(),
                caption = "Pick your vibe",
                hashtags = listOf("#poll"),
                pollQuestion = "Film or Neon?",
                pollOptions = listOf("Film", "Neon")
            ).getOrThrow()
        }

        signInTestUser("bob_${UUID.randomUUID()}@vibe.test")
        val bobRepo = SocialRepository(firestore, auth)
        val likeRes = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.toggleLike(postId, false) }
        assertTrue(likeRes.isSuccess)

        val commentRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.addComment(postId, "bob", "", "Film all day!")
        }
        assertTrue(commentRes.isSuccess)

        val voteRes = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.castPollVote(postId, 0) }
        assertTrue(voteRes.isSuccess)
    }

    @Test
    fun deletePost_crossUserAttempt_failsWithPermissionDenied(): Unit = runBlocking {
        val aliceUid = signInTestUser("alice3_${UUID.randomUUID()}@vibe.test")
        val aliceRepo = SocialRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureUserBootstrapped(aliceUid, "Alice", null, null).getOrThrow()
        }
        val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createPost(
                authorHandle = "alice",
                authorAvatar = "",
                mediaType = "photo",
                mediaPayloads = listOf("data:image/jpeg;base64,ZmFrZQ=="),
                caption = "Protected post",
                hashtags = emptyList()
            ).getOrThrow()
        }

        signInTestUser("bob3_${UUID.randomUUID()}@vibe.test")
        val bobRepo = SocialRepository(firestore, auth)
        val deleteRes = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.deletePost(postId) }
        assertTrue(deleteRes.isFailure)
        val fsEx = deleteRes.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observePublicPosts_unauthenticatedCaller_failsWithPermissionDenied(): Unit = runBlocking {
        auth.signOut()
        val repo = SocialRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observePublicPosts().first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull(caught)
        val fsEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun messaging_participantsCanSendAndObserve_succeeds(): Unit = runBlocking {
        val aliceUid = signInTestUser("msg_alice_${UUID.randomUUID()}@vibe.test")
        val bobUid = signInTestUser("msg_bob_${UUID.randomUUID()}@vibe.test")

        val msgRepo = MessagingRepository(firestore, auth)
        val convRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            msgRepo.getOrCreateDirectConversation("bob", aliceUid, "alice")
        }
        assertTrue("convRes failed: ${convRes.exceptionOrNull()}", convRes.isSuccess)
        val conv = convRes.getOrThrow()

        val sendRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            msgRepo.sendMessage(conv.id, conv.participantIds, "bob", "Hey from Vibe!")
        }
        assertTrue("sendRes failed: ${sendRes.exceptionOrNull()}", sendRes.isSuccess)

        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            msgRepo.observeMessages(conv.id, bobUid).first { it.isNotEmpty() }
        }
        assertEquals("Hey from Vibe!", messages.last().text)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
