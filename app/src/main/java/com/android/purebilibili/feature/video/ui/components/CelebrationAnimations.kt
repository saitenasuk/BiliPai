package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.AppIcons
import com.android.purebilibili.feature.video.ui.feedback.resolveTripleActionMotionSpec
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun LikeBurstAnimation(
    visible: Boolean,
    reducedMotion: Boolean = false,
    onAnimationEnd: () -> Unit = {}
) {
    if (!visible) return

    val progress = remember { Animatable(0f) }
    val durationMillis = if (reducedMotion) 260 else 420

    LaunchedEffect(visible, reducedMotion) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
        )
        delay(80)
        onAnimationEnd()
    }

    val primary = MaterialTheme.colorScheme.primary
    val currentProgress = progress.value
    val iconScale by animateFloatAsState(
        targetValue = if (currentProgress < 0.45f) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "likeBurstScale"
    )

    Box(
        modifier = Modifier.size(if (reducedMotion) 72.dp else 88.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val pulseRadius = size.minDimension * (0.18f + currentProgress * 0.24f)
            val ringRadius = size.minDimension * (0.2f + currentProgress * 0.28f)
            val glowAlpha = (1f - currentProgress).coerceIn(0f, 1f) * if (reducedMotion) 0.28f else 0.5f
            val ringAlpha = (1f - currentProgress).coerceIn(0f, 1f) * 0.9f

            drawCircle(
                color = primary.copy(alpha = glowAlpha),
                radius = pulseRadius,
                center = center
            )
            drawCircle(
                color = primary.copy(alpha = ringAlpha),
                radius = ringRadius,
                center = center,
                style = Stroke(width = size.minDimension * 0.045f)
            )
        }

        Icon(
            imageVector = Icons.Rounded.ThumbUp,
            contentDescription = null,
            tint = primary.copy(alpha = 0.98f),
            modifier = Modifier
                .size(if (reducedMotion) 26.dp else 30.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    alpha = 1f - (currentProgress * 0.14f)
                }
        )
    }
}

@Composable
fun TripleSuccessAnimation(
    visible: Boolean,
    isCompact: Boolean = false,
    reducedMotion: Boolean = false,
    onAnimationEnd: () -> Unit = {}
) {
    if (!visible) return

    val motionSpec = resolveTripleActionMotionSpec(reducedMotion = reducedMotion)
    val totalDurationMillis =
        motionSpec.activationDurationMillis +
            motionSpec.convergenceDurationMillis +
            motionSpec.resolutionDurationMillis +
            motionSpec.dwellDurationMillis
    val progress = remember { Animatable(0f) }

    LaunchedEffect(visible, isCompact, reducedMotion) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = totalDurationMillis, easing = FastOutSlowInEasing)
        )
        delay(120)
        onAnimationEnd()
    }

    val totalDurationFloat = totalDurationMillis.toFloat().coerceAtLeast(1f)
    val activationEnd = motionSpec.activationDurationMillis / totalDurationFloat
    val convergenceEnd =
        (motionSpec.activationDurationMillis + motionSpec.convergenceDurationMillis) / totalDurationFloat
    val resolutionEnd =
        (
            motionSpec.activationDurationMillis +
                motionSpec.convergenceDurationMillis +
                motionSpec.resolutionDurationMillis
            ) / totalDurationFloat

    val currentProgress = progress.value
    val activationProgress = phaseProgress(currentProgress, 0f, activationEnd)
    val convergenceProgress = phaseProgress(currentProgress, activationEnd, convergenceEnd)
    val resolutionProgress = phaseProgress(currentProgress, convergenceEnd, resolutionEnd)
    val dissolveProgress = phaseProgress(currentProgress, resolutionEnd, 1f)
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val baseDistance = if (isCompact) 26f else 34f
    val baseYOffset = if (isCompact) 10f else 14f
    val iconSize = if (isCompact) 18.dp else 22.dp
    val containerWidth = if (isCompact) 190.dp else 228.dp
    val containerHeight = if (isCompact) 82.dp else 104.dp
    val badgeScale = 0.9f + (resolutionProgress * 0.1f) - (dissolveProgress * 0.04f)
    val badgeAlpha = (resolutionProgress * (1f - dissolveProgress * 0.6f)).coerceIn(0f, 1f)
    val trailAlpha = (convergenceProgress * (1f - dissolveProgress)).coerceIn(0f, 1f) * 0.5f

    Box(
        modifier = Modifier
            .width(containerWidth)
            .height(containerHeight),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val iconStarts = listOf(
                Offset(center.x - baseDistance.dp.toPx(), center.y + baseYOffset.dp.toPx()),
                Offset(center.x, center.y - (baseYOffset + 12f).dp.toPx()),
                Offset(center.x + baseDistance.dp.toPx(), center.y + baseYOffset.dp.toPx())
            )

            if (!reducedMotion) {
                iconStarts.forEach { start ->
                    val end = center.copy(y = center.y - 2.dp.toPx())
                    val currentX = lerp(start.x, end.x, convergenceProgress)
                    val currentY = lerp(start.y, end.y, convergenceProgress)
                    drawCircle(
                        color = primary.copy(alpha = trailAlpha * 0.55f),
                        radius = if (isCompact) 3.5.dp.toPx() else 4.5.dp.toPx(),
                        center = Offset(currentX, currentY)
                    )
                }
            }

            if (badgeAlpha > 0f) {
                val ringRadius = if (isCompact) 18.dp.toPx() else 22.dp.toPx()
                drawCircle(
                    color = primary.copy(alpha = badgeAlpha * 0.16f),
                    radius = ringRadius * (1.18f - dissolveProgress * 0.08f),
                    center = center
                )
                drawCircle(
                    color = primary.copy(alpha = badgeAlpha * 0.95f),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = if (isCompact) 2.dp.toPx() else 2.5.dp.toPx())
                )
                drawCircle(
                    color = surface.copy(alpha = badgeAlpha * 0.92f),
                    radius = ringRadius * 0.66f,
                    center = center
                )
            }
        }

        if (!reducedMotion) {
            TripleActionIcon(
                image = Icons.Rounded.ThumbUp,
                tint = primary,
                baseX = -baseDistance,
                baseY = baseYOffset,
                activationProgress = iconActivationProgress(activationProgress, 0),
                convergenceProgress = convergenceProgress,
                dissolveProgress = dissolveProgress,
                iconSize = iconSize
            )
            TripleActionIcon(
                image = AppIcons.BiliCoin,
                tint = primary,
                baseX = 0f,
                baseY = -(baseYOffset + 12f),
                activationProgress = iconActivationProgress(activationProgress, 1),
                convergenceProgress = convergenceProgress,
                dissolveProgress = dissolveProgress,
                iconSize = iconSize
            )
            TripleActionIcon(
                image = Icons.Outlined.StarBorder,
                tint = primary,
                baseX = baseDistance,
                baseY = baseYOffset,
                activationProgress = iconActivationProgress(activationProgress, 2),
                convergenceProgress = convergenceProgress,
                dissolveProgress = dissolveProgress,
                iconSize = iconSize
            )
        }

        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = badgeScale
                scaleY = badgeScale
                alpha = badgeAlpha
            },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.BiliCoin,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
            )
        }

        Text(
            text = "三连完成",
            color = primary.copy(alpha = badgeAlpha),
            fontSize = if (isCompact) 14.sp else 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = if (isCompact) (-2).dp else (-4).dp)
                .graphicsLayer {
                    alpha = badgeAlpha
                    translationY = (1f - resolutionProgress).coerceIn(0f, 1f) * 8f
                }
        )
    }
}

