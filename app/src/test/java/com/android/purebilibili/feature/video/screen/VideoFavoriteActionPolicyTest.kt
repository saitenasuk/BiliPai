package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals

class VideoFavoriteActionPolicyTest {

    @Test
    fun `fullscreen overlay favorite tap should toggle immediately`() {
        assertEquals(
            VideoFavoriteAction.ToggleFavorite,
            resolveVideoFavoriteAction(VideoFavoriteEntryPoint.FullscreenOverlay)
        )
    }

    @Test
    fun `detail action row favorite tap should toggle immediately`() {
        assertEquals(
            VideoFavoriteAction.ToggleFavorite,
            resolveVideoFavoriteAction(VideoFavoriteEntryPoint.DetailActionRow)
        )
    }

    @Test
    fun `bottom input bar favorite tap should toggle immediately`() {
        assertEquals(
            VideoFavoriteAction.ToggleFavorite,
            resolveVideoFavoriteAction(VideoFavoriteEntryPoint.BottomInputBar)
        )
    }
}
