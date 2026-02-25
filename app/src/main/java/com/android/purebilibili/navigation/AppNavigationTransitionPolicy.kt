package com.android.purebilibili.navigation

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

private fun isVideoDetailRoute(route: String?): Boolean {
    return route?.startsWith("${VideoRoute.base}/") == true
}
