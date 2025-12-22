// 文件路径: feature/video/ui/overlay/LandscapeTopControlBar.kt
package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.util.FormatUtils

/**
 * 🔥🔥🔥 横屏顶部控制栏（官方 B 站样式）
 * 
 * 布局结构：
 * - 左侧：返回按钮 + 标题 + 观看人数
 * - 右侧：点赞(带数字) + 投币 + 分享 + 更多
 */
@Composable
fun LandscapeTopControlBar(
    title: String,
    onlineCount: String = "",
    // 操作按钮状态
    likeCount: Long = 0,
    isLiked: Boolean = false,
    hasCoin: Boolean = false,
    // 回调
    onBack: () -> Unit,
    onLikeClick: () -> Unit = {},
    onCoinClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.7f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 🔥 左侧：返回 + 标题
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color.White
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            // 标题
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // 观看人数
            if (onlineCount.isNotEmpty()) {
                Text(
                    text = onlineCount,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // 🔥 右侧：操作按钮（官方样式：图标+数字，横向排列）
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 点赞按钮（带数字）
            TopBarActionButton(
                icon = Icons.Rounded.ThumbUp,
                label = FormatUtils.formatStat(likeCount),
                isActive = isLiked,
                activeColor = BiliPink,
                onClick = onLikeClick
            )
            
            // 投币按钮
            TopBarActionButton(
                icon = CoinIcon,
                label = if (hasCoin) "已投" else "",
                isActive = hasCoin,
                activeColor = Color(0xFFFFCA28),
                onClick = onCoinClick
            )
            
            // 分享按钮
            IconButton(onClick = onShareClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "分享",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            // 更多按钮
            IconButton(onClick = onMoreClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "更多",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * 🔥 顶部栏操作按钮
 */
@Composable
private fun TopBarActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    activeColor: Color = BiliPink,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) activeColor else Color.White,
                modifier = Modifier.size(22.dp)
            )
            if (label.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    color = if (isActive) activeColor else Color.White,
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// 投币图标占位
private val CoinIcon: ImageVector
    get() = Icons.Default.Star
