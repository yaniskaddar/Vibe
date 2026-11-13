package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.MoodboardCollection
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.ui.components.PostCard
import com.example.ui.theme.VibeAmber
import com.example.ui.theme.VibeCoralRed
import com.example.ui.theme.VibeMagenta
import com.example.ui.theme.VibeSocialBlue
import com.example.ui.theme.VibeStudioGradient

private enum class PollFeedFilter(val label: String) {
    TRENDING("Trending"),
    NEWEST("Newest"),
    UNVOTED("Unvoted")
}

private data class PollStarterTemplate(
    val chipLabel: String,
    val question: String,
    val options: List<String>,
    val tags: String
)

private val pollTemplates = listOf(
    PollStarterTemplate(
        chipLabel = "Weekend Mood",
        question = "What is the best weekend creative vibe?",
        options = listOf("Golden hour photo walk", "Cozy café sketching", "Night street cinema"),
        tags = "#weekend #vibe #creative"
    ),
    PollStarterTemplate(
        chipLabel = "Shot Style",
        question = "Which camera look hits hardest right now?",
        options = listOf("Warm 35mm film", "Crisp monochrome", "Neon cyber dusk"),
        tags = "#photography #filters"
    ),
    PollStarterTemplate(
        chipLabel = "Color Palette",
        question = "Pick tomorrow's community moodboard theme!",
        options = listOf("Sunset Amber & Coral", "Obsidian Minimal", "Electric Violet"),
        tags = "#moodboard #design"
    )
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExplorePollsScreen(
    currentUserId: String,
    feedState: UiState<List<Post>>,
    likedPostIds: Set<String>,
    followingIds: Set<String>,
    collections: List<MoodboardCollection>,
    myPollVotes: Map<String, Int>,
    isPublishingPoll: Boolean,
    onBack: () -> Unit,
    onPublishPoll: (question: String, options: List<String>, caption: String, hashtags: String, onSuccess: () -> Unit) -> Unit,
    onVotePoll: (String, Int) -> Unit,
    onLike: (Post) -> Unit,
    onComment: (Post) -> Unit,
    onShare: (Post) -> Unit,
    onSave: (Post) -> Unit,
    onAuthorClick: (String) -> Unit,
    onFollowClick: (String) -> Unit,
    onDeletePost: (String) -> Unit
) {
    var showComposer by remember { mutableStateOf(true) }
    var question by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var hashtagsText by remember { mutableStateOf("#vibe #poll") }
    val options = remember { mutableStateListOf("", "") }

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(PollFeedFilter.TRENDING) }

    val allPosts = (feedState as? UiState.Success)?.data.orEmpty()
    val savedPostIds = remember(collections) { collections.flatMap { it.postIds }.toSet() }

    val allPolls = remember(allPosts) {
        allPosts.filter { post ->
            (post.mediaType == "poll" || post.pollQuestion.isNotBlank()) && post.pollOptions.size >= 2
        }
    }

    val totalVotesCommunity = remember(allPolls) {
        allPolls.sumOf { it.pollCounts.sum() }
    }

    val displayedPolls = remember(allPolls, searchQuery, activeFilter, myPollVotes) {
        val q = searchQuery.trim().lowercase()
        val filtered = allPolls.filter { post ->
            val matchesQuery = q.isEmpty() ||
                post.pollQuestion.lowercase().contains(q) ||
                post.caption.lowercase().contains(q) ||
                post.authorHandle.lowercase().contains(q) ||
                post.pollOptions.any { it.lowercase().contains(q) } ||
                post.hashtags.any { it.lowercase().contains(q) }
            val matchesFilter = when (activeFilter) {
                PollFeedFilter.UNVOTED -> post.id !in myPollVotes
                else -> true
            }
            matchesQuery && matchesFilter
        }
        when (activeFilter) {
            PollFeedFilter.NEWEST -> filtered.sortedByDescending { it.createdAt?.seconds ?: 0L }
            PollFeedFilter.TRENDING, PollFeedFilter.UNVOTED -> filtered.sortedWith(
                compareByDescending<Post> { pollEngagementScore(it) }
                    .thenByDescending { it.createdAt?.seconds ?: 0L }
            )
        }
    }

    val nonBlankOptionsCount = options.count { it.isNotBlank() }
    val canPublish = !isPublishingPoll && question.isNotBlank() && nonBlankOptionsCount >= 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("explore_polls_screen")
    ) {
        TopAppBar(
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("explore_polls_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Explore"
                    )
                }
            },
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.explore_polls_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${allPolls.size} live polls • $totalVotesCommunity votes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = { showComposer = !showComposer },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("toggle_poll_composer_button")
                ) {
                    Icon(
                        imageVector = if (showComposer) Icons.Filled.Close else Icons.Filled.Add,
                        contentDescription = if (showComposer) "Hide poll creator" else "Create poll",
                        tint = VibeSocialBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showComposer) "Hide" else "New Poll",
                        color = VibeSocialBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .testTag("trending_polls_list"),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                // Poll Creation Tool
                item(key = "poll_creation_tool") {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showComposer,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        PollCreationToolCard(
                            question = question,
                            onQuestionChange = { if (it.length <= 200) question = it },
                            options = options,
                            onOptionChange = { idx, value ->
                                if (idx in options.indices && value.length <= 80) {
                                    options[idx] = value
                                }
                            },
                            onAddOption = {
                                if (options.size < 4) options.add("")
                            },
                            onRemoveOption = { idx ->
                                if (options.size > 2 && idx in options.indices) {
                                    options.removeAt(idx)
                                }
                            },
                            caption = caption,
                            onCaptionChange = { if (it.length <= 300) caption = it },
                            hashtagsText = hashtagsText,
                            onHashtagsChange = { hashtagsText = it },
                            onApplyTemplate = { template ->
                                question = template.question
                                options.clear()
                                options.addAll(template.options)
                                hashtagsText = template.tags
                            },
                            isPublishing = isPublishingPoll,
                            canPublish = canPublish,
                            onPublish = {
                                onPublishPoll(
                                    question,
                                    options.toList(),
                                    caption,
                                    hashtagsText
                                ) {
                                    question = ""
                                    caption = ""
                                    hashtagsText = "#vibe #poll"
                                    options.clear()
                                    options.addAll(listOf("", ""))
                                }
                            }
                        )
                    }
                }

                // Search & Trending Sort Bar
                item(key = "trending_polls_controls") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = VibeMagenta,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Trending Polls",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                color = VibeSocialBlue.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "${displayedPolls.size} ${if (displayedPolls.size == 1) "poll" else "polls"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = VibeSocialBlue,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search polls, topics, or @creators...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search polls") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("polls_search_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(PollFeedFilter.entries) { filter ->
                                FilterChip(
                                    selected = activeFilter == filter,
                                    onClick = { activeFilter = filter },
                                    label = { Text(filter.label) },
                                    modifier = Modifier.testTag("poll_filter_${filter.name.lowercase()}")
                                )
                            }
                        }
                    }
                }

                // Feed States
                when (feedState) {
                    is UiState.Loading -> {
                        item(key = "polls_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = VibeSocialBlue)
                            }
                        }
                    }

                    is UiState.Error -> {
                        item(key = "polls_error") {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = feedState.message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }

                    is UiState.Success -> {
                        if (displayedPolls.isEmpty()) {
                            item(key = "polls_empty") {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 1.dp,
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Poll,
                                            contentDescription = null,
                                            tint = VibeSocialBlue,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = if (searchQuery.isNotBlank() || activeFilter == PollFeedFilter.UNVOTED) {
                                                "No polls match this filter"
                                            } else {
                                                "No trending polls yet"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = stringResource(R.string.explore_polls_subtitle),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (!showComposer) {
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Button(
                                                onClick = { showComposer = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = VibeSocialBlue),
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Text("Launch a Poll")
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = displayedPolls,
                                key = { _, post -> post.id }
                            ) { index, pollPost ->
                                TrendingPollItemCard(
                                    rank = index + 1,
                                    post = pollPost,
                                    currentUserId = currentUserId,
                                    isLiked = pollPost.id in likedPostIds,
                                    isSaved = pollPost.id in savedPostIds,
                                    isFollowing = pollPost.authorId in followingIds,
                                    myPollVote = myPollVotes[pollPost.id],
                                    onVotePoll = { optIndex -> onVotePoll(pollPost.id, optIndex) },
                                    onLike = { onLike(pollPost) },
                                    onComment = { onComment(pollPost) },
                                    onShare = { onShare(pollPost) },
                                    onSave = { onSave(pollPost) },
                                    onAuthorClick = onAuthorClick,
                                    onFollowClick = { onFollowClick(pollPost.authorId) },
                                    onDelete = if (pollPost.authorId == currentUserId) {
                                        { onDeletePost(pollPost.id) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PollCreationToolCard(
    question: String,
    onQuestionChange: (String) -> Unit,
    options: List<String>,
    onOptionChange: (Int, String) -> Unit,
    onAddOption: () -> Unit,
    onRemoveOption: (Int) -> Unit,
    caption: String,
    onCaptionChange: (String) -> Unit,
    hashtagsText: String,
    onHashtagsChange: (String) -> Unit,
    onApplyTemplate: (PollStarterTemplate) -> Unit,
    isPublishing: Boolean,
    canPublish: Boolean,
    onPublish: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("poll_creation_tool"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Signature gradient accent strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(VibeStudioGradient)
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = VibeSocialBlue.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Poll,
                                contentDescription = null,
                                tint = VibeSocialBlue,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Poll Creation Tool",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ask Vibe and track live votes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "${question.length}/200",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick starter templates
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    pollTemplates.forEach { template ->
                        AssistChip(
                            onClick = { onApplyTemplate(template) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = VibeMagenta
                                )
                            },
                            label = {
                                Text(
                                    text = template.chipLabel,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = question,
                    onValueChange = onQuestionChange,
                    label = { Text("Poll Question") },
                    placeholder = { Text("Ask something engaging...") },
                    maxLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("poll_question_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                options.forEachIndexed { idx, optValue ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = optValue,
                            onValueChange = { onOptionChange(idx, it) },
                            label = { Text("Option ${idx + 1}") },
                            placeholder = { Text("Choice ${idx + 1}") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("poll_option_input_$idx")
                        )
                        if (options.size > 2) {
                            IconButton(
                                onClick = { onRemoveOption(idx) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove option ${idx + 1}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (options.size < 4) {
                    OutlinedButton(
                        onClick = onAddOption,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("add_poll_option_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add option (${options.size}/4)")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = caption,
                        onValueChange = onCaptionChange,
                        label = { Text("Context (optional)") },
                        placeholder = { Text("Why this matters...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("poll_caption_input")
                    )
                    OutlinedTextField(
                        value = hashtagsText,
                        onValueChange = onHashtagsChange,
                        label = { Text("Tags") },
                        placeholder = { Text("#vibe") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("poll_hashtags_input")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onPublish,
                    enabled = canPublish,
                    colors = ButtonDefaults.buttonColors(containerColor = VibeSocialBlue),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("publish_poll_button")
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Publishing...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Poll,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.create_poll_cta),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendingPollItemCard(
    rank: Int,
    post: Post,
    currentUserId: String,
    isLiked: Boolean,
    isSaved: Boolean,
    isFollowing: Boolean,
    myPollVote: Int?,
    onVotePoll: (Int) -> Unit,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onFollowClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val totalVotes = post.pollCounts.sum()
    val rankTint = when (rank) {
        1 -> VibeCoralRed
        2 -> VibeAmber
        3 -> VibeMagenta
        else -> VibeSocialBlue
    }
    val rankLabel = when (rank) {
        1 -> "#1 Trending"
        2 -> "#2 Hot"
        3 -> "#3 Rising"
        else -> "#$rank Poll"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .testTag("trending_poll_card_${post.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = rankTint.copy(alpha = 0.14f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = rankTint,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = rankLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = rankTint,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "$totalVotes ${if (totalVotes == 1L) "vote" else "votes"} • ${post.likesCount} likes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
        ) {
            PostCard(
                post = post,
                currentUserId = currentUserId,
                isLiked = isLiked,
                isSaved = isSaved,
                isFollowing = isFollowing,
                myPollVote = myPollVote,
                onLike = onLike,
                onComment = onComment,
                onShare = onShare,
                onSave = onSave,
                onAuthorClick = onAuthorClick,
                onFollowClick = onFollowClick,
                onVotePoll = onVotePoll,
                onDelete = onDelete
            )
        }
    }
}

private fun pollEngagementScore(post: Post): Long {
    val votes = post.pollCounts.sum()
    return (votes * 3L) + (post.likesCount * 2L) + post.commentsCount
}
