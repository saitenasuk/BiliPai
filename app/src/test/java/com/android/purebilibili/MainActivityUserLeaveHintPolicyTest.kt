package com.android.purebilibili

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MainActivityUserLeaveHintPolicyTest {

    @Test
    fun forceStopsWhenLeavingVideoDetailAndStopOnExitEnabled() {
        assertTrue(
            shouldForceStopPlaybackOnUserLeaveHint(
                isInVideoDetail = true,
                stopPlaybackOnExit = true,
                shouldTriggerPip = false
            )
        )
    }

    @Test
    fun doesNotForceStopWhenNotInVideoDetail() {
        assertFalse(
            shouldForceStopPlaybackOnUserLeaveHint(
                isInVideoDetail = false,
                stopPlaybackOnExit = true,
                shouldTriggerPip = false
            )
        )
    }

    @Test
    fun doesNotForceStopWhenStopOnExitDisabled() {
        assertFalse(
            shouldForceStopPlaybackOnUserLeaveHint(
                isInVideoDetail = true,
                stopPlaybackOnExit = false,
                shouldTriggerPip = false
            )
        )
    }

    @Test
    fun doesNotForceStopWhenPipWillTrigger() {
        assertFalse(
            shouldForceStopPlaybackOnUserLeaveHint(
                isInVideoDetail = true,
                stopPlaybackOnExit = true,
                shouldTriggerPip = true
            )
        )
    }
}
