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

internal fun resolveHomePreloadAheadCount(
    isDataSaverActive: Boolean,
    normalPreloadAheadCount: Int
): Int {
    if (isDataSaverActive) return 0
    return normalPreloadAheadCount.coerceAtLeast(0).coerceAtMost(3)
}

internal fun resolveHomePerformanceConfig(
    headerBlurEnabled: Boolean,
    bottomBarBlurEnabled: Boolean,
    liquidGlassEnabled: Boolean,
    cardAnimationEnabled: Boolean,
    cardTransitionEnabled: Boolean,
    isDataSaverActive: Boolean,
    smartVisualGuardEnabled: Boolean,
    normalPreloadAheadCount: Int = 5
): HomePerformanceConfig {
    // Feature retired: keep parameter for compatibility, but never apply runtime smoothness downgrade.
    val shouldPrioritizeSmoothness = false
    val effectiveDataSaver = isDataSaverActive
    val effectiveLiquidGlass = liquidGlassEnabled && !shouldPrioritizeSmoothness
    val effectivePreloadAheadCount = when {
        shouldPrioritizeSmoothness -> normalPreloadAheadCount.coerceAtLeast(0).coerceAtMost(2)
        else -> resolveHomePreloadAheadCount(
            isDataSaverActive = effectiveDataSaver,
            normalPreloadAheadCount = normalPreloadAheadCount
        )
    }

    return HomePerformanceConfig(
        headerBlurEnabled = headerBlurEnabled,
        bottomBarBlurEnabled = bottomBarBlurEnabled,
        liquidGlassEnabled = effectiveLiquidGlass,
        cardAnimationEnabled = cardAnimationEnabled,
        cardTransitionEnabled = cardTransitionEnabled,
        isDataSaverActive = effectiveDataSaver,
        preloadAheadCount = effectivePreloadAheadCount
    )
}
