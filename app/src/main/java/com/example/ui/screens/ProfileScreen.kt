package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.core.ImageUtils
import com.example.core.signOutUser
import com.example.data.model.MoodboardCollection
import com.example.data.model.Post
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.ui.components.PostCard
import com.example.ui.components.VibeAvatar
import com.example.ui.components.VibeMediaImage
import com.example.ui.theme.VibeSocialBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentUserId: String,
    myProfile: PublicProfile?,
    feedState: UiState<List<Post>>,
    collections: List<MoodboardCollection>,
    likedPostIds: Set<String>,
    followingIds: Set<String>,
    myPollVotes: Map<String, Int>,
    onUpdateProfile: (String, String, String, String?) -> Unit,
    onCreateCollection: (String) -> Unit,
    onLike: (Post) -> Unit,
    onComment: (Post) -> Unit,
    onShare: (Post) -> Unit,
    onSave: (Post) -> Unit,
    onVotePoll: (String, Int) -> Unit,
    onDeletePost: (String) -> Unit,
    onOpenMessages: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showNewBoardDialog by remember { mutableStateOf(false) }
    var selectedBoard by remember { mutableStateOf<MoodboardCollection?>(null) }
    var selectedPost by remember { mutableStateOf<Post?>(null) }

    val allPosts = (feedState as? UiState.Success)?.data.orEmpty()
    val myPosts = remember(allPosts, currentUserId) {
        allPosts.filter { it.authorId == currentUserId }
    }
    val myReels = remember(myPosts) {
        myPosts.filter { it.mediaType == "reel" }
    }
    val savedIds = remember(collections) { collections.flatMap { it.postIds }.toSet() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "@${myProfile?.handle ?: "creator"}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("edit_profile_button")
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_profile))
                }
                IconButton(
                    onClick = {
                        signOutUser(credentialManager, scope)
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.sign_out))
                }
            }
        )
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Profile Header
            item(span = { GridItemSpan(3) }, key = "profile_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VibeAvatar(
                            avatarData = myProfile?.avatarData.orEmpty(),
                            handle = myProfile?.handle ?: "V",
                            size = 82.dp
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatColumn(count = myPosts.size.toLong(), label = "Posts")
                            StatColumn(count = myProfile?.followersCount ?: 0L, label = "Followers")
                            StatColumn(count = myProfile?.followingCount ?: 0L, label = "Following")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = myProfile?.displayName ?: "Vibe Creator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!myProfile?.bio.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = myProfile?.bio.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                        ) {
                            Text(stringResource(R.string.edit_profile))
                        }
                        OutlinedButton(
                            onClick = onOpenMessages,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                        ) {
                            Text(stringResource(R.string.messages_title))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Filled.GridOn, contentDescription = "Posts") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Filled.Movie, contentDescription = "Reels") }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Filled.Bookmark, contentDescription = "Moodboards") }
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    if (myPosts.isEmpty()) {
                        item(span = { GridItemSpan(3) }) {
                            EmptyProfileTab("No posts yet. Share your first moment in Create Studio!")
                        }
                    } else {
                        items(myPosts, key = { it.id }) { post ->
                            ProfileGridTile(post = post, onClick = { selectedPost = post })
                        }
                    }
                }
                1 -> {
                    if (myReels.isEmpty()) {
                        item(span = { GridItemSpan(3) }) {
                            EmptyProfileTab("No Reels yet. Publish a video or motion Reel in Create Studio!")
                        }
                    } else {
                        items(myReels, key = { it.id }) { post ->
                            ProfileGridTile(post = post, onClick = { selectedPost = post })
                        }
                    }
                }
                2 -> {
                    item(span = { GridItemSpan(3) }, key = "moodboards_bar") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Saved Moodboards",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { showNewBoardDialog = true }) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Board")
                            }
                        }
                    }
                    items(collections, key = { it.id }) { col ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .padding(6.dp)
                                .aspectRatio(0.9f)
                                .clickable { selectedBoard = col }
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (col.coverPayload.isNotBlank()) {
                                        VibeMediaImage(
                                            payload = col.coverPayload,
                                            contentDescription = col.name,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Bookmark,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = col.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${col.postIds.size} items",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Makers Credit Footer
            item(span = { GridItemSpan(3) }, key = "makers_footer") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Vibe Social Studio",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.vibe_makers_credit),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog && myProfile != null) {
        var handleInput by remember(myProfile) { mutableStateOf(myProfile.handle) }
        var nameInput by remember(myProfile) { mutableStateOf(myProfile.displayName) }
        var bioInput by remember(myProfile) { mutableStateOf(myProfile.bio) }
        var newAvatarPayload by remember { mutableStateOf<String?>(null) }

        val avatarPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri ->
                if (uri != null) {
                    scope.launch {
                        val bmp = ImageUtils.decodeUriToBitmap(context, uri, maxDim = 400)
                        if (bmp != null) {
                            newAvatarPayload = withContext(Dispatchers.Default) {
                                ImageUtils.compressBitmapToDataUri(bmp, maxDimension = 360, maxBytes = 85_000)
                            }
                        }
                    }
                }
            }
        )

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.edit_profile)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VibeAvatar(
                            avatarData = newAvatarPayload ?: myProfile.avatarData,
                            handle = handleInput,
                            size = 54.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(onClick = {
                            avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Text("Change Photo")
                        }
                    }
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Display Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = handleInput,
                        onValueChange = { handleInput = it },
                        label = { Text("Handle") },
                        prefix = { Text("@") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        label = { Text("Bio") },
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(handleInput, nameInput, bioInput, newAvatarPayload)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VibeSocialBlue)
                ) {
                    Text(stringResource(R.string.save_changes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showNewBoardDialog) {
        var boardName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewBoardDialog = false },
            title = { Text("New Moodboard") },
            text = {
                OutlinedTextField(
                    value = boardName,
                    onValueChange = { boardName = it },
                    label = { Text("Board Name") },
                    placeholder = { Text("e.g. Neon Nightscapes") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (boardName.isNotBlank()) {
                            onCreateCollection(boardName.trim())
                            showNewBoardDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VibeSocialBlue)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewBoardDialog = false }) { Text("Cancel") }
            }
        )
    }

    selectedBoard?.let { board ->
        val boardPosts = allPosts.filter { it.id in board.postIds }
        ModalBottomSheet(onDismissRequest = { selectedBoard = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = board.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (boardPosts.isEmpty()) {
                    Text(
                        text = "No saved posts in this moodboard yet. Bookmark posts from your feed!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    boardPosts.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedBoard = null
                                    selectedPost = p
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (p.mediaPayloads.isNotEmpty()) {
                                VibeMediaImage(
                                    payload = p.mediaPayloads.first(),
                                    filterPreset = p.filterPreset,
                                    contentDescription = p.caption,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "@${p.authorHandle}", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = p.caption.ifBlank { p.pollQuestion },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                    onFollowClick = {},
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

@Composable
private fun StatColumn(count: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileGridTile(post: Post, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        if (post.mediaPayloads.isNotEmpty()) {
            VibeMediaImage(
                payload = post.mediaPayloads.first(),
                filterPreset = post.filterPreset,
                contentDescription = post.caption,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = post.pollQuestion.ifBlank { post.caption },
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyProfileTab(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
