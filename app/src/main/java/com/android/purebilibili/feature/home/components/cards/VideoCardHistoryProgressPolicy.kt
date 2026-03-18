package com.android.purebilibili.feature.home.components.cards

import com.android.purebilibili.feature.list.VideoProgressDisplayState
import com.android.purebilibili.feature.list.resolveVideoDisplayProgressState

internal fun resolveVideoCardHistoryProgressState(
    viewAt: Long,
    durationSec: Int,
    progressSec: Int
): VideoProgressDisplayState {
    return resolveVideoDisplayProgressState(
        serverProgressSec = progressSec,
        durationSec = durationSec,
        localPositionMs = 0L,
        viewAt = viewAt
    )
}

internal fun shouldShowVideoCardHistoryProgressBar(
    viewAt: Long,
    durationSec: Int,
    progressSec: Int
): Boolean {
    return resolveVideoCardHistoryProgressState(
        viewAt = viewAt,
        durationSec = durationSec,
        progressSec = progressSec
    ).showProgressBar
}

internal fun resolveVideoCardHistoryProgressFraction(
    progressSec: Int,
    durationSec: Int
): Float {
    return resolveVideoCardHistoryProgressState(
        viewAt = 1L,
        durationSec = durationSec,
        progressSec = progressSec
    ).progressFraction
}
