package com.android.purebilibili.feature.home

import kotlin.math.min
import kotlin.math.max

internal fun shouldResetToTopOnRefreshStart(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int
): Boolean {
    return firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0
}

internal fun shouldResetToTopAfterIncrementalRefresh(
    currentCategory: HomeCategory,
    newItemsCount: Int?,
    isRefreshing: Boolean,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int
): Boolean {
    if (currentCategory != HomeCategory.RECOMMEND) return false
    if ((newItemsCount ?: 0) <= 0) return false
    if (isRefreshing) return false
    return shouldResetToTopOnRefreshStart(
        firstVisibleItemIndex = firstVisibleItemIndex,
        firstVisibleItemScrollOffset = firstVisibleItemScrollOffset
    )
}

internal fun shouldShowReleaseToRefreshHint(
    progress: Float,
    isRefreshing: Boolean,
    isStateAnimating: Boolean
): Boolean {
    if (isRefreshing) return false
    if (progress < 1f) return false
    return !isStateAnimating
}

internal fun resolvePullRefreshHintText(
    progress: Float,
    isRefreshing: Boolean,
    isStateAnimating: Boolean
): String {
    return when {
        isRefreshing -> "正在刷新..."
        shouldShowReleaseToRefreshHint(
            progress = progress,
            isRefreshing = isRefreshing,
            isStateAnimating = isStateAnimating
        ) -> "松手刷新"
        progress > 0f -> "下拉刷新..."
        else -> ""
    }
}

internal fun resolvePullIndicatorTranslationY(
    dragOffsetPx: Float,
    indicatorHeightPx: Float,
    minGapPx: Float,
    isRefreshing: Boolean
): Float {
    if (isRefreshing) return 0f
    if (dragOffsetPx <= 0f) return -indicatorHeightPx
    val centeredY = (dragOffsetPx / 2f) - (indicatorHeightPx / 2f)
    val maxAllowedY = dragOffsetPx - indicatorHeightPx - minGapPx
    return min(centeredY, maxAllowedY)
}

internal fun resolvePullContentOffsetFraction(
    distanceFraction: Float,
    isRefreshing: Boolean
): Float {
    val clampedDistance = distanceFraction.coerceAtMost(2f).coerceAtLeast(0f)
    return if (isRefreshing) {
        max(clampedDistance, 1f) * 0.5f
    } else {
        clampedDistance * 0.5f
    }
}
