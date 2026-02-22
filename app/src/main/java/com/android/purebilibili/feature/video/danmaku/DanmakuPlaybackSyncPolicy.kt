package com.android.purebilibili.feature.video.danmaku

import kotlin.math.abs

private const val NORMAL_SYNC_INTERVAL_MS = 5000L

internal fun resolveDanmakuDriftSyncIntervalMs(videoSpeed: Float): Long {
    return when {
        videoSpeed >= 1.75f -> 900L
        videoSpeed >= 1.25f -> 1200L
        videoSpeed > 1.02f -> 1600L
        videoSpeed <= 0.75f -> 3000L
        videoSpeed < 0.98f -> 3500L
        else -> NORMAL_SYNC_INTERVAL_MS
    }
}

internal fun shouldForceDanmakuDataResync(videoSpeed: Float, tickCount: Int): Boolean {
    if (tickCount <= 0) return false
    if (abs(videoSpeed - 1.0f) <= 0.02f) return false
    return tickCount % 3 == 0
}
