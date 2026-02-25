package com.android.purebilibili.feature.video.subtitle

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val content: String
)

data class SubtitleTrackMeta(
    val lan: String,
    val lanDoc: String,
    val subtitleUrl: String
)

data class SubtitleLanguageSelection(
    val primaryLanguage: String?,
    val secondaryLanguage: String?
)

enum class SubtitleDisplayMode {
    OFF,
    PRIMARY_ONLY,
    SECONDARY_ONLY,
    BILINGUAL
}

data class SubtitleDisplayOption(
    val mode: SubtitleDisplayMode,
    val label: String,
    val enabled: Boolean
)

private val SUBTITLE_JSON = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

fun normalizeBilibiliSubtitleUrl(raw: String): String {
    val trimmed = raw.trim()
    return when {
        trimmed.isEmpty() -> ""
        trimmed.startsWith("//") -> "https:$trimmed"
        trimmed.startsWith("http://") -> "https://${trimmed.removePrefix("http://")}"
        else -> trimmed
    }
}

fun parseBiliSubtitleBody(rawJson: String): List<SubtitleCue> {
    if (rawJson.isBlank()) return emptyList()
    return try {
        val root = SUBTITLE_JSON.parseToJsonElement(rawJson).jsonObject
        val body = root["body"]?.asJsonArrayOrNull().orEmpty()
        body.mapNotNull { item ->
            val obj = item.asJsonObjectOrNull() ?: return@mapNotNull null
            val fromSeconds = obj["from"].asDoubleOrNull() ?: return@mapNotNull null
            val toSeconds = obj["to"].asDoubleOrNull() ?: return@mapNotNull null
            val content = obj["content"]?.jsonPrimitive?.content?.trim().orEmpty()
            if (content.isBlank()) return@mapNotNull null
            val startMs = (fromSeconds * 1000.0).toLong().coerceAtLeast(0L)
            val endMs = (toSeconds * 1000.0).toLong().coerceAtLeast(startMs)
            SubtitleCue(
                startMs = startMs,
                endMs = endMs,
                content = content
            )
        }.sortedBy { cue -> cue.startMs }
    } catch (_: Throwable) {
        emptyList()
    }
}

fun resolveDefaultSubtitleLanguages(tracks: List<SubtitleTrackMeta>): SubtitleLanguageSelection {
    if (tracks.isEmpty()) {
        return SubtitleLanguageSelection(
            primaryLanguage = null,
            secondaryLanguage = null
        )
    }

    val primary = tracks.firstOrNull { track ->
        track.lan.equals("zh-Hans", ignoreCase = true)
    } ?: tracks.firstOrNull { track ->
        track.lan.equals("zh-CN", ignoreCase = true)
    } ?: tracks.firstOrNull { track ->
        track.lan.startsWith("zh", ignoreCase = true)
    } ?: tracks.first()

    val secondary = tracks.firstOrNull { track ->
        track.lan.equals("en-US", ignoreCase = true)
    } ?: tracks.firstOrNull { track ->
        track.lan.equals("en-GB", ignoreCase = true)
    } ?: tracks.firstOrNull { track ->
        track.lan.startsWith("en", ignoreCase = true)
    } ?: tracks.firstOrNull { track ->
        track.lan != primary.lan
    }

    return SubtitleLanguageSelection(
        primaryLanguage = primary.lan,
        secondaryLanguage = secondary?.lan?.takeIf { it != primary.lan }
    )
}

fun resolveDefaultSubtitleDisplayMode(
    hasPrimaryTrack: Boolean,
    hasSecondaryTrack: Boolean
): SubtitleDisplayMode = when {
    hasPrimaryTrack && hasSecondaryTrack -> SubtitleDisplayMode.BILINGUAL
    hasPrimaryTrack -> SubtitleDisplayMode.PRIMARY_ONLY
    hasSecondaryTrack -> SubtitleDisplayMode.SECONDARY_ONLY
    else -> SubtitleDisplayMode.OFF
}

fun normalizeSubtitleDisplayMode(
    preferredMode: SubtitleDisplayMode,
    hasPrimaryTrack: Boolean,
    hasSecondaryTrack: Boolean
): SubtitleDisplayMode {
    if (!hasPrimaryTrack && !hasSecondaryTrack) {
        return SubtitleDisplayMode.OFF
    }
    return when (preferredMode) {
        SubtitleDisplayMode.OFF -> SubtitleDisplayMode.OFF
        SubtitleDisplayMode.PRIMARY_ONLY -> {
            if (hasPrimaryTrack) SubtitleDisplayMode.PRIMARY_ONLY else SubtitleDisplayMode.SECONDARY_ONLY
        }
        SubtitleDisplayMode.SECONDARY_ONLY -> {
            if (hasSecondaryTrack) SubtitleDisplayMode.SECONDARY_ONLY else SubtitleDisplayMode.PRIMARY_ONLY
        }
        SubtitleDisplayMode.BILINGUAL -> {
            resolveDefaultSubtitleDisplayMode(
                hasPrimaryTrack = hasPrimaryTrack,
                hasSecondaryTrack = hasSecondaryTrack
            )
        }
    }
}

fun resolveSubtitleDisplayOptions(
    primaryLabel: String,
    secondaryLabel: String,
    hasPrimaryTrack: Boolean,
    hasSecondaryTrack: Boolean
): List<SubtitleDisplayOption> {
    val options = mutableListOf(
        SubtitleDisplayOption(
            mode = SubtitleDisplayMode.OFF,
            label = "关闭",
            enabled = true
        )
    )
    if (hasPrimaryTrack) {
        options += SubtitleDisplayOption(
            mode = SubtitleDisplayMode.PRIMARY_ONLY,
            label = primaryLabel,
            enabled = true
        )
    }
    if (hasSecondaryTrack) {
        options += SubtitleDisplayOption(
            mode = SubtitleDisplayMode.SECONDARY_ONLY,
            label = secondaryLabel,
            enabled = true
        )
    }
    if (hasPrimaryTrack && hasSecondaryTrack) {
        options += SubtitleDisplayOption(
            mode = SubtitleDisplayMode.BILINGUAL,
            label = "双语",
            enabled = true
        )
    }
    return options
}

fun shouldRenderPrimarySubtitle(mode: SubtitleDisplayMode): Boolean {
    return mode == SubtitleDisplayMode.PRIMARY_ONLY || mode == SubtitleDisplayMode.BILINGUAL
}

fun shouldRenderSecondarySubtitle(mode: SubtitleDisplayMode): Boolean {
    return mode == SubtitleDisplayMode.SECONDARY_ONLY || mode == SubtitleDisplayMode.BILINGUAL
}

fun resolveSubtitleTextAt(cues: List<SubtitleCue>, positionMs: Long): String? {
    if (cues.isEmpty()) return null
    if (positionMs < 0L) return null

    var low = 0
    var high = cues.lastIndex
    while (low <= high) {
        val mid = (low + high) ushr 1
        val cue = cues[mid]
        when {
            positionMs < cue.startMs -> high = mid - 1
            positionMs > cue.endMs -> low = mid + 1
            else -> return cue.content
        }
    }
    return null
}

private fun JsonElement?.asJsonObjectOrNull(): JsonObject? {
    return (this as? JsonObject) ?: runCatching { this?.jsonObject }.getOrNull()
}

private fun JsonElement?.asJsonArrayOrNull(): JsonArray? {
    return (this as? JsonArray) ?: runCatching { this?.jsonArray }.getOrNull()
}

private fun JsonElement?.asDoubleOrNull(): Double? {
    return when (this) {
        null -> null
        else -> this.jsonPrimitive.doubleOrNull
    }
}
