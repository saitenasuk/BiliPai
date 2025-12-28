// 文件路径: feature/video/MiniPlayerOverlay.kt
package com.android.purebilibili.feature.video.ui.overlay

import com.android.purebilibili.feature.video.player.MiniPlayerManager

import com.android.purebilibili.core.util.Logger
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// 🍎 Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.media3.ui.PlayerView
// 🔥 已改用 MaterialTheme.colorScheme.primary
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TAG = "MiniPlayerOverlay"
private const val AUTO_HIDE_DELAY_MS = 3000L

/**
 * 🔥 小窗播放器覆盖层
 * 
 * 交互说明：
 * - 拖动顶部标题栏区域 → 移动小窗位置
 * - 在视频区域左右滑动 → 调节播放进度
 * - 单击 → 显示/隐藏控制按钮
 * - 双击 → 展开到全屏
 * - 点击关闭按钮(×) → 关闭小窗
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun MiniPlayerOverlay(
    miniPlayerManager: MiniPlayerManager,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // 🔥🔥 [调试] 打印当前模式状态
    val currentMode = miniPlayerManager.getCurrentMode()
    com.android.purebilibili.core.util.Logger.d("MiniPlayerOverlay", 
        "🎬 Overlay: mode=$currentMode, isMiniMode=${miniPlayerManager.isMiniMode}, isActive=${miniPlayerManager.isActive}")
    
    // 🔥🔥 [重构] 使用新的模式判断方法，替代原来的 bgPlay 检查
    // 小窗模式为 OFF 时不渲染
    if (miniPlayerManager.isMiniPlayerDisabled()) {
        return
    }

    
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val miniPlayerWidth = 220.dp
    val miniPlayerHeight = 130.dp
    val padding = 12.dp
    val headerHeight = 28.dp // 顶部可拖动区域高度

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val miniPlayerWidthPx = with(density) { miniPlayerWidth.toPx() }
    val miniPlayerHeightPx = with(density) { miniPlayerHeight.toPx() }
    val paddingPx = with(density) { padding.toPx() }

    // 🔥 获取入场方向（在计算初始位置前获取）
    val entryFromLeft = miniPlayerManager.entryFromLeft
    
    // 🔥🔥 [修复] 位置状态 - 根据卡片位置决定初始位置
    // 左边视频 → 小窗在左侧，右边视频 → 小窗在右侧
    var offsetX by remember(entryFromLeft) { 
        mutableFloatStateOf(
            if (entryFromLeft) paddingPx else screenWidthPx - miniPlayerWidthPx - paddingPx
        ) 
    }
    var offsetY by remember { mutableFloatStateOf(screenHeightPx - miniPlayerHeightPx - paddingPx - 100.dp.value * density.density) }
    
    // 控制按钮显示状态
    var showControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    // 进度拖动状态
    var isDraggingProgress by remember { mutableStateOf(false) }
    var dragProgressDelta by remember { mutableFloatStateOf(0f) }
    var seekPreviewPosition by remember { mutableLongStateOf(0L) }
    
    // 位置拖动状态
    var isDraggingPosition by remember { mutableStateOf(false) }
    
    // 播放器状态
    val player = miniPlayerManager.player
    var isPlaying by remember { mutableStateOf(player?.isPlaying ?: false) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    
    // 持续监听播放器状态 (🔥 优化：降低轮询频率)
    LaunchedEffect(player) {
        while (true) {
            player?.let {
                isPlaying = it.isPlaying
                duration = it.duration.coerceAtLeast(1L)
                currentPosition = it.currentPosition
                if (!isDraggingProgress) {
                    currentProgress = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                }
            }
            delay(300) // 🔥 从 200ms 改为 300ms，减少 CPU 消耗
        }
    }
    
    // 自动隐藏控制按钮
    LaunchedEffect(showControls, lastInteractionTime) {
        if (showControls && !isDraggingPosition && !isDraggingProgress) {
            delay(AUTO_HIDE_DELAY_MS)
            if (System.currentTimeMillis() - lastInteractionTime >= AUTO_HIDE_DELAY_MS) {
                showControls = false
            }
        }
    }

    // 动画 - 只有在非拖动时才使用动画
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = if (isDraggingPosition) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "offsetX"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = if (isDraggingPosition) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "offsetY"
    )


    AnimatedVisibility(
        visible = miniPlayerManager.isMiniMode && miniPlayerManager.isActive,
        // 🔥🔥 根据入场方向决定动画方向
        enter = slideInHorizontally(
            initialOffsetX = { if (entryFromLeft) -it else it }  // 左边视频从左入，右边视频从右入
        ) + fadeIn(),
        exit = slideOutHorizontally(
            targetOffsetX = { if (entryFromLeft) -it else it }
        ) + fadeOut(),
        modifier = modifier.zIndex(100f)
    ) {
        Card(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt(), animatedOffsetY.roundToInt()) }
                .width(miniPlayerWidth)
                .height(miniPlayerHeight)
                .shadow(16.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 视频画面
                player?.let { exoPlayer ->
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                this.player = exoPlayer
                                useController = false
                                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                            }
                        },
                        update = { view -> view.player = exoPlayer },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            // 🔥 视频区域：左右滑动调节进度
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragStart = { 
                                        isDraggingProgress = true
                                        dragProgressDelta = 0f
                                        seekPreviewPosition = currentPosition
                                        showControls = true
                                        lastInteractionTime = System.currentTimeMillis()
                                    },
                                    onDragEnd = {
                                        if (isDraggingProgress && abs(dragProgressDelta) > 10f) {
                                            val seekDelta = (dragProgressDelta / miniPlayerWidthPx * duration).toLong()
                                            val newPosition = (currentPosition + seekDelta).coerceIn(0L, duration)
                                            player?.seekTo(newPosition)
                                        }
                                        isDraggingProgress = false
                                        dragProgressDelta = 0f
                                    },
                                    onDragCancel = {
                                        isDraggingProgress = false
                                        dragProgressDelta = 0f
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        dragProgressDelta += dragAmount
                                        val seekDelta = (dragProgressDelta / miniPlayerWidthPx * duration).toLong()
                                        seekPreviewPosition = (currentPosition + seekDelta).coerceIn(0L, duration)
                                        currentProgress = (seekPreviewPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        showControls = !showControls
                                        if (showControls) {
                                            lastInteractionTime = System.currentTimeMillis()
                                        }
                                    },
                                    onDoubleTap = { onExpandClick() }
                                )
                            }
                    )
                }

                // 🔥 顶部拖动区域 - 用于移动小窗位置
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            )
                        )
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    isDraggingPosition = true
                                    showControls = true
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                onDragEnd = {
                                    isDraggingPosition = false
                                    // 吸附到屏幕边缘
                                    offsetX = if (offsetX < screenWidthPx / 2 - miniPlayerWidthPx / 2) {
                                        paddingPx
                                    } else {
                                        screenWidthPx - miniPlayerWidthPx - paddingPx
                                    }
                                    offsetY = offsetY.coerceIn(
                                        paddingPx + 50.dp.value * density.density,
                                        screenHeightPx - miniPlayerHeightPx - paddingPx - 100.dp.value * density.density
                                    )
                                },
                                onDragCancel = {
                                    isDraggingPosition = false
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    offsetX += dragAmount.x
                                    offsetY += dragAmount.y
                                }
                            )
                        }
                ) {
                    // 标题
                    Text(
                        text = miniPlayerManager.currentTitle,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp, end = 60.dp)
                    )
                    
                    // 🔥 右上角按钮组
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // 展开按钮
                        Surface(
                            onClick = { onExpandClick() },
                            modifier = Modifier.size(24.dp),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f)
                        ) {
                            Icon(
                                imageVector = CupertinoIcons.Default.ArrowUpLeftAndArrowDownRight,
                                contentDescription = "展开",
                                tint = Color.White,
                                modifier = Modifier.padding(5.dp).size(14.dp)
                            )
                        }
                        
                        // 关闭按钮
                        Surface(
                            onClick = { miniPlayerManager.dismiss() },
                            modifier = Modifier.size(24.dp),
                            shape = CircleShape,
                            color = Color.Red.copy(alpha = 0.7f)
                        ) {
                            Icon(
                                imageVector = CupertinoIcons.Default.Xmark,
                                contentDescription = "关闭",
                                tint = Color.White,
                                modifier = Modifier.padding(4.dp).size(16.dp)
                            )
                        }
                    }
                }

                // 控制层 - 播放按钮等（位于中间和底部）
                if (showControls || isDraggingProgress) {
                    // 底部渐变
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                )
                            )
                    )

                    // 播放/暂停按钮
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
                            imageVector = if (isPlaying) CupertinoIcons.Default.Pause else CupertinoIcons.Default.Play,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier.padding(10.dp).size(28.dp)
                        )
                    }
                    
                    // 底部提示
                    if (isDraggingProgress) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.7f)
                        ) {
                            val timeText = "${formatMiniTime(seekPreviewPosition)} / ${formatMiniTime(duration)}"
                            Text(
                                text = timeText,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    } else if (!isDraggingPosition) {
                        Text(
                            text = "拖动顶部移动 | 左右滑动调进度",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                        )
                    }
                }

                // 进度条 - 始终显示
                LinearProgressIndicator(
                    progress = { currentProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                    color = if (isDraggingProgress) Color.Yellow else MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

private fun formatMiniTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
