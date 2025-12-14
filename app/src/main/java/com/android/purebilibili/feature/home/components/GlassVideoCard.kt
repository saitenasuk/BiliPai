// 文件路径: feature/home/components/GlassVideoCard.kt
package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
 * 🍎 玻璃拟态卡片 - Vision Pro 风格 (增强版)
 * 
 * 特点：
 * - 明显的毛玻璃背景色
 * - 渐变发光边框
 * - 内发光高光效果
 * - 浮动阴影
 */
@Composable
fun GlassVideoCard(
    video: VideoItem,
    onClick: (String, Long) -> Unit
) {
    val coverUrl = remember(video.bvid) {
        FormatUtils.fixImageUrl(if (video.pic.startsWith("//")) "https:${video.pic}" else video.pic)
    }
    
    // 🍎 使用 Material Theme 颜色系统 - 自动适配深色/浅色主题
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    
    // 🔥 使用主题颜色 - 卡片背景使用 surfaceVariant，自动适配主题
    val cardBackground = surfaceVariant.copy(alpha = 0.85f)
    
    // 边框颜色也使用主题色
    val borderColors = listOf(
        onSurface.copy(alpha = 0.15f),
        primaryColor.copy(alpha = 0.3f),
        onSurface.copy(alpha = 0.08f),
        primaryColor.copy(alpha = 0.2f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
    ) {
        // 🍎 外发光效果 (背后的光晕)
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(2.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = primaryColor.copy(alpha = 0.2f),
                    spotColor = primaryColor.copy(alpha = 0.3f)
                )
        )
        
        // 🍎 玻璃卡片主体
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                // 🍎 渐变边框
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(colors = borderColors),
                    shape = RoundedCornerShape(18.dp)
                )
                // 🍎 毛玻璃背景
                .background(cardBackground)
                .iOSTapEffect(scale = 0.96f, hapticEnabled = true) {
                    onClick(video.bvid, 0)
                }
        ) {
            // 🍎 顶部高光条 (玻璃反射效果)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.5f),
                                Color.White.copy(alpha = 0.7f),
                                Color.White.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 🍎 封面区域 - 内嵌式
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        // 🍎 封面内阴影
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(14.dp),
                            ambientColor = Color.Black.copy(alpha = 0.2f)
                        )
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverUrl)
                            .crossfade(120)
                            .size(480, 300)
                            .memoryCacheKey("glass_${video.bvid}")
                            .diskCacheKey("glass_${video.bvid}")
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // 🍎 封面边缘渐暗 (内阴影)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.15f)
                                    ),
                                    radius = 600f
                                )
                            )
                    )
                    
                    // 🍎 时长标签 - 玻璃风格
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = FormatUtils.formatDuration(video.duration),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                // 🍎 信息区域 - 增加内边距
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    // 标题 - 使用主题的 onSurface 颜色
                    Text(
                        text = video.title,
                        color = onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 19.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 数据行
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // UP主名称 - 使用主题色
                        Text(
                            text = video.owner.name,
                            color = primaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // 播放量 - 使用次级文字颜色
                        Text(
                            text = "${FormatUtils.formatStat(video.stat.view.toLong())}播放",
                            color = onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
