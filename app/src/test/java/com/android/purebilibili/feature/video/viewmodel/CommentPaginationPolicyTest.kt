package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.data.model.response.ReplyCursor
import com.android.purebilibili.data.model.response.ReplyData
import com.android.purebilibili.data.model.response.ReplyItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommentPaginationPolicyTest {

    @Test
    fun `guest hot comments should not show zero count when visible comments exist`() {
        val data = ReplyData(
            cursor = ReplyCursor(allCount = 0, isEnd = false, next = 2),
            replies = emptyList(),
            hots = listOf(ReplyItem(rpid = 1L))
        )

        val resolution = resolveCommentPageResolution(
            data = data,
            pageToLoad = 1,
            combinedRepliesSize = 1,
            newRepliesSize = 0
        )

        assertEquals(1, resolution.totalCount)
        assertFalse(resolution.isEnd)
    }

    @Test
    fun `cursor is_end should terminate pagination`() {
        val data = ReplyData(
            cursor = ReplyCursor(allCount = 0, isEnd = true, next = 0),
            replies = listOf(ReplyItem(rpid = 1L))
        )

        val resolution = resolveCommentPageResolution(
            data = data,
            pageToLoad = 2,
            combinedRepliesSize = 1,
            newRepliesSize = 0
        )

        assertTrue(resolution.isEnd)
    }

    @Test
    fun `legacy page count should remain preferred when available`() {
        val data = ReplyData(
            replies = listOf(ReplyItem(rpid = 1L)),
            page = com.android.purebilibili.data.model.response.ReplyPage(count = 56)
        )

        val resolution = resolveCommentPageResolution(
            data = data,
            pageToLoad = 1,
            combinedRepliesSize = 1,
            newRepliesSize = 1
        )

        assertEquals(56, resolution.totalCount)
        assertFalse(resolution.isEnd)
    }
}
