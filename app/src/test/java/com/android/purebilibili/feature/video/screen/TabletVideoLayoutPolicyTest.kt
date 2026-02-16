package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TabletVideoLayoutPolicyTest {

    @Test
    fun expandedTablet_usesDefaultTwoPaneRatio() {
        val policy = resolveTabletVideoLayoutPolicy(widthDp = 1280, isTv = false)

        assertEquals(0.65f, policy.primaryRatio)
        assertEquals(1000, policy.playerMaxWidthDp)
        assertEquals(980, policy.infoMaxWidthDp)
    }

    @Test
    fun ultraWideTablet_capsPlayerWidth_andSlightlyShrinksPrimaryPane() {
        val policy = resolveTabletVideoLayoutPolicy(widthDp = 1920, isTv = false)

        assertEquals(0.60f, policy.primaryRatio)
        assertTrue(policy.playerMaxWidthDp >= 1100)
        assertTrue(policy.infoMaxWidthDp >= 1080)
    }

    @Test
    fun tv_prefersStablePaneRatio_withWiderPlayerCap() {
        val policy = resolveTabletVideoLayoutPolicy(widthDp = 1920, isTv = true)

        assertEquals(0.62f, policy.primaryRatio)
        assertEquals(1180, policy.playerMaxWidthDp)
        assertEquals(1120, policy.infoMaxWidthDp)
    }
}
