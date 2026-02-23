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
                hasCompletedOnboarding = false,
                hasAcceptedReleaseDisclaimer = false
            )
        )
        assertFalse(
            shouldEnableSplashFlyoutAnimation(
                hasCompletedOnboarding = false,
                hasAcceptedReleaseDisclaimer = true
            )
        )
        assertFalse(
            shouldEnableSplashFlyoutAnimation(
                hasCompletedOnboarding = true,
                hasAcceptedReleaseDisclaimer = false
            )
        )
        assertTrue(
            shouldEnableSplashFlyoutAnimation(
                hasCompletedOnboarding = true,
                hasAcceptedReleaseDisclaimer = true
            )
        )
    }
}
