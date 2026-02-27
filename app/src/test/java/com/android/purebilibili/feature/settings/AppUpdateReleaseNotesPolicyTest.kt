package com.android.purebilibili.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateReleaseNotesPolicyTest {

    @Test
    fun `resolveUpdateReleaseNotesText should return placeholder when notes blank`() {
        assertEquals("暂无更新说明", resolveUpdateReleaseNotesText("   "))
    }

    @Test
    fun `resolveUpdateReleaseNotesText should keep long notes without truncation`() {
        val longNotes = buildString {
            append("更新内容：")
            repeat(80) { append("功能优化、修复问题。") }
        }

        val resolved = resolveUpdateReleaseNotesText(longNotes)

        assertEquals(longNotes, resolved)
        assertTrue(resolved.length > 240)
    }
}

