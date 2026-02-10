// 文件路径: feature/home/components/LiquidIndicator.kt
package com.android.purebilibili.feature.home.components



import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.blur
import com.android.purebilibili.core.store.LiquidGlassStyle

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
 * @param hazeState HazeState 实例（用于模糊效果）
 * @param modifier Modifier
 */
@Composable
fun LiquidIndicator(
    position: Float,
    itemWidth: Dp,
    itemCount: Int,
    isDragging: Boolean,
    velocity: Float = 0f,
    startPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
    isLiquidGlassEnabled: Boolean = false,
    clampToBounds: Boolean = false,
    edgeInset: Dp = 0.dp,
    viewportShiftPx: Float = 0f,
    indicatorWidthMultiplier: Float = 1.42f,
    indicatorMinWidth: Dp = 104.dp,
    indicatorMaxWidth: Dp = 136.dp,
    indicatorHeight: Dp = 54.dp,
    lensIntensityBoost: Float = 1f,
    edgeWarpBoost: Float = 1f,
    chromaticBoost: Float = 1f,
    liquidGlassStyle: LiquidGlassStyle = LiquidGlassStyle.CLASSIC, // [New]
    backdrop: LayerBackdrop? = null // [New] Backdrop for refraction
) {
    val density = LocalDensity.current
    val lensProfile = remember(isDragging, velocity, lensIntensityBoost, edgeWarpBoost, chromaticBoost) {
        resolveLiquidLensProfile(
            isDragging = isDragging,
            velocityPxPerSecond = velocity,
            dragMotionFloor = 0.38f,
            lensIntensityBoost = lensIntensityBoost,
            edgeWarpBoost = edgeWarpBoost,
            chromaticBoost = chromaticBoost
        )
    }
    
    // 默认保持底栏宽胶囊；顶部可通过参数做轻量化适配
    val indicatorWidth = (itemWidth * indicatorWidthMultiplier)
        .coerceIn(indicatorMinWidth, indicatorMaxWidth)
    
    // [优化] 使用 graphicsLayer 进行位移，避免 Layout 重排
    // 计算位置 (Px)
    val itemWidthPx = with(density) { itemWidth.toPx() }
    val startPaddingPx = with(density) { startPadding.toPx() }
    val edgeInsetPx = with(density) { edgeInset.toPx() }
    // 居中偏移：(Item宽度 - 指示器宽度) / 2
    val centerOffsetPx = with(density) { (itemWidth.toPx() - indicatorWidth.toPx()) / 2 }
    
    // 速度形变
    val deformation = lensProfile.motionFraction * 0.34f
    
    val targetScaleX = 1f + deformation
    val targetScaleY = 1f - (deformation * 0.52f)
    
    val scaleX by animateFloatAsState(targetValue = targetScaleX, animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f), label = "scaleX")
    val scaleY by animateFloatAsState(targetValue = targetScaleY, animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f), label = "scaleY")
    val dragScale by animateFloatAsState(targetValue = if (isDragging) 1.0f else 1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f), label = "dragScale")

    val finalScaleX = scaleX * dragScale
    val finalScaleY = scaleY * dragScale

    // 指示器形状
    val shape = RoundedCornerShape(indicatorHeight / 2)
    
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
        val containerWidthPx = with(density) { maxWidth.toPx() }
         Box(
            modifier = Modifier
                .graphicsLayer {
                    // [核心优化] 在绘制阶段计算位移
                    translationX = resolveIndicatorTranslationXPx(
                        position = position,
                        itemWidthPx = itemWidthPx,
                        indicatorWidthPx = with(density) { indicatorWidth.toPx() },
                        startPaddingPx = startPaddingPx,
                        containerWidthPx = containerWidthPx,
                        clampToBounds = clampToBounds,
                        edgeInsetPx = edgeInsetPx,
                        viewportShiftPx = viewportShiftPx
                    )
                    
                    this.scaleX = finalScaleX
                    this.scaleY = finalScaleY
                    shadowElevation = 0f
                }
                .size(indicatorWidth, indicatorHeight)
                .clip(shape)
                .run {
                    if (isLiquidGlassEnabled && backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // [Effect] Strong refraction for the indicator (Magnifying Glass effect)
                        if (liquidGlassStyle == LiquidGlassStyle.CLASSIC) {
                            if (lensProfile.shouldRefract) {
                                this.drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { shape },
                                    effects = {
                                        lens(
                                            refractionHeight = lensProfile.refractionHeight,
                                            refractionAmount = lensProfile.refractionAmount,
                                            depthEffect = true,
                                            chromaticAberration = lensProfile.aberrationStrength > 0.01f
                                        )
                                    },
                                    onDrawSurface = {
                                        drawLiquidSphereSurface(color, lensProfile)
                                    }
                                )
                            } else {
                                // 静止态仅保留玻璃感，不做折射
                                this.drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { shape },
                                    effects = { blur(18f) },
                                    onDrawSurface = {
                                        drawLiquidSphereSurface(color, lensProfile)
                                    }
                                )
                            }
                         } else {
                             // [Style: SimpMusic] Frosted Glass (Blur Only, No Deformation)
                             val blurRadius = 36f
                            this.drawBackdrop(
                                backdrop = backdrop,
                                shape = { shape },
                                effects = {
                                    blur(blurRadius)
                                    // No lens effect here
                                },
                                onDrawSurface = {
                                    // More visible tint for frosted glass
                                    drawRect(color.copy(alpha = 0.18f))
                                    drawRect(Color.White.copy(alpha = 0.08f))
                                }
                            )
                        }
                    } else {
                        // Fallback
                         this.background(color)
                    }
                }
        )
    }
}


