package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.MoodboardCollection
import com.example.data.model.Post
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.ui.components.PostCard
import com.example.ui.components.VibeAvatar
import com.example.ui.components.VibeMediaImage
import com.example.ui.theme.VibeMagenta
import com.example.ui.theme.VibeSocialBlue
import com.example.ui.theme.VibeStudioGradient

private val exploreCategories = listOf("For You", "Photos", "AI Studio", "Polls", "Reels")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    currentUserId: String,
    feedState: UiState<List<Post>>,
    allProfiles: List<PublicProfile>,
    followingIds: Set<String>,
    likedPostIds: Set<String>,
    collections: List<MoodboardCollection>,
    myPollVotes: Map<String, Int>,
    onFollowClick: (String) -> Unit,
    onMessageCreator: (PublicProfile) -> Unit,
    onLike: (Post) -> Unit,
    onComment: (Post) -> Unit,
    onShare: (Post) -> Unit,
    onSave: (Post) -> Unit,
    onVotePoll: (String, Int) -> Unit,
    onDeletePost: (String) -> Unit,
    onOpenPollsFeed: () -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("For You") }
    var selectedPost by remember { mutableStateOf<Post?>(null) }

    val allPosts = (feedState as? UiState.Success)?.data.orEmpty()
    val savedIds = remember(collections) { collections.flatMap { it.postIds }.toSet() }
    val pollPosts = remember(allPosts) {
        allPosts.filter { it.mediaType == "poll" || it.pollQuestion.isNotBlank() }
    }
    val topTrendingPoll = remember(pollPosts) {
        pollPosts.maxByOrNull { it.pollCounts.sum() * 3L + it.likesCount * 2L + it.commentsCount }
    }

    val filteredPosts = remember(allPosts, query, selectedCategory) {
        allPosts.filter { post ->
            val matchesCategory = when (selectedCategory) {
                "Photos" -> post.mediaType == "photo" || post.mediaType == "carousel"
                "AI Studio" -> post.mediaType == "ai_art"
                "Polls" -> post.mediaType == "poll"
                "Reels" -> post.mediaType == "reel"
                else -> true
            }
            val q = query.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                post.caption.lowercase().contains(q) ||
                post.authorHandle.lowercase().contains(q) ||
                post.hashtags.any { it.lowercase().contains(q) } ||
                post.pollQuestion.lowercase().contains(q)
            matchesCategory && matchesQuery
        }
    }

    val matchingCreators = remember(allProfiles, query, currentUserId) {
        val q = query.trim().lowercase().removePrefix("@")
        allProfiles
            .filter { it.userId != currentUserId }
            .filter { q.isEmpty() || it.handle.lowercase().contains(q) || it.displayName.lowercase().contains(q) }
            .take(12)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar + Quick Polls Feed Shortcut
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search creators, #tags, polls, AI art...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("explore_search_input")
            )
            Surface(
                color = VibeSocialBlue.copy(alpha = 0.14f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenPollsFeed)
                    .minimumInteractiveComponentSize()
                    .testTag("explore_polls_shortcut")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Poll,
                        contentDescription = "Open Explore Polls",
                        tint = VibeSocialBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Polls",
                        style = MaterialTheme.typography.labelLarge,
                        color = VibeSocialBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Category Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(exploreCategories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 118.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (selectedCategory == "For You" || selectedCategory == "Polls") {
                item(span = { GridItemSpan(maxLineSpan) }, key = "explore_polls_banner") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onOpenPollsFeed)
                            .testTag("open_explore_polls_button")
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(VibeStudioGradient)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = null,
                                            tint = VibeMagenta,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Trending Polls & Poll Tool",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = topTrendingPoll?.pollQuestion?.let {
                                            "Hot now: \"$it\""
                                        } ?: "Create community polls & vote on trending prompts (${pollPosts.size} live)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Button(
                                    onClick = onOpenPollsFeed,
                                    colors = ButtonDefaults.buttonColors(containerColor = VibeSocialBlue),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Text(
                                        text = "Open",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (matchingCreators.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "creators_header") {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Creators on Vibe",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(matchingCreators, key = { it.userId }) { creator ->
                                val isFollowing = creator.userId in followingIds
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 1.dp,
                                    modifier = Modifier.width(156.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        VibeAvatar(
                                            avatarData = creator.avatarData,
                                            handle = creator.handle,
                                            size = 52.dp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = creator.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "@${creator.handle}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(
                                                onClick = { onFollowClick(creator.userId) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Text(
                                                    text = if (isFollowing) "Following" else "Follow",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else VibeSocialBlue
                                                )
                                            }
                                            IconButton(
                                                onClick = { onMessageCreator(creator) },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Text(
                                                    text = "DM",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = VibeSocialBlue,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (filteredPosts.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "explore_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching posts yet. Try another category or create the first one!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredPosts, key = { it.id }) { post ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedPost = post }
                    ) {
                        if (post.mediaPayloads.isNotEmpty()) {
                            VibeMediaImage(
                                payload = post.mediaPayloads.first(),
                                filterPreset = post.filterPreset,
                                contentDescription = post.caption.ifBlank { "Explore post" },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = post.pollQuestion.ifBlank { post.caption },
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        val badgeIcon = when (post.mediaType) {
                            "carousel" -> Icons.Filled.Collections
                            "reel" -> Icons.Filled.Movie
                            "poll" -> Icons.Filled.Poll
                            "ai_art" -> Icons.Filled.AutoAwesome
                            else -> null
                        }
                        if (badgeIcon != null) {
                            Icon(
                                imageVector = badgeIcon,
                                contentDescription = post.mediaType,
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    selectedPost?.let { post ->
        val freshPost = allPosts.firstOrNull { it.id == post.id } ?: post
        ModalBottomSheet(onDismissRequest = { selectedPost = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                PostCard(
                    post = freshPost,
                    currentUserId = currentUserId,
                    isLiked = freshPost.id in likedPostIds,
                    isSaved = freshPost.id in savedIds,
                    isFollowing = freshPost.authorId in followingIds,
                    myPollVote = myPollVotes[freshPost.id],
                    onLike = { onLike(freshPost) },
                    onComment = { onComment(freshPost) },
                    onShare = { onShare(freshPost) },
                    onSave = { onSave(freshPost) },
                    onAuthorClick = {},
                    onFollowClick = { onFollowClick(freshPost.authorId) },
                    onVotePoll = { idx -> onVotePoll(freshPost.id, idx) },
                    onDelete = if (freshPost.authorId == currentUserId) {
                        {
                            onDeletePost(freshPost.id)
                            selectedPost = null
                        }
                    } else null
                )
            }
        }
    }
}
