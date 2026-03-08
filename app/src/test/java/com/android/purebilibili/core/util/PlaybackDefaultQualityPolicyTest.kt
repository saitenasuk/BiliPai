package com.android.purebilibili.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackDefaultQualityPolicyTest {

    @Test
    fun `non vip login should normalize 1080p60 default to 1080p`() {
        assertEquals(
            80,
            resolvePlayableDefaultQualityId(
                storedQuality = 116,
                isLoggedIn = true,
                isVip = false
            )
        )
    }

    @Test
    fun `guest should normalize 1080p60 default to 720p`() {
        assertEquals(
            64,
            resolvePlayableDefaultQualityId(
                storedQuality = 116,
                isLoggedIn = false,
                isVip = false
            )
        )
    }

    @Test
    fun `vip should keep 1080p60 default`() {
        assertEquals(
            116,
            resolvePlayableDefaultQualityId(
                storedQuality = 116,
                isLoggedIn = true,
                isVip = true
            )
        )
    }

    @Test
    fun `non vip login should normalize other vip-only tiers to 1080p`() {
        listOf(112, 120, 125, 126, 127).forEach { quality ->
            assertEquals(
                "quality=$quality",
                80,
                resolvePlayableDefaultQualityId(
                    storedQuality = quality,
                    isLoggedIn = true,
                    isVip = false
                )
            )
        }
    }
}