/**
 * 简化版液态指示器（不依赖 Backdrop）
 * 
 * 使用标准 Compose 动画实现类似效果
 */
/**
 * 简化版液态指示器（适用于 TabRow 等变长场景）
 * 
 * 使用标准 Compose 动画实现类似效果
 */
@Composable
fun SimpleLiquidIndicator(
    position: Float, // [修复] 直接接受 Float 而非 State，简化 API
    itemWidthPx: Float, // [修复] 使用像素值计算
    isDragging: Boolean,
    velocityPxPerSecond: Float = 0f,
    isLiquidGlassEnabled: Boolean = false,
    liquidGlassStyle: LiquidGlassStyle = LiquidGlassStyle.CLASSIC,
    backdrop: LayerBackdrop? = null,
    indicatorHeight: Dp = 34.dp,
    cornerRadius: Dp = 16.dp,
    widthRatio: Float = 0.78f,
    minWidth: Dp = 48.dp,
    horizontalInset: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val lensProfile = remember(isDragging, velocityPxPerSecond) {
        resolveLiquidLensProfile(
            isDragging = isDragging,
            velocityPxPerSecond = velocityPxPerSecond
        )
    }

    val minWidthPx = with(density) { minWidth.toPx() }
    val horizontalInsetPx = with(density) { horizontalInset.toPx() }
    val indicatorWidthPx = resolveTopTabIndicatorWidthPx(
        itemWidthPx = itemWidthPx,
        widthRatio = widthRatio,
        minWidthPx = minWidthPx,
        horizontalInsetPx = horizontalInsetPx
    )
    val indicatorWidth = with(density) { indicatorWidthPx.toDp() }
    val indicatorHeightPx = with(density) { indicatorHeight.toPx() }
    
    // [修复] 居中偏移：将指示器居中放置在每个 Tab 单元格内
    val centerOffsetPx = (itemWidthPx - indicatorWidthPx) / 2f
    
    val scale by animateFloatAsState(
        targetValue = 1f + lensProfile.motionFraction * 0.12f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "scale"
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (isLiquidGlassEnabled) 0.10f else 0.12f,
        animationSpec = tween(180),
        label = "indicatorAlpha"
    )
    
    // [Updated] Match BottomBar style: Primary color with alpha
    val indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = indicatorAlpha)
    
    // [修复] 使用 BoxWithConstraints 获取父容器高度来计算垂直居中
    BoxWithConstraints(
        modifier = modifier.fillMaxHeight()
    ) {
        val parentHeightPx = with(density) { maxHeight.toPx() }
        val verticalCenterOffsetPx = (parentHeightPx - indicatorHeightPx) / 2f
        
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = position * itemWidthPx + centerOffsetPx
                    translationY = verticalCenterOffsetPx
                    
                    this.scaleX = scale
                    this.scaleY = 1f - lensProfile.motionFraction * 0.08f
                }
                .size(indicatorWidth, indicatorHeight)
                .clip(RoundedCornerShape(cornerRadius))
                .run {
                    if (isLiquidGlassEnabled && backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (liquidGlassStyle == LiquidGlassStyle.CLASSIC) {
                            if (lensProfile.shouldRefract) {
                                this.drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { RoundedCornerShape(cornerRadius) },
                                    effects = {
                                        lens(
                                            refractionHeight = lensProfile.refractionHeight,
                                            refractionAmount = lensProfile.refractionAmount,
                                            depthEffect = true,
                                            chromaticAberration = lensProfile.aberrationStrength > 0.01f
                                        )
                                    },
                                    onDrawSurface = {
                                        drawLiquidSphereSurface(indicatorColor, lensProfile)
                                    }
                                )
                            } else {
                                this.drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { RoundedCornerShape(cornerRadius) },
                                    effects = { blur(16f) },
                                    onDrawSurface = {
                                        drawLiquidSphereSurface(indicatorColor, lensProfile)
                                    }
                                )
                            }
                        } else {
                            this.drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedCornerShape(cornerRadius) },
                                effects = { blur(32f) },
                                onDrawSurface = {
                                    drawRect(indicatorColor.copy(alpha = 0.16f))
                                    drawRect(Color.White.copy(alpha = 0.06f))
                                }
                            )
                        }
                    } else {
                        this.background(indicatorColor)
                    }
                }
                .border(
                    width = 0.7.dp,
                    color = Color.White.copy(alpha = if (isLiquidGlassEnabled) 0.62f else 0.25f),
                    shape = RoundedCornerShape(cornerRadius)
                )
        )
    }
}

