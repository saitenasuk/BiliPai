package com.android.purebilibili.core.ui.blur

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecoverableVisualEffectsPolicyTest {

    @Test
    fun enablesHeavyVisualEffectsOnlyWhenForegroundAndUserEnabled() {
        assertTrue(
            shouldEnableRecoverableHeavyVisualEffects(
                userEnabled = true,
                isAppInBackground = false
            )
        )
        assertFalse(
            shouldEnableRecoverableHeavyVisualEffects(
                userEnabled = true,
                isAppInBackground = true
            )
        )
        assertFalse(
            shouldEnableRecoverableHeavyVisualEffects(
                userEnabled = false,
                isAppInBackground = false
            )
        )
    }

    @Test
    fun recreatesRecoverableHazeStateOnAndroid16AndAbove() {
        assertTrue(
            shouldRecreateRecoverableHazeState(
                sdkInt = 36
            )
        )
        assertFalse(
            shouldRecreateRecoverableHazeState(
                sdkInt = 35
            )
        )
    }

    @Test
    fun directHazeLiquidGlassFallbackIsDisabledOnAndroid16AndAbove() {
        assertFalse(
            shouldAllowDirectHazeLiquidGlassFallback(
                sdkInt = 36
            )
        )
        assertTrue(
            shouldAllowDirectHazeLiquidGlassFallback(
                sdkInt = 35
            )
        )
    }

    @Test
    fun homeChromeLiquidGlassIsDisabledOnAndroid13AndEnabledOnAndroid14Plus() {
        assertFalse(
            shouldAllowHomeChromeLiquidGlass(
                sdkInt = 33
            )
        )
        assertTrue(
            shouldAllowHomeChromeLiquidGlass(
                sdkInt = 34
            )
        )
    }
}
