package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals

class WatchLaterQueueLayoutPolicyTest {

    @Test
    fun listMaxHeightUsesScreenRatioOnCommonPhones() {
        assertEquals(562, resolveWatchLaterQueueListMaxHeightDp(screenHeightDp = 780))
    }

    @Test
    fun listMaxHeightHasMinimumBoundOnShortScreens() {
        assertEquals(420, resolveWatchLaterQueueListMaxHeightDp(screenHeightDp = 520))
    }

    @Test
    fun bottomSpacerAddsSafeInsetAndBaselineGap() {
        assertEquals(8, resolveWatchLaterQueueBottomSpacerDp(navigationBarBottomDp = 0))
        assertEquals(30, resolveWatchLaterQueueBottomSpacerDp(navigationBarBottomDp = 22))
    }
}
