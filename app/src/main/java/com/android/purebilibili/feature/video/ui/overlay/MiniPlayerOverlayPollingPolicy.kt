package com.android.purebilibili.feature.video.ui.overlay

internal fun shouldPollMiniPlayerProgress(
    playerExists: Boolean,
    isMiniMode: Boolean,
    isActive: Boolean,
    isLiveMode: Boolean = false
): Boolean {
    if (isLiveMode) return false  // 📺 直播不需要进度轮询
    return playerExists && isMiniMode && isActive
}

internal fun resolveMiniPlayerPollingIntervalMs(
    isPlaying: Boolean
): Long {
    return if (isPlaying) 300L else 600L
}
