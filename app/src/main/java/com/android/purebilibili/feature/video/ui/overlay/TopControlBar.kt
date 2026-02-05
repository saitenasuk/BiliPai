// File: feature/video/ui/overlay/TopControlBar.kt
package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Top Control Bar Component
 * 
 * Displays the top control bar with:
 * - Back button
 * - Video title
 * - Danmaku toggle
 * - Quality selector
 * 
 * Requirement Reference: AC2.2 - Reusable TopControlBar
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopControlBar(
    title: String,
    isFullscreen: Boolean,
    currentQualityLabel: String,
    onBack: () -> Unit,
    onQualityClick: () -> Unit,
    // Danmaku controls
    danmakuEnabled: Boolean = true,
    onDanmakuToggle: () -> Unit = {},
    onDanmakuSettingsClick: () -> Unit = {},
    // 👀 [新增] 在线观看人数
    onlineCount: String = "",
    // [新增] 侧边栏回调
    onDrawerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            //  只在全屏模式下添加状态栏padding，非全屏时视频区域已在状态栏下方
            .then(if (isFullscreen) Modifier.statusBarsPadding() else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(CupertinoIcons.Default.ChevronBackward, contentDescription = "Back", tint = Color.White)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            // 👀 在线人数
            if (onlineCount.isNotEmpty()) {
                Text(
                    text = onlineCount,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
        // Danmaku toggle button with settings indicator
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
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (danmakuEnabled) CupertinoIcons.Default.TextBubble else CupertinoIcons.Outlined.TextBubble,
                    contentDescription = if (danmakuEnabled) "Disable danmaku" else "Enable danmaku",
                    tint = if (danmakuEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        //  独立的弹幕设置按钮，更直观
        Spacer(modifier = Modifier.width(4.dp))
        Surface(
            onClick = onDanmakuSettingsClick,
            color = Color.White.copy(alpha = 0.2f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CupertinoIcons.Default.Gearshape,
                    contentDescription = "弹幕设置",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        // 侧边栏按钮 (Sidebar Toggle)
        Surface(
            onClick = onDrawerClick,
            color = Color.White.copy(alpha = 0.2f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CupertinoIcons.Default.SidebarRight,
                    contentDescription = "侧边栏",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
