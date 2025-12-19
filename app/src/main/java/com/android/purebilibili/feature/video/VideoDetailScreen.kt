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
import androidx.compose.ui.draw.clipToBounds
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.theme.BiliPink

import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.model.response.ViewInfo
// Refactored UI components
import com.android.purebilibili.feature.video.ui.section.VideoTitleSection
import com.android.purebilibili.feature.video.ui.section.VideoTitleWithDesc
import com.android.purebilibili.feature.video.ui.section.UpInfoSection
import com.android.purebilibili.feature.video.ui.section.DescriptionSection
import com.android.purebilibili.feature.video.ui.section.ActionButtonsRow
import com.android.purebilibili.feature.video.ui.section.ActionButton
import com.android.purebilibili.feature.video.ui.components.RelatedVideosHeader
import com.android.purebilibili.feature.video.ui.components.RelatedVideoItem
import com.android.purebilibili.feature.video.ui.components.CoinDialog
import com.android.purebilibili.feature.video.ui.components.PagesSelector
// Imports for moved classes
import com.android.purebilibili.feature.video.viewmodel.PlayerViewModel
import com.android.purebilibili.feature.video.viewmodel.PlayerUiState
import com.android.purebilibili.feature.video.viewmodel.VideoCommentViewModel
import com.android.purebilibili.feature.video.state.VideoPlayerState
import com.android.purebilibili.feature.video.state.rememberVideoPlayerState
import com.android.purebilibili.feature.video.ui.section.VideoPlayerSection
import com.android.purebilibili.feature.video.ui.components.SubReplySheet
import com.android.purebilibili.feature.video.ui.components.ReplyHeader
import com.android.purebilibili.feature.video.ui.components.ReplyItemView
import com.android.purebilibili.feature.video.ui.components.LikeBurstAnimation
import com.android.purebilibili.feature.video.ui.components.TripleSuccessAnimation
import com.android.purebilibili.feature.video.ui.components.VideoDetailSkeleton
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// 🔥 共享元素过渡
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope

