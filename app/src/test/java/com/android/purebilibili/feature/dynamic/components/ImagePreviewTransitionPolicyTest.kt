package com.android.purebilibili.feature.dynamic.components

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImagePreviewTransitionPolicyTest {

    @Test
    fun resolveImagePreviewTransitionFrame_clampsVisualProgressButKeepsLayoutOvershoot() {
        val frame = resolveImagePreviewTransitionFrame(
            rawProgress = -0.2f,
            hasSourceRect = true,
            sourceCornerRadiusDp = 12f
        )

        assertEquals(-0.08f, frame.layoutProgress)
        assertEquals(0f, frame.visualProgress)
        assertEquals(12f, frame.cornerRadiusDp)
    }

    @Test
    fun resolveImagePreviewTransitionFrame_keepsCornerRadiusConstantDuringTransition() {
        val frame = resolveImagePreviewTransitionFrame(
            rawProgress = 0.5f,
            hasSourceRect = true,
            sourceCornerRadiusDp = 12f
        )

        assertEquals(12f, frame.cornerRadiusDp)
    }

    @Test
    fun resolveImagePreviewTransitionFrame_usesZeroCornerWhenNoSourceRect() {
        val frame = resolveImagePreviewTransitionFrame(
            rawProgress = 0.5f,
            hasSourceRect = false,
            sourceCornerRadiusDp = 12f
        )

        assertEquals(0f, frame.cornerRadiusDp)
    }

    @Test
    fun imagePreviewDismissMotion_returnsOvershootThenSettleTargets() {
        val motion = imagePreviewDismissMotion()

        assertEquals(-0.06f, motion.overshootTarget)
        assertEquals(0f, motion.settleTarget)
    }

    @Test
    fun resolvePredictiveBackAnimationProgress_isInverseOfGestureProgress() {
        assertEquals(1f, resolvePredictiveBackAnimationProgress(0f))
        assertEquals(0.5f, resolvePredictiveBackAnimationProgress(0.5f))
        assertEquals(0f, resolvePredictiveBackAnimationProgress(1f))
    }

    @Test
    fun resolvePredictiveBackAnimationProgress_clampsOutOfRangeInput() {
        assertEquals(1f, resolvePredictiveBackAnimationProgress(-0.3f))
        assertEquals(0f, resolvePredictiveBackAnimationProgress(1.6f))
    }

    @Test
    fun resolveImagePreviewDismissTransform_returnsIdentityWithoutSourceRect() {
        val transform = resolveImagePreviewDismissTransform(
            transitionProgress = 0.3f,
            sourceRect = null,
            displayedImageRect = null
        )

        assertEquals(1f, transform.scale)
        assertEquals(0f, transform.translationXPx)
        assertEquals(0f, transform.translationYPx)
    }

    @Test
    fun resolveImagePreviewDismissTransform_usesUniformScaleAndCenterTranslation() {
        val source = Rect(
            left = 100f,
            top = 400f,
            right = 300f,
            bottom = 500f
        )
        val start = resolveImagePreviewDismissTransform(
            transitionProgress = 1f,
            sourceRect = source,
            displayedImageRect = Rect(
                left = 0f,
                top = 660f,
                right = 1080f,
                bottom = 1260f
            )
        )
        val middle = resolveImagePreviewDismissTransform(
            transitionProgress = 0.5f,
            sourceRect = source,
            displayedImageRect = Rect(
                left = 0f,
                top = 660f,
                right = 1080f,
                bottom = 1260f
            )
        )
        val end = resolveImagePreviewDismissTransform(
            transitionProgress = 0f,
            sourceRect = source,
            displayedImageRect = Rect(
                left = 0f,
                top = 660f,
                right = 1080f,
                bottom = 1260f
            )
        )
        val overshoot = resolveImagePreviewDismissTransform(
            transitionProgress = -0.06f,
            sourceRect = source,
            displayedImageRect = Rect(
                left = 0f,
                top = 660f,
                right = 1080f,
                bottom = 1260f
            )
        )

        assertEquals(1f, start.scale, 0.0001f)
        assertEquals(0f, start.translationXPx, 0.0001f)
        assertEquals(0f, start.translationYPx, 0.0001f)

        assertTrue(middle.scale in 0.7f..0.85f)
        assertTrue(middle.translationXPx < 0f && middle.translationXPx > -340f)
        assertTrue(middle.translationYPx < 0f && middle.translationYPx > -535f)

        assertEquals(0.16666669f, end.scale, 0.0001f)
        assertEquals(-340f, end.translationXPx, 0.0001f)
        assertEquals(-510f, end.translationYPx, 0.0001f)

        assertTrue(overshoot.translationXPx < end.translationXPx)
        assertTrue(overshoot.translationYPx < end.translationYPx)
        assertTrue(overshoot.scale in 0.01f..end.scale)
    }

    @Test
    fun resolveImagePreviewDismissBackdropAlpha_keepsBackdropLongerThenFades() {
        val start = resolveImagePreviewDismissBackdropAlpha(1f)
        val middle = resolveImagePreviewDismissBackdropAlpha(0.5f)
        val end = resolveImagePreviewDismissBackdropAlpha(0f)

        assertEquals(1f, start, 0.0001f)
        assertEquals(0.7320428f, middle, 0.0001f)
        assertEquals(0f, end, 0.0001f)
    }
}
