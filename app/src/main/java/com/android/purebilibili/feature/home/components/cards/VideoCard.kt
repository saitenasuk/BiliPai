// 文件路径: feature/home/components/cards/VideoCard.kt
package com.android.purebilibili.feature.home.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.core.theme.iOSSystemGray
import com.android.purebilibili.core.util.iOSTapEffect

/**
 * 🔥 官方 B 站风格视频卡片
 * 采用与 Bilibili 官方 App 一致的设计：
 * - 封面 16:10 比例
 * - 左下角：播放量 + 弹幕数
 * - 右下角：时长
 * - 标题：2行
 * - 底部：「已关注」标签 + UP主名称
 */
@Composable
fun ElegantVideoCard(
    video: VideoItem,
    index: Int,
    refreshKey: Long = 0L,
    isFollowing: Boolean = false,  // 🔥 是否已关注该 UP 主
    onClick: (String, Long) -> Unit
) {
    val haptic = rememberHapticFeedback()
    
    val coverUrl = remember(video.bvid) {
        FormatUtils.fixImageUrl(if (video.pic.startsWith("//")) "https:${video.pic}" else video.pic)
    }
    
    // 🔥 判断是否为竖屏视频（通过封面图 URL 中的尺寸信息或默认不显示）
    // B站封面 URL 通常包含尺寸信息，如 width=X&height=Y
    // 简单方案：暂不显示竖屏标签（因推荐API不提供视频尺寸信息）

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .iOSTapEffect(
                scale = 0.97f,
                hapticEnabled = true
            ) {
                onClick(video.bvid, 0)
            }
            .padding(bottom = 12.dp)
    ) {
        // 🔥 封面容器 - 官方 B 站风格
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .shadow(
                    elevation = 1.dp,
                    shape = RoundedCornerShape(8.dp),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.10f)
                )
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // 封面图
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .size(480, 300)
                    .crossfade(120)
                    .memoryCacheKey("cover_${video.bvid}")
                    .diskCacheKey("cover_${video.bvid}")
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // 🔥 底部渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
            
            // 🔥 时长标签 - 右下角 (官方风格)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Text(
                    text = FormatUtils.formatDuration(video.duration),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            
            // 🔥 播放量和弹幕数 - 左下角 (官方风格)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 播放量
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "▶",
                        color = Color.White.copy(0.9f),
                        fontSize = 9.sp
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (video.stat.view > 0) FormatUtils.formatStat(video.stat.view.toLong())
                               else FormatUtils.formatProgress(video.progress, video.duration),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // 弹幕数 (如果有)
                if (video.stat.view > 0 && video.stat.danmaku > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "💬",
                            fontSize = 9.sp
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = FormatUtils.formatStat(video.stat.danmaku.toLong()),
                            color = Color.White.copy(0.9f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 🔥 标题 - 2行，官方风格
        Text(
            text = video.title,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // 🔥 底部信息行 - 官方 B 站风格
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 🔥 已关注标签（红色文字，官方风格）
            if (isFollowing) {
                Text(
                    text = "已关注",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFB7299)  // B站粉红色
                )
            }
            
            // 🔥 UP主头像（小圆形，官方风格）
            if (video.owner.face.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(FormatUtils.fixImageUrl(video.owner.face))
                        .crossfade(150)
                        .size(48, 48)
                        .memoryCacheKey("avatar_${video.owner.mid}")
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
            }
            
            // 🔥 UP主名称
            Text(
                text = video.owner.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = iOSSystemGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 简化版视频网格项 (用于搜索结果等)
 * 注意: onClick 只接收 bvid，不接收 cid
 */
@Composable
fun VideoGridItem(video: VideoItem, index: Int, onClick: (String) -> Unit) {
    ElegantVideoCard(video, index) { bvid, _ -> onClick(bvid) }
}
