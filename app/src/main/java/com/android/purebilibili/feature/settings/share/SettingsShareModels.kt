package com.android.purebilibili.feature.settings.share

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

const val SETTINGS_SHARE_SCHEMA_VERSION = 1

@Serializable
data class SettingsShareProfile(
    val schemaVersion: Int = SETTINGS_SHARE_SCHEMA_VERSION,
    val app: String = "BiliPai",
    val appVersion: String,
    val exportedAtIso: String,
    val profileName: String,
    val sections: SettingsShareSections = SettingsShareSections()
)

@Serializable
data class SettingsShareSections(
    val appearance: Map<String, JsonElement> = emptyMap(),
    val playback: Map<String, JsonElement> = emptyMap(),
    val gesture: Map<String, JsonElement> = emptyMap(),
    val danmaku: Map<String, JsonElement> = emptyMap(),
    val navigation: Map<String, JsonElement> = emptyMap()
)

enum class SettingsShareSection(val wireKey: String, val label: String) {
    APPEARANCE("appearance", "外观"),
    PLAYBACK("playback", "播放"),
    GESTURE("gesture", "手势"),
    DANMAKU("danmaku", "弹幕"),
    NAVIGATION("navigation", "导航");
}

data class SettingsShareEntryDefinition(
    val storageKey: String,
    val section: SettingsShareSection
)

data class SettingsShareImportPreview(
    val profileName: String,
    val importableSections: List<SettingsShareSection>,
    val skippedKeys: List<String>
)

data class SettingsShareApplyResult(
    val appliedKeys: List<String>,
    val skippedKeys: List<String>
)

data class SettingsShareExportArtifact(
    val fileName: String,
    val json: String,
    val profile: SettingsShareProfile
)

data class SettingsShareImportSession(
    val profile: SettingsShareProfile,
    val preview: SettingsShareImportPreview,
    val rawJson: String
)
