package com.android.purebilibili.feature.video.player

import com.android.purebilibili.R
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun `fallback notification icon should follow selected app icon`() {
        assertEquals(
            R.mipmap.ic_launcher_blue,
            resolvePlaybackServiceFallbackIconRes("icon_blue")
        )
        assertEquals(
            R.mipmap.ic_launcher_3d,
            resolvePlaybackServiceFallbackIconRes("unknown")
        )
    }
}
