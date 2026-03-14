package com.android.purebilibili.feature.video.screen

import kotlin.math.roundToInt

internal const val AUDIO_MODE_COVER_WIDTH_FRACTION = 0.75f
internal const val AUDIO_MODE_COVER_MIN_VERTICAL_CLEARANCE_DP = 24

internal fun resolveAudioModeCenteredCoverSizeDp(
    availableWidthDp: Int,
    availableHeightDp: Int,
    widthFraction: Float = AUDIO_MODE_COVER_WIDTH_FRACTION,
    minVerticalClearanceDp: Int = AUDIO_MODE_COVER_MIN_VERTICAL_CLEARANCE_DP
): Int {
    val widthBound = (availableWidthDp * widthFraction).roundToInt()
    val heightBound = (availableHeightDp - (minVerticalClearanceDp * 2)).coerceAtLeast(0)
    return widthBound.coerceAtMost(heightBound)
}
