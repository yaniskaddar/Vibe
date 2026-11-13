package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PublicProfile
import com.example.data.model.Story
import com.example.ui.theme.VibeSocialBlue
import com.example.ui.theme.VibeStoryGradient

/**
 * Horizontal 24-hour Stories indicator strip rendered above the main feed.
 * Displays the signed-in user's story indicator followed by active creator story rings,
 * prioritizing unseen stories first.
 */
@Composable
fun StoriesBar(
    currentUserId: String,
    myProfile: PublicProfile?,
    stories: List<Story>,
    seenStoryIds: Set<String>,
    onOpenStories: (List<Story>) -> Unit,
    onCreateStory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val groupedStories = remember(stories) {
        stories.groupBy { it.authorId }
    }
    val myStories = remember(groupedStories, currentUserId) {
        groupedStories[currentUserId].orEmpty()
    }
    val creatorEntries = remember(groupedStories, currentUserId, seenStoryIds) {
        groupedStories.entries
            .filter { it.key != currentUserId && it.value.isNotEmpty() }
            .sortedBy { (_, list) -> list.all { it.id in seenStoryIds } }
            .toList()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .testTag("stories_bar")
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item(key = "your_story") {
                val allMySeen = myStories.isNotEmpty() && myStories.all { it.id in seenStoryIds }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(76.dp)
                        .minimumInteractiveComponentSize()
                        .semantics(mergeDescendants = true) {}
                        .clickable {
                            if (myStories.isNotEmpty()) {
                                onOpenStories(myStories)
                            } else {
                                onCreateStory()
                            }
                        }
                        .testTag("your_story_bubble")
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        VibeAvatar(
                            avatarData = myProfile?.avatarData.orEmpty(),
                            handle = myProfile?.handle ?: "you",
                            size = 68.dp,
                            hasStoryRing = myStories.isNotEmpty(),
                            seenStory = allMySeen
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.background)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(VibeSocialBlue)
                                .clickable(onClick = onCreateStory),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add 24h Story",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = stringResource(R.string.your_story),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (myStories.isNotEmpty() && !allMySeen) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            items(
                items = creatorEntries,
                key = { it.key }
            ) { (authorId, authorStories) ->
                val first = authorStories.first()
                val allSeen = authorStories.all { it.id in seenStoryIds }
                StoryAvatarIndicator(
                    authorId = authorId,
                    avatarData = first.authorAvatar,
                    handle = first.authorHandle,
                    storyCount = authorStories.size,
                    allSeen = allSeen,
                    onClick = { onOpenStories(authorStories) }
                )
            }

            if (creatorEntries.isEmpty()) {
                item(key = "stories_24h_prompt") {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .clickable(onClick = onCreateStory)
                            .testTag("stories_empty_prompt")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .border(2.dp, VibeStoryGradient, CircleShape)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = VibeSocialBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "24h Stories",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Share a moment that vanishes in 24h",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun StoryAvatarIndicator(
    authorId: String,
    avatarData: String,
    handle: String,
    storyCount: Int,
    allSeen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(76.dp)
            .minimumInteractiveComponentSize()
            .semantics(mergeDescendants = true) {}
            .clickable(onClick = onClick)
            .testTag("story_avatar_$authorId")
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            VibeAvatar(
                avatarData = avatarData,
                handle = handle,
                size = 68.dp,
                hasStoryRing = true,
                seenStory = allSeen
            )
            if (storyCount > 1) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = CircleShape,
                    modifier = Modifier.padding(1.dp)
                ) {
                    Surface(
                        color = if (allSeen) MaterialTheme.colorScheme.surfaceVariant else VibeSocialBlue,
                        shape = CircleShape,
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Text(
                            text = "$storyCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (allSeen) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = handle,
            style = MaterialTheme.typography.labelSmall,
            color = if (allSeen) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
            fontWeight = if (allSeen) FontWeight.Normal else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
