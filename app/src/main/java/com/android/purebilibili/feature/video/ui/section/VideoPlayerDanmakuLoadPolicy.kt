package com.android.purebilibili.feature.video.ui.section

data class VideoPlayerDanmakuLoadPolicy(
    val shouldEnable: Boolean,
    val shouldLoad: Boolean
)

fun resolveVideoPlayerDanmakuLoadPolicy(
    cid: Long,
    danmakuEnabled: Boolean
): VideoPlayerDanmakuLoadPolicy {
    val canLoad = cid > 0 && danmakuEnabled
    return VideoPlayerDanmakuLoadPolicy(
        shouldEnable = canLoad,
        shouldLoad = canLoad
    )
}
