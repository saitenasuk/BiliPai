package com.android.purebilibili.feature.video.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReplyComponentsPolicyTest {

    @Test
    fun `collectRenderableEmoteKeys only keeps used and mapped tokens`() {
        val emoteMap = mapOf(
            "[doge]" to "url_doge",
            "[笑哭]" to "url_laugh",
            "[不存在]" to "url_none"
        )

        val keys = collectRenderableEmoteKeys(
            text = "测试 [doge] 还有 [笑哭] 以及 [未收录]",
            emoteMap = emoteMap
        )

        assertEquals(setOf("[doge]", "[笑哭]"), keys)
    }

    @Test
    fun `shouldEnableRichCommentSelection disables expensive mixed mode`() {
        assertFalse(
            shouldEnableRichCommentSelection(
                hasRenderableEmotes = true,
                hasInteractiveAnnotations = true
            )
        )
        assertFalse(
            shouldEnableRichCommentSelection(
                hasRenderableEmotes = true,
                hasInteractiveAnnotations = false
            )
        )
        assertFalse(
            shouldEnableRichCommentSelection(
                hasRenderableEmotes = false,
                hasInteractiveAnnotations = true
            )
        )
        assertTrue(
            shouldEnableRichCommentSelection(
                hasRenderableEmotes = false,
                hasInteractiveAnnotations = false
            )
        )
    }
}
