package com.android.purebilibili.navigation

internal enum class VideoPopExitDirection {
    LEFT,
    RIGHT,
    DOWN
}

internal fun shouldUseTabletSeamlessBackTransition(
    isTabletLayout: Boolean,
    cardTransitionEnabled: Boolean,
    fromRoute: String?,
    toRoute: String?
): Boolean {
    return isTabletLayout &&
        cardTransitionEnabled &&
        isVideoDetailRoute(fromRoute) &&
        toRoute == ScreenRoutes.Home.route
}

internal fun shouldStopPlaybackEagerlyOnVideoRouteExit(
    fromRoute: String?,
    toRoute: String?
): Boolean {
    return isVideoDetailRoute(fromRoute) &&
        !isVideoDetailRoute(toRoute) &&
        toRoute != ScreenRoutes.AudioMode.route
}

internal fun resolveVideoPopExitDirection(
    targetRoute: String?,
    isSingleColumnCard: Boolean,
    lastClickedCardCenterX: Float?
): VideoPopExitDirection {
    val isCardOnLeft = (lastClickedCardCenterX ?: 0.5f) < 0.5f
    if (targetRoute == ScreenRoutes.Home.route) {
        return if (isCardOnLeft) VideoPopExitDirection.LEFT else VideoPopExitDirection.RIGHT
    }
    if (isSingleColumnCard) return VideoPopExitDirection.DOWN
    return if (isCardOnLeft) VideoPopExitDirection.LEFT else VideoPopExitDirection.RIGHT
}

private fun isVideoDetailRoute(route: String?): Boolean {
    return route?.startsWith("${VideoRoute.base}/") == true
}
