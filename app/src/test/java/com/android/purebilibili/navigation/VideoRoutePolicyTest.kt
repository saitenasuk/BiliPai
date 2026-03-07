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
            startAudio = true,
            autoPortrait = false
        )

        assertEquals(
            "video/BV1abc?cid=233&cover=https%3A%2F%2Fimg&startAudio=true&autoPortrait=false",
            route
        )
    }

    @Test
    fun resolveVideoRoutePath_defaultsToStartAudioFalseWhenDisabled() {
        val route = VideoRoute.resolveVideoRoutePath(
            bvid = "BV9xyz",
            cid = 0L,
            encodedCover = "",
            startAudio = false,
            autoPortrait = true
        )

        assertEquals(
            "video/BV9xyz?cid=0&cover=&startAudio=false&autoPortrait=true",
            route
        )
    }

    @Test
    fun standardVideoRoute_disablesAutoPortraitByDefault() {
        assertEquals(
            "video/BV1std?cid=77&cover=https%3A%2F%2Fimg.test%2Fcover.jpg&startAudio=false&autoPortrait=false",
            resolveStandardVideoRoute(
                bvid = "BV1std",
                cid = 77L,
                coverUrl = "https://img.test/cover.jpg"
            )
        )
    }

    @Test
    fun standardVideoRoute_keepsAudioModeWithoutAutoPortrait() {
        assertEquals(
            "video/BV1audio?cid=9&cover=&startAudio=true&autoPortrait=false",
            resolveStandardVideoRoute(
                bvid = "BV1audio",
                cid = 9L,
                coverUrl = "",
                startAudio = true
            )
        )
    }
}
