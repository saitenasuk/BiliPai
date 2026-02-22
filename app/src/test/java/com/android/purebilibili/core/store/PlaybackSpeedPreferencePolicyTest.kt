package com.android.purebilibili.core.store

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackSpeedPreferencePolicyTest {

    @Test
    fun `preferred speed should use default when remember-last disabled`() {
        assertEquals(
            1.3f,
            resolvePreferredPlaybackSpeed(
                defaultSpeed = 1.3f,
                rememberLastSpeed = false,
                lastSpeed = 1.8f
            )
        )
    }

    @Test
    fun `preferred speed should use last when remember-last enabled`() {
        assertEquals(
            1.8f,
            resolvePreferredPlaybackSpeed(
                defaultSpeed = 1.3f,
                rememberLastSpeed = true,
                lastSpeed = 1.8f
            )
        )
    }

    @Test
    fun `playback speed should be clamped into supported range`() {
        assertEquals(0.1f, normalizePlaybackSpeed(0.0f))
        assertEquals(8.0f, normalizePlaybackSpeed(9.5f))
    }
}
