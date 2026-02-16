package com.android.purebilibili.feature.video.screen

import android.view.KeyEvent

internal enum class VideoDetailTvFocusTarget {
    PLAYER,
    CONTENT
}

internal fun resolveInitialVideoDetailTvFocusTarget(isTv: Boolean): VideoDetailTvFocusTarget? {
    return if (isTv) VideoDetailTvFocusTarget.PLAYER else null
}

internal fun resolveVideoDetailTvFocusTarget(
    current: VideoDetailTvFocusTarget,
    keyCode: Int,
    action: Int
): VideoDetailTvFocusTarget {
    if (action != KeyEvent.ACTION_UP) return current
    return when {
        current == VideoDetailTvFocusTarget.PLAYER && keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> {
            VideoDetailTvFocusTarget.CONTENT
        }

        current == VideoDetailTvFocusTarget.CONTENT && keyCode == KeyEvent.KEYCODE_DPAD_UP -> {
            VideoDetailTvFocusTarget.PLAYER
        }

        else -> current
    }
}
