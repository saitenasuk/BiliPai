// 文件路径: feature/video/VideoDetailScreen.kt
package com.android.purebilibili.feature.video

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.theme.BiliPink

import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.model.response.ViewInfo
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun VideoDetailScreen(
    bvid: String,
    coverUrl: String,
    onBack: () -> Unit,
    onUpClick: (Long) -> Unit = {},  // 🔥 点击 UP 主头像
    miniPlayerManager: MiniPlayerManager? = null,
    isInPipMode: Boolean = false,
    isVisible: Boolean = true,
    startInFullscreen: Boolean = false,  // 🔥 从小窗展开时自动进入全屏
    viewModel: PlayerViewModel = viewModel(),
    commentViewModel: VideoCommentViewModel = viewModel() // 🔥
) {
    val context = LocalContext.current
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val uiState by viewModel.uiState.collectAsState()
    
    // 🔥 监听评论状态
    val commentState by commentViewModel.commentState.collectAsState()
    val subReplyState by commentViewModel.subReplyState.collectAsState()

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var isPipMode by remember { mutableStateOf(isInPipMode) }
    LaunchedEffect(isInPipMode) { isPipMode = isInPipMode }
    
    // 🔥 从小窗展开时自动进入横屏全屏
    LaunchedEffect(startInFullscreen) {
        if (startInFullscreen && !isLandscape) {
            context.findActivity()?.let { activity ->
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }
    }

    // 退出重置亮度
    DisposableEffect(Unit) {
        onDispose {
            val window = context.findActivity()?.window
            val layoutParams = window?.attributes
            layoutParams?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window?.attributes = layoutParams
        }
    }
    
    // 🔥🔥 新增：监听消息事件（关注/收藏反馈）- 使用居中弹窗
    var popupMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            popupMessage = message
            // 2秒后自动隐藏
            kotlinx.coroutines.delay(2000)
            popupMessage = null
        }
    }

    // 初始化播放器状态
    val playerState = rememberVideoPlayerState(
        context = context,
        viewModel = viewModel,
        bvid = bvid
    )
    
    // 🔥🔥 [性能优化] 生命周期感知：进入后台时暂停播放，返回前台时继续
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    playerState.player.pause()
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    playerState.player.play()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 🔥🔥🔥 核心修改：初始化评论 & 媒体中心信息
    LaunchedEffect(uiState) {
        if (uiState is PlayerUiState.Success) {
            val info = (uiState as PlayerUiState.Success).info
            val success = uiState as PlayerUiState.Success
            
            // 初始化评论
            commentViewModel.init(info.aid)
            
            playerState.updateMediaMetadata(
                title = info.title,
                artist = info.owner.name,
                coverUrl = info.pic
            )
            
            // 🔥 同步视频信息到小窗管理器（为小窗模式做准备）
            com.android.purebilibili.core.util.Logger.d("VideoDetailScreen", "🔥 miniPlayerManager=${if (miniPlayerManager != null) "存在" else "null"}, bvid=$bvid")
            if (miniPlayerManager != null) {
                com.android.purebilibili.core.util.Logger.d("VideoDetailScreen", "🔥 调用 setVideoInfo: title=${info.title}")
                miniPlayerManager.setVideoInfo(
                    bvid = bvid,
                    title = info.title,
                    cover = info.pic,
                    owner = info.owner.name,
                    cid = info.cid,  // 🔥🔥 传递 cid 用于弹幕加载
                    externalPlayer = playerState.player
                )
                // 🔥🔥 [新增] 缓存完整 UI 状态，用于从小窗返回时恢复
                miniPlayerManager.cacheUiState(success)
                com.android.purebilibili.core.util.Logger.d("VideoDetailScreen", "✅ setVideoInfo + cacheUiState 调用完成")
            } else {
                android.util.Log.w("VideoDetailScreen", "⚠️ miniPlayerManager 是 null!")
            }
        } else if (uiState is PlayerUiState.Loading) {
            playerState.updateMediaMetadata(
                title = "加载中...",
                artist = "",
                coverUrl = coverUrl
            )
        }
    }
    
    // 🔥🔥🔥 弹幕加载逻辑已移至 VideoPlayerState 内部处理
    // 避免在此处重复消耗 InputStream

    // 辅助函数：切换屏幕方向
    fun toggleOrientation() {
        val activity = context.findActivity() ?: return
        if (isLandscape) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    // 🔥 拦截系统返回键：如果是全屏模式，则先退出全屏
    BackHandler(enabled = isLandscape) {
        toggleOrientation()
    }

    // 沉浸式状态栏控制
    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightBackground = remember(backgroundColor) { backgroundColor.luminance() > 0.5f }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context.findActivity())?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)

            if (isLandscape) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                window.statusBarColor = Color.Black.toArgb()
                window.navigationBarColor = Color.Black.toArgb()
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                insetsController.isAppearanceLightStatusBars = isLightBackground
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isLandscape) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        // 🔥 横竖屏过渡动画
        AnimatedContent(
            targetState = isLandscape,
            transitionSpec = {
                (fadeIn(animationSpec = tween(300)) + 
                 scaleIn(initialScale = 0.92f, animationSpec = tween(300)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(200)) + 
                        scaleOut(targetScale = 1.08f, animationSpec = tween(200))
                    )
            },
            label = "orientation_transition"
        ) { targetIsLandscape ->
            if (targetIsLandscape) {
                VideoPlayerSection(
                    playerState = playerState,
                    uiState = uiState,
                    isFullscreen = true,
                    isInPipMode = isPipMode,
                    onToggleFullscreen = { toggleOrientation() },
                    onQualityChange = { qid, pos -> viewModel.changeQuality(qid, pos) },
                    onBack = { toggleOrientation() },
                    // 🧪 实验性功能：双击点赞
                    onDoubleTapLike = { viewModel.toggleLike() }
                )
            } else {
                // 🔥🔥 B站风格布局：视频 + 内容区域
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. 播放器区域（4:3 比例，更接近官方 B站 App 竖屏播放器大小）
                    val screenWidthDp = configuration.screenWidthDp.dp
                    val playerHeight = screenWidthDp * 3f / 4f  // 🔥 使用 4:3 比例，让播放器更大
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(playerHeight)
                            .background(Color.Black)
                    ) {
                        VideoPlayerSection(
                            playerState = playerState,
                            uiState = uiState,
                            isFullscreen = false,
                            isInPipMode = isPipMode,
                            onToggleFullscreen = { toggleOrientation() },
                            onQualityChange = { qid, pos -> viewModel.changeQuality(qid, pos) },
                            onBack = onBack,
                            // 🧪 实验性功能：双击点赞
                            onDoubleTapLike = { viewModel.toggleLike() }
                        )
                    }

                    // 2. 内容区域（填充剩余空间）
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        when (uiState) {
                            is PlayerUiState.Loading -> {
                                val loadingState = uiState as PlayerUiState.Loading
                                // 🔥 显示重试进度
                                if (loadingState.retryAttempt > 0) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            // 🍎 iOS 风格加载
                                            CupertinoActivityIndicator()
                                            Spacer(Modifier.height(16.dp))
                                            Text(
                                                text = "正在重试 ${loadingState.retryAttempt}/${loadingState.maxAttempts}...",
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                } else {
                                    VideoDetailSkeleton()
                                }
                            }

                            is PlayerUiState.Success -> {
                                val success = uiState as PlayerUiState.Success
                                // 🔥 计算当前分P索引
                                val currentPageIndex = success.info.pages.indexOfFirst { it.cid == success.info.cid }.coerceAtLeast(0)
                                
                                VideoContentSection(
                                    info = success.info,
                                    relatedVideos = success.related,
                                    replies = commentState.replies,
                                    replyCount = commentState.replyCount,
                                    emoteMap = success.emoteMap,
                                    isRepliesLoading = commentState.isRepliesLoading,
                                    isFollowing = success.isFollowing,
                                    isFavorited = success.isFavorited,
                                    isLiked = success.isLiked,
                                    coinCount = success.coinCount,
                                    currentPageIndex = currentPageIndex,
                                    onFollowClick = { viewModel.toggleFollow() },
                                    onFavoriteClick = { viewModel.toggleFavorite() },
                                    onLikeClick = { viewModel.toggleLike() },
                                    onCoinClick = { viewModel.openCoinDialog() },
                                    onTripleClick = { viewModel.doTripleAction() },
                                    onPageSelect = { viewModel.switchPage(it) },
                                    onUpClick = onUpClick,
                                    onRelatedVideoClick = { vid -> viewModel.loadVideo(vid) },
                                    onSubReplyClick = { commentViewModel.openSubReply(it) },
                                    onLoadMoreReplies = { commentViewModel.loadComments() }
                                )
                            }

                            is PlayerUiState.Error -> {
                                val errorState = uiState as PlayerUiState.Error
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(32.dp)
                                    ) {
                                        // 🔥 根据错误类型显示不同图标
                                        Text(
                                            text = when (errorState.error) {
                                                is com.android.purebilibili.data.model.VideoLoadError.NetworkError -> "📡"
                                                is com.android.purebilibili.data.model.VideoLoadError.VideoNotFound -> "🔍"
                                                is com.android.purebilibili.data.model.VideoLoadError.RegionRestricted -> "🌐"
                                                else -> "⚠️"
                                            },
                                            fontSize = 48.sp
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            text = errorState.msg,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 16.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        // 🔥 只有可重试的错误才显示重试按钮
                                        if (errorState.canRetry) {
                                            Spacer(Modifier.height(24.dp))
                                            Button(
                                                onClick = { viewModel.retry() },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary
                                                )
                                            ) {
                                                Text("重试")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 🔥🔥 [新增] 投币对话框
        val coinDialogVisible by viewModel.coinDialogVisible.collectAsState()
        val currentCoinCount = (uiState as? PlayerUiState.Success)?.coinCount ?: 0
        CoinDialog(
            visible = coinDialogVisible,
            currentCoinCount = currentCoinCount,
            onDismiss = { viewModel.closeCoinDialog() },
            onConfirm = { count, alsoLike -> viewModel.doCoin(count, alsoLike) }
        )
        
        // 🔥 评论二级弹窗
        if (subReplyState.visible) {
            BackHandler {
                commentViewModel.closeSubReply()
            }
            val successState = uiState as? PlayerUiState.Success
            SubReplySheet(
                state = subReplyState,
                emoteMap = successState?.emoteMap ?: emptyMap(),
                onDismiss = { commentViewModel.closeSubReply() },
                onLoadMore = { commentViewModel.loadMoreSubReplies() }
            )
        }
        
        // 🎉 点赞成功爆裂动画
        val likeBurstVisible by viewModel.likeBurstVisible.collectAsState()
        if (likeBurstVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-50).dp)
            ) {
                LikeBurstAnimation(
                    visible = true,
                    onAnimationEnd = { viewModel.dismissLikeBurst() }
                )
            }
        }
        
        // 🎉 三连成功庆祝动画
        val tripleCelebrationVisible by viewModel.tripleCelebrationVisible.collectAsState()
        if (tripleCelebrationVisible) {
            Box(
                modifier = Modifier.align(Alignment.Center)
            ) {
                TripleSuccessAnimation(
                    visible = true,
                    onAnimationEnd = { viewModel.dismissTripleCelebration() }
                )
            }
        }
        
        // 🔥🔥 居中弹窗提示（关注/收藏反馈）
        androidx.compose.animation.AnimatedVisibility(
            visible = popupMessage != null,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                tonalElevation = 8.dp
            ) {
                Text(
                    text = popupMessage ?: "",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                )
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

// VideoContentSection 保持原样，无需修改
@Composable
fun VideoContentSection(
    info: ViewInfo,
    relatedVideos: List<RelatedVideo>,
    replies: List<ReplyItem>,
    replyCount: Int,
    emoteMap: Map<String, String>,
    isRepliesLoading: Boolean,
    isFollowing: Boolean,
    isFavorited: Boolean,
    isLiked: Boolean,
    coinCount: Int,
    currentPageIndex: Int,
    onFollowClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCoinClick: () -> Unit,
    onTripleClick: () -> Unit,
    onPageSelect: (Int) -> Unit,
    onUpClick: (Long) -> Unit,
    onRelatedVideoClick: (String) -> Unit,
    onSubReplyClick: (ReplyItem) -> Unit,
    onLoadMoreReplies: () -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Tab 状态
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("简介", "评论 $replyCount")
    
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. 标题和统计行 (置顶)
        item {
            VideoTitleSection(
                info = info,
                onUpClick = onUpClick
            )
        }

        // 2. 操作按钮行
        item {
            ActionButtonsRow(
                info = info,
                isFavorited = isFavorited,
                isLiked = isLiked,
                coinCount = coinCount,
                onFavoriteClick = onFavoriteClick,
                onLikeClick = onLikeClick,
                onCoinClick = onCoinClick,
                onTripleClick = onTripleClick,
                onCommentClick = {
                    selectedTabIndex = 1 // 切换到评论 Tab
                    // 可选：滚动到评论位置
                }
            )
        }

        // 3. Tab 栏
        item { // 使用 stickyHeader 如果想吸顶，但这里普通 item 即可，或者 lazyColumn 外面套 column
             Column {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = BiliPink,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = BiliPink
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    title,
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selectedContentColor = BiliPink,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            }
        }

        // 4. Tab 内容
        if (selectedTabIndex == 0) {
            // === 简介 Tab 内容 ===

            // UP主信息
            item {
                UpInfoSection(
                    info = info,
                    isFollowing = isFollowing,
                    onFollowClick = onFollowClick,
                    onUpClick = onUpClick
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }

            // 视频简介
            item { DescriptionSection(desc = info.desc) }

            // 分P选择器 (仅多P视频显示)
            if (info.pages.size > 1) {
                item {
                    PagesSelector(
                        pages = info.pages,
                        currentPageIndex = currentPageIndex,
                        onPageSelect = onPageSelect
                    )
                }
            }


            // 相关视频推荐
            item { 
                Spacer(Modifier.height(8.dp))
                VideoRecommendationHeader() 
            }

            items(relatedVideos, key = { it.bvid }) { video ->
                RelatedVideoItem(video = video, onClick = { onRelatedVideoClick(video.bvid) })
            }
            
        } else {
            // === 评论 Tab 内容 ===
            item { ReplyHeader(count = replyCount) }
            
            if (isRepliesLoading && replies.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CupertinoActivityIndicator()
                    }
                }
            } else if (replies.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("暂无评论", color = Color.Gray)
                    }
                }
            } else {
                items(items = replies, key = { it.rpid }) { reply ->
                    ReplyItemView(
                        item = reply,
                        emoteMap = emoteMap,
                        onClick = {},
                        onSubClick = { onSubReplyClick(reply) }
                    )
                }
                
                // 加载更多提示
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (replies.size < replyCount) {
                             LaunchedEffect(Unit) { onLoadMoreReplies() }
                             CupertinoActivityIndicator()
                        } else {
                             Text("—— end ——", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
    }
        }
    }
}

// 辅助组件：推荐视频标题
@Composable
private fun VideoRecommendationHeader() {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "相关推荐",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}