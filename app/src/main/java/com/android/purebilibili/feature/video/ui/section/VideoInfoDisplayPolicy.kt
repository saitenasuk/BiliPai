package com.android.purebilibili.feature.video.ui.section

import com.android.purebilibili.data.model.response.AiSummaryData

internal fun shouldShowAiSummaryEntry(
    aiSummary: AiSummaryData?,
    isAiSummaryEntryEnabled: Boolean
): Boolean {
    return isAiSummaryEntryEnabled && hasAiSummaryContent(aiSummary)
}

internal fun hasAiSummaryContent(aiSummary: AiSummaryData?): Boolean {
    val modelResult = aiSummary?.modelResult ?: return false
    if (aiSummary.code != 0) return false
    return modelResult.summary.isNotBlank() || modelResult.outline.isNotEmpty()
}

internal fun shouldShowInlineOwnerIdentity(showOwnerAvatar: Boolean): Boolean {
    return !showOwnerAvatar
}
