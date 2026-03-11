package com.android.purebilibili.feature.video.ui.pager

internal fun shouldApplyPortraitProgressSync(
    snapshotBvid: String?,
    snapshotCid: Long,
    currentBvid: String?,
    currentCid: Long
): Boolean {
    if (snapshotBvid.isNullOrBlank()) return false
    if (currentBvid.isNullOrBlank()) return false
    if (snapshotBvid != currentBvid) return false
    if (snapshotCid <= 0L || currentCid <= 0L) return true
    return snapshotCid == currentCid
}
