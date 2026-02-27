package com.android.purebilibili.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class VideoRoutePolicyTest {

    @Test
    fun resolveVideoRoutePath_includesStartAudioFlag() {
        val route = VideoRoute.resolveVideoRoutePath(
            bvid = "BV1abc",
            cid = 233L,
            encodedCover = "https%3A%2F%2Fimg",
            startAudio = true
        )

        assertEquals(
            "video/BV1abc?cid=233&cover=https%3A%2F%2Fimg&startAudio=true",
            route
        )
    }

    @Test
    fun resolveVideoRoutePath_defaultsToStartAudioFalseWhenDisabled() {
        val route = VideoRoute.resolveVideoRoutePath(
            bvid = "BV9xyz",
            cid = 0L,
            encodedCover = "",
            startAudio = false
        )

        assertEquals(
            "video/BV9xyz?cid=0&cover=&startAudio=false",
            route
        )
    }
}
