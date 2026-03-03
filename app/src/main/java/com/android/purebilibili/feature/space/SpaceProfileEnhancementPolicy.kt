package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.FavFolder

internal fun shouldEnableSpaceTopPhotoPreview(topPhotoUrl: String): Boolean {
    return normalizeSpaceTopPhotoUrl(topPhotoUrl).isNotBlank()
}

internal fun resolveSpaceTopPhoto(
    topPhoto: String,
    cardLargePhoto: String,
    cardSmallPhoto: String
): String {
    return sequenceOf(topPhoto, cardLargePhoto, cardSmallPhoto)
        .map { normalizeSpaceTopPhotoUrl(it) }
        .firstOrNull { it.isNotEmpty() }
        .orEmpty()
}

internal fun normalizeSpaceTopPhotoUrl(url: String): String {
    val candidate = url.trim()
    if (candidate.isEmpty()) return ""
    val lower = candidate.lowercase()
    if (lower == "null" || lower == "nil" || lower == "none" || lower == "undefined") {
        return ""
    }
    return when {
        candidate.startsWith("//") -> "https:$candidate"
        candidate.startsWith("http://", ignoreCase = true) -> {
            "https://${candidate.substring(startIndex = "http://".length)}"
        }
        else -> candidate
    }
}

internal fun resolveSpaceFavoriteFoldersForDisplay(folders: List<FavFolder>): List<FavFolder> {
    if (folders.isEmpty()) return emptyList()
    val seenIds = HashSet<Long>()
    return folders.filter { folder ->
        val valid = folder.id > 0L &&
            folder.title.isNotBlank() &&
            folder.media_count > 0
        valid && seenIds.add(folder.id)
    }
}

internal fun resolveSpaceCollectionTabCount(
    seasonCount: Int,
    seriesCount: Int,
    createdFavoriteCount: Int,
    collectedFavoriteCount: Int
): Int {
    return seasonCount.coerceAtLeast(0) +
        seriesCount.coerceAtLeast(0) +
        createdFavoriteCount.coerceAtLeast(0) +
        collectedFavoriteCount.coerceAtLeast(0)
}
