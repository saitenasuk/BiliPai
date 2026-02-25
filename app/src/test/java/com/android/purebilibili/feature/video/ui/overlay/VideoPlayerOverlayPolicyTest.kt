package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoPlayerOverlayPolicyTest {

    @Test
    fun episodeEntryShownWhenRelatedVideosExist() {
        assertTrue(
            shouldShowEpisodeEntryFromVideoData(
                relatedVideosCount = 1,
                hasSeasonEpisodes = false,
                pagesCount = 1
            )
        )
    }

    @Test
    fun episodeEntryShownWhenSeasonEpisodesExist() {
        assertTrue(
            shouldShowEpisodeEntryFromVideoData(
                relatedVideosCount = 0,
                hasSeasonEpisodes = true,
                pagesCount = 1
            )
        )
    }

    @Test
    fun episodeEntryShownWhenPagesExist() {
        assertTrue(
            shouldShowEpisodeEntryFromVideoData(
                relatedVideosCount = 0,
                hasSeasonEpisodes = false,
                pagesCount = 3
            )
        )
    }

    @Test
    fun episodeEntryHiddenWhenNoEpisodeData() {
        assertFalse(
            shouldShowEpisodeEntryFromVideoData(
                relatedVideosCount = 0,
                hasSeasonEpisodes = false,
                pagesCount = 1
            )
        )
    }

    @Test
    fun nextEpisodeTargetPrefersPageNext() {
        val target = resolveNextEpisodeTarget(
            pagesCount = 3,
            currentPageIndex = 0,
            seasonEpisodeBvids = listOf("BV1", "BV2"),
            currentBvid = "BV1",
            relatedBvids = listOf("BV3")
        )
        assertTrue(target?.nextPageIndex == 1)
    }

    @Test
    fun nextEpisodeTargetFallsBackToSeasonThenRelated() {
        val seasonTarget = resolveNextEpisodeTarget(
            pagesCount = 1,
            currentPageIndex = 0,
            seasonEpisodeBvids = listOf("BV1", "BV2"),
            currentBvid = "BV1",
            relatedBvids = listOf("BV3")
        )
        assertTrue(seasonTarget?.nextBvid == "BV2")

        val relatedTarget = resolveNextEpisodeTarget(
            pagesCount = 1,
            currentPageIndex = 0,
            seasonEpisodeBvids = listOf("BV1"),
            currentBvid = "BV1",
            relatedBvids = listOf("BV1", "BV3")
        )
        assertTrue(relatedTarget?.nextBvid == "BV3")
    }

    @Test
    fun nextEpisodeTargetReturnsNullWhenNoCandidate() {
        assertTrue(
            resolveNextEpisodeTarget(
                pagesCount = 1,
                currentPageIndex = 0,
                seasonEpisodeBvids = emptyList(),
                currentBvid = "BV1",
                relatedBvids = emptyList()
            )
                == null
        )
    }

    @Test
    fun drawerVisibleShouldConsumeBackgroundGestures() {
        assertTrue(
            shouldConsumeBackgroundGesturesForEndDrawer(
                endDrawerVisible = true
            )
        )
    }

    @Test
    fun drawerHiddenShouldNotConsumeBackgroundGestures() {
        assertFalse(
            shouldConsumeBackgroundGesturesForEndDrawer(
                endDrawerVisible = false
            )
        )
    }
}
