package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VisualEffectTogglePolicyTest {

    @Test
    fun `enabling bottom bar blur disables liquid glass`() {
        val result = resolveBottomBarBlurToggleState(
            enableBottomBarBlur = true
        )

        assertTrue(result.bottomBarBlurEnabled)
        assertFalse(result.liquidGlassEnabled)
    }

    @Test
    fun `disabling bottom bar blur restores liquid glass to keep transparent style`() {
        val result = resolveBottomBarBlurToggleState(
            enableBottomBarBlur = false
        )

        assertFalse(result.bottomBarBlurEnabled)
        assertTrue(result.liquidGlassEnabled)
    }

    @Test
    fun `enabling liquid glass disables bottom bar blur`() {
        val result = resolveLiquidGlassToggleState(enableLiquidGlass = true)
        assertTrue(result.liquidGlassEnabled)
        assertFalse(result.bottomBarBlurEnabled)
    }

    @Test
    fun `disabling liquid glass enables bottom bar blur`() {
        val result = resolveLiquidGlassToggleState(enableLiquidGlass = false)
        assertFalse(result.liquidGlassEnabled)
        assertTrue(result.bottomBarBlurEnabled)
    }
}
