package com.android.purebilibili.feature.video.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

    @Test
    fun `timestamp parser supports spaces and full-width colon`() {
        val text = "自用18: 07\n19：30"
        val matches = COMMENT_TIMESTAMP_PATTERN.findAll(text).toList()
        assertEquals(2, matches.size)

        val firstSeconds = parseCommentTimestampSeconds(matches[0])
        val secondSeconds = parseCommentTimestampSeconds(matches[1])
        assertEquals(18 * 60L + 7L, firstSeconds)
        assertEquals(19 * 60L + 30L, secondSeconds)
    }

    @Test
    fun `timestamp parser keeps hour format and rejects invalid second width`() {
        val match = COMMENT_TIMESTAMP_PATTERN.find("1:02:03")
        assertNotNull(match)
        assertEquals(3723L, parseCommentTimestampSeconds(match))

        val invalid = COMMENT_TIMESTAMP_PATTERN.find("3:5")
        assertNull(invalid)
    }
}
