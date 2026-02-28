package com.android.purebilibili.feature.video.danmaku

import kotlin.math.abs

private const val NORMAL_SYNC_INTERVAL_MS = 2200L
private const val NORMAL_SPEED_FORCE_RESYNC_INTERVAL_TICKS = 6
private const val NON_NORMAL_SPEED_FORCE_RESYNC_INTERVAL_TICKS = 3

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
    val isNearNormalSpeed = abs(videoSpeed - 1.0f) <= 0.02f
    val interval = if (isNearNormalSpeed) {
        NORMAL_SPEED_FORCE_RESYNC_INTERVAL_TICKS
    } else {
        NON_NORMAL_SPEED_FORCE_RESYNC_INTERVAL_TICKS
    }
    return tickCount % interval == 0
}
