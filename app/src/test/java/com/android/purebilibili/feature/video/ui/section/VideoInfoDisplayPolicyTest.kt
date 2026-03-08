package com.android.purebilibili.feature.video.ui.section

import com.android.purebilibili.data.model.response.AiModelResult
import com.android.purebilibili.data.model.response.AiOutline
import com.android.purebilibili.data.model.response.AiSummaryData
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoInfoDisplayPolicyTest {

    @Test
    fun aiSummaryEntryShownOnlyWhenEnabledAndContentExists() {
        val aiSummary = AiSummaryData(
            code = 0,
            modelResult = AiModelResult(
                summary = "这是一段 AI 摘要",
                outline = listOf(AiOutline(title = "开场", timestamp = 12))
            )
        )

        assertTrue(shouldShowAiSummaryEntry(aiSummary, isAiSummaryEntryEnabled = true))
        assertFalse(shouldShowAiSummaryEntry(aiSummary, isAiSummaryEntryEnabled = false))
    }

    @Test
    fun aiSummaryEntryHiddenWhenPayloadMissingOrEmpty() {
        val emptySummary = AiSummaryData(
            code = 0,
            modelResult = AiModelResult(summary = "", outline = emptyList())
        )

        assertFalse(shouldShowAiSummaryEntry(aiSummary = null, isAiSummaryEntryEnabled = true))
        assertFalse(shouldShowAiSummaryEntry(emptySummary, isAiSummaryEntryEnabled = true))
    }

    @Test
    fun inlineOwnerIdentityShownOnlyWhenLeadingAvatarHidden() {
        assertTrue(shouldShowInlineOwnerIdentity(showOwnerAvatar = false))
        assertFalse(shouldShowInlineOwnerIdentity(showOwnerAvatar = true))
    }
}
