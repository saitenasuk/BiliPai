package com.android.purebilibili.feature.video.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals

class CommentReplyTargetPolicyTest {

    @Test
    fun `top-level comment should target itself as root and parent`() {
        val (root, parent) = resolveCommentReplyTargets(
            replyRpid = 1001L,
            replyRoot = 0L
        )
        assertEquals(1001L, root)
        assertEquals(1001L, parent)
    }

    @Test
    fun `sub-reply should keep thread root and current parent`() {
        val (root, parent) = resolveCommentReplyTargets(
            replyRpid = 2002L,
            replyRoot = 1001L
        )
        assertEquals(1001L, root)
        assertEquals(2002L, parent)
    }

    @Test
    fun `new root comment should use zero targets`() {
        val (root, parent) = resolveCommentReplyTargets(
            replyRpid = null,
            replyRoot = null
        )
        assertEquals(0L, root)
        assertEquals(0L, parent)
    }
}
