package com.android.purebilibili.feature.settings

private const val EMPTY_RELEASE_NOTES_PLACEHOLDER = "暂无更新说明"

internal fun resolveUpdateReleaseNotesText(releaseNotes: String): String {
    return releaseNotes.trim().ifBlank { EMPTY_RELEASE_NOTES_PLACEHOLDER }
}

