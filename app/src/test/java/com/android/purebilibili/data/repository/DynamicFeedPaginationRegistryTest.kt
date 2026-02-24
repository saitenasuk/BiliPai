package com.android.purebilibili.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicFeedPaginationRegistryTest {

    @Test
    fun states_areIsolatedBetweenScopes() {
        val registry = DynamicFeedPaginationRegistry()

        registry.update(
            scope = DynamicFeedScope.HOME_FOLLOW,
            offset = "home_offset",
            hasMore = false
        )
        registry.update(
            scope = DynamicFeedScope.DYNAMIC_SCREEN,
            offset = "dynamic_offset",
            hasMore = true
        )

        assertEquals("home_offset", registry.offset(DynamicFeedScope.HOME_FOLLOW))
        assertEquals("dynamic_offset", registry.offset(DynamicFeedScope.DYNAMIC_SCREEN))
        assertFalse(registry.hasMore(DynamicFeedScope.HOME_FOLLOW))
        assertTrue(registry.hasMore(DynamicFeedScope.DYNAMIC_SCREEN))
    }

    @Test
    fun reset_onlyAffectsTargetScope() {
        val registry = DynamicFeedPaginationRegistry()
        registry.update(DynamicFeedScope.HOME_FOLLOW, "home_offset", hasMore = false)
        registry.update(DynamicFeedScope.DYNAMIC_SCREEN, "dynamic_offset", hasMore = false)

        registry.reset(DynamicFeedScope.HOME_FOLLOW)

        assertEquals("", registry.offset(DynamicFeedScope.HOME_FOLLOW))
        assertTrue(registry.hasMore(DynamicFeedScope.HOME_FOLLOW))
        assertEquals("dynamic_offset", registry.offset(DynamicFeedScope.DYNAMIC_SCREEN))
        assertFalse(registry.hasMore(DynamicFeedScope.DYNAMIC_SCREEN))
    }
}

