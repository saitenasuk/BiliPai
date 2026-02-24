package com.android.purebilibili.feature.home.components.cards

internal fun shouldShowVideoCardHistoryProgressBar(
    viewAt: Long,
    durationSec: Int,
    progressSec: Int
): Boolean {
    if (viewAt <= 0L || durationSec <= 0) return false
    return progressSec >= -1
}

internal fun resolveVideoCardHistoryProgressFraction(
    progressSec: Int,
    durationSec: Int
): Float {
    if (durationSec <= 0) return 0f
    if (progressSec == -1) return 1f
    if (progressSec < 0) return 0f
    return (progressSec.toFloat() / durationSec.toFloat()).coerceIn(0f, 1f)
}
