package com.android.purebilibili.feature.dynamic

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class DynamicVideoCardLayoutMode {
    VERTICAL,
    HORIZONTAL
}

internal fun resolveDynamicFeedMaxWidth(): Dp = 700.dp

internal fun resolveDynamicVideoCardLayoutMode(containerWidthDp: Int): DynamicVideoCardLayoutMode {
    return if (containerWidthDp >= 620) {
        DynamicVideoCardLayoutMode.HORIZONTAL
    } else {
        DynamicVideoCardLayoutMode.VERTICAL
    }
}

internal fun resolveDynamicHorizontalUserListHorizontalPadding(): Dp = 10.dp

internal fun resolveDynamicHorizontalUserListSpacing(): Dp = 10.dp

internal fun resolveDynamicTopBarHorizontalPadding(): Dp = 14.dp

internal fun resolveDynamicTopBarTabEndPadding(): Dp = 20.dp

internal fun resolveDynamicSidebarWidth(isExpanded: Boolean): Dp {
    return if (isExpanded) 68.dp else 60.dp
}

internal fun resolveDynamicCardOuterPadding(): Dp = 10.dp

internal fun resolveDynamicCardContentPadding(): Dp = 14.dp
