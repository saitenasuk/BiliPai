package com.android.purebilibili.feature.dynamic.components

import androidx.compose.ui.geometry.Rect
import kotlin.math.min
import kotlin.math.pow

private const val LAYOUT_PROGRESS_MIN = -0.08f
private const val LAYOUT_PROGRESS_MAX = 1.02f
private const val FALLBACK_START_SCALE = 0.96f
private const val DISMISS_OVERSHOOT_FACTOR = 0.03f

internal data class ImagePreviewTransitionFrame(
    val layoutProgress: Float,
    val visualProgress: Float,
    val cornerRadiusDp: Float,
    val fallbackScale: Float
)

internal data class ImagePreviewVisualFrame(
    val contentAlpha: Float,
    val backdropAlpha: Float,
    val blurRadiusPx: Float
)

internal data class ImagePreviewDismissMotion(
    val overshootTarget: Float,
    val settleTarget: Float
)

internal data class ImagePreviewDismissTransform(
    val scale: Float,
    val translationXPx: Float,
    val translationYPx: Float
)

internal fun resolveImagePreviewTransitionFrame(
    rawProgress: Float,
    hasSourceRect: Boolean,
    sourceCornerRadiusDp: Float
): ImagePreviewTransitionFrame {
    val layoutProgress = rawProgress.coerceIn(LAYOUT_PROGRESS_MIN, LAYOUT_PROGRESS_MAX)
    val visualProgress = rawProgress.coerceIn(0f, 1f)
    val cornerRadiusDp = if (hasSourceRect) {
        sourceCornerRadiusDp.coerceAtLeast(0f)
    } else {
        0f
    }
    val fallbackScale = lerpFloat(FALLBACK_START_SCALE, 1f, visualProgress)
    return ImagePreviewTransitionFrame(
        layoutProgress = layoutProgress,
        visualProgress = visualProgress,
        cornerRadiusDp = cornerRadiusDp,
        fallbackScale = fallbackScale
    )
}

internal fun resolveImagePreviewVisualFrame(
    visualProgress: Float,
    transitionEnabled: Boolean,
    maxBlurRadiusPx: Float
): ImagePreviewVisualFrame {
    val progress = visualProgress.coerceIn(0f, 1f)
    if (!transitionEnabled) {
        return ImagePreviewVisualFrame(
            contentAlpha = 1f,
            backdropAlpha = progress,
            blurRadiusPx = 0f
        )
    }

    return ImagePreviewVisualFrame(
        contentAlpha = lerpFloat(0.9f, 1f, progress),
        backdropAlpha = progress,
        blurRadiusPx = maxBlurRadiusPx.coerceAtLeast(0f) * (1f - progress)
    )
}

internal fun imagePreviewDismissMotion(): ImagePreviewDismissMotion {
    return ImagePreviewDismissMotion(
        overshootTarget = -0.06f,
        settleTarget = 0f
    )
}

internal fun resolveImagePreviewDismissTransform(
    transitionProgress: Float,
    sourceRect: Rect?,
    displayedImageRect: Rect?
): ImagePreviewDismissTransform {
    if (sourceRect == null || displayedImageRect == null) {
        return ImagePreviewDismissTransform(
            scale = 1f,
            translationXPx = 0f,
            translationYPx = 0f
        )
    }

    val clampedProgress = transitionProgress.coerceIn(LAYOUT_PROGRESS_MIN, 1f)
    val baseDismiss = (1f - clampedProgress.coerceIn(0f, 1f)).pow(1.6f)
    val overshoot = if (clampedProgress < 0f) {
        ((-clampedProgress) / (-LAYOUT_PROGRESS_MIN)) * DISMISS_OVERSHOOT_FACTOR
    } else {
        0f
    }
    val dismissFraction = baseDismiss + overshoot
    val targetScale = min(
        sourceRect.width / displayedImageRect.width,
        sourceRect.height / displayedImageRect.height
    ).coerceIn(0f, 1f)
    val containerCenterX = (displayedImageRect.left + displayedImageRect.right) / 2f
    val containerCenterY = (displayedImageRect.top + displayedImageRect.bottom) / 2f
    val sourceCenterX = (sourceRect.left + sourceRect.right) / 2f
    val sourceCenterY = (sourceRect.top + sourceRect.bottom) / 2f
    val targetTranslationX = sourceCenterX - containerCenterX
    val targetTranslationY = sourceCenterY - containerCenterY

    return ImagePreviewDismissTransform(
        scale = lerpFloat(1f, targetScale, dismissFraction).coerceAtLeast(0.01f),
        translationXPx = lerpFloat(0f, targetTranslationX, dismissFraction),
        translationYPx = lerpFloat(0f, targetTranslationY, dismissFraction)
    )
}

internal fun resolveImagePreviewDismissBackdropAlpha(
    visualProgress: Float
): Float {
    return visualProgress.coerceIn(0f, 1f).pow(0.45f)
}

internal fun resolvePredictiveBackAnimationProgress(backGestureProgress: Float): Float {
    val clamped = backGestureProgress.coerceIn(0f, 1f)
    return 1f - clamped
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
