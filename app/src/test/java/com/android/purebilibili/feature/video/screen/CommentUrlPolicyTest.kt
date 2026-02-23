package com.android.purebilibili.feature.video.screen

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentUrlPolicyTest {

    @Test
    fun `bilibili hosts should open in app`() {
        assertTrue(shouldOpenCommentUrlInApp("https://www.bilibili.com/video/BV1xx411c7mD"))
        assertTrue(shouldOpenCommentUrlInApp("https://m.bilibili.com/video/BV1xx411c7mD"))
        assertTrue(shouldOpenCommentUrlInApp("https://b23.tv/abcd123"))
    }

    @Test
    fun `non bilibili hosts should not be forced in app`() {
        assertFalse(shouldOpenCommentUrlInApp("https://example.com/video/1"))
        assertFalse(shouldOpenCommentUrlInApp("mailto:demo@example.com"))
    }
}
