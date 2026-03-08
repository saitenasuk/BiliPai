package com.android.purebilibili.feature.settings.share

import kotlinx.serialization.json.JsonElement
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SETTINGS_SHARE_FILE_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.US)

internal fun flattenSettingsShareSections(
    sections: SettingsShareSections
): Map<String, JsonElement> {
    return buildMap {
        putAll(sections.appearance)
        putAll(sections.playback)
        putAll(sections.gesture)
        putAll(sections.danmaku)
        putAll(sections.navigation)
    }
}

fun buildSettingsShareProfile(
    profileName: String,
    appVersion: String,
    exportedAtIso: String,
    rawSettings: Map<String, JsonElement>,
    definitions: List<SettingsShareEntryDefinition>
): SettingsShareProfile {
    val byKey = definitions.associateBy { it.storageKey }
    val appearance = linkedMapOf<String, JsonElement>()
    val playback = linkedMapOf<String, JsonElement>()
    val gesture = linkedMapOf<String, JsonElement>()
    val danmaku = linkedMapOf<String, JsonElement>()
    val navigation = linkedMapOf<String, JsonElement>()

    rawSettings.forEach { (key, value) ->
        when (byKey[key]?.section) {
            SettingsShareSection.APPEARANCE -> appearance[key] = value
            SettingsShareSection.PLAYBACK -> playback[key] = value
            SettingsShareSection.GESTURE -> gesture[key] = value
            SettingsShareSection.DANMAKU -> danmaku[key] = value
            SettingsShareSection.NAVIGATION -> navigation[key] = value
            null -> Unit
        }
    }

    return SettingsShareProfile(
        appVersion = appVersion,
        exportedAtIso = exportedAtIso,
        profileName = profileName,
        sections = SettingsShareSections(
            appearance = appearance,
            playback = playback,
            gesture = gesture,
            danmaku = danmaku,
            navigation = navigation
        )
    )
}

fun resolveSettingsShareImportPreview(
    profile: SettingsShareProfile,
    definitions: List<SettingsShareEntryDefinition>
): SettingsShareImportPreview {
    val importableKeys = definitions.mapTo(linkedSetOf()) { it.storageKey }
    val allKeys = flattenSettingsShareSections(profile.sections).keys
    val importableSections = definitions
        .filter { definition -> allKeys.contains(definition.storageKey) }
        .map { it.section }
        .distinct()
    val skippedKeys = allKeys
        .filterNot { importableKeys.contains(it) }
        .sorted()

    return SettingsShareImportPreview(
        profileName = profile.profileName,
        importableSections = importableSections,
        skippedKeys = skippedKeys
    )
}

fun buildSettingsShareFileName(
    appVersion: String,
    epochMs: Long
): String {
    val timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneOffset.UTC)
    return "bilipai-settings-$appVersion-${timestamp.format(SETTINGS_SHARE_FILE_TIME_FORMATTER)}.json"
}
