package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.UiPreset

data class AppearanceUiPresetDescription(
    val title: String,
    val summary: String
)

internal fun resolveAppearanceUiPresetDescription(
    preset: UiPreset,
    iosTitle: String,
    iosSummary: String,
    androidTitle: String,
    androidSummary: String
): AppearanceUiPresetDescription {
    return when (preset) {
        UiPreset.IOS -> AppearanceUiPresetDescription(
            title = iosTitle,
            summary = iosSummary
        )

        UiPreset.MD3 -> AppearanceUiPresetDescription(
            title = androidTitle,
            summary = androidSummary
        )
    }
}
