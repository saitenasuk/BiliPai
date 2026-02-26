package com.android.purebilibili.feature.dynamic

internal fun shouldShowDynamicErrorOverlay(
    error: String?,
    activeItemsCount: Int
): Boolean {
    return !error.isNullOrBlank() && activeItemsCount == 0
}

internal fun shouldShowDynamicLoadingFooter(
    isLoading: Boolean,
    activeItemsCount: Int
): Boolean {
    return isLoading && activeItemsCount > 0
}

internal fun shouldShowDynamicNoMoreFooter(
    hasMore: Boolean,
    activeItemsCount: Int
): Boolean {
    return !hasMore && activeItemsCount > 0
}
