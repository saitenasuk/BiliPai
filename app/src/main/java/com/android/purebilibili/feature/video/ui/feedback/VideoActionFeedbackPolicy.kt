package com.android.purebilibili.feature.video.ui.feedback

import androidx.compose.ui.graphics.Color

enum class VideoFeedbackAnchor {
    BottomCenter,
    BottomTrailing
}

data class VideoFeedbackPlacement(
    val anchor: VideoFeedbackAnchor,
    val bottomInsetDp: Int,
    val sideInsetDp: Int
)

fun resolveVideoActionTint(
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color
): Color = if (isActive) activeColor else inactiveColor

fun resolveVideoActionCountTint(
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color
): Color = if (isActive) activeColor else inactiveColor

fun resolveVideoFeedbackPlacement(
    isFullscreen: Boolean,
    isLandscape: Boolean,
    bottomInsetDp: Int
): VideoFeedbackPlacement {
    return if (isFullscreen && isLandscape) {
        VideoFeedbackPlacement(
            anchor = VideoFeedbackAnchor.BottomTrailing,
            bottomInsetDp = bottomInsetDp,
            sideInsetDp = bottomInsetDp
        )
    } else {
        VideoFeedbackPlacement(
            anchor = VideoFeedbackAnchor.BottomCenter,
            bottomInsetDp = bottomInsetDp,
            sideInsetDp = 0
        )
    }
}
