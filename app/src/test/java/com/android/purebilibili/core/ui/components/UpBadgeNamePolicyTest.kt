package com.android.purebilibili.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpBadgeNamePolicyTest {

    @Test
    fun `resolveUpStatsText returns null when both stats missing`() {
        assertNull(resolveUpStatsText(followerCount = null, videoCount = null))
        assertNull(resolveUpStatsText(followerCount = 0, videoCount = 0))
    }

    @Test
    fun `resolveUpStatsText joins follower and video count when available`() {
        assertEquals(
            "粉丝 1200 · 视频 56",
            resolveUpStatsText(followerCount = 1200, videoCount = 56)
        )
    }

    @Test
    fun `resolveUpStatsText keeps available part when only one stat exists`() {
        assertEquals(
            "粉丝 328",
            resolveUpStatsText(followerCount = 328, videoCount = null)
        )
        assertEquals(
            "视频 9",
            resolveUpStatsText(followerCount = null, videoCount = 9)
        )
    }
}