internal fun resolveTopTabIndicatorWidthPx(
    itemWidthPx: Float,
    widthRatio: Float,
    minWidthPx: Float,
    horizontalInsetPx: Float
): Float {
    if (itemWidthPx <= 0f) return 0f
    val minBound = minWidthPx.coerceAtMost(itemWidthPx)
    val maxWidth = (itemWidthPx - horizontalInsetPx).coerceAtLeast(minBound)
    val desired = itemWidthPx * widthRatio
    return desired.coerceIn(minBound, maxWidth)
}

internal fun resolveIndicatorTranslationXPx(
    position: Float,
    itemWidthPx: Float,
    indicatorWidthPx: Float,
    startPaddingPx: Float,
    containerWidthPx: Float,
    clampToBounds: Boolean,
    edgeInsetPx: Float,
    viewportShiftPx: Float = 0f
): Float {
    val centerOffsetPx = (itemWidthPx - indicatorWidthPx) / 2f
    val raw = startPaddingPx + position * itemWidthPx + centerOffsetPx
    if (!clampToBounds) return raw

    val minX = edgeInsetPx.coerceAtLeast(0f) + viewportShiftPx
    val maxX = (containerWidthPx - indicatorWidthPx - edgeInsetPx + viewportShiftPx).coerceAtLeast(minX)
    return raw.coerceIn(minX, maxX)
}

internal data class LiquidLensProfile(
    val shouldRefract: Boolean,
    val motionFraction: Float,
    val refractionAmount: Float,
    val refractionHeight: Float,
    val centerHighlightAlpha: Float,
    val edgeCompressionAlpha: Float,
    val aberrationStrength: Float
)

