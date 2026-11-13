package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.ui.theme.VibeSocialBlue

/**
 * Reusable vertical [LazyColumn] feed component displaying photo, carousel, video, AI art,
 * and poll [PostCard] items with user header info, media content, and like/comment/share/save actions.
 */
@Composable
fun FeedLazyColumn(
    feedState: UiState<List<Post>>,
    currentUserId: String,
    likedPostIds: Set<String>,
    savedPostIds: Set<String>,
    followingIds: Set<String>,
    myPollVotes: Map<String, Int>,
    onLike: (Post) -> Unit,
    onComment: (Post) -> Unit,
    onShare: (Post) -> Unit,
    onSave: (Post) -> Unit,
    onAuthorClick: (String) -> Unit,
    onFollowClick: (String) -> Unit,
    onVotePoll: (String, Int) -> Unit,
    onDeletePost: (String) -> Unit,
    onEmptyCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    headerContent: (@Composable () -> Unit)? = null
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 640.dp)
            .testTag("vertical_post_feed")
    ) {
        if (headerContent != null) {
            item(key = "feed_header") {
                headerContent()
            }
        }

        when (feedState) {
            is UiState.Loading -> {
                item(key = "feed_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = VibeSocialBlue)
                    }
                }
            }
            is UiState.Error -> {
                item(key = "feed_error") {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = feedState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                if (feedState.data.isEmpty()) {
                    item(key = "feed_empty") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 56.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = VibeSocialBlue,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.empty_feed_title),
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.empty_feed_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onEmptyCreateClick,
                                colors = ButtonDefaults.buttonColors(containerColor = VibeSocialBlue),
                                modifier = Modifier.testTag("empty_create_cta")
                            ) {
                                Text(stringResource(R.string.create_now))
                            }
                        }
                    }
                } else {
                    items(feedState.data, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            currentUserId = currentUserId,
                            isLiked = post.id in likedPostIds,
                            isSaved = post.id in savedPostIds,
                            isFollowing = post.authorId in followingIds,
                            myPollVote = myPollVotes[post.id],
                            onLike = { onLike(post) },
                            onComment = { onComment(post) },
                            onShare = { onShare(post) },
                            onSave = { onSave(post) },
                            onAuthorClick = onAuthorClick,
                            onFollowClick = { onFollowClick(post.authorId) },
                            onVotePoll = { optionIdx -> onVotePoll(post.id, optionIdx) },
                            onDelete = if (post.authorId == currentUserId) {
                                { onDeletePost(post.id) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}
