package com.android.purebilibili.feature.video.screen

import android.view.KeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoDetailTvFocusPolicyTest {

    @Test
    fun tvInitialFocusDefaultsToPlayer() {
        val initial = resolveInitialVideoDetailTvFocusTarget(isTv = true)
        assertEquals(VideoDetailTvFocusTarget.PLAYER, initial)
    }

    @Test
    fun nonTvHasNoForcedInitialFocus() {
        val initial = resolveInitialVideoDetailTvFocusTarget(isTv = false)
        assertEquals(null, initial)
    }

    @Test
    fun downFromPlayerMovesToContent() {
        val next = resolveVideoDetailTvFocusTarget(
            current = VideoDetailTvFocusTarget.PLAYER,
            keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
            action = KeyEvent.ACTION_UP
        )

        assertEquals(VideoDetailTvFocusTarget.CONTENT, next)
    }

    @Test
    fun upFromContentMovesToPlayer() {
        val next = resolveVideoDetailTvFocusTarget(
            current = VideoDetailTvFocusTarget.CONTENT,
            keyCode = KeyEvent.KEYCODE_DPAD_UP,
            action = KeyEvent.ACTION_UP
        )

        assertEquals(VideoDetailTvFocusTarget.PLAYER, next)
    }

    @Test
    fun nonNavigationKeyKeepsCurrentTarget() {
        val next = resolveVideoDetailTvFocusTarget(
            current = VideoDetailTvFocusTarget.CONTENT,
            keyCode = KeyEvent.KEYCODE_DPAD_LEFT,
            action = KeyEvent.ACTION_UP
        )

        assertEquals(VideoDetailTvFocusTarget.CONTENT, next)
    }
}
