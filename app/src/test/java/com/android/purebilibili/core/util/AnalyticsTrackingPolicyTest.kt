package com.android.purebilibili.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsTrackingPolicyTest {

    @Test
    fun normalizeAnalyticsDedupeToken_collapsesWhitespaceAndLimitsLength() {
        val normalized = normalizeAnalyticsDedupeToken("  screen   home  ")
        assertEquals("screen_home", normalized)

        val longInput = "x".repeat(120)
        assertEquals(80, normalizeAnalyticsDedupeToken(longInput).length)
    }

    @Test
    fun shouldSkipAnalyticsEvent_respectsMinInterval() {
        assertFalse(
            shouldSkipAnalyticsEvent(
                lastLoggedAtMs = null,
                nowElapsedMs = 1000L,
                minIntervalMs = 800L
            )
        )
        assertTrue(
            shouldSkipAnalyticsEvent(
                lastLoggedAtMs = 1500L,
                nowElapsedMs = 2000L,
                minIntervalMs = 800L
            )
        )
        assertFalse(
            shouldSkipAnalyticsEvent(
                lastLoggedAtMs = 1000L,
                nowElapsedMs = 2000L,
                minIntervalMs = 800L
            )
        )
    }
}
