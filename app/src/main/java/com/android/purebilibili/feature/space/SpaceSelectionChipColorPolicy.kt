package com.android.purebilibili.feature.space

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.android.purebilibili.core.theme.calculateContrastRatio

internal data class SpaceSelectionChipColors(
    val backgroundColor: Color,
    val textColor: Color
)

internal fun resolveSpaceSelectionChipColors(
    isSelected: Boolean,
    colorScheme: ColorScheme,
    unselectedAlpha: Float = 0.5f
): SpaceSelectionChipColors {
    if (!isSelected) {
        return SpaceSelectionChipColors(
            backgroundColor = colorScheme.surfaceVariant.copy(alpha = unselectedAlpha),
            textColor = colorScheme.onSurfaceVariant
        )
    }

    val primaryContrast = calculateContrastRatio(colorScheme.onPrimary, colorScheme.primary)
    val shouldAvoidPureBrightPrimary = colorScheme.surface.luminance() < 0.35f &&
        colorScheme.primary.luminance() > 0.88f
    val useContainerColors = shouldAvoidPureBrightPrimary || primaryContrast < 4.5f

    return if (useContainerColors) {
        SpaceSelectionChipColors(
            backgroundColor = colorScheme.primaryContainer,
            textColor = colorScheme.onPrimaryContainer
        )
    } else {
        SpaceSelectionChipColors(
            backgroundColor = colorScheme.primary,
            textColor = colorScheme.onPrimary
        )
    }
}
