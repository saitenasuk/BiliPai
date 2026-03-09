package com.android.purebilibili.feature.video.ui.section

data class VideoPlayerDanmakuLoadPolicy(
    val shouldEnable: Boolean,
    val shouldLoadImmediately: Boolean,
    val durationHintMs: Long
) {
    val shouldLoad: Boolean get() = shouldLoadImmediately
}

fun resolveVideoPlayerDanmakuLoadPolicy(
    cid: Long,
    danmakuEnabled: Boolean,
    durationHintMs: Long = 0L
): VideoPlayerDanmakuLoadPolicy {
    val canLoad = cid > 0 && danmakuEnabled
    return VideoPlayerDanmakuLoadPolicy(
        shouldEnable = canLoad,
        shouldLoadImmediately = canLoad,
        durationHintMs = durationHintMs.coerceAtLeast(0L)
    )
}
