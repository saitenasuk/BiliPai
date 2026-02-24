package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals

class FullscreenDoubleTapPolicyTest {

    @Test
    fun seekDisabled_alwaysTogglePlayPause() {
        assertEquals(
            FullscreenDoubleTapAction.TogglePlayPause,
            resolveFullscreenDoubleTapAction(relativeX = 0.1f, doubleTapSeekEnabled = false)
        )
        assertEquals(
            FullscreenDoubleTapAction.TogglePlayPause,
            resolveFullscreenDoubleTapAction(relativeX = 0.9f, doubleTapSeekEnabled = false)
        )
    }

    @Test
    fun seekEnabled_leftZoneSeekBackward() {
        assertEquals(
            FullscreenDoubleTapAction.SeekBackward,
            resolveFullscreenDoubleTapAction(relativeX = 0.2f, doubleTapSeekEnabled = true)
        )
    }

    @Test
    fun seekEnabled_centerZoneTogglePlayPause() {
        assertEquals(
            FullscreenDoubleTapAction.TogglePlayPause,
            resolveFullscreenDoubleTapAction(relativeX = 0.5f, doubleTapSeekEnabled = true)
        )
    }

    @Test
    fun seekEnabled_rightZoneSeekForward() {
        assertEquals(
            FullscreenDoubleTapAction.SeekForward,
            resolveFullscreenDoubleTapAction(relativeX = 0.8f, doubleTapSeekEnabled = true)
        )
    }
}
