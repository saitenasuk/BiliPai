package com.android.purebilibili

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupSplashPolicyTest {

    @Test
    fun readsCustomSplashPreferencesOnlyWhenFlyoutDisabled() {
        assertFalse(shouldReadCustomSplashPreferences(splashFlyoutEnabled = true))
        assertTrue(shouldReadCustomSplashPreferences(splashFlyoutEnabled = false))
    }

    @Test
    fun doesNotStartLocalProxyDuringColdStartByDefault() {
        assertFalse(shouldStartLocalProxyOnAppLaunch())
    }

    @Test
    fun enablesSplashFlyoutOnlyAfterStartupPrivacyFlowCompleted() {
        assertFalse(
            shouldEnableSplashFlyoutAnimation(
                sdkInt = 30,
                hasCompletedOnboarding = false,
                hasAcceptedReleaseDisclaimer = false
            )
        )
        assertFalse(
            shouldEnableSplashFlyoutAnimation(
                sdkInt = 30,
                hasCompletedOnboarding = false,
                hasAcceptedReleaseDisclaimer = true
            )
        )
        assertFalse(
            shouldEnableSplashFlyoutAnimation(
                sdkInt = 30,
                hasCompletedOnboarding = true,
                hasAcceptedReleaseDisclaimer = false
            )
        )
        assertFalse(
            shouldEnableSplashFlyoutAnimation(
                sdkInt = 30,
                hasCompletedOnboarding = true,
                hasAcceptedReleaseDisclaimer = true
            )
        )
        assertTrue(
            shouldEnableSplashFlyoutAnimation(
                sdkInt = 31,
                hasCompletedOnboarding = true,
                hasAcceptedReleaseDisclaimer = true
            )
        )
    }
}
