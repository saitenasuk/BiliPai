package com.android.purebilibili.data.repository

internal fun shouldFallbackGuestVideoSearch(
    isLoggedIn: Boolean,
    page: Int,
    primaryResultCount: Int
): Boolean {
    return !isLoggedIn && page == 1 && primaryResultCount == 0
}
