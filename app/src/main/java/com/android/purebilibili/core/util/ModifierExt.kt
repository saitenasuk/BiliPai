package com.android.purebilibili.core.util

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * 骨架屏闪光特效 Modifier
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000)
        ),
        label = "shimmer_offset"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFE0E0E0), // 浅灰
                Color(0xFFF5F5F5), // 亮灰 (高光)
                Color(0xFFE0E0E0), // 浅灰
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}

/**
 * 一个假的视频卡片组件 (用于 Loading 时占位)
 */
@Composable
fun VideoGridItemSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        // 封面占位
        Box(
            modifier = Modifier
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(8.dp))
                .shimmerEffect() // ✨ 加上闪光特效
        )
        Spacer(modifier = Modifier.height(8.dp))
        // 标题占位 (两行)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(6.dp))
        // 作者占位
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )
    }
}

// =============================================================================
// 🔥 Android 特有功能：触觉反馈 + 弹性点击
// =============================================================================

/**
 * 🔥 触觉反馈类型枚举
 */
enum class HapticType {
    LIGHT,      // 轻触 (选择/切换)
    MEDIUM,     // 中等 (确认)
    HEAVY,      // 重击 (警告/删除)
    SELECTION   // 选择变化
}

/**
 * 🔥 触发触觉反馈
 * 
 * - Android 12+: 使用新的 GESTURE_START/END 等常量
 * - 旧版本: 使用 LONG_PRESS/KEYBOARD_TAP 等
 */
@Composable
fun rememberHapticFeedback(): (HapticType) -> Unit {
    val view = LocalView.current
    return remember(view) {
        { type: HapticType ->
            val feedbackConstant = when (type) {
                HapticType.LIGHT -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        HapticFeedbackConstants.CONFIRM
                    } else {
                        HapticFeedbackConstants.KEYBOARD_TAP
                    }
                }
                HapticType.MEDIUM -> HapticFeedbackConstants.LONG_PRESS
                HapticType.HEAVY -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        HapticFeedbackConstants.REJECT
                    } else {
                        HapticFeedbackConstants.LONG_PRESS
                    }
                }
                HapticType.SELECTION -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
                    } else {
                        HapticFeedbackConstants.CLOCK_TICK
                    }
                }
            }
            view.performHapticFeedback(feedbackConstant)
        }
    }
}

/**
 * 🔥 弹性点击 Modifier (带缩放动画 + 触觉反馈)
 * 
 * Android 特有的交互体验：
 * - 按压时缩放到 0.95
 * - 弹性回弹动画
 * - 自动触觉反馈
 */
fun Modifier.bouncyClickable(
    hapticType: HapticType = HapticType.LIGHT,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = rememberHapticFeedback()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounce_scale"
    )
    
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled
        ) {
            haptic(hapticType)
            onClick()
        }
}

/**
 * 🔥 带涟漪效果的触觉点击 (Material 3 风格)
 */
fun Modifier.hapticClickable(
    hapticType: HapticType = HapticType.LIGHT,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val haptic = rememberHapticFeedback()
    
    this.clickable(enabled = enabled) {
        haptic(hapticType)
        onClick()
    }
}

/**
 * 🍎 iOS 风格点击效果 Modifier
 * 
 * 特性：
 * - 按压时缩放到 0.96f (iOS 默认值)
 * - 弹性回弹动画 (damping=0.6f)
 * - 自动触发轻量触觉反馈
 * 
 * @param scale 按压时的缩放比例，默认 0.96f
 * @param hapticEnabled 是否启用触觉反馈
 * @param onClick 点击回调
 */
fun Modifier.iOSTapEffect(
    scale: Float = 0.96f,
    hapticEnabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = rememberHapticFeedback()
    
    // 🍎 iOS 风格弹性动画
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) scale else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,    // iOS 弹性感
            stiffness = 400f        // 适中的动画速度
        ),
        label = "ios_tap_scale"
    )
    
    this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null
        ) {
            if (hapticEnabled) {
                haptic(HapticType.LIGHT)
            }
            onClick()
        }
}

/**
 * 🍎 iOS 风格点击效果 (仅动画，不处理点击事件)
 * 
 * 用于需要自定义点击处理的场景
 */
fun Modifier.iOSTapScale(
    scale: Float = 0.96f
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) scale else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 400f
        ),
        label = "ios_tap_scale_only"
    )
    
    this.graphicsLayer {
        scaleX = animatedScale
        scaleY = animatedScale
    }
}