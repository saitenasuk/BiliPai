package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.core.store.DanmakuPanelWidthMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DanmakuSettingsPanelPolicyTest {

    @Test
    fun portraitPanelAnchorsToBottomAndUsesWiderSheetWidth() {
        val policy = resolveDanmakuSettingsPanelLayoutPolicy(
            isFullscreen = false,
            screenWidthDp = 411,
            screenHeightDp = 915
        )

        assertEquals(
            DanmakuSettingsPanelPresentation.BottomSheet,
            policy.presentation
        )
        assertEquals(16, policy.horizontalPaddingDp)
        assertEquals(20, policy.bottomPaddingDp)
        assertTrue(policy.maxWidthDp >= 520)
    }

    @Test
    fun fullscreenPanelKeepsCenteredDialogPresentation() {
        val policy = resolveDanmakuSettingsPanelLayoutPolicy(
            isFullscreen = true,
            screenWidthDp = 915,
            screenHeightDp = 411
        )

        assertEquals(
            DanmakuSettingsPanelPresentation.CenteredDialog,
            policy.presentation
        )
        assertEquals(0, policy.bottomPaddingDp)
        assertEquals(480, policy.maxHeightDp)
    }

    @Test
    fun fullscreenPanelWidthMode_isFixedToQuarterWidth() {
        val fullWidth = resolveDanmakuSettingsPanelLayoutPolicy(
            isFullscreen = true,
            screenWidthDp = 915,
            screenHeightDp = 411,
            fullscreenWidthMode = DanmakuPanelWidthMode.FULL
        )
        val halfWidth = resolveDanmakuSettingsPanelLayoutPolicy(
            isFullscreen = true,
            screenWidthDp = 915,
            screenHeightDp = 411,
            fullscreenWidthMode = DanmakuPanelWidthMode.HALF
        )
        val thirdWidth = resolveDanmakuSettingsPanelLayoutPolicy(
            isFullscreen = true,
            screenWidthDp = 915,
            screenHeightDp = 411,
            fullscreenWidthMode = DanmakuPanelWidthMode.THIRD
        )

        assertEquals(221, fullWidth.maxWidthDp)
        assertEquals(221, halfWidth.maxWidthDp)
        assertEquals(221, thirdWidth.maxWidthDp)
    }

    @Test
    fun backdropTapDismissesPanelWhenPointerStaysWithinTouchSlop() {
        assertTrue(
            shouldDismissDanmakuSettingsPanelFromBackdropGesture(
                maxDragDistancePx = 4f,
                touchSlopPx = 8f
            )
        )
    }

    @Test
    fun backdropDragDoesNotDismissPanel() {
        assertFalse(
            shouldDismissDanmakuSettingsPanelFromBackdropGesture(
                maxDragDistancePx = 18f,
                touchSlopPx = 8f
            )
        )
    }
}
