package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals

class WatchLaterQueueSheetPresentationPolicyTest {

    @Test
    fun useInlineSheetWhenRealtimeHazeRequired() {
        assertEquals(
            WatchLaterQueueSheetPresentation.INLINE_HAZE,
            resolveWatchLaterQueueSheetPresentation(requireRealtimeHaze = true)
        )
    }

    @Test
    fun canFallbackToModalWhenRealtimeHazeNotRequired() {
        assertEquals(
            WatchLaterQueueSheetPresentation.MODAL,
            resolveWatchLaterQueueSheetPresentation(requireRealtimeHaze = false)
        )
    }
}
