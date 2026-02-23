package com.android.purebilibili.feature.video.ui.section

import kotlin.test.Test
import kotlin.test.assertEquals

class VideoPlayerDanmakuLoadPolicyTest {

    @Test
    fun shouldLoadAndEnableWhenDanmakuEnabledAndCidIsValid() {
        val policy = resolveVideoPlayerDanmakuLoadPolicy(
            cid = 12345L,
            danmakuEnabled = true
        )

        assertEquals(
            VideoPlayerDanmakuLoadPolicy(
                shouldEnable = true,
                shouldLoad = true
            ),
            policy
        )
    }

    @Test
    fun shouldDisableAndSkipLoadWhenDanmakuDisabled() {
        val policy = resolveVideoPlayerDanmakuLoadPolicy(
            cid = 12345L,
            danmakuEnabled = false
        )

        assertEquals(
            VideoPlayerDanmakuLoadPolicy(
                shouldEnable = false,
                shouldLoad = false
            ),
            policy
        )
    }

    @Test
    fun shouldDisableAndSkipLoadWhenCidInvalid() {
        val policy = resolveVideoPlayerDanmakuLoadPolicy(
            cid = 0L,
            danmakuEnabled = true
        )

        assertEquals(
            VideoPlayerDanmakuLoadPolicy(
                shouldEnable = false,
                shouldLoad = false
            ),
            policy
        )
    }
}
