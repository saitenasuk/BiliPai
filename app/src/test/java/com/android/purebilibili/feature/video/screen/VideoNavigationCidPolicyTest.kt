package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.data.model.response.UgcEpisode
import com.android.purebilibili.data.model.response.UgcSeason
import com.android.purebilibili.data.model.response.UgcSection
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoNavigationCidPolicyTest {

    @Test
    fun `explicit cid has highest priority`() {
        val season = UgcSeason(
            sections = listOf(
                UgcSection(
                    episodes = listOf(
                        UgcEpisode(bvid = "BV1A", cid = 1001L)
                    )
                )
            )
        )

        val resolved = resolveNavigationTargetCid(
            targetBvid = "BV1A",
            explicitCid = 2002L,
            ugcSeason = season
        )

        assertEquals(2002L, resolved)
    }

    @Test
    fun `falls back to season episode cid when explicit cid missing`() {
        val season = UgcSeason(
            sections = listOf(
                UgcSection(
                    episodes = listOf(
                        UgcEpisode(bvid = "BV2B", cid = 3003L)
                    )
                )
            )
        )

        val resolved = resolveNavigationTargetCid(
            targetBvid = "BV2B",
            explicitCid = 0L,
            ugcSeason = season
        )

        assertEquals(3003L, resolved)
    }

    @Test
    fun `returns 0 when no cid can be resolved`() {
        val resolved = resolveNavigationTargetCid(
            targetBvid = "BV3C",
            explicitCid = 0L,
            ugcSeason = null
        )

        assertEquals(0L, resolved)
    }
}
