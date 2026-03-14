package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals

class AudioModeCoverLayoutPolicyTest {

    @Test
    fun usesWidthFractionWhenContentAreaIsTallEnough() {
        assertEquals(
            295,
            resolveAudioModeCenteredCoverSizeDp(
                availableWidthDp = 393,
                availableHeightDp = 420
            )
        )
    }

    @Test
    fun shrinksCoverWhenContentAreaNeedsTopAndBottomClearance() {
        assertEquals(
            252,
            resolveAudioModeCenteredCoverSizeDp(
                availableWidthDp = 393,
                availableHeightDp = 300
            )
        )
    }

    @Test
    fun neverReturnsNegativeCoverSizeOnVeryShortLayouts() {
        assertEquals(
            0,
            resolveAudioModeCenteredCoverSizeDp(
                availableWidthDp = 393,
                availableHeightDp = 40
            )
        )
    }
}
