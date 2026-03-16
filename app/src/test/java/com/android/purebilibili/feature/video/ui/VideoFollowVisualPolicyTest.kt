package com.android.purebilibili.feature.video.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VideoFollowVisualPolicyTest {

    @Test
    fun `followed state uses themed secondary accent instead of neutral styling`() {
        val policy = resolveVideoFollowVisualPolicy(isFollowing = true)

        assertEquals(FollowButtonTone.PRIMARY_CONTAINER, policy.detailButtonTone)
        assertEquals(FollowTextTone.ON_PRIMARY_CONTAINER, policy.detailTextTone)
        assertEquals(FollowBadgeTone.PRIMARY, policy.relatedBadgeTone)
    }

    @Test
    fun `unfollowed state keeps primary call to action styling`() {
        val policy = resolveVideoFollowVisualPolicy(isFollowing = false)

        assertEquals(FollowButtonTone.PRIMARY, policy.detailButtonTone)
        assertEquals(FollowTextTone.ON_PRIMARY, policy.detailTextTone)
        assertNull(policy.relatedBadgeTone)
    }
}
