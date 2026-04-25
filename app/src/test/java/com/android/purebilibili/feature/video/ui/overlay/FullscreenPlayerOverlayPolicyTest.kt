package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FullscreenPlayerOverlayPolicyTest {

    @Test
    fun fullscreenDragGestures_enabledWhenControlsHidden() {
        assertTrue(
            shouldStartFullscreenDragGesture(
                gesturesEnabled = true,
                showControls = false,
                startY = 540f,
                screenHeight = 1080f,
                statusBarExclusionZonePx = 40f,
                visibleTopControlsHeightPx = 96f,
                visibleBottomControlsHeightPx = 120f
            )
        )
    }

    @Test
    fun fullscreenDragGestures_protectVisibleTopControls() {
        assertFalse(
            shouldStartFullscreenDragGesture(
                gesturesEnabled = true,
                showControls = true,
                startY = 72f,
                screenHeight = 1080f,
                statusBarExclusionZonePx = 40f,
                visibleTopControlsHeightPx = 96f,
                visibleBottomControlsHeightPx = 120f
            )
        )
    }

    @Test
    fun fullscreenCenterSeekGesture_staysEnabledWhenControlsVisible() {
        assertTrue(
            shouldStartFullscreenDragGesture(
                gesturesEnabled = true,
                showControls = true,
                startY = 540f,
                screenHeight = 1080f,
                statusBarExclusionZonePx = 40f,
                visibleTopControlsHeightPx = 96f,
                visibleBottomControlsHeightPx = 120f
            )
        )
    }

    @Test
    fun fullscreenDragGestures_protectVisibleBottomControls() {
        assertFalse(
            shouldStartFullscreenDragGesture(
                gesturesEnabled = true,
                showControls = true,
                startY = 1000f,
                screenHeight = 1080f,
                statusBarExclusionZonePx = 40f,
                visibleTopControlsHeightPx = 96f,
                visibleBottomControlsHeightPx = 120f
            )
        )
    }

    @Test
    fun fullscreenDragGestures_disabledWhenDialogsAlreadyBlockGestures() {
        assertFalse(
            shouldStartFullscreenDragGesture(
                gesturesEnabled = false,
                showControls = false,
                startY = 540f,
                screenHeight = 1080f,
                statusBarExclusionZonePx = 40f,
                visibleTopControlsHeightPx = 96f,
                visibleBottomControlsHeightPx = 120f
            )
        )
    }
}
