package com.android.purebilibili.feature.home

internal fun shouldHandleRefreshNewItemsEvent(
    refreshKey: Long,
    handledKey: Long
): Boolean {
    if (refreshKey <= 0L) return false
    return refreshKey > handledKey
}

internal fun shouldShowRecommendOldContentDivider(
    currentCategory: HomeCategory,
    refreshNewItemsKey: Long,
    revealedRefreshKey: Long,
    anchorBvid: String?,
    oldContentStartIndex: Int?
): Boolean {
    if (currentCategory != HomeCategory.RECOMMEND) return false
    if (refreshNewItemsKey <= 0L || revealedRefreshKey != refreshNewItemsKey) return false
    return !anchorBvid.isNullOrBlank() || (oldContentStartIndex != null && oldContentStartIndex > 0)
}
