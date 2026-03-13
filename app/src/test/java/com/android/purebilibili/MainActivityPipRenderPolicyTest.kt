package com.android.purebilibili

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MainActivityPipRenderPolicyTest {

    @Test
    fun `foreground mode keeps mini player overlay and skips dedicated pip player`() {
        val state = resolveMainActivityPlaybackOverlayState(isInPipMode = false)

        assertTrue(state.showMiniPlayerOverlay)
        assertFalse(state.showDedicatedPipPlayer)
    }

    @Test
    fun `system pip reuses existing player surface instead of creating dedicated pip player`() {
        val state = resolveMainActivityPlaybackOverlayState(isInPipMode = true)

        assertFalse(state.showMiniPlayerOverlay)
        assertFalse(state.showDedicatedPipPlayer)
    }
}
