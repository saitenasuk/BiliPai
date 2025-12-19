// 文件路径: feature/home/components/cards/GlassVideoCard.kt
package com.android.purebilibili.feature.home.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.iOSCardTapEffect
import com.android.purebilibili.core.util.animateEnter
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.data.model.response.VideoItem
// 🔥 共享元素过渡
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.spring
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope

/**
 * 🍎 玻璃拟态卡片 - Vision Pro 风格 (性能优化版)
 * 
 * 特点：
 * - 彩虹渐变边框
 * - 轻量阴影
 * - 悬浮播放按钮
 * 
 * 🚀 性能优化：移除了昂贵的 blur() 和多层阴影
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GlassVideoCard(
    video: VideoItem,
    index: Int = 0,  // 🔥🔥 [新增] 索引用于动画延迟
    animationEnabled: Boolean = true,  // 🔥 卡片动画开关
    transitionEnabled: Boolean = false, // 🔥 卡片过渡动画开关
    onClick: (String, Long) -> Unit
) {
    val coverUrl = remember(video.bvid) {
        FormatUtils.fixImageUrl(if (video.pic.startsWith("//")) "https:${video.pic}" else video.pic)
    }
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    // 🍎 玻璃背景色 - 使用系统主题色自动适配
    val glassBackground = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    
    // 🔥 获取屏幕尺寸用于计算归一化坐标
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    // 🔥 记录卡片位置
    var cardBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    
    // 🔥 尝试获取共享元素作用域
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    
    // 🌈 彩虹渐变边框色
    val rainbowColors = remember {
        listOf(
            Color(0xFFFF6B6B),  // 珊瑩红
            Color(0xFFFF8E53),  // 橙色
            Color(0xFFFFD93D),  // 金黄
            Color(0xFF6BCB77),  // 翠绿
            Color(0xFF4D96FF),  // 天蓝
            Color(0xFF9B59B6),  // 紫色
            Color(0xFFFF6B6B)   // 循环回红色
        )
    }
    
    // 🔥 卡片容器 - 支持共享元素过渡（受开关控制）
    val cardModifier = if (transitionEnabled && sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "video_cover_${video.bvid}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    // 🔥 添加回弹效果的 spring 动画
                    boundsTransform = { _, _ ->
                        spring(
                            dampingRatio = 0.7f,   // 轻微回弹
                            stiffness = 300f       // 适中速度
                        )
                    },
                    clipInOverlayDuringTransition = OverlayClip(
                        RoundedCornerShape(20.dp)  // 🔥 过渡时保持圆角
                    )
                )
        }
    } else {
        Modifier
    }

    Box(
        modifier = cardModifier
            .fillMaxWidth()
            .padding(6.dp)
            // 🔥🔥 [新增] 进场动画 - 支持开关控制
            .animateEnter(index = index, key = video.bvid, animationEnabled = animationEnabled)
            // 🔥🔥 [新增] 记录卡片位置
            .onGloballyPositioned { coordinates ->
                cardBounds = coordinates.boundsInRoot()
            }
    ) {
        // 🚀 [性能优化] 移除 blur() 层，改用静态渐变色
        // 原：blur(radius = 20.dp) 成本很高
        // 新：单层轻量阴影
        
        // 🍎 玻璃卡片主体
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                // 🌈 彩虹渐变边框
                .border(
                    width = 1.5.dp,
                    brush = Brush.sweepGradient(
                        colors = rainbowColors.map { it.copy(alpha = 0.6f) }
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                // 🍎 毛玻璃背景
                .background(glassBackground)
                .iOSCardTapEffect(
                    pressScale = 0.96f,
                    pressTranslationY = 8f,
                    hapticEnabled = true
                ) {
                    // 🔥🔥 点击时保存卡片位置
                    cardBounds?.let { bounds ->
                        CardPositionManager.recordCardPosition(bounds, screenWidthPx, screenHeightPx)
                    }
                    onClick(video.bvid, 0)
                }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 🍎 封面区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .padding(10.dp)
                ) {
                    // 封面图片 - 圆角内嵌
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = Color.Black.copy(alpha = 0.3f)
                            )
                    ) {
                        // 🚀 [性能优化] 降低图片尺寸
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(coverUrl)
                                .crossfade(100)  // 🚀 缩短淡入时间
                                .size(360, 225)  // 🚀 优化：360x225 替代 480x300
                                .memoryCacheKey("glass_${video.bvid}")
                                .diskCacheKey("glass_${video.bvid}")
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // 🍎 底部渐变遮罩
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.7f)
                                        )
                                    )
                                )
                        )
                        
                        // 🔥 已删除悬浮播放按钮
                        // 🍎 时长标签 - 玻璃胶囊
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = FormatUtils.formatDuration(video.duration),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                
                // 🍎 信息区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 14.dp)
                ) {
                    // 标题
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
                        // UP主名称 - 使用主题色 + 渐变背景
                        Surface(
                            color = primaryColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = video.owner.name,
                                color = primaryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // 播放量 - 🔥 [修复] 只在有播放量时显示
                        if (video.stat.view > 0) {
                            Text(
                                text = "${FormatUtils.formatStat(video.stat.view.toLong())}播放",
                                color = onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
            
            // 🌟 顶部高光线
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.6f),
                                Color.White.copy(alpha = 0.8f),
                                Color.White.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}
