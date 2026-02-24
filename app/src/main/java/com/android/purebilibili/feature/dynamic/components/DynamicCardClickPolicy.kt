package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.core.util.BilibiliUrlParser
import com.android.purebilibili.data.model.response.ArchiveMajor
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.UgcSeasonMajor

internal sealed interface DynamicCardClickAction {
    data class OpenVideo(val bvid: String) : DynamicCardClickAction
    data class OpenDynamicDetail(val dynamicId: String) : DynamicCardClickAction
    data object None : DynamicCardClickAction
}

internal fun resolveBvidFromRawVideoTarget(rawValue: String?): String? {
    val target = rawValue?.trim().orEmpty()
    if (target.isEmpty()) return null
    val parsed = BilibiliUrlParser.parse(target)
    val bvid = parsed.bvid?.trim()
    if (!bvid.isNullOrEmpty()) return bvid
    return parsed.aid?.takeIf { it > 0 }?.let { "av$it" }
}

internal fun resolveArchivePlayableBvid(archive: ArchiveMajor): String? {
    return resolveBvidFromRawVideoTarget(archive.bvid)
        ?: resolveBvidFromRawVideoTarget(archive.jump_url)
        ?: archive.aid.trim().toLongOrNull()?.takeIf { it > 0L }?.let { "av$it" }
}

internal fun resolveUgcSeasonPlayableBvid(season: UgcSeasonMajor): String? {
    return season.archive?.let(::resolveArchivePlayableBvid)
        ?: resolveBvidFromRawVideoTarget(season.jump_url)
        ?: season.aid.takeIf { it > 0L }?.let { "av$it" }
}

internal fun resolveUgcSeasonArchiveFallback(season: UgcSeasonMajor): ArchiveMajor? {
    season.archive?.let { return it }
    val hasRenderableContent = season.title.isNotBlank() || season.cover.isNotBlank()
    if (!hasRenderableContent) return null
    val bvid = resolveUgcSeasonPlayableBvid(season).orEmpty()
    return ArchiveMajor(
        aid = season.aid.takeIf { it > 0 }?.toString().orEmpty(),
        bvid = bvid,
        title = season.title,
        cover = season.cover,
        desc = season.desc.ifBlank { season.intro },
        duration_text = season.duration_text,
        stat = com.android.purebilibili.data.model.response.ArchiveStat(
            play = season.stat.play,
            danmaku = season.stat.danmaku
        ),
        jump_url = season.jump_url
    )
}

internal fun resolveDynamicCardClickAction(item: DynamicItem): DynamicCardClickAction {
    val major = item.modules.module_dynamic?.major
    val bvid = major?.archive?.let(::resolveArchivePlayableBvid)
        ?: major?.ugc_season?.let(::resolveUgcSeasonPlayableBvid)
    if (bvid != null) {
        return DynamicCardClickAction.OpenVideo(bvid)
    }

    val dynamicId = item.id_str.trim().takeIf { it.isNotEmpty() } ?: return DynamicCardClickAction.None
    return DynamicCardClickAction.OpenDynamicDetail(dynamicId)
}
