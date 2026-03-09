package com.android.purebilibili.feature.video.ui.section

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoPlayerDanmakuLoadPolicyTest {

    @Test
    fun immediateLoad_startsAsSoonAsCidIsAvailable_evenWithoutDurationHint() {
        val policy = resolveVideoPlayerDanmakuLoadPolicy(
            cid = 10086L,
            danmakuEnabled = true,
            durationHintMs = 0L
        )

        assertTrue(policy.shouldEnable)
        assertTrue(policy.shouldLoadImmediately)
        assertEquals(0L, policy.durationHintMs)
    }

    @Test
    fun immediateLoad_keepsDurationHintWhenPlayerAlreadyKnowsIt() {
        val policy = resolveVideoPlayerDanmakuLoadPolicy(
            cid = 10010L,
            danmakuEnabled = true,
            durationHintMs = 360_000L
        )

        assertTrue(policy.shouldLoadImmediately)
        assertEquals(360_000L, policy.durationHintMs)
    }

    @Test
    fun immediateLoad_staysDisabledWhenCidInvalidOrDanmakuOff() {
        assertFalse(
            resolveVideoPlayerDanmakuLoadPolicy(
                cid = 0L,
                danmakuEnabled = true,
                durationHintMs = 0L
            ).shouldLoadImmediately
        )

        assertFalse(
            resolveVideoPlayerDanmakuLoadPolicy(
                cid = 10010L,
                danmakuEnabled = false,
                durationHintMs = 0L
            ).shouldLoadImmediately
        )
    }
}
