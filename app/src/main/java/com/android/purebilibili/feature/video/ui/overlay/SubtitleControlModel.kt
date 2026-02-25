package com.android.purebilibili.feature.video.ui.overlay

import com.android.purebilibili.feature.video.subtitle.SubtitleDisplayMode

data class SubtitleControlUiState(
    val trackAvailable: Boolean = false,
    val primaryAvailable: Boolean = false,
    val secondaryAvailable: Boolean = false,
    val enabled: Boolean = true,
    val displayMode: SubtitleDisplayMode = SubtitleDisplayMode.OFF,
    val primaryLabel: String = "中文",
    val secondaryLabel: String = "英文",
    val largeTextEnabled: Boolean = false
)

data class SubtitleControlCallbacks(
    val onDisplayModeChange: (SubtitleDisplayMode) -> Unit = {},
    val onEnabledChange: (Boolean) -> Unit = {},
    val onLargeTextChange: (Boolean) -> Unit = {}
)
