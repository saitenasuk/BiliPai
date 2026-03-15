package com.android.purebilibili.feature.dynamic

internal fun resolveDynamicListTopPaddingExtraDp(isHorizontalMode: Boolean): Int {
    return if (isHorizontalMode) 168 else 100
}

internal fun resolveDynamicSelectedUserIdAfterClick(
    selectedUserId: Long?,
    clickedUserId: Long?
): Long? {
    if (clickedUserId == null) return null
    return if (selectedUserId == clickedUserId) null else clickedUserId
}

internal fun resolveHorizontalUserListVerticalPaddingDp(): Int {
    return 4
}

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

internal fun shouldShowDynamicCommentSheet(selectedDynamicId: String?): Boolean {
    return !selectedDynamicId.isNullOrBlank()
}

internal fun resolveDynamicCommentSheetTotalCount(
    liveCount: Int,
    fallbackCount: Int
): Int {
    return if (liveCount > 0) liveCount else fallbackCount.coerceAtLeast(0)
}

internal fun shouldResetFollowedUserListToTopOnRefresh(
    boundaryKey: String?,
    prependedCount: Int,
    selectedUserId: Long?,
    handledBoundaryKey: String?
): Boolean {
    if (boundaryKey.isNullOrBlank()) return false
    if (prependedCount <= 0) return false
    if (selectedUserId != null) return false
    return boundaryKey != handledBoundaryKey
}
