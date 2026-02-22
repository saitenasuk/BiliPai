package com.android.purebilibili.data.repository

import com.android.purebilibili.core.store.SettingsManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePreloadPolicyTest {

    @Test
    fun primesBuvidOnlyForMobileFeedApi() {
        assertFalse(shouldPrimeBuvidForHomePreload(SettingsManager.FeedApiType.WEB))
        assertTrue(shouldPrimeBuvidForHomePreload(SettingsManager.FeedApiType.MOBILE))
    }

    @Test
    fun reusesInFlightPreloadOnlyForInitialHomeRequest() {
        assertTrue(
            shouldReuseInFlightPreloadForHomeRequest(
                idx = 0,
                isPreloading = true,
                hasPreloadedData = false
            )
        )
        assertFalse(
            shouldReuseInFlightPreloadForHomeRequest(
                idx = 1,
                isPreloading = true,
                hasPreloadedData = false
            )
        )
        assertFalse(
            shouldReuseInFlightPreloadForHomeRequest(
                idx = 0,
                isPreloading = false,
                hasPreloadedData = false
            )
        )
        assertFalse(
            shouldReuseInFlightPreloadForHomeRequest(
                idx = 0,
                isPreloading = true,
                hasPreloadedData = true
            )
        )
    }

    @Test
    fun reportsHomeDataReadyAfterPreloadCompletesEvenIfCacheWasConsumed() {
        assertTrue(
            shouldReportHomeDataReadyForSplash(
                hasCompletedPreload = true,
                hasPreloadedData = false
            )
        )
        assertTrue(
            shouldReportHomeDataReadyForSplash(
                hasCompletedPreload = false,
                hasPreloadedData = true
            )
        )
        assertFalse(
            shouldReportHomeDataReadyForSplash(
                hasCompletedPreload = false,
                hasPreloadedData = false
            )
        )
    }
}
