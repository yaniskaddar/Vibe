package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.example.core.FilterPreset
import com.example.core.ImageUtils
import com.example.data.model.MoodboardCollection
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.PublicProfile
import com.example.ui.theme.VibeAiViolet
import com.example.ui.theme.VibeCoralRed
import com.example.ui.theme.VibeSocialBlue
import com.example.ui.theme.VibeStoryGradient
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun VibeAvatar(
    avatarData: String,
    handle: String,
    size: Dp = 38.dp,
    hasStoryRing: Boolean = false,
    seenStory: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val decodedBitmap by produceState<Bitmap?>(initialValue = null, avatarData) {
        value = if (avatarData.startsWith("data:")) {
            withContext(Dispatchers.IO) { ImageUtils.decodeDataUriToBitmap(avatarData) }
        } else null
    }
    val ringModifier = when {
        hasStoryRing && !seenStory -> Modifier
            .border(2.5.dp, VibeStoryGradient, CircleShape)
            .padding(4.dp)
        hasStoryRing && seenStory -> Modifier
            .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .padding(4.dp)
        else -> Modifier
    }
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    val avatarBitmap = decodedBitmap
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(ringModifier)
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        when {
            avatarBitmap != null -> {
                Image(
                    bitmap = avatarBitmap.asImageBitmap(),
                    contentDescription = "Avatar for @$handle",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            avatarData.startsWith("http") -> {
                AsyncImage(
                    model = avatarData,
                    contentDescription = "Avatar for @$handle",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(VibeStoryGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = handle.firstOrNull()?.uppercase() ?: "V",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun VibeMediaImage(
    payload: String,
    filterPreset: String = "ORIGINAL",
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val decoded by produceState<Bitmap?>(initialValue = null, payload) {
        value = if (payload.startsWith("data:")) {
            withContext(Dispatchers.IO) { ImageUtils.decodeDataUriToBitmap(payload) }
        } else null
    }
    val colorFilter = remember(filterPreset) {
        FilterPreset.fromName(filterPreset).toColorFilter()
    }

    val mediaBitmap = decoded
    when {
        mediaBitmap != null -> {
            Image(
                bitmap = mediaBitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                colorFilter = colorFilter
            )
        }
        payload.startsWith("http") -> {
            AsyncImage(
                model = payload,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                colorFilter = colorFilter
            )
        }
        else -> {
            Box(
                modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostCard(
    post: Post,
    currentUserId: String,
    isLiked: Boolean,
    isSaved: Boolean,
    isFollowing: Boolean,
    myPollVote: Int?,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onFollowClick: () -> Unit,
    onVotePoll: (Int) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var showDoubleTapHeart by remember { mutableStateOf(false) }
    LaunchedEffect(showDoubleTapHeart) {
        if (showDoubleTapHeart) {
            delay(750)
            showDoubleTapHeart = false
        }
    }
    val likeScale by animateFloatAsState(
        targetValue = if (isLiked) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "likeScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .testTag("post_card_${post.id}")
    ) {
        // Author Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VibeAvatar(
                avatarData = post.authorAvatar,
                handle = post.authorHandle,
                size = 38.dp,
                onClick = { onAuthorClick(post.authorId) }
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onAuthorClick(post.authorId) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "@${post.authorHandle}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (post.mediaType == "ai_art") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = VibeAiViolet.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = "AI Studio creation",
                                    tint = VibeAiViolet,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "AI Studio",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VibeAiViolet
                                )
                            }
                        }
                    } else if (post.mediaType == "reel") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Movie,
                            contentDescription = "Reel",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = formatRelativeTime(post.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (post.authorId != currentUserId) {
                TextButton(
                    onClick = onFollowClick,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(
                        text = if (isFollowing) "Following" else "Follow",
                        color = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else VibeSocialBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete post",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Media Content (Video Reel / Photo Carousel / Single Photo) with Double-Tap Heart
        val isVideoPost = post.mediaType == "reel"
        val hasLocalVideo = remember(post.localVideoPath) {
            post.localVideoPath.isNotBlank() && File(post.localVideoPath).exists()
        }
        var isPlayingVideo by remember(post.id) { mutableStateOf(false) }
        var isVideoMuted by remember(post.id) { mutableStateOf(false) }

        if (post.mediaPayloads.isNotEmpty() || (isVideoPost && hasLocalVideo)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color.Black)
                    .pointerInput(post.id, isLiked, isVideoPost, hasLocalVideo) {
                        detectTapGestures(
                            onDoubleTap = {
                                showDoubleTapHeart = true
                                if (!isLiked) onLike()
                            },
                            onTap = {
                                if (isVideoPost && hasLocalVideo) {
                                    isPlayingVideo = !isPlayingVideo
                                }
                            }
                        )
                    }
            ) {
                if (isVideoPost && hasLocalVideo && isPlayingVideo) {
                    val context = LocalContext.current
                    val exoPlayer = remember(post.localVideoPath) {
                        ExoPlayer.Builder(context).build().apply {
                            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(post.localVideoPath))))
                            repeatMode = Player.REPEAT_MODE_ALL
                            prepare()
                            playWhenReady = true
                        }
                    }
                    LaunchedEffect(isVideoMuted) {
                        exoPlayer.volume = if (isVideoMuted) 0f else 1f
                    }
                    DisposableEffect(exoPlayer) {
                        onDispose { exoPlayer.release() }
                    }
                    AndroidView(
                        factory = { ctx ->
                            TextureView(ctx).also { tv ->
                                exoPlayer.setVideoTextureView(tv)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clickable { isVideoMuted = !isVideoMuted }
                    ) {
                        Icon(
                            imageVector = if (isVideoMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isVideoMuted) "Unmute video" else "Mute video",
                            tint = Color.White,
                            modifier = Modifier.padding(8.dp).size(18.dp)
                        )
                    }
                } else if (post.mediaPayloads.size <= 1) {
                    val payload = post.mediaPayloads.firstOrNull().orEmpty()
                    if (payload.isNotBlank()) {
                        VibeMediaImage(
                            payload = payload,
                            filterPreset = post.filterPreset,
                            contentDescription = post.caption.ifBlank { "Vibe media by @${post.authorHandle}" },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (isVideoPost) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Movie,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "VIDEO",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (hasLocalVideo) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Play video",
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val pagerState = rememberPagerState(pageCount = { post.mediaPayloads.size })
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        VibeMediaImage(
                            payload = post.mediaPayloads[page],
                            filterPreset = post.filterPreset,
                            contentDescription = "Slide ${page + 1} by @${post.authorHandle}",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1}/${post.mediaPayloads.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        repeat(post.mediaPayloads.size) { index ->
                            val selected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .size(if (selected) 7.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) VibeSocialBlue else Color.White.copy(alpha = 0.65f))
                            )
                        }
                    }
                }

                // Double-tap heart overlay
                androidx.compose.animation.AnimatedVisibility(
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

        // Interactive Community Poll Card
        if (post.pollQuestion.isNotBlank() && post.pollOptions.isNotEmpty()) {
            PollBlock(
                question = post.pollQuestion,
                options = post.pollOptions,
                counts = post.pollCounts,
                myVote = myPollVote,
                onVote = onVotePoll
            )
        }

        // Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onLike,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("like_button_${post.id}")
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isLiked) "Unlike" else "Like",
                    tint = if (isLiked) VibeCoralRed else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.scale(likeScale)
                )
            }
            IconButton(
                onClick = onComment,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("comment_button_${post.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Comments"
                )
            }
            IconButton(
                onClick = onShare,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("share_button_${post.id}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Share via Direct Message"
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onSave,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("bookmark_button_${post.id}")
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save to moodboard collection",
                    tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Likes + Caption + Tags + Comment Preview
        Column(modifier = Modifier.padding(horizontal = 14.dp)) {
            if (post.likesCount > 0) {
                Text(
                    text = "${post.likesCount} ${if (post.likesCount == 1L) "like" else "likes"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (post.caption.isNotBlank()) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("@${post.authorHandle} ")
                        }
                        append(post.caption)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (post.hashtags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    post.hashtags.forEach { tag ->
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelMedium,
                            color = VibeSocialBlue
                        )
                    }
                }
            }
            if (post.commentsCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "View all ${post.commentsCount} comments",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable(onClick = onComment)
                        .padding(vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun PollBlock(
    question: String,
    options: List<String>,
    counts: List<Long>,
    myVote: Int?,
    onVote: (Int) -> Unit
) {
    val totalVotes = counts.sum().coerceAtLeast(0L)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Poll,
                    contentDescription = "Community Poll",
                    tint = VibeSocialBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            options.forEachIndexed { idx, label ->
                val count = counts.getOrElse(idx) { 0L }
                val pct = if (totalVotes > 0) ((count * 100f) / totalVotes).toInt() else 0
                val isSelected = myVote == idx
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = myVote == null) { onVote(idx) },
                    color = if (isSelected) VibeSocialBlue.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (myVote != null && totalVotes > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(pct / 100f)
                                    .height(44.dp)
                                    .background(
                                        if (isSelected) VibeSocialBlue.copy(alpha = 0.25f)
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                    )
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Your vote",
                                        tint = VibeSocialBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (myVote != null) {
                                Text(
                                    text = "$pct%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) VibeSocialBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (myVote == null) "Tap an option to vote • $totalVotes votes" else "$totalVotes total votes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    comments: List<PostComment>,
    onDismiss: () -> Unit,
    onSubmitComment: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Comments",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

            if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No comments yet. Start the conversation!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                    items(comments, key = { it.id }) { comment ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            VibeAvatar(
                                avatarData = comment.authorAvatar,
                                handle = comment.authorHandle,
                                size = 32.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "@${comment.authorHandle}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatRelativeTime(comment.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = comment.text,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Add a comment...") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            onSubmitComment(draft)
                            draft = ""
                        }
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("submit_comment_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Post comment",
                        tint = VibeSocialBlue
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveToCollectionBottomSheet(
    post: Post,
    collections: List<MoodboardCollection>,
    onDismiss: () -> Unit,
    onToggleCollection: (String) -> Unit,
    onCreateCollection: (String) -> Unit
) {
    var newName by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Save to Moodboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            collections.forEach { col ->
                val isInCol = post.id in col.postIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleCollection(col.id) }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = col.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${col.postIds.size} saved",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (isInCol) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isInCol) "Saved" else "Save",
                        tint = if (isInCol) VibeSocialBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("New moodboard name...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onCreateCollection(newName.trim())
                            newName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VibeSocialBlue),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Create")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Create")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePostBottomSheet(
    profiles: List<PublicProfile>,
    currentUserId: String,
    onDismiss: () -> Unit,
    onSendToUser: (PublicProfile) -> Unit
) {
    val peers = profiles.filter { it.userId != currentUserId }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Send via Vibe DM",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (peers.isEmpty()) {
                Text(
                    text = "No other creators discovered yet. Invite a friend to join Vibe!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(peers, key = { it.userId }) { peer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VibeAvatar(avatarData = peer.avatarData, handle = peer.handle, size = 40.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = peer.displayName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "@${peer.handle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = { onSendToUser(peer) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text("Send")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatRelativeTime(timestamp: Timestamp?): String {
    if (timestamp == null) return "Just now"
    val diffSec = ((System.currentTimeMillis() - timestamp.toDate().time) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 60 -> "Just now"
        diffSec < 3600 -> "${diffSec / 60}m ago"
        diffSec < 86400 -> "${diffSec / 3600}h ago"
        diffSec < 604800 -> "${diffSec / 86400}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(timestamp.toDate())
    }
}