@OptIn(ExperimentalSharedTransitionApi::class)
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
    transitionEnabled: Boolean = false,  // 🔥 卡片过渡动画开关
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
    
    // 🚀 空降助手状态
    val sponsorSegment by viewModel.currentSponsorSegment.collectAsState()
    val showSponsorSkipButton by viewModel.showSkipButton.collectAsState()
    val sponsorBlockEnabled by com.android.purebilibili.core.store.SettingsManager
        .getSponsorBlockEnabled(context)
        .collectAsState(initial = false)

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var isPipMode by remember { mutableStateOf(isInPipMode) }
    LaunchedEffect(isInPipMode) { isPipMode = isInPipMode }
    
    // 🚀 空降助手：检查播放位置（🔧 性能优化：自适应检查间隔）
    LaunchedEffect(sponsorBlockEnabled, uiState) {
        if (sponsorBlockEnabled && uiState is PlayerUiState.Success) {
            while (true) {
                // 🚀 根据是否有即将到来的片段动态调整检查频率
                // 无片段或远离片段时：1000ms；接近片段时：300ms
                val interval = if (sponsorSegment != null || (viewModel.sponsorSegments.value.isNotEmpty())) {
                    300L  // 有活跃片段或片段列表非空时，更频繁检查
                } else {
                    1000L // 无片段时降低检查频率，节省 CPU
                }
                kotlinx.coroutines.delay(interval)
                viewModel.checkAndSkipSponsor(context)
            }
        }
    }
    
    // 🔥 从小窗展开时自动进入横屏全屏
    LaunchedEffect(startInFullscreen) {
        if (startInFullscreen && !isLandscape) {
            context.findActivity()?.let { activity ->
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }
    }

    // 🔥🔥 用于跟踪组件是否正在退出，防止 SideEffect 覆盖恢复操作
    var isScreenActive by remember { mutableStateOf(true) }
    
    // 🔥🔥 [关键] 保存进入前的状态栏配置（在 DisposableEffect 外部定义以便复用）
    val activity = remember { context.findActivity() }
    val window = remember { activity?.window }
    val insetsController = remember {
        if (window != null && activity != null) {
            WindowCompat.getInsetsController(window, window.decorView)
        } else null
    }
    val originalStatusBarColor = remember { window?.statusBarColor ?: android.graphics.Color.TRANSPARENT }
    val originalLightStatusBars = remember { insetsController?.isAppearanceLightStatusBars ?: true }
    
    // 🔥🔥 [新增] 恢复状态栏的函数（可复用）
    val restoreStatusBar = remember {
        {
            if (window != null && insetsController != null) {
                insetsController.isAppearanceLightStatusBars = originalLightStatusBars
                window.statusBarColor = originalStatusBarColor
            }
        }
    }
    
    // 🔥🔥 [新增] 包装的 onBack，在导航之前立即恢复状态栏
    val handleBack = remember(onBack) {
        {
            isScreenActive = false  // 标记页面正在退出
            restoreStatusBar()      // 🔥 立即恢复状态栏（动画开始前）
            onBack()                // 执行实际的返回导航
        }
    }
    
    // 退出重置亮度 + 🔥 屏幕常亮管理 + 状态栏恢复（作为安全网）
    DisposableEffect(Unit) {
        // 🔥🔥 [沉浸式] 启用边到边显示，让内容延伸到状态栏下方
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        
        // 🔥🔥 [修复] 进入视频页时保持屏幕常亮，防止自动熄屏
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        onDispose {
            // 🔥🔥 [关键] 标记页面正在退出，防止 SideEffect 覆盖
            isScreenActive = false
            
            val layoutParams = window?.attributes
            layoutParams?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window?.attributes = layoutParams
            
            // 🔥🔥 [修复] 离开视频页时取消屏幕常亮
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            // 🔥🔥 [安全网] 确保状态栏被恢复（以防 handleBack 未被调用，如系统返回）
            restoreStatusBar()
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
    
    // 🔥 初始化进度持久化存储
    LaunchedEffect(Unit) {
        viewModel.initWithContext(context)
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

    // 🔥🔥 iOS风格：竖屏时状态栏黑色背景（与播放器融为一体）
    // 🔥🔥 只在页面活跃时修改状态栏，避免退出时覆盖恢复操作
    if (!view.isInEditMode && isScreenActive) {
        SideEffect {
            val window = (view.context.findActivity())?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)

            if (isLandscape) {
                // 全屏隐藏状态栏
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                window.statusBarColor = Color.Black.toArgb()
                window.navigationBarColor = Color.Black.toArgb()
            } else {
                // 🔥🔥 [沉浸式] 竖屏时状态栏透明，让视频延伸到状态栏下方
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                insetsController.isAppearanceLightStatusBars = false  // 白色图标（视频区域是深色的）
                window.statusBarColor = Color.Transparent.toArgb()  // 透明状态栏
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
                    onDoubleTapLike = { viewModel.toggleLike() },
                    // 🚀 空降助手
                    sponsorSegment = sponsorSegment,
                    showSponsorSkipButton = showSponsorSkipButton,
                    onSponsorSkip = { viewModel.skipCurrentSponsorSegment() },
                    onSponsorDismiss = { viewModel.dismissSponsorSkipButton() }
                )
            } else {
                // 🔥🔥 沉浸式布局：视频延伸到状态栏 + 内容区域
                Column(modifier = Modifier.fillMaxSize()) {
                    // 🔥🔥 [沉浸式] 获取状态栏高度
                    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    val screenWidthDp = configuration.screenWidthDp.dp
                    val videoHeight = screenWidthDp * 9f / 16f  // 16:9 比例
                    
                    // 🔥🔥 注意：移除了状态栏黑色 Spacer
                    // 播放器将延伸到状态栏下方，共享元素过渡更流畅
                    
                    // ✅ 视频播放器区域 - 包含状态栏高度
                    // 🔥 尝试获取共享元素作用域
                    val sharedTransitionScope = LocalSharedTransitionScope.current
                    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
                    
                    // 🔥 为播放器容器添加共享元素标记（受开关控制）
                    val playerContainerModifier = if (transitionEnabled && sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            Modifier
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState(key = "video_cover_$bvid"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    // 🔥 添加回弹效果的 spring 动画
                                    boundsTransform = { _, _ ->
                                        spring(
                                            dampingRatio = 0.7f,   // 轻微回弹
                                            stiffness = 300f       // 适中速度
                                        )
                                    },
                                    clipInOverlayDuringTransition = OverlayClip(
                                        RoundedCornerShape(0.dp)  // 🔥 播放器无圆角
                                    )
                                )
                        }
                    } else {
                        Modifier
                    }
                    
                    // 🔥🔥 播放器容器包含状态栏高度，让视频延伸到顶部
                    Box(
                        modifier = playerContainerModifier
                            .fillMaxWidth()
                            .height(videoHeight + statusBarHeight)  // 🔥 包含状态栏高度
                            .background(Color.Black)  // 黑色背景
                            .clipToBounds()
                    ) {
                        // 🔥 播放器内部使用 padding 避开状态栏
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = statusBarHeight)  // 🔥 顶部 padding 避开状态栏
                        ) {
                            VideoPlayerSection(
                                playerState = playerState,
                                uiState = uiState,
                                isFullscreen = false,
                                isInPipMode = isPipMode,
                                onToggleFullscreen = { toggleOrientation() },
                                onQualityChange = { qid, pos -> viewModel.changeQuality(qid, pos) },
                                onBack = handleBack,
                                onDoubleTapLike = { viewModel.toggleLike() },
                                sponsorSegment = sponsorSegment,
                                showSponsorSkipButton = showSponsorSkipButton,
                                onSponsorSkip = { viewModel.skipCurrentSponsorSegment() },
                                onSponsorDismiss = { viewModel.dismissSponsorSkipButton() }
                            )
                        }
                    }

                    // ✅ 第3层：内容区域
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
        // 🔥🔥 [官方布局] 1. UP主信息 (置顶)
        item {
            UpInfoSection(
                info = info,
                isFollowing = isFollowing,
                onFollowClick = onFollowClick,
                onUpClick = onUpClick
            )
        }

        // 🔥🔥 [官方布局] 2. 标题 + 统计 + 描述 (紧凑排列)
        item {
            VideoTitleWithDesc(
                info = info
            )
        }

        // 🔥🔥 [官方布局] 3. 操作按钮行
        item {
            ActionButtonsRow(
                info = info,
                isFavorited = isFavorited,
                isLiked = isLiked,
                coinCount = coinCount,
                onFavoriteClick = onFavoriteClick,
                onLikeClick = onLikeClick,
                onCoinClick = onCoinClick,
                onTripleClick = onTripleClick
                // 🔥🔥 [删除] onCommentClick 已移除，因下方有评论 Tab
            )
        }

        // 🔥🔥 [官方布局] 4. Tab 栏（简介/评论 + 发弹幕入口）
        item {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧 Tab 按钮
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTabIndex == index
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedTabIndex = index }
                                .padding(vertical = 6.dp, horizontal = 6.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) BiliPink else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            // 下划线指示器
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(2.dp)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(1.dp))
                                    .background(if (isSelected) BiliPink else Color.Transparent)
                            )
                        }
                        if (index < tabs.lastIndex) {
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // 右侧发弹幕入口
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { /* TODO: 打开弹幕发送框 */ }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "点我发弹幕",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        // 弹幕图标
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                .background(BiliPink),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "弹",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            }
        }

        // 5. Tab 内容
        if (selectedTabIndex == 0) {
            // === 简介 Tab 内容 ===

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
                Spacer(Modifier.height(4.dp))
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