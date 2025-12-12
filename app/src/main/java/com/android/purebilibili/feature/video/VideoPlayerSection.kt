// 文件路径: feature/video/VideoPlayerSection.kt
package com.android.purebilibili.feature.video

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
import androidx.compose.ui.graphics.Color
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
    onBack: () -> Unit
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
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls }
                )
            }
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

                                    getActivity()?.window?.attributes = getActivity()?.window?.attributes?.apply {
                                        screenBrightness = newBrightness
                                    }
                                    gesturePercent = newBrightness
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
    ) {
        // 🔥🔥 弹幕管理器
        val scope = rememberCoroutineScope()
        val danmakuManager = remember(context, scope) { DanmakuManager(context, scope) }
        
        // 🔥 弹幕开关设置
        val danmakuEnabled by com.android.purebilibili.core.store.SettingsManager
            .getDanmakuEnabled(context)
            .collectAsState(initial = true)
        
        // 🔥 当视频加载成功时加载弹幕
        LaunchedEffect(uiState) {
            if (uiState is PlayerUiState.Success) {
                val cid = uiState.info.cid
                if (cid > 0) {
                    danmakuManager.isEnabled = danmakuEnabled
                    danmakuManager.loadDanmaku(cid)
                }
            }
        }
        
        // 🔥 弹幕开关变化时更新
        LaunchedEffect(danmakuEnabled) {
            danmakuManager.isEnabled = danmakuEnabled
        }
        
        // 🔥 绑定 Player
        DisposableEffect(playerState.player) {
            danmakuManager.attachPlayer(playerState.player)
            onDispose {
                danmakuManager.release()
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
                }
            },
            update = { playerView ->
                playerView.player = playerState.player
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // 2. DanmakuView (覆盖在 PlayerView 上方)
        if (!isInPipMode) {
            AndroidView(
                factory = { ctx ->
                    master.flame.danmaku.ui.widget.DanmakuView(ctx).apply {
                        // 🔥🔥 [调试] 设置透明背景
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        danmakuManager.attachView(this)
                        android.util.Log.d("DanmakuManager", "🎨 DanmakuView created in factory")
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    // 🔥🔥 [调试] 添加红色边框验证视图位置
                    // .border(2.dp, Color.Red)
            )
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
                onDanmakuOpacityChange = { danmakuManager.opacity = it },
                onDanmakuFontScaleChange = { danmakuManager.fontScale = it },
                onDanmakuSpeedChange = { danmakuManager.speedFactor = it }
            )
        }
    }
}