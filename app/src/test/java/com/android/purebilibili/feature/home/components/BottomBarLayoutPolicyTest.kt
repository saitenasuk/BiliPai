package com.android.purebilibili.feature.home.components

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BottomBarLayoutPolicyTest {

    @Test
    fun `floating five items keeps compact width with safe per-item size`() {
        val policy = resolveBottomBarLayoutPolicy(
            containerWidth = 393.dp,
            itemCount = 5,
            isTablet = false,
            labelMode = 0,
            isFloating = true
        )

        val perItemWidth = (policy.maxBarWidth - (policy.rowPadding * 2)) / 5
        assertTrue(policy.maxBarWidth.value < 340f)
        assertTrue(policy.horizontalPadding.value > 24f)
        assertTrue(perItemWidth.value >= 52f)
    }

    @Test
    fun `floating four items can use wider bar than five items`() {
        val policyForFour = resolveBottomBarLayoutPolicy(
            containerWidth = 393.dp,
            itemCount = 4,
            isTablet = false,
            labelMode = 0,
            isFloating = true
        )
        val policyForFive = resolveBottomBarLayoutPolicy(
            containerWidth = 393.dp,
            itemCount = 5,
            isTablet = false,
            labelMode = 0,
            isFloating = true
        )

        assertTrue(policyForFour.maxBarWidth.value > policyForFive.maxBarWidth.value)
    }

    @Test
    fun `docked mode stays full width with no horizontal inset`() {
        val policy = resolveBottomBarLayoutPolicy(
            containerWidth = 393.dp,
            itemCount = 5,
            isTablet = false,
            labelMode = 0,
            isFloating = false
        )

        assertEquals(0.dp, policy.horizontalPadding)
        assertEquals(393.dp, policy.maxBarWidth)
    }
}
