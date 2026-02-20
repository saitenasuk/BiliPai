package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitFullscreenOverlayPolicyTest {

    @Test
    fun useCompactTopBarOnNarrowScreen() {
        assertTrue(
            resolvePortraitFullscreenOverlayLayoutPolicy(
                widthDp = 360
            ).compactMode
        )
        assertTrue(
            resolvePortraitFullscreenOverlayLayoutPolicy(
                widthDp = 320
            ).compactMode
        )
    }

    @Test
    fun keepNormalTopBarOnWideScreen() {
        assertFalse(
            resolvePortraitFullscreenOverlayLayoutPolicy(
                widthDp = 411
            ).compactMode
        )
    }

    @Test
    fun hideViewCountInCompactMode() {
        assertFalse(shouldShowPortraitViewCount(viewCount = 12345, compactMode = true))
    }

    @Test
    fun showViewCountInNormalModeWhenHasData() {
        assertTrue(shouldShowPortraitViewCount(viewCount = 12345, compactMode = false))
    }

    @Test
    fun hideViewCountWhenNoData() {
        assertFalse(shouldShowPortraitViewCount(viewCount = 0, compactMode = false))
    }

    @Test
    fun hideTopMoreActionToAvoidDuplicateWithBottomBar() {
        assertFalse(shouldShowPortraitTopMoreAction())
    }
}
