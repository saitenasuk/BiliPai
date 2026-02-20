package com.android.purebilibili.feature.home

internal data class HomePerformanceConfig(
    val headerBlurEnabled: Boolean,
    val bottomBarBlurEnabled: Boolean,
    val liquidGlassEnabled: Boolean,
    val cardAnimationEnabled: Boolean,
    val cardTransitionEnabled: Boolean,
    val isDataSaverActive: Boolean,
    val preloadAheadCount: Int
)

internal fun resolveHomePerformanceConfig(
    headerBlurEnabled: Boolean,
    bottomBarBlurEnabled: Boolean,
    liquidGlassEnabled: Boolean,
    cardAnimationEnabled: Boolean,
    cardTransitionEnabled: Boolean,
    isDataSaverActive: Boolean,
    normalPreloadAheadCount: Int = 5
): HomePerformanceConfig {
    return HomePerformanceConfig(
        headerBlurEnabled = headerBlurEnabled,
        bottomBarBlurEnabled = bottomBarBlurEnabled,
        liquidGlassEnabled = liquidGlassEnabled,
        cardAnimationEnabled = cardAnimationEnabled,
        cardTransitionEnabled = cardTransitionEnabled,
        isDataSaverActive = isDataSaverActive,
        preloadAheadCount = if (isDataSaverActive) 0 else normalPreloadAheadCount.coerceAtLeast(0)
    )
}
