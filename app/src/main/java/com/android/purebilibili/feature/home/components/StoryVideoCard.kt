// 文件路径: feature/home/components/StoryVideoCard.kt
package com.android.purebilibili.feature.home.components

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
import com.android.purebilibili.core.util.iOSTapEffect
import com.android.purebilibili.data.model.response.VideoItem

/**
 * 🎬 故事卡片 - Apple TV+ 风格
 * 
 * 特点：
 * - 2:1 电影宽屏比例
 * - 大圆角 (24dp)
 * - 标题叠加在封面底部
 * - 沉浸电影感
 */
@Composable
fun StoryVideoCard(
    video: VideoItem,
    onClick: (String, Long) -> Unit
) {
    val coverUrl = remember(video.bvid) {
        FormatUtils.fixImageUrl(if (video.pic.startsWith("//")) "https:${video.pic}" else video.pic)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.2f)
            )
            .clip(RoundedCornerShape(24.dp))
            .iOSTapEffect(scale = 0.98f, hapticEnabled = true) {
                onClick(video.bvid, 0)
            }
    ) {
        // 🎬 封面 - 2:1 电影宽屏
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(coverUrl)
                .crossfade(150)
                .memoryCacheKey("story_${video.bvid}")
                .diskCacheKey("story_${video.bvid}")
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 1f),
            contentScale = ContentScale.Crop
        )
        
        // 🎬 底部渐变遮罩
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 1f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )
        
        // 🎬 时长标签
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            color = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = FormatUtils.formatDuration(video.duration),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        
        // 🎬 底部信息区
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题 - 大字体
            Text(
                text = video.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // UP主信息 + 数据
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // UP主头像
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(FormatUtils.fixImageUrl(video.owner.face))
                        .crossfade(100)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // UP主名称
                Text(
                    text = video.owner.name,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 播放量
                Text(
                    text = "${FormatUtils.formatStat(video.stat.view.toLong())}播放",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // 弹幕
                Text(
                    text = "${FormatUtils.formatStat(video.stat.danmaku.toLong())}弹幕",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}
