package com.android.purebilibili.feature.download

import com.android.purebilibili.data.model.response.ViewInfo

internal data class BatchDownloadCandidate(
    val id: String,
    val bvid: String,
    val cid: Long,
    val title: String,
    val label: String,
    val cover: String,
    val ownerName: String,
    val selected: Boolean
)

internal fun resolveBatchDownloadCandidates(info: ViewInfo): List<BatchDownloadCandidate> {
    val pageCandidates = info.pages.mapIndexedNotNull { index, page ->
        val cid = page.cid.takeIf { it > 0L } ?: return@mapIndexedNotNull null
        val pageNo = page.page.takeIf { it > 0 } ?: (index + 1)
        val partLabel = page.part.trim().ifBlank { info.title }
        BatchDownloadCandidate(
            id = "${info.bvid}#$cid",
            bvid = info.bvid,
            cid = cid,
            title = info.title,
            label = "P$pageNo $partLabel".trim(),
            cover = info.pic,
            ownerName = info.owner.name,
            selected = cid == info.cid
        )
    }
    if (pageCandidates.isNotEmpty()) {
        return pageCandidates.distinctBy { it.id }
    }

    return info.ugc_season
        ?.sections
        .orEmpty()
        .flatMap { section -> section.episodes }
        .mapNotNull { episode ->
            val bvid = episode.bvid.trim().ifBlank { return@mapNotNull null }
            val cid = episode.cid.takeIf { it > 0L } ?: return@mapNotNull null
            val title = episode.arc?.title?.trim().orEmpty().ifBlank {
                episode.title.trim().ifBlank { info.title }
            }
            val cover = episode.arc?.pic?.takeIf { it.isNotBlank() } ?: info.pic
            BatchDownloadCandidate(
                id = "$bvid#$cid",
                bvid = bvid,
                cid = cid,
                title = title,
                label = title,
                cover = cover,
                ownerName = info.owner.name,
                selected = cid == info.cid
            )
        }
        .distinctBy { it.id }
}
