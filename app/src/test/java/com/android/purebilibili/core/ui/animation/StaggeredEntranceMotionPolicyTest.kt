package com.android.purebilibili.core.ui.animation

import com.android.purebilibili.core.ui.adaptive.MotionTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StaggeredEntranceMotionPolicyTest {

    @Test
    fun reducedMotion_usesShorterDelayAndSmallerOffset() {
        val policy = resolveStaggeredEntranceMotionPolicy(MotionTier.Reduced)

        assertTrue(policy.delayStepMs <= 12)
        assertTrue(policy.offsetFactor <= 0.4f)
        assertTrue(policy.initialScale >= 0.97f)
    }

    @Test
    fun normalMotion_keepsBaselineDelay() {
        val policy = resolveStaggeredEntranceMotionPolicy(MotionTier.Normal)

        assertEquals(35, policy.delayStepMs)
        assertEquals(0.94f, policy.initialScale)
    }

    @Test
    fun enhancedMotion_isMoreExpressiveThanNormal() {
        val normal = resolveStaggeredEntranceMotionPolicy(MotionTier.Normal)
        val enhanced = resolveStaggeredEntranceMotionPolicy(MotionTier.Enhanced)

        assertTrue(enhanced.offsetFactor >= normal.offsetFactor)
        assertTrue(enhanced.initialScale < normal.initialScale)
    }

    @Test
    fun initialState_shouldStartAtFinalValuesWhenAlreadyVisible() {
        val policy = resolveStaggeredEntranceMotionPolicy(MotionTier.Normal)

        val state = resolveStaggeredEntranceInitialState(
            visible = true,
            offsetDistance = 50f,
            policy = policy
        )

        assertEquals(1f, state.alpha)
        assertEquals(0f, state.translationY)
        assertEquals(1f, state.scale)
    }

    @Test
    fun shouldAnimate_falseWhenAlreadyAtFinalState() {
        assertFalse(
            shouldRunStaggeredEntranceAnimation(
                visible = true,
                currentAlpha = 1f,
                currentTranslationY = 0f,
                currentScale = 1f
            )
        )
    }
}
