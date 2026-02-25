package com.android.purebilibili.feature.home

fun resolveHomeTopTabsRevealDelayMs(
    isReturningFromDetail: Boolean,
    cardTransitionEnabled: Boolean,
    isQuickReturnFromDetail: Boolean
): Long {
    // 返回首页时顶部标签页全程可见，不再做延迟隐藏。
    return 0L
}

fun resolveHomeTopTabsVisible(
    isDelayedForCardSettle: Boolean,
    isForwardNavigatingToDetail: Boolean,
    isReturningFromDetail: Boolean
): Boolean {
    if (isReturningFromDetail) return true
    return !isDelayedForCardSettle && !isForwardNavigatingToDetail
}
