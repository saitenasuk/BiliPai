package com.android.purebilibili.core.util

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 🔥 列表项进场动画 (Premium 非线性动画)
 * 
 * 特点：
 * - 交错延迟实现波浪效果
 * - 从下方滑入 + 缩放 + 淡入
 * - 非线性缓动曲线 (FastOutSlowIn)
 * - Q弹果冻回弹效果
 * 
 * @param index: 列表项的索引，用于计算延迟时间
 * @param key: 用于触发重置动画的键值 (通常传视频ID)
 * @param initialOffsetY: 初始 Y 偏移量
 * @param animationEnabled: 是否启用动画 (设置开关)
 */
fun Modifier.animateEnter(
    index: Int = 0,
    key: Any? = Unit,
    initialOffsetY: Float = 80f,
    animationEnabled: Boolean = true
): Modifier = composed {
    // 🔥 如果动画被禁用，直接返回无动画效果
    if (!animationEnabled) {
        return@composed this
    }
    
    // 动画状态 - 始终初始化为需要动画的状态
    val alpha = remember(key) { Animatable(0f) }
    val translationY = remember(key) { Animatable(initialOffsetY) }
    val scale = remember(key) { Animatable(0.85f) }

    LaunchedEffect(key) {
        // 🔥🔥 在 LaunchedEffect 内部检查，确保每次执行时都检查最新状态
        if (CardPositionManager.isReturningFromDetail) {
            // 🔥 直接设置为最终值，不播放动画
            alpha.snapTo(1f)
            translationY.snapTo(0f)
            scale.snapTo(1f)
            // 延迟清除标记，确保所有卡片都读取到
            delay(100)
            CardPositionManager.clearReturning()
            return@LaunchedEffect
        }
        
        // 🔥 交错延迟：每个卡片延迟 40ms，最多 300ms
        val delayMs = (index * 40L).coerceAtMost(300L)
        delay(delayMs)

        // 🔥 并行启动动画
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 350,
                    easing = FastOutSlowInEasing // 非线性缓动
                )
            )
        }
        launch {
            translationY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.65f,    // 轻微过冲
                    stiffness = 300f         // 适中的弹性
                )
            )
        }
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.7f,     // 轻微过冲
                    stiffness = 350f         // 稍快的回弹
                )
            )
        }
    }

    this.graphicsLayer {
        this.alpha = alpha.value
        this.translationY = translationY.value
        this.scaleX = scale.value
        this.scaleY = scale.value
    }
}

/**
 * 2. Q弹点击效果 (按压缩放)
 */
fun Modifier.bouncyClickable(
    scaleDown: Float = 0.90f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "BouncyScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}