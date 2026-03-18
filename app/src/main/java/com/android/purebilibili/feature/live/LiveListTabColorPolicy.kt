package com.android.purebilibili.feature.live

import androidx.compose.ui.graphics.Color

internal data class LiveListTabColors(
    val selectedContainerColor: Color,
    val selectedContentColor: Color,
    val unselectedContainerColor: Color,
    val unselectedContentColor: Color
)

internal fun resolveLiveListTabColors(
    primary: Color,
    onPrimary: Color,
    surfaceVariant: Color,
    onSurfaceVariant: Color
): LiveListTabColors {
    return LiveListTabColors(
        selectedContainerColor = primary,
        selectedContentColor = onPrimary,
        unselectedContainerColor = surfaceVariant,
        unselectedContentColor = onSurfaceVariant
    )
}
