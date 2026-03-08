package com.android.purebilibili.data.repository

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchLoadPolicyTest {

    @Test
    fun `guest first page video search retries fallback when primary result is empty`() {
        assertTrue(
            shouldFallbackGuestVideoSearch(
                isLoggedIn = false,
                page = 1,
                primaryResultCount = 0
            )
        )
    }

    @Test
    fun `logged in users keep primary result even when empty`() {
        assertFalse(
            shouldFallbackGuestVideoSearch(
                isLoggedIn = true,
                page = 1,
                primaryResultCount = 0
            )
        )
    }

    @Test
    fun `guest later pages do not trigger fallback on empty result`() {
        assertFalse(
            shouldFallbackGuestVideoSearch(
                isLoggedIn = false,
                page = 2,
                primaryResultCount = 0
            )
        )
    }
}
