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
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
//  已改用 MaterialTheme.colorScheme.primary
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TAG = "MiniPlayerOverlay"
private const val AUTO_HIDE_DELAY_MS = 3000L

/**
 *  小窗播放器覆盖层
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
    
    //  [调试] 仅在状态变化时记录，避免组合阶段高频日志
    val currentMode = miniPlayerManager.getCurrentMode()
    LaunchedEffect(currentMode, miniPlayerManager.isMiniMode, miniPlayerManager.isActive) {
        Logger.d(
            "MiniPlayerOverlay",
            "Overlay state changed: mode=$currentMode, isMiniMode=${miniPlayerManager.isMiniMode}, isActive=${miniPlayerManager.isActive}"
        )
    }
    
    //  [简化] 小窗可见性由 AnimatedVisibility 的 isMiniMode && isActive 控制
    // 不再需要额外的模式检查

    
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val layoutPolicy = remember(configuration.screenWidthDp) {
        resolveMiniPlayerOverlayLayoutPolicy(
            widthDp = configuration.screenWidthDp
        )
    }

    val miniPlayerWidth = layoutPolicy.miniPlayerWidthDp.dp
    val miniPlayerHeight = layoutPolicy.miniPlayerHeightDp.dp
    val padding = layoutPolicy.outerPaddingDp.dp
    val headerHeight = layoutPolicy.headerHeightDp.dp // 顶部可拖动区域高度

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val miniPlayerWidthPx = with(density) { miniPlayerWidth.toPx() }
    val miniPlayerHeightPx = with(density) { miniPlayerHeight.toPx() }
    val paddingPx = with(density) { padding.toPx() }

    //  [新增] 获取卡片位置信息，用于初始落位和动画适配
    val cardBounds = com.android.purebilibili.core.util.CardPositionManager.lastClickedCardBounds
    val cardPosition = com.android.purebilibili.core.util.CardPositionManager.cardHorizontalPosition
    val entryFromLeft = miniPlayerManager.entryFromLeft
    
    //  [修复] 位置状态 - 优先使用卡片原始位置，否则使用贴边逻辑
    // 左边视频 → 小窗在左侧，右边视频 → 小窗在右侧
    var offsetX by remember(entryFromLeft, cardBounds) { 
        mutableFloatStateOf(
            cardBounds?.left ?: if (entryFromLeft) paddingPx else screenWidthPx - miniPlayerWidthPx - paddingPx
        ) 
    }
    var offsetY by remember(cardBounds) { 
        mutableFloatStateOf(
            cardBounds?.top ?: (
                screenHeightPx - miniPlayerHeightPx - paddingPx - with(density) {
                    layoutPolicy.dragBottomInsetDp.dp.toPx()
                }
            )
        ) 
    }
    
    // 控制按钮显示状态
    var showControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // [新增] 贴边隐藏状态
    var isStashed by remember { mutableStateOf(false) }
    // 记录隐藏在哪一侧 (Left or Right), 默认为 Right，后续根据位置计算
    var stashSide by remember { mutableStateOf(com.android.purebilibili.core.util.CardPositionManager.CardHorizontalPosition.RIGHT) }
    
    // [新增] Stashed 状态下的 Y 轴偏移量（允许在隐藏时上下拖动）
    var stashedOffsetY by remember { mutableFloatStateOf(offsetY) }

    
    // 进度拖动状态
    var isDraggingProgress by remember { mutableStateOf(false) }
    var dragProgressDelta by remember { mutableFloatStateOf(0f) }
    var seekPreviewPosition by remember { mutableLongStateOf(0L) }
    
    // 位置拖动状态
    var isDraggingPosition by remember { mutableStateOf(false) }
    
    // 播放器状态
    //  [修复] 在退出动画期间保持旧 Player 引用，防止画面跳变
    // 当 isMiniMode 为 false (正在退出) 时，不再更新 player，保持最后一帧画面
    val rawPlayer = miniPlayerManager.player
    var player by remember { mutableStateOf(rawPlayer) }
    
    if (miniPlayerManager.isMiniMode && rawPlayer != null) {
        player = rawPlayer
    }
    
    var isPlaying by remember { mutableStateOf(player?.isPlaying ?: false) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    
    val shouldPollProgress = shouldPollMiniPlayerProgress(
        playerExists = player != null,
        isMiniMode = miniPlayerManager.isMiniMode,
        isActive = miniPlayerManager.isActive,
        isLiveMode = miniPlayerManager.isLiveMode  // 📺 直播不轮询进度
    )
    // 仅在小窗真实可用时轮询播放器状态，避免后台空转。
    LaunchedEffect(player, shouldPollProgress, isDraggingProgress) {
        if (!shouldPollProgress) return@LaunchedEffect
        while (true) {
            val currentPlayer = player ?: break
            val currentIsPlaying = currentPlayer.isPlaying
            isPlaying = currentIsPlaying
            duration = currentPlayer.duration.coerceAtLeast(1L)
            currentPosition = currentPlayer.currentPosition
            if (!isDraggingProgress) {
                currentProgress = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            }
            delay(resolveMiniPlayerPollingIntervalMs(isPlaying = currentIsPlaying))
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
    // 如果是 Stashed 状态，TargetX 应该在屏幕边缘外只露一点，或者贴在边缘
    val targetOffsetX = if (isStashed) {
        if (stashSide == com.android.purebilibili.core.util.CardPositionManager.CardHorizontalPosition.LEFT) {
            0f // 左侧贴边
        } else {
            // 右侧贴边
            with(density) { screenWidthPx - layoutPolicy.stashedWidthDp.dp.toPx() }
        }
    } else {
        offsetX
    }

    val targetOffsetY = if (isStashed) stashedOffsetY else offsetY

    val animatedOffsetX by animateFloatAsState(
        targetValue = targetOffsetX,
        animationSpec = if (isDraggingPosition) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "offsetX"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = targetOffsetY,
        animationSpec = if (isDraggingPosition) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "offsetY"
    )

    AnimatedVisibility(
        visible = miniPlayerManager.isMiniMode && miniPlayerManager.isActive,
        //  [修改] 根据卡片在屏幕中的水平分块决定动画方向
        //  Left: 从左往右飞出 (SlideIn Left)
        //  Right: 从右往左飞出 (SlideIn Right)
        //  Middle: 从上往下飞出 (SlideIn Top)
        enter = (when (cardPosition) {
            com.android.purebilibili.core.util.CardPositionManager.CardHorizontalPosition.LEFT -> 
                slideInHorizontally(initialOffsetX = { -it }) // 从左侧滑入
            com.android.purebilibili.core.util.CardPositionManager.CardHorizontalPosition.RIGHT -> 
                slideInHorizontally(initialOffsetX = { it })  // 从右侧滑入
            com.android.purebilibili.core.util.CardPositionManager.CardHorizontalPosition.MIDDLE -> 
                slideInVertically(initialOffsetY = { -it })   // 从顶部滑入
        }) + fadeIn(),
        
        exit = if (miniPlayerManager.shouldAnimateExit) {
            (when (cardPosition) {
                com.android.purebilibili.core.util.CardPositionManager.CardHorizontalPosition.LEFT -> 
                    slideOutHorizontally(targetOffsetX = { -it })
                com.android.purebilibili.core.util.CardPositionManager.CardHorizontalPosition.RIGHT -> 
                    slideOutHorizontally(targetOffsetX = { it })
                com.android.purebilibili.core.util.CardPositionManager.CardHorizontalPosition.MIDDLE -> 
                    slideOutVertically(targetOffsetY = { -it })
            }) + fadeOut()
        } else {
            ExitTransition.None
        },
            modifier = modifier.zIndex(100f)
    ) {
        if (isStashed) {
            // [新增] 贴边隐藏的小胶囊视图
            StashedMiniPlayerView(
                modifier = Modifier
                    .offset { IntOffset(animatedOffsetX.roundToInt(), animatedOffsetY.roundToInt()) }
                    .zIndex(101f),
                layoutPolicy = layoutPolicy,
                side = stashSide,
                onUnstash = {
                    isStashed = false
                    // 恢复时，X 坐标应该弹回正常位置
                    offsetX = if (stashSide == com.android.purebilibili.core.util.CardPositionManager.CardHorizontalPosition.LEFT) {
                        paddingPx
                    } else {
                        screenWidthPx - miniPlayerWidthPx - paddingPx
                    }
                    // Y 坐标保持同步
                    offsetY = stashedOffsetY
                },
                onDrag = { deltaY ->
                    with(density) {
                        stashedOffsetY = (stashedOffsetY + deltaY).coerceIn(
                            paddingPx + layoutPolicy.dragTopInsetDp.dp.toPx(),
                            screenHeightPx - paddingPx - layoutPolicy.dragBottomInsetDp.dp.toPx()
                        )
                    }
                }
            )
        } else {
            // 正常播放器视图
            Card(
                modifier = Modifier
                    .offset { IntOffset(animatedOffsetX.roundToInt(), animatedOffsetY.roundToInt()) }
                    .width(miniPlayerWidth)
                    .height(miniPlayerHeight)
                    .shadow(
                        layoutPolicy.cardShadowDp.dp,
                        RoundedCornerShape(layoutPolicy.cardCornerRadiusDp.dp)
                    ),
                shape = RoundedCornerShape(layoutPolicy.cardCornerRadiusDp.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                elevation = CardDefaults.cardElevation(defaultElevation = layoutPolicy.cardElevationDp.dp)
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
                            .clip(RoundedCornerShape(layoutPolicy.cardCornerRadiusDp.dp))
                            //  视频区域：左右滑动调节进度（直播模式禁用）
                            .then(
                                if (!miniPlayerManager.isLiveMode) {
                                    Modifier.pointerInput(Unit) {
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
                                } else Modifier
                            )
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

                //  顶部拖动区域 - 用于移动小窗位置
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
                                        paddingPx + with(density) { layoutPolicy.dragTopInsetDp.dp.toPx() },
                                        screenHeightPx - miniPlayerHeightPx - paddingPx - with(density) {
                                            layoutPolicy.dragBottomInsetDp.dp.toPx()
                                        }
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
                        fontSize = layoutPolicy.titleFontSp.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(
                                start = layoutPolicy.titleStartPaddingDp.dp,
                                end = layoutPolicy.titleEndPaddingDp.dp
                            )
                    )
                    
                    //  右上角按钮组
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = layoutPolicy.headerButtonRowEndPaddingDp.dp),
                        horizontalArrangement = Arrangement.spacedBy(layoutPolicy.headerButtonSpacingDp.dp)
                    ) {
                        // [新增] 贴边隐藏按钮
                        Surface(
                            onClick = {
                                // 计算最近的边
                                val centerX = offsetX + miniPlayerWidthPx / 2
                                stashSide = if (centerX < screenWidthPx / 2) {
                                    com.android.purebilibili.core.util.CardPositionManager.CardHorizontalPosition.LEFT
                                } else {
                                    com.android.purebilibili.core.util.CardPositionManager.CardHorizontalPosition.RIGHT
                                }
                                stashedOffsetY = offsetY
                                isStashed = true
                            },
                            modifier = Modifier.size(layoutPolicy.headerButtonSizeDp.dp),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f)
                        ) {
                            Icon(
                                imageVector = CupertinoIcons.Default.Minus, // 使用 Minus 图标作为隐藏/最小化
                                contentDescription = "隐藏",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(layoutPolicy.headerButtonIconPaddingDp.dp)
                                    .size(layoutPolicy.headerButtonIconSizeDp.dp)
                            )
                        }

                        // 展开按钮
                        Surface(
                            onClick = { onExpandClick() },
                            modifier = Modifier.size(layoutPolicy.headerButtonSizeDp.dp),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f)
                        ) {
                            Icon(
                                imageVector = CupertinoIcons.Default.ArrowUpLeftAndArrowDownRight,
                                contentDescription = "展开",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(layoutPolicy.headerButtonIconPaddingDp.dp)
                                    .size(layoutPolicy.headerButtonIconSizeDp.dp)
                            )
                        }
                        
                        // 关闭按钮
                        Surface(
                            onClick = { miniPlayerManager.dismiss() },
                            modifier = Modifier.size(layoutPolicy.headerButtonSizeDp.dp),
                            shape = CircleShape,
                            color = com.android.purebilibili.core.theme.iOSRed.copy(alpha = 0.7f)
                        ) {
                            Icon(
                                imageVector = CupertinoIcons.Default.Xmark,
                                contentDescription = "关闭",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(layoutPolicy.headerButtonIconPaddingDp.dp)
                                    .size(layoutPolicy.headerButtonIconSizeDp.dp)
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
                            .height(layoutPolicy.controlsGradientHeightDp.dp)
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
                            modifier = Modifier
                                .padding(layoutPolicy.centerPlayIconPaddingDp.dp)
                                .size(layoutPolicy.centerPlayIconSizeDp.dp)
                        )
                    }
                    
                    // 底部提示
                    if (isDraggingProgress) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = layoutPolicy.seekHintBottomPaddingDp.dp),
                            shape = RoundedCornerShape(layoutPolicy.seekHintCornerRadiusDp.dp),
                            color = Color.Black.copy(alpha = 0.7f)
                        ) {
                            val timeText = "${formatMiniTime(seekPreviewPosition)} / ${formatMiniTime(duration)}"
                            Text(
                                text = timeText,
                                color = Color.White,
                                fontSize = layoutPolicy.seekHintFontSp.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(
                                    horizontal = layoutPolicy.seekHintHorizontalPaddingDp.dp,
                                    vertical = layoutPolicy.seekHintVerticalPaddingDp.dp
                                )
                            )
                        }
                    } else if (!isDraggingPosition) {
                        Text(
                            text = if (miniPlayerManager.isLiveMode) "拖动顶部移动 | 双击展开" else "拖动顶部移动 | 左右滑动调进度",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = layoutPolicy.dragHintFontSp.sp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = layoutPolicy.dragHintBottomPaddingDp.dp)
                        )
                    }
                    
                    // 📺 [新增] 直播角标
                    if (miniPlayerManager.isLiveMode) {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 8.dp, top = 8.dp)
                                .background(
                                    color = Color(0xFFFF4444).copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "直播",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 进度条 - 仅视频模式显示（直播没有进度）
                if (!miniPlayerManager.isLiveMode) {
                LinearProgressIndicator(
                    progress = { currentProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(layoutPolicy.progressBarHeightDp.dp)
                        .align(Alignment.BottomCenter)
                        .clip(
                            RoundedCornerShape(
                                bottomStart = layoutPolicy.cardCornerRadiusDp.dp,
                                bottomEnd = layoutPolicy.cardCornerRadiusDp.dp
                            )
                        ),
                    color = if (isDraggingProgress) Color.Yellow else MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                }
            }
        }
    }
}
}

/**
 * [新增] 贴边隐藏的小胶囊视图
 */
