package com.android.purebilibili.feature.video.ui.section

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoPlayerCoverPolicyTest {

    @Test
    fun `returning from detail should force cover visible`() {
        assertTrue(
            shouldForceCoverDuringReturnAnimation(
                forceCoverOnly = false,
                isReturningFromDetail = true
            )
        )
    }

    @Test
    fun `explicit force cover should win even when not returning`() {
        assertTrue(
            shouldForceCoverDuringReturnAnimation(
                forceCoverOnly = true,
                isReturningFromDetail = false
            )
        )
    }

    @Test
    fun `normal playback with first frame rendered should hide cover`() {
        assertFalse(
            shouldShowCoverImage(
                isFirstFrameRendered = true,
                forceCoverDuringReturnAnimation = false
            )
        )
    }

    @Test
    fun `forced return cover should stay visible even after first frame`() {
        assertTrue(
            shouldShowCoverImage(
                isFirstFrameRendered = true,
                forceCoverDuringReturnAnimation = true
            )
        )
    }

    @Test
    fun `forced return cover should disable fade animation`() {
        assertTrue(shouldDisableCoverFadeAnimation(forceCoverDuringReturnAnimation = true))
        assertFalse(shouldDisableCoverFadeAnimation(forceCoverDuringReturnAnimation = false))
    }
}
