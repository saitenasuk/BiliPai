package com.android.purebilibili.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class BilibiliNavigationTargetParserTest {

    @Test
    fun parse_wrappedSpaceUrl_resolvesSpaceTarget() {
        val target = BilibiliNavigationTargetParser.parse(
            "bilibili://browser?url=https%3A%2F%2Fspace.bilibili.com%2F123456"
        )

        assertIs<BilibiliNavigationTarget.Space>(target)
        assertEquals(123456L, target.mid)
    }

    @Test
    fun parse_wrappedLiveUrl_resolvesLiveTarget() {
        val target = BilibiliNavigationTargetParser.parse(
            "bilibili://browser?url=https%3A%2F%2Flive.bilibili.com%2F456789"
        )

        assertIs<BilibiliNavigationTarget.Live>(target)
        assertEquals(456789L, target.roomId)
    }

    @Test
    fun parse_bangumiSeasonUrl_resolvesSeasonTarget() {
        val target = BilibiliNavigationTargetParser.parse(
            "https://www.bilibili.com/bangumi/play/ss39708"
        )

        assertIs<BilibiliNavigationTarget.BangumiSeason>(target)
        assertEquals(39708L, target.seasonId)
    }

    @Test
    fun parse_followingDetailDeepLink_resolvesDynamicTarget() {
        val target = BilibiliNavigationTargetParser.parse(
            "bilibili://following/detail/1015637114125025318"
        )

        assertIs<BilibiliNavigationTarget.Dynamic>(target)
        assertEquals("1015637114125025318", target.dynamicId)
    }

    @Test
    fun parse_nonBilibiliUrl_returnsNull() {
        val target = BilibiliNavigationTargetParser.parse("https://example.com/video/1")

        assertNull(target)
    }
}
