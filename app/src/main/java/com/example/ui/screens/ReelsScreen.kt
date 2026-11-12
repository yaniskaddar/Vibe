package com.example.ui.screens

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.MoodboardCollection
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.ui.components.VibeAvatar
import com.example.ui.components.VibeMediaImage
import com.example.ui.theme.VibeCoralRed
import com.example.ui.theme.VibeSocialBlue
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun ReelsScreen(
    currentUserId: String,
    feedState: UiState<List<Post>>,
    likedPostIds: Set<String>,
    followingIds: Set<String>,
    collections: List<MoodboardCollection>,
    onOpenCreateReel: () -> Unit,
    onLike: (Post) -> Unit,
    onComment: (Post) -> Unit,
    onShare: (Post) -> Unit,
    onSave: (Post) -> Unit,
    onFollowClick: (String) -> Unit
) {
    val allPosts = (feedState as? UiState.Success)?.data.orEmpty()
    // Prioritize video Reels first, and also show visual motion posts if no Reels exist yet
    val reels = remember(allPosts) {
        val dedicated = allPosts.filter { it.mediaType == "reel" }
        dedicated.ifEmpty { allPosts.filter { it.mediaPayloads.isNotEmpty() } }
    }
    val savedIds = remember(collections) { collections.flatMap { it.postIds }.toSet() }

    if (reels.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Movie,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Reels Stream",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pick a video or motion frame in Create Studio to publish your first Vibe Reel.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFA1A1AA),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onOpenCreateReel,
                    colors = ButtonDefaults.buttonColors(containerColor = VibeSocialBlue)
                ) {
                    Text("Create a Reel")
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { reels.size })
    var isMuted by remember { mutableStateOf(false) }

    VerticalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) { page ->
        val post = reels[page]
        val isActive = pagerState.currentPage == page
        val isLiked = post.id in likedPostIds
        val isSaved = post.id in savedIds
        val isFollowing = post.authorId in followingIds

        ReelPage(
            post = post,
            isActive = isActive,
            isMuted = isMuted,
            currentUserId = currentUserId,
            isLiked = isLiked,
            isSaved = isSaved,
            isFollowing = isFollowing,
            onToggleMute = { isMuted = !isMuted },
            onLike = { onLike(post) },
            onComment = { onComment(post) },
            onShare = { onShare(post) },
            onSave = { onSave(post) },
            onFollowClick = { onFollowClick(post.authorId) }
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ReelPage(
    post: Post,
    isActive: Boolean,
    isMuted: Boolean,
    currentUserId: String,
    isLiked: Boolean,
    isSaved: Boolean,
    isFollowing: Boolean,
    onToggleMute: () -> Unit,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onFollowClick: () -> Unit
) {
    val context = LocalContext.current
    val hasLocalVideo = remember(post.localVideoPath) {
        post.localVideoPath.isNotBlank() && File(post.localVideoPath).exists()
    }
    var showDoubleTapHeart by remember { mutableStateOf(false) }
    LaunchedEffect(showDoubleTapHeart) {
        if (showDoubleTapHeart) {
            delay(750)
            showDoubleTapHeart = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(post.id, isLiked) {
                detectTapGestures(
                    onDoubleTap = {
                        showDoubleTapHeart = true
                        if (!isLiked) onLike()
                    },
                    onTap = { onToggleMute() }
                )
            }
    ) {
        if (hasLocalVideo && isActive) {
            val exoPlayer = remember(post.localVideoPath) {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(Uri.fromFile(File(post.localVideoPath))))
                    repeatMode = Player.REPEAT_MODE_ALL
                    prepare()
                    playWhenReady = true
                }
            }
            LaunchedEffect(isMuted) {
                exoPlayer.volume = if (isMuted) 0f else 1f
            }
            DisposableEffect(exoPlayer) {
                onDispose { exoPlayer.release() }
            }
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        player = exoPlayer
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (post.mediaPayloads.isNotEmpty()) {
            VibeMediaImage(
                payload = post.mediaPayloads.first(),
                filterPreset = post.filterPreset,
                contentDescription = post.caption,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Bottom gradient scrim
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )

        // Top Mute Pill
        Surface(
            color = Color.Black.copy(alpha = 0.45f),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clickable(onClick = onToggleMute)
        ) {
            Icon(
                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint = Color.White,
                modifier = Modifier.padding(8.dp).size(20.dp)
            )
        }

        // Right-Side Vertical Action Rail
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onLike, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like reel",
                        tint = if (isLiked) VibeCoralRed else Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Text(text = "${post.likesCount}", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onComment, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(text = "${post.commentsCount}", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onShare, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Share reel",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(onClick = onSave, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(
                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save reel",
                    tint = if (isSaved) VibeSocialBlue else Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Bottom Left Author & Caption
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 16.dp, bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VibeAvatar(avatarData = post.authorAvatar, handle = post.authorHandle, size = 36.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "@${post.authorHandle}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (post.authorId != currentUserId) {
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = onFollowClick,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text(
                            text = if (isFollowing) "Following" else "Follow",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            if (post.caption.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = post.caption,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Original Vibe Audio • @${post.authorHandle}",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        AnimatedVisibility(
            visible = showDoubleTapHeart,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Liked",
                tint = VibeCoralRed,
                modifier = Modifier.size(96.dp)
            )
        }
    }
}
