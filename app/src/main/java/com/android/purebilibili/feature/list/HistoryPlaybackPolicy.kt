package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.HistoryItem

internal fun resolveHistoryPlaybackCid(
    clickedCid: Long,
    historyItem: HistoryItem?
): Long {
    val historyCid = historyItem?.cid ?: 0L
    return if (historyCid > 0L) historyCid else clickedCid.coerceAtLeast(0L)
}

internal fun resolveHistoryDisplayProgress(
    serverProgressSec: Int,
    durationSec: Int,
    localPositionMs: Long
): Int {
    if (durationSec <= 0) return serverProgressSec
    if (serverProgressSec == -1) return -1
    if (serverProgressSec > 0) return serverProgressSec.coerceAtMost(durationSec)

    val localSec = (localPositionMs / 1000L).toInt()
    if (localSec <= 0) return serverProgressSec

    val completedThreshold = (durationSec * 0.95f).toInt().coerceAtLeast(1)
    val clamped = localSec.coerceAtMost(durationSec)
    return if (clamped >= completedThreshold) -1 else clamped
}
