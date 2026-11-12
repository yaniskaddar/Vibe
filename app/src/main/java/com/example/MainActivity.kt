package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.authStateFlow
import com.example.core.configureAppCheck
import com.example.data.model.Post
import com.example.data.repository.FirebaseAiRepository
import com.example.data.repository.MessagingRepository
import com.example.data.repository.SocialRepository
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.SaveToCollectionBottomSheet
import com.example.ui.components.SharePostBottomSheet
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.CreateStudioScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReelsScreen
import com.example.ui.theme.VibeTheme
import com.example.ui.viewmodel.CameraCaptureTarget
import com.example.ui.viewmodel.CameraViewModel
import com.example.ui.viewmodel.CreateAiViewModel
import com.example.ui.viewmodel.CreateMode
import com.example.ui.viewmodel.MessagesViewModel
import com.example.ui.viewmodel.SocialViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_HOME = "home"
const val ROUTE_EXPLORE = "explore"
const val ROUTE_CREATE = "create"
const val ROUTE_REELS = "reels"
const val ROUTE_PROFILE = "profile"
const val ROUTE_MESSAGES = "messages"
const val ROUTE_CAMERA = "camera"

private data class VibeNavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val vibeNavTabs = listOf(
    VibeNavTab(ROUTE_HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    VibeNavTab(ROUTE_EXPLORE, R.string.nav_explore, Icons.Filled.Explore, Icons.Outlined.Explore),
    VibeNavTab(ROUTE_CREATE, R.string.nav_create, Icons.Filled.AddBox, Icons.Outlined.AddBox),
    VibeNavTab(ROUTE_REELS, R.string.nav_reels, Icons.Filled.Movie, Icons.Outlined.Movie),
    VibeNavTab(ROUTE_PROFILE, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Mandatory Step 1: Configure App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        setContent {
            VibeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            VibeAppShell(user = user)
        }
    }
}

@Composable
private fun VibeAppShell(user: FirebaseUser) {
    val uid = user.uid
    val socialViewModel: SocialViewModel = viewModel(
        key = "social_$uid",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                SocialViewModel(
                    repository = SocialRepository(db),
                    currentUserId = uid,
                    initialDisplayName = user.displayName,
                    initialEmail = user.email,
                    initialPhotoUrl = user.photoUrl?.toString()
                )
            }
        }
    )

    val createViewModel: CreateAiViewModel = viewModel(
        key = "create_$uid",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                CreateAiViewModel(
                    socialRepository = SocialRepository(db),
                    aiRepository = FirebaseAiRepository()
                )
            }
        }
    )

    val messagesViewModel: MessagesViewModel = viewModel(
        key = "messages_$uid",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                MessagesViewModel(
                    repository = MessagingRepository(db),
                    currentUserId = uid
                )
            }
        }
    )

    val cameraViewModel: CameraViewModel = viewModel(key = "camera_$uid")

    val myProfile by socialViewModel.myProfile.collectAsStateWithLifecycle()
    val allProfiles by socialViewModel.allProfiles.collectAsStateWithLifecycle()
    val feedState by socialViewModel.feedPosts.collectAsStateWithLifecycle()
    val stories by socialViewModel.activeStories.collectAsStateWithLifecycle()
    val likedPostIds by socialViewModel.likedPostIds.collectAsStateWithLifecycle()
    val followingIds by socialViewModel.followingIds.collectAsStateWithLifecycle()
    val collections by socialViewModel.myCollections.collectAsStateWithLifecycle()
    val myPollVotes by socialViewModel.myPollVotes.collectAsStateWithLifecycle()
    val selectedCommentsPostId by socialViewModel.selectedCommentsPostId.collectAsStateWithLifecycle()
    val comments by socialViewModel.commentsForSelectedPost.collectAsStateWithLifecycle()
    val feedback by socialViewModel.actionFeedback.collectAsStateWithLifecycle()
    val msgError by messagesViewModel.errorFeedback.collectAsStateWithLifecycle()

    var postToSave by remember { mutableStateOf<Post?>(null) }
    var postToShare by remember { mutableStateOf<Post?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedback) {
        feedback?.let {
            snackbarHostState.showSnackbar(it)
            socialViewModel.clearFeedback()
        }
    }
    LaunchedEffect(msgError) {
        msgError?.let {
            snackbarHostState.showSnackbar(it)
            messagesViewModel.clearError()
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME
    val showChromeNav = currentRoute != ROUTE_MESSAGES && currentRoute != ROUTE_CAMERA

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showChromeNav && !useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.background
                    ) {
                        vibeNavTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(tab.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showChromeNav && useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.background
                    ) {
                        vibeNavTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(tab.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("rail_tab_${tab.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HOME
                    ) {
                        composable(ROUTE_HOME) {
                            HomeScreen(
                                currentUserId = uid,
                                myProfile = myProfile,
                                feedState = feedState,
                                stories = stories,
                                likedPostIds = likedPostIds,
                                followingIds = followingIds,
                                collections = collections,
                                myPollVotes = myPollVotes,
                                onOpenMessages = { navController.navigate(ROUTE_MESSAGES) },
                                onOpenCamera = {
                                    cameraViewModel.setCaptureTarget(CameraCaptureTarget.STORY)
                                    navController.navigate(ROUTE_CAMERA)
                                },
                                onOpenCreateStory = {
                                    createViewModel.setMode(CreateMode.STORY)
                                    navigateToTab(ROUTE_CREATE)
                                },
                                onOpenCreatePost = {
                                    createViewModel.setMode(CreateMode.POST)
                                    navigateToTab(ROUTE_CREATE)
                                },
                                onLike = socialViewModel::toggleLike,
                                onComment = { post -> socialViewModel.openComments(post.id) },
                                onShare = { post -> postToShare = post },
                                onSave = { post -> postToSave = post },
                                onAuthorClick = { authorId ->
                                    if (authorId == uid) {
                                        navigateToTab(ROUTE_PROFILE)
                                    } else {
                                        val peer = allProfiles.firstOrNull { it.userId == authorId }
                                        if (peer != null) {
                                            messagesViewModel.startDirectChat(
                                                myHandle = myProfile?.handle ?: "creator",
                                                target = peer
                                            ) {
                                                navController.navigate(ROUTE_MESSAGES)
                                            }
                                        }
                                    }
                                },
                                onFollowClick = socialViewModel::toggleFollow,
                                onVotePoll = socialViewModel::castPollVote,
                                onDeletePost = socialViewModel::deletePost,
                                onReplyStory = { story, replyText ->
                                    val peer = allProfiles.firstOrNull { it.userId == story.authorId }
                                    if (peer != null) {
                                        messagesViewModel.startDirectChat(
                                            myHandle = myProfile?.handle ?: "creator",
                                            target = peer
                                        ) {
                                            messagesViewModel.sendMessage(
                                                myHandle = myProfile?.handle ?: "creator",
                                                text = "Replied to your story: $replyText"
                                            )
                                            navController.navigate(ROUTE_MESSAGES)
                                        }
                                    }
                                }
                            )
                        }

                        composable(ROUTE_EXPLORE) {
                            ExploreScreen(
                                currentUserId = uid,
                                feedState = feedState,
                                allProfiles = allProfiles,
                                followingIds = followingIds,
                                likedPostIds = likedPostIds,
                                collections = collections,
                                myPollVotes = myPollVotes,
                                onFollowClick = socialViewModel::toggleFollow,
                                onMessageCreator = { creator ->
                                    messagesViewModel.startDirectChat(
                                        myHandle = myProfile?.handle ?: "creator",
                                        target = creator
                                    ) {
                                        navController.navigate(ROUTE_MESSAGES)
                                    }
                                },
                                onLike = socialViewModel::toggleLike,
                                onComment = { post -> socialViewModel.openComments(post.id) },
                                onShare = { post -> postToShare = post },
                                onSave = { post -> postToSave = post },
                                onVotePoll = socialViewModel::castPollVote,
                                onDeletePost = socialViewModel::deletePost
                            )
                        }

                        composable(ROUTE_CREATE) {
                            CreateStudioScreen(
                                viewModel = createViewModel,
                                myProfile = myProfile,
                                onOpenCamera = { target ->
                                    cameraViewModel.setCaptureTarget(target)
                                    navController.navigate(ROUTE_CAMERA)
                                },
                                onPublishedSuccess = { mode ->
                                    if (mode == CreateMode.REEL) {
                                        navigateToTab(ROUTE_REELS)
                                    } else {
                                        navigateToTab(ROUTE_HOME)
                                    }
                                }
                            )
                        }

                        composable(ROUTE_CAMERA) {
                            CameraScreen(
                                viewModel = cameraViewModel,
                                onClose = { navController.popBackStack() },
                                onPhotoConfirmed = { bitmap, target ->
                                    createViewModel.applyCameraCapture(
                                        bitmap = bitmap,
                                        target = target,
                                        filter = com.example.core.FilterPreset.ORIGINAL
                                    )
                                    navController.popBackStack()
                                    navigateToTab(ROUTE_CREATE)
                                }
                            )
                        }

                        composable(ROUTE_REELS) {
                            ReelsScreen(
                                currentUserId = uid,
                                feedState = feedState,
                                likedPostIds = likedPostIds,
                                followingIds = followingIds,
                                collections = collections,
                                onOpenCreateReel = {
                                    createViewModel.setMode(CreateMode.REEL)
                                    navigateToTab(ROUTE_CREATE)
                                },
                                onLike = socialViewModel::toggleLike,
                                onComment = { post -> socialViewModel.openComments(post.id) },
                                onShare = { post -> postToShare = post },
                                onSave = { post -> postToSave = post },
                                onFollowClick = socialViewModel::toggleFollow
                            )
                        }

                        composable(ROUTE_PROFILE) {
                            ProfileScreen(
                                currentUserId = uid,
                                myProfile = myProfile,
                                feedState = feedState,
                                collections = collections,
                                likedPostIds = likedPostIds,
                                followingIds = followingIds,
                                myPollVotes = myPollVotes,
                                onUpdateProfile = socialViewModel::updateProfile,
                                onCreateCollection = { name ->
                                    socialViewModel.createCollectionAndSave(name, null)
                                },
                                onLike = socialViewModel::toggleLike,
                                onComment = { post -> socialViewModel.openComments(post.id) },
                                onShare = { post -> postToShare = post },
                                onSave = { post -> postToSave = post },
                                onVotePoll = socialViewModel::castPollVote,
                                onDeletePost = socialViewModel::deletePost,
                                onOpenMessages = { navController.navigate(ROUTE_MESSAGES) }
                            )
                        }

                        composable(ROUTE_MESSAGES) {
                            MessagesScreen(
                                viewModel = messagesViewModel,
                                myProfile = myProfile,
                                allProfiles = allProfiles,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedCommentsPostId?.let { postId ->
        CommentsBottomSheet(
            comments = comments,
            onDismiss = socialViewModel::closeComments,
            onSubmitComment = { text -> socialViewModel.submitComment(postId, text) }
        )
    }

    postToSave?.let { post ->
        SaveToCollectionBottomSheet(
            post = post,
            collections = collections,
            onDismiss = { postToSave = null },
            onToggleCollection = { colId ->
                socialViewModel.togglePostInCollection(colId, post)
            },
            onCreateCollection = { name ->
                socialViewModel.createCollectionAndSave(name, post)
            }
        )
    }

    postToShare?.let { post ->
        SharePostBottomSheet(
            profiles = allProfiles,
            currentUserId = uid,
            onDismiss = { postToShare = null },
            onSendToUser = { peer ->
                messagesViewModel.sharePostToUser(
                    myHandle = myProfile?.handle ?: "creator",
                    target = peer,
                    postId = post.id,
                    postCaption = post.caption
                )
                postToShare = null
            }
        )
    }
}
