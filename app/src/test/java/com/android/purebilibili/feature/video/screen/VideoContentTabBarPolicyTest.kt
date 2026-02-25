package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoContentTabBarPolicyTest {

    @Test
    fun `danmaku input visible when player is expanded`() {
        assertTrue(
            shouldShowDanmakuSendInput(
                isPlayerCollapsed = false
            )
        )
    }

    @Test
    fun `danmaku input hidden when player is collapsed`() {
        assertFalse(
            shouldShowDanmakuSendInput(
                isPlayerCollapsed = true
            )
        )
    }
}
