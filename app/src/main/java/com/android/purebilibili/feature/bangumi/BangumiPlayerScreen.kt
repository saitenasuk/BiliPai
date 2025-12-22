// 文件路径: feature/bangumi/BangumiPlayerScreen.kt
package com.android.purebilibili.feature.bangumi

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.BangumiDetail
import com.android.purebilibili.data.model.response.BangumiEpisode
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import com.android.purebilibili.feature.video.ui.components.SponsorSkipButton
import com.android.purebilibili.feature.video.danmaku.DanmakuManager
import com.android.purebilibili.feature.video.danmaku.rememberDanmakuManager

/**
 * 番剧播放页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BangumiPlayerScreen(
    seasonId: Long,
    epId: Long,
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit = {},  // 🔥 新增：导航到登录页
    viewModel: BangumiPlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val uiState by viewModel.uiState.collectAsState()
    
    // 🚀 空降助手状态
    val sponsorSegment by viewModel.currentSponsorSegment.collectAsState()
    val showSponsorSkipButton by viewModel.showSkipButton.collectAsState()
    val sponsorBlockEnabled by com.android.purebilibili.core.store.SettingsManager
        .getSponsorBlockEnabled(context)
        .collectAsState(initial = false)
    
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    // 创建 ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }
    
    // 附加播放器到 ViewModel
    LaunchedEffect(exoPlayer) {
        viewModel.attachPlayer(exoPlayer)
    }
    
    // 加载番剧
    LaunchedEffect(seasonId, epId) {
        viewModel.loadBangumiPlay(seasonId, epId)
    }
    
    // 🚀 空降助手：定期检查播放位置
    LaunchedEffect(sponsorBlockEnabled, uiState) {
        if (sponsorBlockEnabled && uiState is BangumiPlayerState.Success) {
            while (true) {
                kotlinx.coroutines.delay(500)
                viewModel.checkAndSkipSponsor(context)
            }
        }
    }
    
    // 🔥🔥 [重构] 弹幕管理器 - 使用单例确保横竖屏切换时保持状态
    val danmakuManager = rememberDanmakuManager()
    
    // 弹幕开关设置
    val danmakuEnabled by com.android.purebilibili.core.store.SettingsManager
        .getDanmakuEnabled(context)
        .collectAsState(initial = true)
    
    // 获取当前剧集 cid
    val currentCid = (uiState as? BangumiPlayerState.Success)?.currentEpisode?.cid ?: 0L
    
    // 加载弹幕 - 在父级组件管理
    LaunchedEffect(currentCid, danmakuEnabled) {
        android.util.Log.d("BangumiPlayer", "🎯 Parent Danmaku LaunchedEffect: cid=$currentCid, enabled=$danmakuEnabled")
        if (currentCid > 0 && danmakuEnabled) {
            danmakuManager.isEnabled = true
            danmakuManager.loadDanmaku(currentCid)
        } else {
            danmakuManager.isEnabled = false
        }
    }
    
    // 绑定 Player
    DisposableEffect(exoPlayer) {
        danmakuManager.attachPlayer(exoPlayer)
        onDispose { /* Player 在另一个 DisposableEffect 中释放 */ }
    }
    
    // 清理弹幕管理器（解绑视图但不释放数据，单例会保持状态）
    DisposableEffect(Unit) {
        onDispose {
            danmakuManager.detachView()
        }
    }
    
    // 注意：视频播放由 ViewModel.playVideo 处理，不在这里设置 MediaItem
    
    // 清理播放器 + 🔥 屏幕常亮管理
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        
        // 🔥🔥 [修复] 进入番剧播放页时保持屏幕常亮，防止自动熄屏
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        onDispose {
            exoPlayer.release()
            // 🔥 恢复默认方向，避免离开播放器后卡在横屏
            context.findActivity()?.requestedOrientation = 
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            
            // 🔥🔥 [修复] 离开番剧播放页时取消屏幕常亮
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    
    // 辅助函数：切换屏幕方向
    fun toggleOrientation() {
        val activity = context.findActivity() ?: return
        if (isLandscape) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }
    
    // 🔥 自动检测设备方向变化并解锁旋转
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val orientationEventListener = object : android.view.OrientationEventListener(context) {
            private var lastOrientation = -1
            
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                
                // 将传感器角度转换为方向类别
                val newOrientation = when (orientation) {
                    in 315..360, in 0..45 -> 0   // Portrait
                    in 46..134 -> 270           // Landscape (reverse)
                    in 135..224 -> 180          // Portrait (upside down)
                    in 225..314 -> 90           // Landscape
                    else -> lastOrientation
                }
                
                // 当设备物理方向与当前锁定方向不同时，解锁旋转
                if (newOrientation != lastOrientation && lastOrientation != -1) {
                    val isDeviceLandscape = newOrientation == 90 || newOrientation == 270
                    val isDevicePortrait = newOrientation == 0 || newOrientation == 180
                    
                    activity?.let { act ->
                        // 如果当前是横屏锁定，用户把手机竖过来，解锁为竖屏
                        if (isLandscape && isDevicePortrait) {
                            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                        // 如果当前是竖屏，用户把手机横过来，解锁为横屏
                        else if (!isLandscape && isDeviceLandscape) {
                            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        }
                    }
                }
                lastOrientation = newOrientation
            }
        }
        
        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable()
        }
        
        onDispose {
            orientationEventListener.disable()
        }
    }
    
    // 拦截系统返回键
    BackHandler(enabled = isLandscape) {
        toggleOrientation()
    }
    
    // 沉浸式状态栏控制
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context.findActivity())?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            
            if (isLandscape) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = 
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                window.statusBarColor = Color.Black.toArgb()
                window.navigationBarColor = Color.Black.toArgb()
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
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
        // 获取当前 cid 用于弹幕
        val currentCid = (uiState as? BangumiPlayerState.Success)?.currentEpisode?.cid ?: 0L
        
        // 🔥 获取清晰度数据
        val successState = uiState as? BangumiPlayerState.Success
        
        if (isLandscape) {
            // 全屏播放
            BangumiPlayerView(
                exoPlayer = exoPlayer,
                danmakuManager = danmakuManager,  // 🔥 传入父级的弹幕管理器
                danmakuEnabled = danmakuEnabled,
                modifier = Modifier.fillMaxSize(),
                isFullscreen = true,
                currentQuality = successState?.quality ?: 0,
                acceptQuality = successState?.acceptQuality ?: emptyList(),
                acceptDescription = successState?.acceptDescription ?: emptyList(),
                onQualityChange = { viewModel.changeQuality(it) },
                onBack = { toggleOrientation() },
                onToggleFullscreen = { toggleOrientation() },
                // 🚀 空降助手
                sponsorSegment = sponsorSegment,
                showSponsorSkipButton = showSponsorSkipButton,
                onSponsorSkip = { viewModel.skipCurrentSponsorSegment() },
                onSponsorDismiss = { viewModel.dismissSponsorSkipButton() }
            )
        } else {
            // 竖屏：播放器 + 内容
            Column(modifier = Modifier.fillMaxSize()) {
                // 🔥 播放器区域 - 放大为 2:3 比例
                val screenWidthDp = configuration.screenWidthDp.dp
                val playerHeight = screenWidthDp * 2f / 3f  // 🔥 放大播放器高度
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(playerHeight)
                        .background(Color.Black)
                ) {
                    BangumiPlayerView(
                        exoPlayer = exoPlayer,
                        danmakuManager = danmakuManager,  // 🔥 传入父级的弹幕管理器
                        danmakuEnabled = danmakuEnabled,
                        modifier = Modifier.fillMaxSize(),
                        isFullscreen = false,
                        currentQuality = successState?.quality ?: 0,
                        acceptQuality = successState?.acceptQuality ?: emptyList(),
                        acceptDescription = successState?.acceptDescription ?: emptyList(),
                        onQualityChange = { viewModel.changeQuality(it) },
                        onBack = onBack,
                        onToggleFullscreen = { toggleOrientation() },
                        // 🚀 空降助手
                        sponsorSegment = sponsorSegment,
                        showSponsorSkipButton = showSponsorSkipButton,
                        onSponsorSkip = { viewModel.skipCurrentSponsorSegment() },
                        onSponsorDismiss = { viewModel.dismissSponsorSkipButton() }
                    )
                }
                
                // 🔥 新增：始终可见的进度条
                BangumiMiniProgressBar(
                    player = exoPlayer,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 内容区域
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (val state = uiState) {
                        is BangumiPlayerState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CupertinoActivityIndicator()
                            }
                        }
                        
                        is BangumiPlayerState.Error -> {
                            BangumiErrorContent(
                                message = state.message,
                                isVipRequired = state.isVipRequired,
                                isLoginRequired = state.isLoginRequired,
                                canRetry = state.canRetry,
                                onRetry = { viewModel.retry() },
                                onLogin = onNavigateToLogin
                            )
                        }
                        
                        is BangumiPlayerState.Success -> {
                            BangumiPlayerContent(
                                detail = state.seasonDetail,
                                currentEpisode = state.currentEpisode,
                                onEpisodeClick = { viewModel.switchEpisode(it) },
                                onFollowClick = { viewModel.toggleFollow() }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 增强版播放器视图
 * 支持：左侧亮度调节、右侧音量调节、进度拖动、弹幕显示
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun BangumiPlayerView(
    exoPlayer: ExoPlayer,
    danmakuManager: DanmakuManager,  // 🔥 从父级传入
    danmakuEnabled: Boolean,  // 🔥 从父级传入
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    // 🔥 新增：清晰度相关参数
    currentQuality: Int = 0,
    acceptQuality: List<Int> = emptyList(),
    acceptDescription: List<String> = emptyList(),
    onQualityChange: (Int) -> Unit = {},
    onBack: () -> Unit,
    onToggleFullscreen: () -> Unit,
    // 🚀 空降助手
    sponsorSegment: com.android.purebilibili.data.model.response.SponsorSegment? = null,
    showSponsorSkipButton: Boolean = false,
    onSponsorSkip: () -> Unit = {},
    onSponsorDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // 音频管理
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) }
    
    // 控制层状态
    var showControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showQualityMenu by remember { mutableStateOf(false) }  // 🔥 新增：画质菜单状态
    
    // 手势状态
    var gestureMode by remember { mutableStateOf(BangumiGestureMode.None) }
    var gestureValue by remember { mutableFloatStateOf(0f) }
    var dragDelta by remember { mutableFloatStateOf(0f) }
    var seekPreviewPosition by remember { mutableLongStateOf(0L) }
    
    // 亮度状态
    var currentBrightness by remember {
        mutableFloatStateOf(
            try {
                android.provider.Settings.System.getInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255f
            } catch (e: Exception) { 0.5f }
        )
    }
    
    // 播放器状态
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(1L) }
    
    // 监听播放器状态
    LaunchedEffect(exoPlayer) {
        while (true) {
            isPlaying = exoPlayer.isPlaying
            duration = exoPlayer.duration.coerceAtLeast(1L)
            currentPosition = exoPlayer.currentPosition
            if (gestureMode != BangumiGestureMode.Seek) {
                currentProgress = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            }
            kotlinx.coroutines.delay(200)
        }
    }
    
    // 自动隐藏控制
    LaunchedEffect(showControls, lastInteractionTime) {
        if (showControls && gestureMode == BangumiGestureMode.None) {
            kotlinx.coroutines.delay(4000)
            if (System.currentTimeMillis() - lastInteractionTime >= 4000) {
                showControls = false
            }
        }
    }
    
    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showControls = !showControls
                        if (showControls) lastInteractionTime = System.currentTimeMillis()
                    },
                    onDoubleTap = {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    }
                )
            }
            .then(
                if (isFullscreen) {
                    Modifier.pointerInput(Unit) {
                        val screenWidth = size.width.toFloat()
                        val screenHeight = size.height.toFloat()
                        
                        detectDragGestures(
                            onDragStart = { offset ->
                                showControls = true
                                lastInteractionTime = System.currentTimeMillis()
                                dragDelta = 0f
                                seekPreviewPosition = currentPosition
                                // 🔥🔥 [修复] 暂不设置模式，等待第一次拖动确定方向
                                gestureMode = BangumiGestureMode.None
                            },
                            onDragEnd = {
                                if (gestureMode == BangumiGestureMode.Seek && kotlin.math.abs(dragDelta) > 20f) {
                                    exoPlayer.seekTo(seekPreviewPosition)
                                }
                                gestureMode = BangumiGestureMode.None
                            },
                            onDragCancel = { gestureMode = BangumiGestureMode.None },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                
                                // 🔥🔥 [修复] 第一次拖动时根据方向决定模式
                                if (gestureMode == BangumiGestureMode.None) {
                                    gestureMode = if (kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y)) {
                                        // 水平拖动 -> 进度调节
                                        BangumiGestureMode.Seek
                                    } else {
                                        // 垂直拖动 -> 根据起始位置决定亮度或音量
                                        if (change.position.x < screenWidth * 0.5f) {
                                            gestureValue = currentBrightness
                                            BangumiGestureMode.Brightness
                                        } else {
                                            gestureValue = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat() / maxVolume
                                            BangumiGestureMode.Volume
                                        }
                                    }
                                }
                                
                                when (gestureMode) {
                                    BangumiGestureMode.Brightness -> {
                                        gestureValue = (gestureValue - dragAmount.y / screenHeight).coerceIn(0f, 1f)
                                        currentBrightness = gestureValue
                                        (context as? Activity)?.window?.let { window ->
                                            val params = window.attributes
                                            params.screenBrightness = gestureValue
                                            window.attributes = params
                                        }
                                    }
                                    BangumiGestureMode.Volume -> {
                                        gestureValue = (gestureValue - dragAmount.y / screenHeight).coerceIn(0f, 1f)
                                        audioManager.setStreamVolume(
                                            android.media.AudioManager.STREAM_MUSIC,
                                            (gestureValue * maxVolume).toInt(),
                                            0
                                        )
                                    }
                                    BangumiGestureMode.Seek -> {
                                        dragDelta += dragAmount.x
                                        val seekDelta = (dragDelta / screenWidth * duration).toLong()
                                        seekPreviewPosition = (currentPosition + seekDelta).coerceIn(0L, duration)
                                        currentProgress = (seekPreviewPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                                    }
                                    else -> {}
                                }
                            }
                        )
                    }
                } else Modifier
            )
    ) {
        // PlayerView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true  // 🔥 确保屏幕常亮
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // 弹幕层 - 使用 DanmakuRenderEngine
        if (danmakuEnabled) {
            AndroidView(
                factory = { ctx ->
                    com.bytedance.danmaku.render.engine.DanmakuView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        danmakuManager.attachView(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // 手势指示器（仅全屏）
        if (isFullscreen && gestureMode != BangumiGestureMode.None) {
            BangumiGestureIndicator(
                mode = gestureMode,
                value = when (gestureMode) {
                    BangumiGestureMode.Brightness -> currentBrightness
                    BangumiGestureMode.Volume -> gestureValue
                    BangumiGestureMode.Seek -> currentProgress
                    else -> 0f
                },
                seekTime = if (gestureMode == BangumiGestureMode.Seek) seekPreviewPosition else null,
                duration = duration,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // 控制层
        androidx.compose.animation.AnimatedVisibility(
            visible = showControls && gestureMode == BangumiGestureMode.None,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 返回按钮
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
                }
                
                // 全屏按钮
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.Fullscreen, "全屏", tint = Color.White)
                }
                
                // 🔥 新增：画质选择按钮
                if (acceptDescription.isNotEmpty()) {
                    val currentQualityLabel = acceptDescription.getOrNull(
                        acceptQuality.indexOf(currentQuality)
                    ) ?: "自动"
                    
                    Surface(
                        onClick = { showQualityMenu = true },
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                    ) {
                        Text(
                            text = currentQualityLabel,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                
                // 播放/暂停按钮
                IconButton(
                    onClick = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                // 进度条（全屏时显示）
                if (isFullscreen) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(FormatUtils.formatDuration((currentPosition / 1000).toInt()), color = Color.White, fontSize = 12.sp)
                        
                        Slider(
                            value = currentProgress,
                            onValueChange = { 
                                lastInteractionTime = System.currentTimeMillis()
                                currentProgress = it 
                            },
                            onValueChangeFinished = {
                                exoPlayer.seekTo((currentProgress * duration).toLong())
                            },
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                        
                        Text(FormatUtils.formatDuration((duration / 1000).toInt()), color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
        
        // 🔥 新增：画质选择菜单
        if (showQualityMenu && acceptDescription.isNotEmpty()) {
            BangumiQualityMenu(
                qualities = acceptDescription,
                qualityIds = acceptQuality,
                currentQualityId = currentQuality,
                onQualitySelected = { qn ->
                    onQualityChange(qn)
                    showQualityMenu = false
                },
                onDismiss = { showQualityMenu = false }
            )
        }
        
        // 🚀 空降助手跳过按钮
        SponsorSkipButton(
            segment = sponsorSegment,
            visible = showSponsorSkipButton,
            onSkip = onSponsorSkip,
            onDismiss = onSponsorDismiss,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

// 手势模式枚举
private enum class BangumiGestureMode { None, Brightness, Volume, Seek }

// 手势指示器
@Composable
private fun BangumiGestureIndicator(
    mode: BangumiGestureMode,
    value: Float,
    seekTime: Long?,
    duration: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.8f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            when (mode) {
                BangumiGestureMode.Brightness -> {
                    Icon(Icons.Default.LightMode, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("亮度", color = Color.White, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("${(value * 100).toInt()}%", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                BangumiGestureMode.Volume -> {
                    Icon(Icons.Default.VolumeUp, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("音量", color = Color.White, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("${(value * 100).toInt()}%", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                BangumiGestureMode.Seek -> {
                    Text(
                        "${FormatUtils.formatDuration(((seekTime ?: 0) / 1000).toInt())} / ${FormatUtils.formatDuration((duration / 1000).toInt())}",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                else -> {}
            }
        }
    }
}

/**
 * 🔥 可拖动的迷你进度条（竖屏模式） - 紧凑样式
 */
@Composable
private fun BangumiMiniProgressBar(
    player: ExoPlayer,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var bufferedProgress by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    
    // 定期更新进度
    LaunchedEffect(player) {
        while (true) {
            if (player.duration > 0 && !isDragging) {
                progress = player.currentPosition.toFloat() / player.duration
                bufferedProgress = player.bufferedPosition.toFloat() / player.duration
            }
            kotlinx.coroutines.delay(200)
        }
    }
    
    // 🔥 使用 Box + pointerInput 实现紧凑的可拖动进度条
    Box(
        modifier = modifier
            .height(12.dp)  // 可点击区域扩大
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    val seekPosition = (fraction * player.duration).toLong()
                    player.seekTo(seekPosition)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        val seekPosition = (dragProgress * player.duration).toLong()
                        player.seekTo(seekPosition)
                        isDragging = false
                    },
                    onDragCancel = { isDragging = false },
                    onDrag = { _, dragAmount ->
                        dragProgress = (dragProgress + dragAmount.x / size.width).coerceIn(0f, 1f)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 进度条容器 - 实际显示的细条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Color.DarkGray.copy(alpha = 0.5f))
        ) {
            // 缓冲进度
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(bufferedProgress.coerceIn(0f, 1f))
                    .background(Color.White.copy(alpha = 0.3f))
            )
            // 播放进度
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((if (isDragging) dragProgress else progress).coerceIn(0f, 1f))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/**
 * 🔥 番剧画质选择菜单
 */
@Composable
private fun BangumiQualityMenu(
    qualities: List<String>,
    qualityIds: List<Int>,
    currentQualityId: Int,
    onQualitySelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // 获取画质标签（VIP/登录）
    fun getQualityTag(qn: Int): String? {
        return when (qn) {
            127, 126, 125, 120, 116, 112 -> "大会员"
            else -> null
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 200.dp, max = 280.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = false) {},
            color = Color(0xFF2B2B2B),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "画质选择",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                HorizontalDivider(color = Color.White.copy(0.1f))
                
                qualities.forEachIndexed { index, quality ->
                    val qn = qualityIds.getOrNull(index) ?: 0
                    val isSelected = qn == currentQualityId
                    val tag = getQualityTag(qn)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onQualitySelected(qn) }
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = quality,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(0.9f),
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        
                        if (tag != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFFFB7299),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = tag,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 番剧播放内容区域
 */
@Composable
private fun BangumiPlayerContent(
    detail: BangumiDetail,
    currentEpisode: BangumiEpisode,
    onEpisodeClick: (BangumiEpisode) -> Unit,
    onFollowClick: () -> Unit
) {
    val isFollowing = detail.userStatus?.follow == 1
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 标题和信息
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = detail.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "正在播放：${currentEpisode.title} ${currentEpisode.longTitle}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                detail.stat?.let { stat ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${FormatUtils.formatStat(stat.views)}播放 · ${FormatUtils.formatStat(stat.danmakus)}弹幕",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // 追番按钮
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Button(
                    onClick = onFollowClick,
                    modifier = Modifier.weight(1f),
                    colors = if (isFollowing) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    } else {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    }
                ) {
                    Icon(
                        if (isFollowing) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isFollowing) "已追番" else "追番")
                }
            }
        }
        
        // 剧集选择
        if (!detail.episodes.isNullOrEmpty()) {
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                
                // 🔥 选集标题和快速跳转
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "选集 (${detail.episodes.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    // 🔥 当集数超过 50 时显示快速跳转
                    if (detail.episodes.size > 50) {
                        var showJumpDialog by remember { mutableStateOf(false) }
                        
                        Surface(
                            onClick = { showJumpDialog = true },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "跳转",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // 🔥 快速跳转对话框
                        if (showJumpDialog) {
                            EpisodeJumpDialog(
                                totalEpisodes = detail.episodes.size,
                                onJump = { epNumber ->
                                    val targetEpisode = detail.episodes.getOrNull(epNumber - 1)
                                    if (targetEpisode != null) {
                                        onEpisodeClick(targetEpisode)
                                    }
                                    showJumpDialog = false
                                },
                                onDismiss = { showJumpDialog = false }
                            )
                        }
                    }
                }
            }
            
            // 🔥 对于超长剧集，添加范围选择器
            if (detail.episodes.size > 50) {
                item {
                    val episodesPerPage = 50
                    val totalPages = (detail.episodes.size + episodesPerPage - 1) / episodesPerPage
                    var selectedPage by remember { mutableIntStateOf(0) }
                    
                    // 当前集所在的页
                    val currentEpisodeIndex = detail.episodes.indexOfFirst { it.id == currentEpisode.id }
                    LaunchedEffect(currentEpisodeIndex) {
                        if (currentEpisodeIndex >= 0) {
                            selectedPage = currentEpisodeIndex / episodesPerPage
                        }
                    }
                    
                    // 范围选择器
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(totalPages) { page ->
                            val start = page * episodesPerPage + 1
                            val end = minOf((page + 1) * episodesPerPage, detail.episodes.size)
                            val isCurrentPage = page == selectedPage
                            
                            Surface(
                                onClick = { selectedPage = page },
                                color = if (isCurrentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "$start-$end",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    color = if (isCurrentPage) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // 当前页的剧集
                    val pageStart = selectedPage * episodesPerPage
                    val pageEnd = minOf(pageStart + episodesPerPage, detail.episodes.size)
                    val pageEpisodes = detail.episodes.subList(pageStart, pageEnd)
                    
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pageEpisodes) { episode ->
                            EpisodeChipSelectable(
                                episode = episode,
                                isSelected = episode.id == currentEpisode.id,
                                onClick = { onEpisodeClick(episode) }
                            )
                        }
                    }
                }
            } else {
                // 普通剧集列表
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(detail.episodes) { episode ->
                            EpisodeChipSelectable(
                                episode = episode,
                                isSelected = episode.id == currentEpisode.id,
                                onClick = { onEpisodeClick(episode) }
                            )
                        }
                    }
                }
            }
        }
        
        // 简介
        if (detail.evaluate.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "简介",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = detail.evaluate,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun EpisodeChipSelectable(
    episode: BangumiEpisode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = episode.title.ifEmpty { "第${episode.id}话" },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * 🔥 快速跳转集数对话框
 */
@Composable
private fun EpisodeJumpDialog(
    totalEpisodes: Int,
    onJump: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("跳转到第几集") },
        text = {
            Column {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { 
                        inputText = it.filter { char -> char.isDigit() }
                        errorMessage = null
                    },
                    label = { Text("集数 (1-$totalEpisodes)") },
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val epNumber = inputText.toIntOrNull()
                    if (epNumber == null || epNumber < 1 || epNumber > totalEpisodes) {
                        errorMessage = "请输入 1-$totalEpisodes 之间的数字"
                    } else {
                        onJump(epNumber)
                    }
                }
            ) {
                Text("跳转")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun BangumiErrorContent(
    message: String,
    isVipRequired: Boolean,
    isLoginRequired: Boolean = false,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onLogin: () -> Unit = {}
) {
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
                text = when {
                    isVipRequired -> "👑"
                    isLoginRequired -> "🔐"
                    else -> "⚠️"
                },
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (isVipRequired) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "开通大会员即可观看",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            // 🔥 新增：登录按钮
            if (isLoginRequired) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onLogin,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("去登录")
                }
            }
            if (canRetry) {
                Spacer(modifier = Modifier.height(if (isLoginRequired) 12.dp else 24.dp))
                if (isLoginRequired) {
                    TextButton(onClick = onRetry) { Text("重试") }
                } else {
                    Button(onClick = onRetry) { Text("重试") }
                }
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
