package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeReturnAnimationPolicyTest {

    @Test
    fun tabletWithCardAnimation_usesLongerSuppressionWindow() {
        val tabletDelay = resolveReturnAnimationSuppressionDurationMs(
            isTabletLayout = true,
            cardAnimationEnabled = true
        )
        val phoneDelay = resolveReturnAnimationSuppressionDurationMs(
            isTabletLayout = false,
            cardAnimationEnabled = true
        )

        assertTrue(tabletDelay > phoneDelay)
        assertEquals(420L, tabletDelay)
    }

    @Test
    fun whenCardAnimationDisabled_keepsShortSuppressionWindow() {
        assertEquals(
            120L,
            resolveReturnAnimationSuppressionDurationMs(
                isTabletLayout = true,
                cardAnimationEnabled = false
            )
        )
    }
}
