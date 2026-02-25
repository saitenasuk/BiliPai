package com.android.purebilibili.core.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Rect

/**
 * 统一物理动画参数
 * 旨在模拟 iOS 系统级别的灵动交互手感
 */
object AnimationSpecs {

    /**
     * iOS 风格的空间弹簧参数 (Spatial Spring)
     * 用于共享元素过渡、卡片展开/收起等空间变换
     *
     * 特点：
     * - 高刚度 (Stiffness ~400f): 响应迅速，跟手感强
     * - 低阻尼 (Damping ~0.78f): 带有微小的过冲 (Overshoot)，富有弹性但不过分晃动
     */
    val BiliPaiSpringSpec = tween<Rect>(
        durationMillis = 320,
        easing = CubicBezierEasing(0.20f, 0.90f, 0.22f, 1.00f)
    )
}
