package com.android.purebilibili.feature.video.screen

internal enum class VideoFavoriteEntryPoint {
    FullscreenOverlay,
    DetailActionRow,
    BottomInputBar
}

internal enum class VideoFavoriteAction {
    ToggleFavorite
}

internal fun resolveVideoFavoriteAction(
    entryPoint: VideoFavoriteEntryPoint
): VideoFavoriteAction {
    return when (entryPoint) {
        VideoFavoriteEntryPoint.FullscreenOverlay,
        VideoFavoriteEntryPoint.DetailActionRow,
        VideoFavoriteEntryPoint.BottomInputBar -> VideoFavoriteAction.ToggleFavorite
    }
}
