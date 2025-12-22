// 文件路径: feature/video/VideoPlayerSection.kt
package com.android.purebilibili.feature.video.ui.section

import com.android.purebilibili.feature.video.danmaku.DanmakuManager
import com.android.purebilibili.feature.video.danmaku.rememberDanmakuManager
import com.android.purebilibili.feature.video.state.VideoPlayerState
import com.android.purebilibili.feature.video.viewmodel.PlayerUiState
import com.android.purebilibili.feature.video.ui.overlay.VideoPlayerOverlay
import com.android.purebilibili.feature.video.ui.components.SponsorSkipButton
import com.android.purebilibili.feature.video.ui.components.VideoAspectRatio

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brightness7
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.ui.PlayerView
import com.android.purebilibili.core.util.FormatUtils
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class VideoGestureMode { None, Brightness, Volume, Seek }

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerSection(
    playerState: VideoPlayerState,
    uiState: PlayerUiState,
    isFullscreen: Boolean,
    isInPipMode: Boolean,
    onToggleFullscreen: () -> Unit,
    onQualityChange: (Int, Long) -> Unit,
    onBack: () -> Unit,
    // 🧪 实验性功能：双击点赞
    onDoubleTapLike: () -> Unit = {},
    // 🚀 空降助手
    sponsorSegment: com.android.purebilibili.data.model.response.SponsorSegment? = null,
    showSponsorSkipButton: Boolean = false,
    onSponsorSkip: () -> Unit = {},
    onSponsorDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    // --- 新增：读取设置中的"详细统计信息"开关 ---
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    // 使用 rememberUpdatedState 确保重组时获取最新值（虽然在单一 Activity 生命周期内可能需要重启生效，但简单场景够用）
    val showStats by remember { mutableStateOf(prefs.getBoolean("show_stats", false)) }
    
    // 🔥🔥 [新增] 读取手势灵敏度设置
    val gestureSensitivity by com.android.purebilibili.core.store.SettingsManager
        .getGestureSensitivity(context)
        .collectAsState(initial = 1.0f)

    // --- 新增：存储真实分辨率 ---
    var realResolution by remember { mutableStateOf("") }
    
    // 🧪 读取双击点赞设置 (从 DataStore 读取)
    val doubleTapLikeEnabled by com.android.purebilibili.core.store.SettingsManager
        .getDoubleTapLike(context)
        .collectAsState(initial = true)

    // --- 新增：监听 ExoPlayer 分辨率变化 ---
    DisposableEffect(playerState.player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                // 当视频流尺寸改变时更新
                if (videoSize.width > 0 && videoSize.height > 0) {
                    realResolution = "${videoSize.width} x ${videoSize.height}"
                }
            }
        }
        playerState.player.addListener(listener)
        // 初始化获取一次
        val size = playerState.player.videoSize
        if (size.width > 0) {
            realResolution = "${size.width} x ${size.height}"
        }

        onDispose {
            playerState.player.removeListener(listener)
        }
    }

    // 控制器显示状态
        var showControls by remember { mutableStateOf(true) }

    var gestureMode by remember { mutableStateOf<VideoGestureMode>(VideoGestureMode.None) }
    var gestureIcon by remember { mutableStateOf<ImageVector?>(null) }
    var gesturePercent by remember { mutableFloatStateOf(0f) }

    // 进度手势相关状态
    var seekTargetTime by remember { mutableLongStateOf(0L) }
    var startPosition by remember { mutableLongStateOf(0L) }
    var isGestureVisible by remember { mutableStateOf(false) }
    
    // 🔥 视频比例状态
    var currentAspectRatio by remember { mutableStateOf(VideoAspectRatio.FIT) }

    // 记录手势开始时的初始值
    var startVolume by remember { mutableIntStateOf(0) }
    var startBrightness by remember { mutableFloatStateOf(0f) }

    // 记录累计拖动距离
    var totalDragDistanceY by remember { mutableFloatStateOf(0f) }
    var totalDragDistanceX by remember { mutableFloatStateOf(0f) }

    fun getActivity(): Activity? = when (context) {
        is Activity -> context
        is ContextWrapper -> context.baseContext as? Activity
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()  // 🔥 确保所有内容（包括弹幕）不会超出边界
            .background(Color.Black)
            // 🔥 先处理拖拽手势
            .pointerInput(isInPipMode) {
                if (!isInPipMode) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isGestureVisible = true
                            gestureMode = VideoGestureMode.None
                            totalDragDistanceY = 0f
                            totalDragDistanceX = 0f

                            startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            startPosition = playerState.player.currentPosition

                            val attributes = getActivity()?.window?.attributes
                            val currentWindowBrightness = attributes?.screenBrightness ?: -1f

                            if (currentWindowBrightness < 0) {
                                try {
                                    val sysBrightness = Settings.System.getInt(
                                        context.contentResolver,
                                        Settings.System.SCREEN_BRIGHTNESS
                                    )
                                    startBrightness = sysBrightness / 255f
                                } catch (e: Exception) {
                                    startBrightness = 0.5f
                                }
                            } else {
                                startBrightness = currentWindowBrightness
                            }
                        },
                        onDragEnd = {
                            if (gestureMode == VideoGestureMode.Seek) {
                                playerState.player.seekTo(seekTargetTime)
                                playerState.player.play()
                            }
                            isGestureVisible = false
                            gestureMode = VideoGestureMode.None
                        },
                        onDragCancel = {
                            isGestureVisible = false
                            gestureMode = VideoGestureMode.None
                        },
                        // 🔥🔥 [修复点] 使用 dragAmount 而不是 change.positionChange()
                        onDrag = { change, dragAmount ->
                            if (gestureMode == VideoGestureMode.None) {
                                if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                    gestureMode = VideoGestureMode.Seek
                                } else {
                                    // 根据起始 X 坐标判断左右屏
                                    val screenWidth = context.resources.displayMetrics.widthPixels
                                    gestureMode = if (change.position.x < screenWidth / 2) {
                                        VideoGestureMode.Brightness
                                    } else {
                                        VideoGestureMode.Volume
                                    }
                                }
                            }

                            when (gestureMode) {
                                VideoGestureMode.Seek -> {
                                    totalDragDistanceX += dragAmount.x
                                    val duration = playerState.player.duration.coerceAtLeast(0L)
                                    // 🔥 应用灵敏度
                                    val seekDelta = (totalDragDistanceX * 200 * gestureSensitivity).toLong()
                                    seekTargetTime = (startPosition + seekDelta).coerceIn(0L, duration)
                                }
                                VideoGestureMode.Brightness -> {
                                    totalDragDistanceY -= dragAmount.y
                                    val screenHeight = context.resources.displayMetrics.heightPixels
                                    // 🔥 应用灵敏度
                                    val deltaPercent = totalDragDistanceY / screenHeight * gestureSensitivity
                                    val newBrightness = (startBrightness + deltaPercent).coerceIn(0f, 1f)
                                    
                                    // 🚀 优化：仅在变化超过阈值时更新（减少 WindowManager 调用）
                                    if (kotlin.math.abs(newBrightness - gesturePercent) > 0.02f) {
                                        getActivity()?.window?.attributes = getActivity()?.window?.attributes?.apply {
                                            screenBrightness = newBrightness
                                        }
                                        gesturePercent = newBrightness
                                    }
                                    gestureIcon = Icons.Rounded.Brightness7
                                }
                                VideoGestureMode.Volume -> {
                                    totalDragDistanceY -= dragAmount.y
                                    val screenHeight = context.resources.displayMetrics.heightPixels
                                    // 🔥 应用灵敏度
                                    val deltaPercent = totalDragDistanceY / screenHeight * gestureSensitivity
                                    val newVolPercent = ((startVolume.toFloat() / maxVolume) + deltaPercent).coerceIn(0f, 1f)
                                    val targetVol = (newVolPercent * maxVolume).toInt()
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                    gesturePercent = newVolPercent
                                    gestureIcon = Icons.Rounded.VolumeUp
                                }
                                else -> {}
                            }
                        }
                    )
                }
            }
            // 🔥 点击/双击手势在拖拽之后处理
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = { offset ->
                        // 🔥 双击暂停/播放（用户体验改进）
                        com.android.purebilibili.core.util.Logger.d("VideoPlayerSection", "🔥 DoubleTap: toggle play/pause")
                        playerState.player.playWhenReady = !playerState.player.playWhenReady
                    }
                )
            }
    ) {
        // 🔥🔥 弹幕管理器 (使用单例模式，确保横竖屏切换时保持状态)
        val scope = rememberCoroutineScope()  // 🔥 用于设置弹幕开关
        val danmakuManager = rememberDanmakuManager()
        
        // 🔥 弹幕开关设置
        val danmakuEnabled by com.android.purebilibili.core.store.SettingsManager
            .getDanmakuEnabled(context)
            .collectAsState(initial = true)
        
        // 🔥 当视频加载成功时加载弹幕（不再依赖 isFullscreen，单例会保持弹幕）
        val cid = (uiState as? PlayerUiState.Success)?.info?.cid ?: 0L
        // 🔥 监听 player 状态，等待 duration 可用后加载弹幕
        LaunchedEffect(cid) {
            if (cid > 0) {
                danmakuManager.isEnabled = danmakuEnabled
                
                // 🔥🔥 [修复] 等待播放器准备好并获取 duration (最多等待 5 秒)
                var durationMs = 0L
                var retries = 0
                while (durationMs <= 0 && retries < 50) {
                    durationMs = playerState.player.duration.takeIf { it > 0 } ?: 0L
                    if (durationMs <= 0) {
                        kotlinx.coroutines.delay(100)
                        retries++
                    }
                }
                
                android.util.Log.d("VideoPlayerSection", "🎯 Loading danmaku for cid=$cid, duration=${durationMs}ms (after $retries retries)")
                danmakuManager.loadDanmaku(cid, durationMs)  // 🔥 传入时长启用 Protobuf API
            }
        }
        
        // 🔥 弹幕开关变化时更新
        LaunchedEffect(danmakuEnabled) {
            danmakuManager.isEnabled = danmakuEnabled
        }
        
        // 🔥 绑定 Player（不在 onDispose 中释放，单例保持状态）
        DisposableEffect(playerState.player) {
            android.util.Log.d("VideoPlayerSection", "🎬 attachPlayer, isFullscreen=$isFullscreen")
            danmakuManager.attachPlayer(playerState.player)
            onDispose {
                // 单例模式不需要释放
            }
        }
        
        // 1. PlayerView (底层)
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = playerState.player
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    useController = false
                    keepScreenOn = true
                    resizeMode = currentAspectRatio.resizeMode
                }
            },
            update = { playerView ->
                playerView.player = playerState.player
                playerView.resizeMode = currentAspectRatio.resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // 2. DanmakuView (使用 ByteDance DanmakuRenderEngine - 覆盖在 PlayerView 上方)
        android.util.Log.d("VideoPlayerSection", "🔍 DanmakuView check: isInPipMode=$isInPipMode, danmakuEnabled=$danmakuEnabled")
        if (!isInPipMode && danmakuEnabled) {
            android.util.Log.d("VideoPlayerSection", "✅ Conditions met, creating DanmakuView...")
            // 🔥 计算状态栏高度
            val statusBarHeightPx = remember(context) {
                val resourceId = context.resources.getIdentifier(
                    "status_bar_height", "dimen", "android"
                )
                if (resourceId > 0) {
                    context.resources.getDimensionPixelSize(resourceId)
                } else {
                    (24 * context.resources.displayMetrics.density).toInt()
                }
            }
            
            // 🔥 非全屏时的顶部偏移量
            val topOffset = if (isFullscreen) 0 else statusBarHeightPx + 20
            
            key(isFullscreen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (!isFullscreen) {
                                Modifier.padding(top = with(LocalContext.current.resources.displayMetrics) {
                                    (topOffset / density).dp
                                })
                            } else Modifier
                        )
                        .clipToBounds()
                ) {
                    AndroidView(
                        factory = { ctx ->
                            com.bytedance.danmaku.render.engine.DanmakuView(ctx).apply {
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                danmakuManager.attachView(this)
                                android.util.Log.d("VideoPlayerSection", "✅ DanmakuView (RenderEngine) created, isFullscreen=$isFullscreen")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (isGestureVisible && !isInPipMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(120.dp)
                    .background(Color.Black.copy(0.7f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (gestureMode == VideoGestureMode.Seek) {
                        val durationSeconds = (playerState.player.duration / 1000).coerceAtLeast(1)
                        val targetSeconds = (seekTargetTime / 1000).toInt()

                        Text(
                            text = "${FormatUtils.formatDuration(targetSeconds)} / ${FormatUtils.formatDuration(durationSeconds.toInt())}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        val deltaSeconds = (seekTargetTime - startPosition) / 1000
                        val sign = if (deltaSeconds > 0) "+" else ""
                        if (deltaSeconds != 0L) {
                            Text(
                                text = "($sign${deltaSeconds}s)",
                                color = if (deltaSeconds > 0) Color.Green else Color.Red,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        Icon(
                            imageVector = gestureIcon ?: Icons.Rounded.Brightness7,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(gesturePercent * 100).toInt()}%",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        )
                    }
                }
            }
        }

        if (uiState is PlayerUiState.Success && !isInPipMode) {
            VideoPlayerOverlay(
                player = playerState.player,
                title = uiState.info.title,
                isVisible = showControls,
                onToggleVisible = { showControls = !showControls },
                isFullscreen = isFullscreen,
                currentQualityLabel = uiState.qualityLabels.getOrNull(uiState.qualityIds.indexOf(uiState.currentQuality)) ?: "自动",
                qualityLabels = uiState.qualityLabels,
                qualityIds = uiState.qualityIds,
                onQualitySelected = { index ->
                    val id = uiState.qualityIds.getOrNull(index) ?: 0
                    onQualityChange(id, playerState.player.currentPosition)
                },
                onBack = onBack,
                onToggleFullscreen = onToggleFullscreen,

                // 🔥🔥 [关键] 传入设置状态和真实分辨率字符串
                showStats = showStats,
                realResolution = realResolution,
                // 🔥🔥 [新增] 传入清晰度切换状态和会员状态
                isQualitySwitching = uiState.isQualitySwitching,
                isLoggedIn = uiState.isLoggedIn,
                isVip = uiState.isVip,
                // 🔥🔥 [新增] 弹幕开关和设置
                danmakuEnabled = danmakuEnabled,
                onDanmakuToggle = {
                    val newState = !danmakuEnabled
                    scope.launch {
                        com.android.purebilibili.core.store.SettingsManager.setDanmakuEnabled(context, newState)
                    }
                },
                danmakuOpacity = danmakuManager.opacity,
                danmakuFontScale = danmakuManager.fontScale,
                danmakuSpeed = danmakuManager.speedFactor,
                danmakuDisplayArea = danmakuManager.displayArea,
                onDanmakuOpacityChange = { danmakuManager.opacity = it },
                onDanmakuFontScaleChange = { danmakuManager.fontScale = it },
                onDanmakuSpeedChange = { danmakuManager.speedFactor = it },
                onDanmakuDisplayAreaChange = { danmakuManager.displayArea = it },
                // 🔥 视频比例调节
                currentAspectRatio = currentAspectRatio,
                onAspectRatioChange = { currentAspectRatio = it }
            )
        }
        
        // 🚀 空降助手跳过按钮
        if (!isInPipMode) {
            SponsorSkipButton(
                segment = sponsorSegment,
                visible = showSponsorSkipButton,
                onSkip = onSponsorSkip,
                onDismiss = onSponsorDismiss,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}