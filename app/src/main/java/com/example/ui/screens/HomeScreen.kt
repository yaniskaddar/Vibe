package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.MoodboardCollection
import com.example.data.model.Post
import com.example.data.model.PublicProfile
import com.example.data.model.Story
import com.example.data.model.UiState
import com.example.ui.components.FeedLazyColumn
import com.example.ui.components.StoriesBar
import com.example.ui.components.VibeAvatar
import com.example.ui.components.VibeMediaImage
import com.example.ui.components.formatRelativeTime
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUserId: String,
    myProfile: PublicProfile?,
    feedState: UiState<List<Post>>,
    stories: List<Story>,
    likedPostIds: Set<String>,
    followingIds: Set<String>,
    collections: List<MoodboardCollection>,
    myPollVotes: Map<String, Int>,
    onOpenMessages: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenCreateStory: () -> Unit,
    onOpenCreatePost: () -> Unit,
    onLike: (Post) -> Unit,
    onComment: (Post) -> Unit,
    onShare: (Post) -> Unit,
    onSave: (Post) -> Unit,
    onAuthorClick: (String) -> Unit,
    onFollowClick: (String) -> Unit,
    onVotePoll: (String, Int) -> Unit,
    onDeletePost: (String) -> Unit,
    onReplyStory: (Story, String) -> Unit
) {
    var activeStoryAuthorStories by remember { mutableStateOf<List<Story>?>(null) }
    var seenStoryIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showActivitySheet by remember { mutableStateOf(false) }

    val savedPostIds = remember(collections) {
        collections.flatMap { it.postIds }.toSet()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(
                        onClick = onOpenCamera,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("home_open_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Open Camera"
                        )
                    }
                    IconButton(
                        onClick = { showActivitySheet = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("activity_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = "Activity"
                        )
                    }
                    IconButton(
                        onClick = onOpenMessages,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("open_messages_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Forum,
                            contentDescription = "Direct Messages"
                        )
                    }
                }
            )
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

            FeedLazyColumn(
                feedState = feedState,
                currentUserId = currentUserId,
                likedPostIds = likedPostIds,
                savedPostIds = savedPostIds,
                followingIds = followingIds,
                myPollVotes = myPollVotes,
                onLike = onLike,
                onComment = onComment,
                onShare = onShare,
                onSave = onSave,
                onAuthorClick = onAuthorClick,
                onFollowClick = onFollowClick,
                onVotePoll = onVotePoll,
                onDeletePost = onDeletePost,
                onEmptyCreateClick = onOpenCreatePost,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                headerContent = {
                    StoriesBar(
                        currentUserId = currentUserId,
                        myProfile = myProfile,
                        stories = stories,
                        seenStoryIds = seenStoryIds,
                        onOpenStories = { authorStories -> activeStoryAuthorStories = authorStories },
                        onCreateStory = onOpenCreateStory
                    )
                }
            )
        }

        // Full-Screen Story Viewer
        activeStoryAuthorStories?.let { storyList ->
            StoryViewerOverlay(
                stories = storyList,
                currentUserId = currentUserId,
                onMarkSeen = { id -> seenStoryIds = seenStoryIds + id },
                onClose = { activeStoryAuthorStories = null },
                onSendReply = { story, text ->
                    onReplyStory(story, text)
                    activeStoryAuthorStories = null
                }
            )
        }
    }

    if (showActivitySheet) {
        val recentPosts = (feedState as? UiState.Success)?.data.orEmpty().take(8)
        ModalBottomSheet(onDismissRequest = { showActivitySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = stringResource(R.string.notifications_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (recentPosts.isEmpty()) {
                    Text(
                        text = "When creators share posts, stories, or polls on Vibe, updates appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    recentPosts.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VibeAvatar(
                                avatarData = item.authorAvatar,
                                handle = item.authorHandle,
                                size = 36.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "@${item.authorHandle} shared a ${item.mediaType.replace('_', ' ')}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${item.likesCount} likes • ${item.commentsCount} comments • ${formatRelativeTime(item.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryViewerOverlay(
    stories: List<Story>,
    currentUserId: String,
    onMarkSeen: (String) -> Unit,
    onClose: () -> Unit,
    onSendReply: (Story, String) -> Unit
) {
    BackHandler { onClose() }

    var index by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var replyText by remember { mutableStateOf("") }

    val current = stories.getOrNull(index) ?: return

    LaunchedEffect(current.id) {
        onMarkSeen(current.id)
        progress = 0f
    }

    LaunchedEffect(current.id, isPaused) {
        while (progress < 1f) {
            delay(50)
            if (!isPaused) {
                progress += 0.012f
            }
        }
        if (index < stories.lastIndex) {
            index += 1
        } else {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(index, stories.size) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        tryAwaitRelease()
                        isPaused = false
                    },
                    onTap = { offset ->
                        if (offset.x < size.width * 0.32f) {
                            if (index > 0) index -= 1
                        } else {
                            if (index < stories.lastIndex) index += 1 else onClose()
                        }
                    }
                )
            }
    ) {
        VibeMediaImage(
            payload = current.mediaPayload,
            contentDescription = "Story by @${current.authorHandle}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stories.forEachIndexed { idx, _ ->
                    val barProgress = when {
                        idx < index -> 1f
                        idx == index -> progress.coerceIn(0f, 1f)
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { barProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.35f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VibeAvatar(
                    avatarData = current.authorAvatar,
                    handle = current.authorHandle,
                    size = 34.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "@${current.authorHandle}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatRelativeTime(current.createdAt),
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (current.moodTag.isNotBlank()) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = current.moodTag,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close story",
                        tint = Color.White
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            if (current.captionOverlay.isNotBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = current.captionOverlay,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            if (current.authorId != currentUserId) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Reply to @${current.authorHandle}...", color = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                onSendReply(current, replyText)
                            }
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send story reply",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
