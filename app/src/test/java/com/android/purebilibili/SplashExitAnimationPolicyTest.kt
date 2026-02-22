package com.android.purebilibili

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashExitAnimationPolicyTest {

    @Test
    fun enablesRealtimeBlurOnAndroid12AndAbove() {
        assertTrue(shouldUseRealtimeSplashBlur(31))
        assertTrue(shouldUseRealtimeSplashBlur(34))
    }

    @Test
    fun disablesRealtimeBlurBelowAndroid12() {
        assertFalse(shouldUseRealtimeSplashBlur(30))
    }

    @Test
    fun disablesCustomSplashOverlayWhenFlyoutEnabled() {
        assertFalse(
            shouldShowCustomSplashOverlay(
                customSplashEnabled = true,
                splashUri = "content://splash.jpg",
                splashFlyoutEnabled = true
            )
        )
    }

    @Test
    fun allowsCustomSplashOverlayWhenFlyoutDisabledAndDataPresent() {
        assertTrue(
            shouldShowCustomSplashOverlay(
                customSplashEnabled = true,
                splashUri = "content://splash.jpg",
                splashFlyoutEnabled = false
            )
        )
    }
}
