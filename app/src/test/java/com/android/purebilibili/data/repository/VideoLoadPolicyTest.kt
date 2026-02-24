package com.android.purebilibili.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoLoadPolicyTest {

    @Test
    fun `resolveVideoInfoLookup prefers bv id`() {
        val input = resolveVideoInfoLookupInput(rawBvid = " BV1xx411c7mD ", aid = 0L)

        assertEquals(VideoInfoLookupInput(bvid = "BV1xx411c7mD", aid = 0L), input)
    }

    @Test
    fun `resolveVideoInfoLookup parses av id when aid missing`() {
        val input = resolveVideoInfoLookupInput(rawBvid = "av1129813966", aid = 0L)

        assertEquals(VideoInfoLookupInput(bvid = "", aid = 1129813966L), input)
    }

    @Test
    fun `resolveVideoInfoLookup falls back to explicit aid`() {
        val input = resolveVideoInfoLookupInput(rawBvid = "", aid = 1756441068L)

        assertEquals(VideoInfoLookupInput(bvid = "", aid = 1756441068L), input)
    }

    @Test
    fun `resolveInitialStartQuality uses stable quality for non vip auto highest`() {
        val quality = resolveInitialStartQuality(
            targetQuality = 127,
            isAutoHighestQuality = true,
            isLogin = true,
            isVip = false,
            auto1080pEnabled = true
        )

        assertEquals(80, quality)
    }

    @Test
    fun `resolveInitialStartQuality keeps high quality for vip auto highest`() {
        val quality = resolveInitialStartQuality(
            targetQuality = 127,
            isAutoHighestQuality = true,
            isLogin = true,
            isVip = true,
            auto1080pEnabled = true
        )

        assertEquals(120, quality)
    }

    @Test
    fun `shouldSkipPlayUrlCache only skips auto highest when vip`() {
        assertFalse(
            shouldSkipPlayUrlCache(
                isAutoHighestQuality = true,
                isVip = false,
                audioLang = null
            )
        )
        assertTrue(
            shouldSkipPlayUrlCache(
                isAutoHighestQuality = true,
                isVip = true,
                audioLang = null
            )
        )
    }

    @Test
    fun `buildDashAttemptQualities falls back to 80 for high target`() {
        assertEquals(listOf(120, 80), buildDashAttemptQualities(120))
        assertEquals(listOf(80), buildDashAttemptQualities(80))
    }

    @Test
    fun `resolveDashRetryDelays allows one retry for standard qualities`() {
        assertEquals(listOf(0L), resolveDashRetryDelays(120))
        assertEquals(listOf(0L, 450L), resolveDashRetryDelays(80))
        assertEquals(listOf(0L, 450L), resolveDashRetryDelays(64))
    }

    @Test
    fun `shouldCallAccessTokenApi respects cooldown`() {
        val now = 1_000L
        assertFalse(shouldCallAccessTokenApi(nowMs = now, cooldownUntilMs = 2_000L, hasAccessToken = true))
        assertTrue(shouldCallAccessTokenApi(nowMs = now, cooldownUntilMs = 500L, hasAccessToken = true))
        assertFalse(shouldCallAccessTokenApi(nowMs = now, cooldownUntilMs = 500L, hasAccessToken = false))
    }

    @Test
    fun `shouldTryAppApiForTargetQuality only enables app api for high quality`() {
        assertFalse(shouldTryAppApiForTargetQuality(80))
        assertFalse(shouldTryAppApiForTargetQuality(64))
        assertTrue(shouldTryAppApiForTargetQuality(112))
        assertTrue(shouldTryAppApiForTargetQuality(120))
    }

    @Test
    fun `buildGuestFallbackQualities prefers 80 before 64`() {
        assertEquals(listOf(80, 64, 32), buildGuestFallbackQualities())
    }

    @Test
    fun `shouldCachePlayUrlResult skips guest source`() {
        assertFalse(shouldCachePlayUrlResult(PlayUrlSource.GUEST, audioLang = null))
        assertTrue(shouldCachePlayUrlResult(PlayUrlSource.DASH, audioLang = null))
        assertFalse(shouldCachePlayUrlResult(PlayUrlSource.DASH, audioLang = "en"))
    }

    @Test
    fun `shouldFetchCommentEmoteMapOnVideoLoad keeps first frame path lean`() {
        assertFalse(shouldFetchCommentEmoteMapOnVideoLoad())
    }

    @Test
    fun `shouldRefreshVipStatusOnVideoLoad keeps first frame path lean`() {
        assertFalse(shouldRefreshVipStatusOnVideoLoad())
    }

    @Test
    fun `shouldFetchInteractionStatusOnVideoLoad keeps first frame path lean`() {
        assertFalse(shouldFetchInteractionStatusOnVideoLoad())
    }
}
