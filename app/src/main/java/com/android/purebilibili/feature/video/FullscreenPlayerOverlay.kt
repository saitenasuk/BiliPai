// 文件路径: feature/video/FullscreenPlayerOverlay.kt
package com.android.purebilibili.feature.video

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Brightness7
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.util.FormatUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.runtime.collectAsState

private const val AUTO_HIDE_DELAY = 4000L

enum class FullscreenGestureMode { None, Brightness, Volume, Seek }

/**
 * 🔥 全屏播放器覆盖层
 * 
 * 从小窗展开时直接显示全屏播放器
 * 包含：亮度调节、音量调节、进度滑动等完整功能
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun FullscreenPlayerOverlay(
    miniPlayerManager: MiniPlayerManager,
    onDismiss: () -> Unit,
    onNavigateToDetail: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    
    var showControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    // 手势状态
    var gestureMode by remember { mutableStateOf(FullscreenGestureMode.None) }
    var gestureValue by remember { mutableFloatStateOf(0f) }
    var dragDelta by remember { mutableFloatStateOf(0f) }
    var seekPreviewPosition by remember { mutableLongStateOf(0L) }
    
    // 亮度状态
    var currentBrightness by remember { 
        mutableFloatStateOf(
            try {
                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
            } catch (e: Exception) { 0.5f }
        )
    }
    
    // 播放器状态
    val player = miniPlayerManager.player
    var isPlaying by remember { mutableStateOf(player?.isPlaying ?: false) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    
    // 进入全屏时设置横屏和沉浸式
    DisposableEffect(Unit) {
        val activity = (context as? Activity) ?: return@DisposableEffect onDispose {}
        val window = activity.window
        val originalOrientation = activity.requestedOrientation
        
        // 设置横屏
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        
        // 设置真正的沉浸式全屏
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        
        // 保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        onDispose {
            // 恢复竖屏
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            
            // 恢复系统栏
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            
            // 取消屏幕常亮
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    
    // 监听播放器状态
    LaunchedEffect(player) {
        while (true) {
            player?.let {
                isPlaying = it.isPlaying
                duration = it.duration.coerceAtLeast(1L)
                currentPosition = it.currentPosition
                if (gestureMode != FullscreenGestureMode.Seek) {
                    currentProgress = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                }
            }
            delay(200)
        }
    }
    
    // 自动隐藏控制按钮
    LaunchedEffect(showControls, lastInteractionTime) {
        if (showControls && gestureMode == FullscreenGestureMode.None) {
            delay(AUTO_HIDE_DELAY)
            if (System.currentTimeMillis() - lastInteractionTime >= AUTO_HIDE_DELAY) {
                showControls = false
            }
        }
    }
    
    // 返回键处理
    BackHandler { onNavigateToDetail() }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showControls = !showControls
                        if (showControls) lastInteractionTime = System.currentTimeMillis()
                    },
                    onDoubleTap = {
                        player?.let { if (it.isPlaying) it.pause() else it.play() }
                    }
                )
            }
            .pointerInput(Unit) {
                val screenWidth = size.width.toFloat()
                val screenHeight = size.height.toFloat()
                
                detectDragGestures(
                    onDragStart = { offset ->
                        showControls = true
                        lastInteractionTime = System.currentTimeMillis()
                        dragDelta = 0f
                        
                        // 根据起始位置决定手势类型
                        gestureMode = when {
                            offset.x < screenWidth * 0.3f -> {
                                gestureValue = currentBrightness
                                FullscreenGestureMode.Brightness
                            }
                            offset.x > screenWidth * 0.7f -> {
                                gestureValue = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
                                FullscreenGestureMode.Volume
                            }
                            else -> {
                                seekPreviewPosition = currentPosition
                                FullscreenGestureMode.Seek
                            }
                        }
                    },
                    onDragEnd = {
                        if (gestureMode == FullscreenGestureMode.Seek && abs(dragDelta) > 20f) {
                            player?.seekTo(seekPreviewPosition)
                        }
                        gestureMode = FullscreenGestureMode.None
                    },
                    onDragCancel = { gestureMode = FullscreenGestureMode.None },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        when (gestureMode) {
                            FullscreenGestureMode.Brightness -> {
                                gestureValue = (gestureValue - dragAmount.y / screenHeight).coerceIn(0f, 1f)
                                currentBrightness = gestureValue
                                (context as? Activity)?.window?.let { window ->
                                    val params = window.attributes
                                    params.screenBrightness = gestureValue
                                    window.attributes = params
                                }
                            }
                            FullscreenGestureMode.Volume -> {
                                gestureValue = (gestureValue - dragAmount.y / screenHeight).coerceIn(0f, 1f)
                                audioManager.setStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    (gestureValue * maxVolume).toInt(),
                                    0
                                )
                            }
                            FullscreenGestureMode.Seek -> {
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
    ) {
        // 🔥🔥 [新增] 弹幕管理器
        val scope = rememberCoroutineScope()
        val danmakuManager = remember(context, scope) { DanmakuManager(context, scope) }
        
        // 🔥 弹幕开关设置
        val danmakuEnabled by com.android.purebilibili.core.store.SettingsManager
            .getDanmakuEnabled(context)
            .collectAsState(initial = true)
        
        // 🔥 获取当前 cid 并加载弹幕
        val currentCid = miniPlayerManager.currentCid
        LaunchedEffect(currentCid, danmakuEnabled) {
            if (currentCid > 0 && danmakuEnabled) {
                danmakuManager.isEnabled = true
                danmakuManager.loadDanmaku(currentCid)
            } else {
                danmakuManager.isEnabled = false
            }
        }
        
        // 🔥 绑定 Player
        DisposableEffect(player) {
            player?.let { danmakuManager.attachPlayer(it) }
            onDispose {
                danmakuManager.release()
            }
        }
        
        // 视频播放器
        player?.let { exoPlayer ->
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = exoPlayer
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    }
                },
                update = { it.player = exoPlayer },
                modifier = Modifier.fillMaxSize()
            )
            
            // 🔥🔥 [新增] DanmakuView (覆盖在 PlayerView 上方)
            if (danmakuEnabled) {
                AndroidView(
                    factory = { ctx ->
                        master.flame.danmaku.ui.widget.DanmakuView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            danmakuManager.attachView(this)
                            com.android.purebilibili.core.util.Logger.d("FullscreenDanmaku", "🎨 DanmakuView created for fullscreen")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // 手势指示器
        if (gestureMode != FullscreenGestureMode.None) {
            GestureIndicator(
                mode = gestureMode,
                value = when (gestureMode) {
                    FullscreenGestureMode.Brightness -> currentBrightness
                    FullscreenGestureMode.Volume -> gestureValue
                    FullscreenGestureMode.Seek -> currentProgress
                    else -> 0f
                },
                seekTime = if (gestureMode == FullscreenGestureMode.Seek) seekPreviewPosition else null,
                duration = duration,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // 控制层
        AnimatedVisibility(
            visible = showControls && gestureMode == FullscreenGestureMode.None,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(300))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 顶部渐变 + 返回按钮和标题
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        IconButton(onClick = onNavigateToDetail) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回详情页", tint = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = miniPlayerManager.currentTitle,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // 中间播放/暂停按钮
                Surface(
                    onClick = {
                        lastInteractionTime = System.currentTimeMillis()
                        player?.let { if (it.isPlaying) it.pause() else it.play() }
                    },
                    modifier = Modifier.align(Alignment.Center),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = Color.White,
                        modifier = Modifier.padding(16.dp).size(48.dp)
                    )
                }
                
                // 底部进度条（可拖动）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).align(Alignment.Center)
                    ) {
                        Text(FormatUtils.formatDuration((currentPosition / 1000).toInt()), color = Color.White, fontSize = 12.sp)
                        
                        // 🔥 使用 Slider 替代 LinearProgressIndicator，支持拖动
                        var isDragging by remember { mutableStateOf(false) }
                        var dragProgress by remember { mutableFloatStateOf(0f) }
                        
                        Slider(
                            value = if (isDragging) dragProgress else currentProgress,
                            onValueChange = { newValue ->
                                isDragging = true
                                dragProgress = newValue
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            onValueChangeFinished = {
                                isDragging = false
                                val newPosition = (dragProgress * duration).toLong()
                                player?.seekTo(newPosition)
                                currentProgress = dragProgress
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
    }
}

@Composable
private fun GestureIndicator(
    mode: FullscreenGestureMode,
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
                FullscreenGestureMode.Brightness -> {
                    Icon(Icons.Rounded.Brightness7, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("亮度", color = Color.White, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("${(value * 100).toInt()}%", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                FullscreenGestureMode.Volume -> {
                    Icon(Icons.Rounded.VolumeUp, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("音量", color = Color.White, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("${(value * 100).toInt()}%", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                FullscreenGestureMode.Seek -> {
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
