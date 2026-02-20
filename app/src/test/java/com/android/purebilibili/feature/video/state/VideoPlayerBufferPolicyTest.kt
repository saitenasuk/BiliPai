package com.android.purebilibili.feature.video.state

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoPlayerBufferPolicyTest {

    @Test
    fun wifiPolicyShouldUseLowerStartupBufferForFasterAutoplay() {
        val policy = resolvePlayerBufferPolicy(isOnWifi = true)

        assertEquals(10000, policy.minBufferMs)
        assertEquals(40000, policy.maxBufferMs)
        assertEquals(900, policy.bufferForPlaybackMs)
        assertEquals(1800, policy.bufferForPlaybackAfterRebufferMs)
    }

    @Test
    fun mobilePolicyShouldKeepConservativeBuffering() {
        val policy = resolvePlayerBufferPolicy(isOnWifi = false)

        assertEquals(15000, policy.minBufferMs)
        assertEquals(50000, policy.maxBufferMs)
        assertEquals(1600, policy.bufferForPlaybackMs)
        assertEquals(3000, policy.bufferForPlaybackAfterRebufferMs)
        assertTrue(policy.bufferForPlaybackAfterRebufferMs >= policy.bufferForPlaybackMs)
    }
}
