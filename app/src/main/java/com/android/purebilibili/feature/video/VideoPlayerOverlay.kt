// 文件路径: feature/video/VideoPlayerOverlay.kt
package com.android.purebilibili.feature.video

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
import kotlinx.coroutines.delay

@Stable
data class PlayerProgress(
    val current: Long = 0L,
    val duration: Long = 0L,
    val buffered: Long = 0L
)

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
    danmakuSpeed: Float = 1.2f,
    onDanmakuOpacityChange: (Float) -> Unit = {},
    onDanmakuFontScaleChange: (Float) -> Unit = {},
    onDanmakuSpeedChange: (Float) -> Unit = {}
) {
    var showQualityMenu by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showDanmakuSettings by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var isPlaying by remember { mutableStateOf(player.isPlaying) }

    val progressState by produceState(initialValue = PlayerProgress(), key1 = player) {
        while (true) {
            if (player.isPlaying) {
                value = PlayerProgress(
                    current = player.currentPosition,
                    duration = if (player.duration < 0) 0L else player.duration,
                    buffered = player.bufferedPosition
                )
                isPlaying = true
            } else {
                isPlaying = false
            }
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

    Box(modifier = Modifier.fillMaxSize()) {
        // --- 1. 顶部渐变遮罩 ---
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.TopCenter)
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
            exit = fadeOut(tween(300))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopControlBar(
                    title = title,
                    isFullscreen = isFullscreen,
                    currentQualityLabel = currentQualityLabel,
                    onBack = onBack,
                    onQualityClick = { showQualityMenu = true },
                    // 🔥🔥 弹幕开关和设置
                    danmakuEnabled = danmakuEnabled,
                    onDanmakuToggle = onDanmakuToggle,
                    onDanmakuSettingsClick = { showDanmakuSettings = true }
                )

                Spacer(modifier = Modifier.weight(1f))

                BottomControlBar(
                    isPlaying = isPlaying,
                    progress = progressState,
                    isFullscreen = isFullscreen,
                    currentSpeed = currentSpeed,
                    onPlayPauseClick = {
                        if (isPlaying) player.pause() else player.play()
                        isPlaying = !isPlaying
                    },
                    onSeek = { position -> player.seekTo(position) },
                    onSpeedClick = { showSpeedMenu = true },
                    onToggleFullscreen = onToggleFullscreen
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
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
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
        
        // --- 8. 🔥🔥 [新增] 弹幕设置面板 ---
        if (showDanmakuSettings) {
            DanmakuSettingsPanel(
                opacity = danmakuOpacity,
                fontScale = danmakuFontScale,
                speed = danmakuSpeed,
                onOpacityChange = onDanmakuOpacityChange,
                onFontScaleChange = onDanmakuFontScaleChange,
                onSpeedChange = onDanmakuSpeedChange,
                onDismiss = { showDanmakuSettings = false }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopControlBar(
    title: String,
    isFullscreen: Boolean,
    currentQualityLabel: String,
    onBack: () -> Unit,
    onQualityClick: () -> Unit,
    // 🔥🔥 [新增] 弹幕开关和设置
    danmakuEnabled: Boolean = true,
    onDanmakuToggle: () -> Unit = {},
    onDanmakuSettingsClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        // 🔥🔥 弹幕开关按钮 - 增强颜色对比度
        Spacer(modifier = Modifier.width(8.dp))
        // 🔥🔥 弹幕开关 (图标版 - 状态更清晰)
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .combinedClickable(
                    onClick = onDanmakuToggle,
                    onLongClick = onDanmakuSettingsClick
                ),
            color = Color.White.copy(alpha = 0.2f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                // 使用图标明确表示开关状态
                Icon(
                    imageVector = if (danmakuEnabled) Icons.Rounded.Subtitles else Icons.Rounded.SubtitlesOff,
                    contentDescription = if (danmakuEnabled) "关闭弹幕" else "开启弹幕",
                    tint = if (danmakuEnabled) Color(0xFFFB7299) else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            onClick = onQualityClick,
            color = Color.White.copy(alpha = 0.2f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = currentQualityLabel,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
fun BottomControlBar(
    isPlaying: Boolean,
    progress: PlayerProgress,
    isFullscreen: Boolean,
    currentSpeed: Float = 1.0f,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedClick: () -> Unit = {},
    onToggleFullscreen: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 4.dp)
            .navigationBarsPadding()
    ) {
        VideoProgressBar(
            currentPosition = progress.current,
            duration = progress.duration,
            bufferedPosition = progress.buffered,
            onSeek = onSeek
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${FormatUtils.formatDuration((progress.current / 1000).toInt())} / ${FormatUtils.formatDuration((progress.duration / 1000).toInt())}",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.weight(1f))
            
            // 🔥🔥 [新增] 倍速按钮
            Surface(
                onClick = onSpeedClick,
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = if (currentSpeed == 1.0f) "倍速" else "${currentSpeed}x",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onToggleFullscreen,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
fun VideoProgressBar(
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    onSeek: (Long) -> Unit
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
    var tempProgress by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(progress) {
        if (!isDragging) {
            tempProgress = progress
        }
    }

    Slider(
        value = if (isDragging) tempProgress else progress,
        onValueChange = {
            isDragging = true
            tempProgress = it
        },
        onValueChangeFinished = {
            isDragging = false
            onSeek((tempProgress * duration).toLong())
        },
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
    )
}

@Composable
fun QualitySelectionMenu(
    qualities: List<String>,
    qualityIds: List<Int> = emptyList(),
    currentQuality: String,
    isLoggedIn: Boolean = false,
    isVip: Boolean = false,
    onQualitySelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    fun getQualityTag(qualityId: Int): String? {
        return when (qualityId) {
            127, 126, 125, 120 -> if (!isVip) "大会员" else null
            116, 112 -> if (!isVip) "大会员" else null
            80 -> if (!isLoggedIn) "登录" else null
            else -> null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
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
                    val isSelected = quality == currentQuality
                    val qualityId = qualityIds.getOrNull(index) ?: 0
                    val tag = getQualityTag(qualityId)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onQualitySelected(index) }
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
                                color = if (tag == "大会员") Color(0xFFFB7299) else Color(0xFF666666),
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
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// 🔥🔥 [新增] 倍速选择菜单
@Composable
fun SpeedSelectionMenu(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 180.dp, max = 240.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = false) {},
            color = Color(0xFF2B2B2B),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "播放速度",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                HorizontalDivider(color = Color.White.copy(0.1f))
                speedOptions.forEach { speed ->
                    val isSelected = speed == currentSpeed
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSpeedSelected(speed) }
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (speed == 1.0f) "正常" else "${speed}x",
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(0.9f),
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (isSelected) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 🔥🔥 弹幕设置面板
 */
@Composable
fun DanmakuSettingsPanel(
    opacity: Float,
    fontScale: Float,
    speed: Float,
    onOpacityChange: (Float) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 360.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = false) {},
            color = Color(0xFF2B2B2B),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // 标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "弹幕设置",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color.White.copy(0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 透明度滑块
                DanmakuSliderItem(
                    label = "透明度",
                    value = opacity,
                    valueRange = 0.3f..1f,
                    displayText = "${(opacity * 100).toInt()}%",
                    onValueChange = onOpacityChange
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 字体大小滑块
                DanmakuSliderItem(
                    label = "字体大小",
                    value = fontScale,
                    valueRange = 0.5f..2f,
                    displayText = "${(fontScale * 100).toInt()}%",
                    onValueChange = onFontScaleChange
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 速度滑块
                DanmakuSliderItem(
                    label = "弹幕速度",
                    value = speed,
                    valueRange = 0.5f..2f,
                    displayText = when {
                        speed <= 0.7f -> "慢"
                        speed >= 1.5f -> "快"
                        else -> "中"
                    },
                    onValueChange = onSpeedChange
                )
            }
        }
    }
}

@Composable
private fun DanmakuSliderItem(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayText: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White.copy(0.9f),
                fontSize = 14.sp
            )
            Text(
                text = displayText,
                color = Color(0xFFFB7299),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFB7299),
                activeTrackColor = Color(0xFFFB7299),
                inactiveTrackColor = Color.White.copy(0.2f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}