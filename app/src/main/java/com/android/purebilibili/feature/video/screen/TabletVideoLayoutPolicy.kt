package com.android.purebilibili.feature.video.screen

data class TabletVideoLayoutPolicy(
    val primaryRatio: Float,
    val playerMaxWidthDp: Int,
    val infoMaxWidthDp: Int
)

fun resolveTabletVideoLayoutPolicy(
    widthDp: Int,
    isTv: Boolean
): TabletVideoLayoutPolicy {
    if (isTv) {
        return TabletVideoLayoutPolicy(
            primaryRatio = 0.62f,
            playerMaxWidthDp = 1180,
            infoMaxWidthDp = 1120
        )
    }

    return when {
        widthDp >= 1600 -> TabletVideoLayoutPolicy(
            primaryRatio = 0.60f,
            playerMaxWidthDp = 1120,
            infoMaxWidthDp = 1080
        )
        else -> TabletVideoLayoutPolicy(
            primaryRatio = 0.65f,
            playerMaxWidthDp = 1000,
            infoMaxWidthDp = 980
        )
    }
}
