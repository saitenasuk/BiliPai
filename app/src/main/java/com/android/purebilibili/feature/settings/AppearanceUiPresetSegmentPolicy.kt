package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.UiPreset

internal fun resolveUiPresetSegmentOptions(): List<PlaybackSegmentOption<UiPreset>> {
    return listOf(
        PlaybackSegmentOption(UiPreset.IOS, UiPreset.IOS.label),
        PlaybackSegmentOption(UiPreset.MD3, UiPreset.MD3.label)
    )
}
