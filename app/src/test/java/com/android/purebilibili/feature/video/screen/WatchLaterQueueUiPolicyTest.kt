package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.feature.video.player.ExternalPlaylistSource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WatchLaterQueueUiPolicyTest {

    @Test
    fun showQueueBarWhenExternalSourceIsWatchLaterAndPlaylistNotEmpty() {
        assertTrue(
            shouldShowWatchLaterQueueBarByPolicy(
                isExternalPlaylist = true,
                externalPlaylistSource = ExternalPlaylistSource.WATCH_LATER,
                playlistSize = 8
            )
        )
    }

    @Test
    fun hideQueueBarWhenSourceIsNotWatchLater() {
        assertFalse(
            shouldShowWatchLaterQueueBarByPolicy(
                isExternalPlaylist = true,
                externalPlaylistSource = ExternalPlaylistSource.SPACE,
                playlistSize = 8
            )
        )
    }

    @Test
    fun hideQueueBarWhenNotExternalPlaylist() {
        assertFalse(
            shouldShowWatchLaterQueueBarByPolicy(
                isExternalPlaylist = false,
                externalPlaylistSource = ExternalPlaylistSource.WATCH_LATER,
                playlistSize = 8
            )
        )
    }

    @Test
    fun hideQueueBarWhenPlaylistEmpty() {
        assertFalse(
            shouldShowWatchLaterQueueBarByPolicy(
                isExternalPlaylist = true,
                externalPlaylistSource = ExternalPlaylistSource.WATCH_LATER,
                playlistSize = 0
            )
        )
    }
}
