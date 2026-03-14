package com.android.purebilibili.feature.video.ui.pager

internal fun shouldUseEmbeddedVideoSubReplyPresentation(): Boolean = true

internal fun shouldShowDetachedVideoSubReplySheet(
    useEmbeddedPresentation: Boolean
): Boolean = !useEmbeddedPresentation

internal fun resolveVideoSubReplySheetMaxHeightFraction(): Float = 1f

internal fun resolveVideoSubReplySheetScrimAlpha(): Float = 0.18f
