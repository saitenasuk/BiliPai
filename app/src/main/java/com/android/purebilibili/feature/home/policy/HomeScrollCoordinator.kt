package com.android.purebilibili.feature.home.policy

import com.android.purebilibili.feature.home.resolveNextHomeGlobalScrollOffset

internal enum class BottomBarVisibilityIntent {
    SHOW,
    HIDE
}

internal data class HomeScrollUpdate(
    val headerOffsetPx: Float,
    val bottomBarVisibilityIntent: BottomBarVisibilityIntent?,
    val globalScrollOffset: Float?
)

internal fun shouldExpandHomeHeaderForSettledPage(
    currentHeaderOffsetPx: Float,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    nearTopScrollOffsetPx: Int = 50
): Boolean {
    if (currentHeaderOffsetPx >= 0f) return false
    if (firstVisibleItemIndex != 0) return false
    return firstVisibleItemScrollOffset <= nearTopScrollOffsetPx
}

internal fun resolveHomeHeaderOffsetForSettledPage(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    maxHeaderCollapsePx: Float
): Float {
    if (maxHeaderCollapsePx <= 0f) return 0f
    if (firstVisibleItemIndex > 0) return -maxHeaderCollapsePx
    val collapsedOffsetPx = firstVisibleItemScrollOffset.toFloat().coerceIn(0f, maxHeaderCollapsePx)
    return if (collapsedOffsetPx == 0f) 0f else -collapsedOffsetPx
}

internal fun reduceHomePreScroll(
    currentHeaderOffsetPx: Float,
    deltaY: Float,
    minHeaderOffsetPx: Float,
    isHeaderCollapseEnabled: Boolean,
    isBottomBarAutoHideEnabled: Boolean,
    useSideNavigation: Boolean,
    liquidGlassEnabled: Boolean,
    currentGlobalScrollOffset: Float,
    bottomBarVisibilityThresholdPx: Float = 10f
): HomeScrollUpdate {
    val nextHeaderOffset = if (isHeaderCollapseEnabled) {
        (currentHeaderOffsetPx + deltaY).coerceIn(minHeaderOffsetPx, 0f)
    } else {
        0f
    }

    val nextBottomBarIntent = when {
        !isBottomBarAutoHideEnabled || useSideNavigation -> null
        deltaY <= -bottomBarVisibilityThresholdPx -> BottomBarVisibilityIntent.HIDE
        deltaY >= bottomBarVisibilityThresholdPx -> BottomBarVisibilityIntent.SHOW
        else -> null
    }

    return HomeScrollUpdate(
        headerOffsetPx = nextHeaderOffset,
        bottomBarVisibilityIntent = nextBottomBarIntent,
        globalScrollOffset = resolveNextHomeGlobalScrollOffset(
            currentOffset = currentGlobalScrollOffset,
            scrollDeltaY = deltaY,
            liquidGlassEnabled = liquidGlassEnabled
        )
    )
}
