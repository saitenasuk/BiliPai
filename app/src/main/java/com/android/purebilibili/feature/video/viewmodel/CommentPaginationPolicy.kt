package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.data.model.response.ReplyData

internal data class CommentPageResolution(
    val totalCount: Int,
    val isEnd: Boolean
)

internal fun resolveCommentPageResolution(
    data: ReplyData,
    pageToLoad: Int,
    combinedRepliesSize: Int,
    newRepliesSize: Int
): CommentPageResolution {
    val totalCount = data.getAllCount().takeIf { it > 0 } ?: combinedRepliesSize.coerceAtLeast(0)
    val hasCursorPaginationSignal =
        data.cursor.allCount > 0 || data.cursor.next > 0 || data.cursor.isEnd
    val isEnd = if (hasCursorPaginationSignal) {
        data.cursor.isEnd
    } else {
        data.getIsEnd(pageToLoad, combinedRepliesSize) ||
            (newRepliesSize == 0 && combinedRepliesSize == 0)
    }
    return CommentPageResolution(
        totalCount = totalCount,
        isEnd = isEnd
    )
}
