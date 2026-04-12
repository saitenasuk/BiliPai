package com.android.purebilibili.feature.video.ui.pager

internal fun shouldUseEmbeddedVideoSubReplyPresentation(): Boolean = true

private const val FULLSCREEN_VIDEO_SUB_REPLY_SHEET_HEIGHT_FRACTION = 1f

internal fun shouldShowDetachedVideoSubReplySheet(
    useEmbeddedPresentation: Boolean
): Boolean = !useEmbeddedPresentation

internal fun shouldOpenPortraitCommentReplyComposer(): Boolean = true

internal fun shouldOpenPortraitCommentThreadDetail(
    useEmbeddedPresentation: Boolean
): Boolean = true

internal fun resolveVideoSubReplySheetMaxHeightFraction(
    screenHeightPx: Int = 0,
    topReservedPx: Int = 0
): Float {
    return FULLSCREEN_VIDEO_SUB_REPLY_SHEET_HEIGHT_FRACTION
}

internal fun resolveVideoSubReplySheetScrimAlpha(): Float = 0f
