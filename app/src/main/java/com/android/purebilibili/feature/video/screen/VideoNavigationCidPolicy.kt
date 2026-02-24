package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.data.model.response.UgcSeason

internal const val VIDEO_NAV_TARGET_CID_KEY = "video_nav_target_cid"

internal fun resolveNavigationTargetCid(
    targetBvid: String,
    explicitCid: Long,
    ugcSeason: UgcSeason?
): Long {
    if (explicitCid > 0L) return explicitCid
    if (targetBvid.isBlank()) return 0L
    return ugcSeason
        ?.sections
        ?.asSequence()
        ?.flatMap { section -> section.episodes.asSequence() }
        ?.firstOrNull { episode -> episode.bvid == targetBvid }
        ?.cid
        ?.takeIf { it > 0L }
        ?: 0L
}
