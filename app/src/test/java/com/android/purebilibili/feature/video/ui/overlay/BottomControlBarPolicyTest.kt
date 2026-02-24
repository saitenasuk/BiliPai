package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomControlBarPolicyTest {

    @Test
    fun aspectRatioButtonVisibleInFullscreen() {
        assertTrue(shouldShowAspectRatioButtonInControlBar(isFullscreen = true))
    }

    @Test
    fun aspectRatioButtonHiddenInPortraitMode() {
        assertFalse(shouldShowAspectRatioButtonInControlBar(isFullscreen = false))
    }

    @Test
    fun portraitSwitchButtonVisibleInFullscreen() {
        assertTrue(shouldShowPortraitSwitchButtonInControlBar(isFullscreen = true))
    }

    @Test
    fun portraitSwitchButtonHiddenInPortraitMode() {
        assertFalse(shouldShowPortraitSwitchButtonInControlBar(isFullscreen = false))
    }

    @Test
    fun episodeButtonVisibleOnlyWhenFullscreenAndHasEntry() {
        assertTrue(
            shouldShowEpisodeButtonInControlBar(
                isFullscreen = true,
                hasEpisodeEntry = true
            )
        )
        assertFalse(
            shouldShowEpisodeButtonInControlBar(
                isFullscreen = true,
                hasEpisodeEntry = false
            )
        )
        assertFalse(
            shouldShowEpisodeButtonInControlBar(
                isFullscreen = false,
                hasEpisodeEntry = true
            )
        )
    }

    @Test
    fun playbackOrderLabelHiddenOnNarrowLandscape() {
        assertTrue(
            shouldShowPlaybackOrderLabelInControlBar(
                isFullscreen = true,
                widthDp = 900
            )
        )
        assertFalse(
            shouldShowPlaybackOrderLabelInControlBar(
                isFullscreen = true,
                widthDp = 700
            )
        )
    }

    @Test
    fun aspectRatioButtonHiddenOnNarrowLandscape() {
        assertTrue(
            shouldShowAspectRatioButtonInControlBar(
                isFullscreen = true,
                widthDp = 900
            )
        )
        assertFalse(
            shouldShowAspectRatioButtonInControlBar(
                isFullscreen = true,
                widthDp = 680
            )
        )
    }

    @Test
    fun nextEpisodeButtonVisibleOnlyWhenNextExists() {
        assertTrue(
            shouldShowNextEpisodeButtonInControlBar(
                isFullscreen = true,
                hasNextEpisode = true
            )
        )
        assertFalse(
            shouldShowNextEpisodeButtonInControlBar(
                isFullscreen = true,
                hasNextEpisode = false
            )
        )
    }
}
