// 文件路径: data/model/response/HistoryModels.kt
package com.android.purebilibili.data.model.response

import kotlinx.serialization.Serializable

/**
 * 历史记录相关数据模型
 * 从 ListModels.kt 拆分出来，提高代码可维护性
 */

// --- 历史记录响应（支持游标分页）---
@Serializable
data class HistoryResponse(
    val code: Int = 0,
    val message: String = "",
    val data: HistoryListData? = null
)

@Serializable
data class HistoryListData(
    val list: List<HistoryData>? = null,
    val cursor: HistoryCursor? = null
)

@Serializable
data class HistoryCursor(
    val max: Long = 0,
    val view_at: Long = 0,
    val business: String = "",
    val ps: Int = 30
)

// --- 历史记录数据项 ---
@Serializable
data class HistoryData(
    val title: String = "",
    val pic: String = "",
    val cover: String = "",
    val author_name: String = "",
    val author_face: String = "",
    val duration: Int = 0,
    val history: HistoryPage? = null,
    val stat: Stat? = null,
    val progress: Int = -1,
    val view_at: Long = 0
) {
    fun toVideoItem(): VideoItem {
        return VideoItem(
            id = history?.oid ?: 0,
            bvid = history?.bvid ?: "",
            title = title,
            pic = if (cover.isNotEmpty()) cover else pic,
            owner = Owner(name = author_name, face = author_face),
            stat = stat ?: Stat(),
            duration = duration,
            progress = progress,
            view_at = view_at
        )
    }
}

@Serializable
data class HistoryPage(
    val oid: Long = 0,
    val bvid: String = ""
)
