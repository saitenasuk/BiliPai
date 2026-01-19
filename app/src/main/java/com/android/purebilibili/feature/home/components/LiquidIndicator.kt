// 文件路径: feature/home/components/LiquidIndicator.kt
package com.android.purebilibili.feature.home.components

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy  // [新增] iOS 26 液态玻璃鲜艳度效果
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow

import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import kotlin.math.abs

/**
 * 🌊 液态玻璃选中指示器
 * 
 * 实现类似 visionOS 的玻璃折射效果：
 * - 透镜折射效果 (Android 13+ 支持)
 * - 拖拽时放大形变
 * - 高光和内阴影
 * 
 * @param position 当前位置（浮点索引）
 * @param itemWidth 单个项目宽度
 * @param itemCount 项目数量
 * @param isDragging 是否正在拖拽
 * @param velocity 当前速度（用于形变）
 * @param backdrop Backdrop 实例（用于透镜效果）
 * @param modifier Modifier
 */
@Composable
fun LiquidIndicator(
    position: Float,
    itemWidth: Dp,
    itemCount: Int,
    isDragging: Boolean,
    velocity: Float = 0f,
    backdrop: Backdrop? = null,
    startPadding: Dp = 0.dp, // [新增] 起始偏移，用于对齐
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f
    
    // 指示器尺寸 - 变矮变长
    val indicatorWidth = itemWidth + 24.dp // [修复] 更宽，显著超出图标区域
    val indicatorHeight = 55.dp // [调整] 用户指定高度 (52 -> 55)
    
    // 计算偏移位置
    // 逻辑：StartPadding + (Index * ItemWidth)
    val itemStartOffset = with(density) {
        (position * itemWidth.toPx()).toDp()
    }
    
    // 拖拽时的缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 400f
        ),
        label = "indicator_scale"
    )
    
    // 速度影响的形变
    val velocityFactor = (velocity / 1000f).coerceIn(-0.15f, 0.15f)
    // 简化：这里只做简单的缩放，不再做复杂的X/Y拉伸，保持胶囊形状稳定
    
    // 指示器背景颜色
    val indicatorColor = if (isDarkTheme) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
    
    // 既然我们已经禁用了透镜效果来消除伪影，
    // 这里直接使用最干净的实现方式：一个带动画的 Box
    // 这样既高效又完全避免了"鬼影"和"重复渲染"的问题
    
    Box(
        modifier = modifier, // 这里从外部传入的是 fillMaxSize
        contentAlignment = Alignment.CenterStart // [修复] 垂直居中，水平靠左开始
    ) {
         Box(
            modifier = Modifier
                // 核心定位逻辑：
                // 1. 找到 Item 槽位的起始点: startPadding + itemStartOffset
                // 2. 找到 Item 槽位的中心点: + itemWidth / 2
                // 3. 减去指示器的一半宽度: - indicatorWidth / 2
                .offset(
                    x = startPadding + itemStartOffset + (itemWidth / 2) - (indicatorWidth / 2), 
                    y = 0.dp // 因为使用了 CenterStart，这里的 y=0 意味着垂直居中
                )
                .size(indicatorWidth, indicatorHeight)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(indicatorHeight / 2)) // 完美圆角
                .background(indicatorColor)
        )
    }
}

/**
 * 简化版液态指示器（不依赖 Backdrop）
 * 
 * 使用标准 Compose 动画实现类似效果
 */
@Composable
fun SimpleLiquidIndicator(
    position: Float,
    itemWidth: Dp,
    isDragging: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f
    
    val indicatorWidth = itemWidth - 8.dp
    val indicatorHeight = 48.dp
    
    val offsetX = with(density) {
        (position * itemWidth.toPx()).toDp()
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "scale"
    )
    
    val indicatorColor = if (isDarkTheme) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
    
    Box(
        modifier = modifier
            .offset(x = offsetX + 4.dp, y = 0.dp)
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
            }
            .size(indicatorWidth, indicatorHeight)
            .clip(RoundedCornerShape(24.dp))
            .background(indicatorColor)
    )
}