internal fun resolveLiquidLensProfile(
    isDragging: Boolean,
    velocityPxPerSecond: Float,
    idleThresholdPxPerSecond: Float = 110f,
    dragMotionFloor: Float = 0.22f,
    lensIntensityBoost: Float = 1f,
    edgeWarpBoost: Float = 1f,
    chromaticBoost: Float = 1f
): LiquidLensProfile {
    val speed = abs(velocityPxPerSecond)
    val threshold = idleThresholdPxPerSecond
    val safeDragFloor = dragMotionFloor.coerceIn(0f, 0.8f)
    val safeLensBoost = lensIntensityBoost.coerceIn(0.8f, 2.2f)
    val safeEdgeWarpBoost = edgeWarpBoost.coerceIn(0.8f, 2.2f)
    val safeChromaBoost = chromaticBoost.coerceIn(0.8f, 2.2f)
    val baseMotion = if (isDragging) safeDragFloor else 0f
    val speedMotion = if (isDragging) {
        (speed / 2600f).coerceIn(0f, 1f)
    } else {
        ((speed - threshold).coerceAtLeast(0f) / 2600f).coerceIn(0f, 1f)
    }
    val motionFraction = (baseMotion + speedMotion * (1f - baseMotion)).coerceIn(0f, 1f)
    val shouldRefract = isDragging || speed > threshold

    if (!shouldRefract) {
        return LiquidLensProfile(
            shouldRefract = false,
            motionFraction = 0f,
            refractionAmount = 0f,
            refractionHeight = 0f,
            centerHighlightAlpha = 0f,
            edgeCompressionAlpha = 0f,
            aberrationStrength = 0f
        )
    }

    val eased = motionFraction * motionFraction * (3f - 2f * motionFraction)
    return LiquidLensProfile(
        shouldRefract = true,
        motionFraction = motionFraction,
        refractionAmount = (58f + eased * 54f) * safeLensBoost,
        refractionHeight = (84f + eased * 96f) * (0.9f + safeLensBoost * 0.1f),
        centerHighlightAlpha = 0.12f + eased * 0.16f,
        edgeCompressionAlpha = (0.06f + eased * 0.16f) * safeEdgeWarpBoost,
        aberrationStrength = ((0.008f + eased * 0.024f) * safeChromaBoost).coerceIn(0f, 0.06f)
    )
}

private fun DrawScope.drawLiquidSphereSurface(
    baseColor: Color,
    lensProfile: LiquidLensProfile
) {
    val isMoving = lensProfile.shouldRefract
    val centerGlowAlpha = if (isMoving) lensProfile.centerHighlightAlpha else 0.10f
    val edgeShadeAlpha = if (isMoving) lensProfile.edgeCompressionAlpha else 0.03f
    val baseAlpha = if (isMoving) 0.08f else 0.14f

    drawRect(baseColor.copy(alpha = baseAlpha))

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = centerGlowAlpha),
                Color.White.copy(alpha = centerGlowAlpha * 0.35f),
                Color.Transparent
            ),
            center = Offset(x = size.width / 2f, y = size.height * 0.54f),
            radius = size.minDimension * 0.9f
        )
    )

    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Black.copy(alpha = edgeShadeAlpha),
                Color.Transparent,
                Color.Transparent,
                Color.Black.copy(alpha = edgeShadeAlpha)
            )
        )
    )

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isMoving) 0.10f else 0.06f),
                Color.Transparent,
                Color.Black.copy(alpha = if (isMoving) 0.09f else 0.04f)
            )
        )
    )

    // 运动时额外的红蓝边缘分离，强化“液态折射”感知
    if (isMoving && lensProfile.aberrationStrength > 0f) {
        val fringe = (lensProfile.aberrationStrength * 3.2f).coerceIn(0f, 0.18f)
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF3DA8FF).copy(alpha = fringe),
                    Color.Transparent,
                    Color.Transparent,
                    Color(0xFFFF4F8F).copy(alpha = fringe)
                )
            )
        )
    }
}