@Composable
private fun BoxScope.TripleActionIcon(
    image: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    baseX: Float,
    baseY: Float,
    activationProgress: Float,
    convergenceProgress: Float,
    dissolveProgress: Float,
    iconSize: androidx.compose.ui.unit.Dp
) {
    val currentX = lerp(baseX, 0f, convergenceProgress)
    val currentY = lerp(baseY, 0f, convergenceProgress)
    val alpha = (activationProgress * (1f - dissolveProgress * 0.85f)).coerceIn(0f, 1f)
    val scale = 0.78f + (activationProgress * 0.24f) - (convergenceProgress * 0.06f)

    Icon(
        imageVector = image,
        contentDescription = null,
        tint = tint.copy(alpha = alpha),
        modifier = Modifier
            .align(Alignment.Center)
            .offset {
                IntOffset(currentX.roundToInt(), currentY.roundToInt())
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .size(iconSize)
    )
}

@Composable
fun CoinSuccessAnimation(
    visible: Boolean,
    coinCount: Int = 1,
    onAnimationEnd: () -> Unit = {}
) {
    if (!visible) return

    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            animatedProgress.snapTo(0f)
            animatedProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
            delay(100)
            onAnimationEnd()
        }
    }

    val progress = animatedProgress.value
    val scale by animateFloatAsState(
        targetValue = if (progress < 0.5f) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "coinScale"
    )

    Box(
        modifier = Modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = AppIcons.BiliCoin,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 1f - progress * 0.12f),
            modifier = Modifier
                .size(if (coinCount >= 2) 32.dp else 28.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationY = -progress * 12f
                }
        )
    }
}

private fun phaseProgress(
    progress: Float,
    start: Float,
    end: Float
): Float {
    if (end <= start) return 0f
    return ((progress - start) / (end - start)).coerceIn(0f, 1f)
}

private fun iconActivationProgress(
    activationProgress: Float,
    index: Int
): Float {
    val stagger = index * 0.18f
    return ((activationProgress - stagger) / 0.64f).coerceIn(0f, 1f)
}

private fun lerp(
    start: Float,
    end: Float,
    progress: Float
): Float {
    return start + (end - start) * progress.coerceIn(0f, 1f)
}
