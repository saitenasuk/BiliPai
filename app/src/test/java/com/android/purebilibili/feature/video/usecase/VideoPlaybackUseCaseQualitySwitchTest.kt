package com.android.purebilibili.feature.video.usecase

import com.android.purebilibili.data.model.response.DashAudio
import com.android.purebilibili.data.model.response.DashVideo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPlaybackUseCaseQualitySwitchTest {

    private val cachedVideos = listOf(
        DashVideo(id = 80, baseUrl = "https://example.com/1080.m4s"),
        DashVideo(id = 64, baseUrl = "https://example.com/720.m4s"),
        DashVideo(id = 32, baseUrl = "https://example.com/480.m4s")
    )

    private val cachedAudios = listOf(
        DashAudio(id = 30280, baseUrl = "https://example.com/audio.m4s")
    )

    @Test
    fun `changeQualityFromCache returns null when target quality not cached`() {
        val useCase = VideoPlaybackUseCase()

        val result = useCase.changeQualityFromCache(
            qualityId = 120,
            cachedVideos = cachedVideos,
            cachedAudios = cachedAudios,
            currentPos = 0L
        )

        assertNull(result)
    }

    @Test
    fun `changeQualityFromCache returns exact match when target quality exists`() {
        val useCase = VideoPlaybackUseCase()

        val result = useCase.changeQualityFromCache(
            qualityId = 64,
            cachedVideos = cachedVideos,
            cachedAudios = cachedAudios,
            currentPos = 0L
        )

        assertNotNull(result)
        assertEquals(64, result?.actualQuality)
    }

    @Test
    fun `mergeQualityOptions keeps api high tiers when dash list misses them`() {
        val useCase = VideoPlaybackUseCase()

        val result = useCase.mergeQualityOptions(
            apiQualities = listOf(120, 116, 80, 64, 32, 16),
            dashVideoIds = listOf(80, 64, 32, 16)
        )

        assertEquals(listOf(120, 116, 80, 64, 32, 16), result.mergedQualityIds)
        assertEquals(listOf(120, 116), result.apiOnlyHighQualities)
    }

    @Test
    fun `mergeQualityOptions keeps api advertised 1080P when dash list is capped at 720P`() {
        val useCase = VideoPlaybackUseCase()

        val result = useCase.mergeQualityOptions(
            apiQualities = listOf(80, 64, 32, 16),
            dashVideoIds = listOf(64, 32, 16)
        )

        assertEquals(listOf(80, 64, 32, 16), result.mergedQualityIds)
        assertEquals(listOf(80), result.apiOnlyHighQualities)
    }

    @Test
    fun `mergeQualityOptions uses dash list when api list is empty`() {
        val useCase = VideoPlaybackUseCase()

        val result = useCase.mergeQualityOptions(
            apiQualities = emptyList(),
            dashVideoIds = listOf(80, 64)
        )

        assertEquals(listOf(80, 64, 32, 16), result.mergedQualityIds)
        assertTrue(result.apiOnlyHighQualities.isEmpty())
    }

    @Test
    fun `resolveAutoHighestTargetQuality caps non vip users at 1080p`() {
        val useCase = VideoPlaybackUseCase()

        val result = useCase.resolveAutoHighestTargetQuality(
            acceptQualities = listOf(120, 116, 112, 80, 64, 32),
            isLoggedIn = true,
            isVip = false,
            isHdrSupported = true,
            isDolbyVisionSupported = true
        )

        assertEquals(80, result)
    }

    @Test
    fun `resolveAutoHighestTargetQuality caps guests at 720p`() {
        val useCase = VideoPlaybackUseCase()

        val result = useCase.resolveAutoHighestTargetQuality(
            acceptQualities = listOf(116, 80, 64, 32),
            isLoggedIn = false,
            isVip = false,
            isHdrSupported = true,
            isDolbyVisionSupported = true
        )

        assertEquals(64, result)
    }

    @Test
    fun `resolveAutoHighestTargetQuality keeps vip highest playable tier`() {
        val useCase = VideoPlaybackUseCase()

        val result = useCase.resolveAutoHighestTargetQuality(
            acceptQualities = listOf(120, 116, 112, 80, 64),
            isLoggedIn = true,
            isVip = true,
            isHdrSupported = true,
            isDolbyVisionSupported = true
        )

        assertEquals(120, result)
    }

    @Test
    fun `buildPlaybackSelectionSummary describes final selection context`() {
        val useCase = VideoPlaybackUseCase()

        val result = useCase.buildPlaybackSelectionSummary(
            bvid = "BV1TEST12345",
            cid = 9527L,
            defaultQuality = 80,
            targetQuality = 80,
            returnedQuality = 64,
            selectedDashQuality = 80,
            mergedQualityIds = listOf(80, 64, 32, 16),
            isLoggedIn = true,
            isVip = false
        )

        assertEquals(
            "PLAY_DIAG playback_selection bvid=BV1TEST12345 cid=9527 default=80 target=80 returned=64 selectedDash=80 merged=[80, 64, 32, 16] isLoggedIn=true isVip=false",
            result
        )
    }
}
