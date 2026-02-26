package com.android.purebilibili.feature.home.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BottomBarIndicatorPolicyTest {

    @Test
    fun `five or more items uses compact indicator geometry`() {
        val policy = resolveBottomBarIndicatorPolicy(itemCount = 5)

        assertTrue(policy.widthMultiplier < 1.42f)
        assertTrue(policy.minWidthDp < 104f)
        assertTrue(policy.maxWidthDp < 136f)
        assertEquals(true, policy.clampToBounds)
        assertTrue(policy.maxWidthToItemRatio < 1.42f)
    }

    @Test
    fun `five or more items keeps elongated ratio while remaining safe`() {
        val policy = resolveBottomBarIndicatorPolicy(itemCount = 5)

        assertTrue(policy.widthMultiplier >= 1.30f)
        assertTrue(policy.maxWidthToItemRatio >= 1.30f)
        assertTrue(policy.minWidthDp >= 88f)
    }

    @Test
    fun `four items keeps legacy geometry while clamping bounds`() {
        val policy = resolveBottomBarIndicatorPolicy(itemCount = 4)

        assertEquals(1.42f, policy.widthMultiplier)
        assertEquals(104f, policy.minWidthDp)
        assertEquals(136f, policy.maxWidthDp)
        assertEquals(true, policy.clampToBounds)
    }

    @Test
    fun `icon and text mode with five items uses flatter indicator height on phone`() {
        assertEquals(
            50f,
            resolveBottomIndicatorHeightDp(
                labelMode = 0,
                isTablet = false,
                itemCount = 5
            )
        )
    }
}
