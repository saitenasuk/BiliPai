// 文件路径: feature/video/VideoPlayerOverlay.kt
package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.SubtitlesOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.util.FormatUtils
// Import reusable components from standalone files
import com.android.purebilibili.feature.video.ui.components.QualitySelectionMenu
import com.android.purebilibili.feature.video.ui.components.SpeedSelectionMenu
import com.android.purebilibili.feature.video.ui.components.DanmakuSettingsPanel
import com.android.purebilibili.feature.video.ui.components.VideoAspectRatio
import com.android.purebilibili.feature.video.ui.components.AspectRatioMenu
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import kotlinx.coroutines.delay


@Composable
fun VideoPlayerOverlay(
    player: Player,
    title: String,
    isVisible: Boolean,
    onToggleVisible: () -> Unit,
    isFullscreen: Boolean,
    currentQualityLabel: String,
    qualityLabels: List<String>,
    qualityIds: List<Int> = emptyList(),
    isLoggedIn: Boolean = false,
    onQualitySelected: (Int) -> Unit,
    onBack: () -> Unit,
    onToggleFullscreen: () -> Unit,
    showStats: Boolean = false,
    realResolution: String = "",
    isQualitySwitching: Boolean = false,
    isVip: Boolean = false,
    // 🔥🔥 [新增] 弹幕开关和设置
    danmakuEnabled: Boolean = true,
    onDanmakuToggle: () -> Unit = {},
    danmakuOpacity: Float = 0.85f,
    danmakuFontScale: Float = 1.2f,
    danmakuSpeed: Float = 1.5f,
    danmakuDisplayArea: Float = 0.5f,
    onDanmakuOpacityChange: (Float) -> Unit = {},
    onDanmakuFontScaleChange: (Float) -> Unit = {},
    onDanmakuSpeedChange: (Float) -> Unit = {},
    onDanmakuDisplayAreaChange: (Float) -> Unit = {},
    // 🧪🧪 [实验性功能] 双击点赞
    doubleTapLikeEnabled: Boolean = true,
    onDoubleTapLike: () -> Unit = {},
    // 🔥 视频比例调节
    currentAspectRatio: VideoAspectRatio = VideoAspectRatio.FIT,
    onAspectRatioChange: (VideoAspectRatio) -> Unit = {}
) {
    var showQualityMenu by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showRatioMenu by remember { mutableStateOf(false) }
    var showDanmakuSettings by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    // 🔥 使用传入的比例状态
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    
    // 🧪 双击检测状态
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var showLikeAnimation by remember { mutableStateOf(false) }

    val progressState by produceState(initialValue = PlayerProgress(), key1 = player) {
        while (true) {
            // 🔥🔥 [修复] 始终更新进度，不仅在播放时
            // 这样横竖屏切换后也能显示正确的进度
            val duration = if (player.duration < 0) 0L else player.duration
            value = PlayerProgress(
                current = player.currentPosition,
                duration = duration,
                buffered = player.bufferedPosition
            )
            isPlaying = player.isPlaying
            delay(200)
        }
    }

    LaunchedEffect(isVisible, isPlaying) {
        if (isVisible && isPlaying) {
            delay(4000)
            if (isVisible) {
                onToggleVisible()
            }
        }
    }
    
    // 🧪 双击点赞动画自动消失
    LaunchedEffect(showLikeAnimation) {
        if (showLikeAnimation) {
            delay(800)
            showLikeAnimation = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- 1. 顶部渐变遮罩 ---
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            // 🔥🔥 [修复] align 必须在 AnimatedVisibility 的 modifier 上，而不是内部 Box 上
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Black.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // --- 2. 底部渐变遮罩 ---
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
            )
        }

        // --- 3. 控制栏内容 ---
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            // 🔥🔥 [修复] 确保 AnimatedVisibility 填充整个父容器
            modifier = Modifier.fillMaxSize()
        ) {
            // 🔥🔥 [修复] 使用 Box 分别定位顶部和底部控制栏
            Box(modifier = Modifier.fillMaxSize()) {
                // 🔥 顶部控制栏 - 仅在横屏（全屏）模式显示标题和清晰度
                if (isFullscreen) {
                    TopControlBar(
                        title = title,
                        isFullscreen = isFullscreen,
                        currentQualityLabel = currentQualityLabel,
                        onBack = onBack,
                        onQualityClick = { showQualityMenu = true },
                        // 🔥🔥 弹幕开关和设置
                        danmakuEnabled = danmakuEnabled,
                        onDanmakuToggle = onDanmakuToggle,
                        onDanmakuSettingsClick = { showDanmakuSettings = true },
                        // 🔥🔥 [修复] 传入 modifier 确保在顶部
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
                
                // 🔥🔥 [修复] 底部控制栏 - 固定在底部
                BottomControlBar(
                    isPlaying = isPlaying,
                    progress = progressState,
                    isFullscreen = isFullscreen,
                    currentSpeed = currentSpeed,
                    currentRatio = currentAspectRatio,
                    onPlayPauseClick = {
                        if (isPlaying) player.pause() else player.play()
                        isPlaying = !isPlaying
                    },
                    onSeek = { position -> player.seekTo(position) },
                    onSpeedClick = { showSpeedMenu = true },
                    onRatioClick = { showRatioMenu = true },
                    onToggleFullscreen = onToggleFullscreen,
                    // 🔥🔥 [修复] 传入 modifier 确保在底部
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }

        // --- 4. 🔥🔥 [新增] 真实分辨率统计信息 (仅在设置开启时显示) ---
        if (showStats && realResolution.isNotEmpty() && isVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 24.dp)
                    .background(Color.Black.copy(0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Resolution: $realResolution",
                    color = Color.Green,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 12.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        // --- 5. 中央播放/暂停大图标 ---
        AnimatedVisibility(
            visible = isVisible && !isPlaying && !isQualitySwitching,
            modifier = Modifier.align(Alignment.Center),
            enter = scaleIn(tween(250)) + fadeIn(tween(200)),
            exit = scaleOut(tween(200)) + fadeOut(tween(200))
        ) {
            Surface(
                onClick = { player.play(); isPlaying = true },
                color = Color.Black.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "播放",
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        }

        // --- 5.5 🔥🔥 清晰度切换中 Loading 指示器 ---
        AnimatedVisibility(
            visible = isQualitySwitching,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    // 🍎 iOS 风格加载器
                    CupertinoActivityIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "正在切换清晰度...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // --- 6. 清晰度菜单 ---
        if (showQualityMenu) {
            QualitySelectionMenu(
                qualities = qualityLabels,
                qualityIds = qualityIds,
                currentQuality = currentQualityLabel,
                isLoggedIn = isLoggedIn,
                isVip = isVip,
                onQualitySelected = { index ->
                    onQualitySelected(index)
                    showQualityMenu = false
                },
                onDismiss = { showQualityMenu = false }
            )
        }
        
        // --- 7. 🔥🔥 [新增] 倍速选择菜单 ---
        if (showSpeedMenu) {
            SpeedSelectionMenu(
                currentSpeed = currentSpeed,
                onSpeedSelected = { speed ->
                    currentSpeed = speed
                    player.setPlaybackSpeed(speed)
                    showSpeedMenu = false
                },
                onDismiss = { showSpeedMenu = false }
            )
        }
        
        // --- 7.5 🔥 [新增] 视频比例选择菜单 ---
        if (showRatioMenu) {
            AspectRatioMenu(
                currentRatio = currentAspectRatio,
                onRatioSelected = { ratio ->
                    onAspectRatioChange(ratio)
                    showRatioMenu = false
                },
                onDismiss = { showRatioMenu = false }
            )
        }
        
        // --- 8. 🔥🔥 [新增] 弹幕设置面板 ---
        if (showDanmakuSettings) {
            DanmakuSettingsPanel(
                opacity = danmakuOpacity,
                fontScale = danmakuFontScale,
                speed = danmakuSpeed,
                displayArea = danmakuDisplayArea,
                onOpacityChange = onDanmakuOpacityChange,
                onFontScaleChange = onDanmakuFontScaleChange,
                onSpeedChange = onDanmakuSpeedChange,
                onDisplayAreaChange = onDanmakuDisplayAreaChange,
                onDismiss = { showDanmakuSettings = false }
            )
        }
    }
}