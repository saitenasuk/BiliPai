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
}