@Composable
private fun StashedMiniPlayerView(
    modifier: Modifier,
    layoutPolicy: MiniPlayerOverlayLayoutPolicy,
    side: com.android.purebilibili.core.util.CardPositionManager.CardHorizontalPosition,
    onUnstash: () -> Unit,
    onDrag: (Float) -> Unit
) {
    val isLeft = side == com.android.purebilibili.core.util.CardPositionManager.CardHorizontalPosition.LEFT
    // 形状：贴边的一侧是平的，另一侧是圆的
    val shape = if (isLeft) {
        RoundedCornerShape(
            topEnd = layoutPolicy.cardCornerRadiusDp.dp + layoutPolicy.stashedSideCornerExtraDp.dp,
            bottomEnd = layoutPolicy.cardCornerRadiusDp.dp + layoutPolicy.stashedSideCornerExtraDp.dp
        )
    } else {
        RoundedCornerShape(
            topStart = layoutPolicy.cardCornerRadiusDp.dp + layoutPolicy.stashedSideCornerExtraDp.dp,
            bottomStart = layoutPolicy.cardCornerRadiusDp.dp + layoutPolicy.stashedSideCornerExtraDp.dp
        )
    }

    Surface(
        modifier = modifier
            .width(layoutPolicy.stashedWidthDp.dp)
            .height(layoutPolicy.stashedHeightDp.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.y)
                    }
                )
        },
        shape = shape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
        shadowElevation = layoutPolicy.stashedShadowDp.dp,
        onClick = onUnstash
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isLeft) Icons.Filled.ChevronRight else Icons.Filled.ChevronLeft,
                contentDescription = "Show",
                tint = Color.White,
                modifier = Modifier.size(layoutPolicy.stashedIconSizeDp.dp)
            )
        }
    }
}

private fun formatMiniTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
