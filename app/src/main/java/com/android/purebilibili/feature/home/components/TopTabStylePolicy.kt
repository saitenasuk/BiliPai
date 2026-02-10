package com.android.purebilibili.feature.home.components

enum class TopTabMaterialMode {
    PLAIN,
    BLUR,
    LIQUID_GLASS
}

data class TopTabVisualState(
    val floating: Boolean,
    val materialMode: TopTabMaterialMode
)

fun resolveTopTabStyle(
    isBottomBarFloating: Boolean,
    isBottomBarBlurEnabled: Boolean,
    isLiquidGlassEnabled: Boolean
): TopTabVisualState {
    val materialMode = when {
        isBottomBarFloating && isLiquidGlassEnabled -> TopTabMaterialMode.LIQUID_GLASS
        isBottomBarBlurEnabled -> TopTabMaterialMode.BLUR
        else -> TopTabMaterialMode.PLAIN
    }

    return TopTabVisualState(
        floating = isBottomBarFloating,
        materialMode = materialMode
    )
}
