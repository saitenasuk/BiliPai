// 文件路径: feature/home/components/VideoCard.kt
package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.core.theme.iOSSystemGray
import com.android.purebilibili.core.theme.iOSPink
import com.android.purebilibili.core.util.iOSTapEffect

/**
 * 🍎 iOS 风格视频卡片
 * 采用 Apple Human Interface Guidelines 设计原则：
 * - 微妙的阴影创造深度感
 * - 更自然的渐变遮罩
 * - 清晰的排版层次
 */
@Composable
fun ElegantVideoCard(
    video: VideoItem,
    index: Int,
    refreshKey: Long = 0L,
    onClick: (String, Long) -> Unit
) {
    val haptic = rememberHapticFeedback()
    
    val coverUrl = remember(video.bvid) {
        FormatUtils.fixImageUrl(if (video.pic.startsWith("//")) "https:${video.pic}" else video.pic)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // 🍎 iOS 点击动画 - 按压时轻微缩小
            .iOSTapEffect(
                scale = 0.97f,
                hapticEnabled = true
            ) {
                onClick(video.bvid, 0)
            }
            .padding(bottom = 14.dp)
    ) {
        // 🍎 封面容器 - iOS 风格圆角 + 微妙阴影
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)  // 🍎 更接近 16:10 的现代比例
                .shadow(
                    elevation = 2.dp,  // 🍎 极轻的阴影
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.12f)
                )
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // 封面图 - 🚀 性能优化：限制尺寸 + placeholder + 缓存
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .size(480, 300)  // 🚀 限制尺寸减少内存占用
                    .crossfade(120)  // 🚀 缩短淡入时间
                    .memoryCacheKey("cover_${video.bvid}")
                    .diskCacheKey("cover_${video.bvid}")
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // 🍎 更自然的底部渐变 - 从完全透明到半透明黑
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.65f)
                            )
                        )
                    )
            )
            
            // 🍎 时长标签 - 药丸形状毛玻璃效果
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    text = FormatUtils.formatDuration(video.duration),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
            
            // 🍎 播放量统计 - 左下角
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 播放量
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "▶",
                        color = Color.White.copy(0.85f),
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (video.stat.view > 0) FormatUtils.formatStat(video.stat.view.toLong())
                               else FormatUtils.formatProgress(video.progress, video.duration),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // 弹幕数
                if (video.stat.view > 0 && video.stat.danmaku > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "💬",
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = FormatUtils.formatStat(video.stat.danmaku.toLong()),
                            color = Color.White.copy(0.9f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // 🍎 标题 - SF Pro 风格排版
        Text(
            text = video.title,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,  // 🍎 iOS 偏好 SemiBold
                fontSize = 14.sp,
                lineHeight = 20.sp,  // 🍎 更紧凑的行高
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.2).sp  // 🍎 iOS 风格负字间距
            ),
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 🍎 UP主信息行
        Row(
            modifier = Modifier.padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 互动数据高亮
            val stat = video.stat
            val bestStat = listOf(
                "赞" to stat.like,
                "币" to stat.coin,
                "藏" to stat.favorite
            ).filter { it.second > 0 }.maxByOrNull { it.second }
            
            if (bestStat != null && bestStat.second >= 100) {
                Text(
                    text = "${FormatUtils.formatStat(bestStat.second.toLong())}${bestStat.first}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = iOSPink
                )
                Text(
                    text = " · ",
                    fontSize = 11.sp,
                    color = iOSSystemGray
                )
            }
            
            // UP主头像
            if (video.owner.face.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(FormatUtils.fixImageUrl(video.owner.face))
                        .crossfade(150)
                        .size(72, 72)
                        .memoryCacheKey("avatar_${video.owner.mid}")
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            
            Text(
                text = video.owner.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = iOSSystemGray,  // 🍎 使用 iOS 系统灰
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                Icons.Default.MoreVert,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = iOSSystemGray.copy(alpha = 0.6f)
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
