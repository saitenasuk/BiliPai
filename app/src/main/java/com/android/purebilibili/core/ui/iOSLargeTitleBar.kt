// 文件路径: core/ui/iOSLargeTitleBar.kt
package com.android.purebilibili.core.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * 🍎 iOS 风格大标题导航栏
 * 
 * 特性：
 * - 大标题模式：标题 34sp，粗体，左对齐
 * - 收缩模式：标题 17sp，居中，磨砂背景
 * - 滚动时平滑过渡动画
 * 
 * @param title 导航栏标题
 * @param scrollOffset 当前滚动偏移量 (像素)
 * @param collapseThreshold 触发收缩的滚动阈值，默认 100dp
 * @param leadingContent 左侧内容 (如返回按钮)
 * @param trailingContent 右侧内容 (如搜索、设置按钮)
 * @param hazeState 磨砂效果状态，用于与列表内容联动
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun iOSLargeTitleBar(
    title: String,
    scrollOffset: Float,
    collapseThreshold: Dp = 100.dp,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    hazeState: HazeState? = null
) {
    // 计算收缩进度 (0.0 = 展开, 1.0 = 完全收缩)
    val thresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 
        collapseThreshold.toPx() 
    }
    val collapseProgress = (scrollOffset / thresholdPx).coerceIn(0f, 1f)
    
    // 是否处于收缩状态
    val isCollapsed = collapseProgress > 0.7f
    
    // 🍎 动画值
    val largeTitleAlpha by animateFloatAsState(
        targetValue = if (isCollapsed) 0f else 1f,
        animationSpec = spring(stiffness = 300f),
        label = "large_title_alpha"
    )
    
    val compactTitleAlpha by animateFloatAsState(
        targetValue = if (isCollapsed) 1f else 0f,
        animationSpec = spring(stiffness = 300f),
        label = "compact_title_alpha"
    )
    
    val barHeight by animateDpAsState(
        targetValue = if (isCollapsed) 56.dp else 96.dp,
        animationSpec = spring(stiffness = 300f),
        label = "bar_height"
    )
    
    val blurAlpha by animateFloatAsState(
        targetValue = if (isCollapsed) 1f else 0f,
        animationSpec = spring(stiffness = 300f),
        label = "blur_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        // 🍎 磨砂背景层 (收缩时显示)
        if (hazeState != null && blurAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .alpha(blurAlpha)
                    .hazeChild(
                        state = hazeState,
                        style = HazeMaterials.thin(),
                        shape = RoundedCornerShape(0.dp)
                    )
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f * blurAlpha)
                    )
            )
        } else if (blurAlpha > 0.01f) {
            // 无 Haze 时使用半透明背景
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .alpha(blurAlpha)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            )
        }
        
        // 🍎 导航栏内容
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
        ) {
            // 顶部工具栏 (左右按钮 + 收缩标题)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧操作区
                Box(modifier = Modifier.width(48.dp)) {
                    leadingContent?.invoke()
                }
                
                // 中间收缩标题 (收缩时显示)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(compactTitleAlpha),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                
                // 右侧操作区
                Box(modifier = Modifier.width(48.dp)) {
                    trailingContent?.invoke()
                }
            }
            
            // 大标题区域 (展开时显示)
            if (largeTitleAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .alpha(largeTitleAlpha)
                ) {
                    Text(
                        text = title,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
        
        // 🍎 底部分隔线 (收缩时显示)
        if (isCollapsed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .align(Alignment.BottomCenter)
                    .alpha(compactTitleAlpha * 0.3f)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            )
        }
    }
}

/**
 * 🍎 简化版大标题栏 (无磨砂效果)
 */
@Composable
fun iOSLargeTitleBarSimple(
    title: String,
    isCollapsed: Boolean,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    iOSLargeTitleBar(
        title = title,
        scrollOffset = if (isCollapsed) 200f else 0f,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        hazeState = null
    )
}
