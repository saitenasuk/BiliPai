package com.android.purebilibili.feature.dynamic

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicScreenStatePolicyTest {

    @Test
    fun `error overlay should show when active list is empty and error exists`() {
        assertTrue(
            shouldShowDynamicErrorOverlay(
                error = "加载失败",
                activeItemsCount = 0
            )
        )
    }

    @Test
    fun `error overlay should hide when active list has data`() {
        assertFalse(
            shouldShowDynamicErrorOverlay(
                error = "加载失败",
                activeItemsCount = 3
            )
        )
    }

    @Test
    fun `loading footer should follow active list size`() {
        assertTrue(shouldShowDynamicLoadingFooter(isLoading = true, activeItemsCount = 1))
        assertFalse(shouldShowDynamicLoadingFooter(isLoading = true, activeItemsCount = 0))
        assertFalse(shouldShowDynamicLoadingFooter(isLoading = false, activeItemsCount = 2))
    }

    @Test
    fun `no more footer should follow active hasMore and list size`() {
        assertTrue(shouldShowDynamicNoMoreFooter(hasMore = false, activeItemsCount = 1))
        assertFalse(shouldShowDynamicNoMoreFooter(hasMore = true, activeItemsCount = 1))
        assertFalse(shouldShowDynamicNoMoreFooter(hasMore = false, activeItemsCount = 0))
    }
}
