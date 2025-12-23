// File: feature/video/ui/overlay/BottomControlBar.kt
package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.SubtitlesOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.feature.video.ui.components.VideoAspectRatio
import com.android.purebilibili.core.theme.BiliPink

/**
 * Bottom Control Bar Component
 * 
 * Displays the bottom control bar with:
 * - Play/pause button
 * - Progress bar
 * - Time display
 * - Speed selector
 * - Fullscreen toggle
 * 
 * Requirement Reference: AC2.3 - Reusable BottomControlBar
 */

/**
 * Player progress data class
 */
data class PlayerProgress(
    val current: Long = 0L,
    val duration: Long = 0L,
    val buffered: Long = 0L
)

@Composable
fun BottomControlBar(
    isPlaying: Boolean,
    progress: PlayerProgress,
    isFullscreen: Boolean,
    currentSpeed: Float = 1.0f,
    currentRatio: VideoAspectRatio = VideoAspectRatio.FIT,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedClick: () -> Unit = {},
    onRatioClick: () -> Unit = {},
    onToggleFullscreen: () -> Unit,
    // 🔥🔥 [新增] 竖屏模式弹幕开关
    danmakuEnabled: Boolean = true,
    onDanmakuToggle: () -> Unit = {},
    // 🔥🔥 [新增] 竖屏模式清晰度选择
    currentQualityLabel: String = "",
    onQualityClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 4.dp)
            // 🔥🔥 只在全屏横屏时才需要避开导航栏
            // 竖屏时导航栏在页面底部，不在播放器区域内
            .let { if (isFullscreen) it.navigationBarsPadding() else it }
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
            verticalAlignment = Alignment.CenterVertically,
            // 🔥 使用 SpaceBetween 确保两端元素始终可见
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：播放按钮和时间
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "${FormatUtils.formatDuration((progress.current / 1000).toInt())} / ${FormatUtils.formatDuration((progress.duration / 1000).toInt())}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
            
            // 中间：功能按钮（自适应空间）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.weight(1f)
            ) {
                // Speed button
                Surface(
                    onClick = onSpeedClick,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (currentSpeed == 1.0f) "倍速" else "${currentSpeed}x",
                        color = if (currentSpeed != 1.0f) BiliPink else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(6.dp))
                
                // 🔥 Aspect Ratio button
                Surface(
                    onClick = onRatioClick,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = currentRatio.displayName,
                        color = if (currentRatio != VideoAspectRatio.FIT) BiliPink else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                // 🔥🔥 [新增] 竖屏模式弹幕开关和清晰度
                if (!isFullscreen) {
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    IconButton(
                        onClick = onDanmakuToggle,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            if (danmakuEnabled) Icons.Rounded.Subtitles else Icons.Rounded.SubtitlesOff,
                            contentDescription = if (danmakuEnabled) "关闭弹幕" else "开启弹幕",
                            tint = if (danmakuEnabled) BiliPink else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // 🔥🔥 清晰度选择 - 限制最大宽度防止截断
                    if (currentQualityLabel.isNotEmpty()) {
                        Surface(
                            onClick = onQualityClick,
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = currentQualityLabel,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            
            // 🔥 右侧：全屏按钮 - 始终显示，不会被挤出
            IconButton(
                onClick = onToggleFullscreen,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * Video Progress Bar
 */
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
