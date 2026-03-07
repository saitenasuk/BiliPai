package com.android.purebilibili.feature.home.components

enum class HomeInteractionMotionBudget {
    FULL,
    REDUCED
}

internal fun resolveHomeInteractionMotionBudget(
    isPagerScrolling: Boolean,
    isProgrammaticPageSwitchInProgress: Boolean,
    isFeedScrolling: Boolean
): HomeInteractionMotionBudget {
    return if (isPagerScrolling || isProgrammaticPageSwitchInProgress || isFeedScrolling) {
        HomeInteractionMotionBudget.REDUCED
    } else {
        HomeInteractionMotionBudget.FULL
    }
}

internal fun shouldAnimateTopTabAutoScroll(
    selectedIndex: Int,
    firstVisibleIndex: Int,
    lastVisibleIndex: Int,
    budget: HomeInteractionMotionBudget
): Boolean {
    if (firstVisibleIndex > lastVisibleIndex) return true
    if (budget == HomeInteractionMotionBudget.REDUCED) {
        return selectedIndex < firstVisibleIndex || selectedIndex > lastVisibleIndex
    }
    return true
}

internal fun shouldSnapHomeTopTabSelection(
    currentPage: Int,
    targetPage: Int
): Boolean = currentPage != targetPage
