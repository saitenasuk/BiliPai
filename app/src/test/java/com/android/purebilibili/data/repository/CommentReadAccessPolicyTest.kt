package com.android.purebilibili.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentReadAccessPolicyTest {

    @Test
    fun `resolveCommentReadPlan prefers auth for logged user`() {
        val plan = resolveCommentReadPlan(hasSession = true)
        assertEquals(CommentReadApiMode.AUTH, plan.primary)
        assertEquals(CommentReadApiMode.GUEST, plan.fallback)
    }

    @Test
    fun `resolveCommentReadPlan prefers guest for anonymous user`() {
        val plan = resolveCommentReadPlan(hasSession = false)
        assertEquals(CommentReadApiMode.GUEST, plan.primary)
        assertEquals(CommentReadApiMode.AUTH, plan.fallback)
    }

    @Test
    fun `shouldFallbackCommentRead covers unstable api errors`() {
        assertTrue(shouldFallbackCommentRead(-101))
        assertTrue(shouldFallbackCommentRead(-111))
        assertTrue(shouldFallbackCommentRead(-352))
        assertTrue(shouldFallbackCommentRead(-412))
        assertFalse(shouldFallbackCommentRead(12002))
    }

    @Test
    fun `resolveCommentReadErrorMessage avoids forcing login for anonymous read`() {
        assertEquals(
            "评论加载失败，请稍后重试或切换排序",
            resolveCommentReadErrorMessage(-101)
        )
    }
}
