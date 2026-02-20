package com.android.purebilibili.feature.video.player

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackServicePolicyTest {

    @Test
    fun `fallback foreground notification required when primary notification is null`() {
        assertTrue(shouldStartForegroundWithFallback(primaryNotification = null))
    }

    @Test
    fun `fallback foreground notification not required when primary notification exists`() {
        val placeholder = Any()
        assertFalse(shouldStartForegroundWithFallback(primaryNotification = placeholder))
    }
}

