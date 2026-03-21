package com.android.purebilibili.feature.video.ui.pager

internal data class PortraitExitRestoreTarget(
    val bvid: String,
    val cid: Long
)

internal fun shouldReloadMainPlayerAfterPortraitExit(
    snapshotBvid: String?,
    snapshotCid: Long,
    currentBvid: String?,
    currentCid: Long
): Boolean {
    if (snapshotBvid.isNullOrBlank()) return false
    if (currentBvid.isNullOrBlank()) return true
    if (snapshotBvid != currentBvid) return true
    if (snapshotCid <= 0L || currentCid <= 0L) return false
    return snapshotCid != currentCid
}

internal fun shouldPauseMainPlayerOnPortraitEnter(useSharedPlayer: Boolean): Boolean {
    return !useSharedPlayer
}

internal fun resolvePortraitInitialPlayingBvid(
    useSharedPlayer: Boolean,
    initialBvid: String
): String? {
    if (!useSharedPlayer) return null
    return initialBvid
}

internal fun shouldMirrorPortraitProgressToMainPlayer(useSharedPlayer: Boolean): Boolean {
    return !useSharedPlayer
}

internal fun shouldExitPortraitForExternalNavigation(isPortraitFullscreen: Boolean): Boolean {
    return isPortraitFullscreen
}

internal fun shouldExitPortraitForUserSpaceNavigation(isPortraitFullscreen: Boolean): Boolean {
    return isPortraitFullscreen
}

internal fun shouldDeferPortraitRestoreUntilForegroundResume(
    isPortraitFullscreen: Boolean,
    isExternalNavigation: Boolean
): Boolean {
    return isPortraitFullscreen && isExternalNavigation
}

internal fun shouldApplyDeferredPortraitRestoreOnResume(
    hasDeferredRestore: Boolean,
    isPortraitFullscreen: Boolean
): Boolean {
    return hasDeferredRestore && !isPortraitFullscreen
}

internal fun resolvePortraitExitRestoreTarget(
    pendingMainReloadBvidAfterPortrait: String?,
    portraitPendingSelectionBvid: String?,
    portraitSyncSnapshotBvid: String?,
    portraitSyncSnapshotCid: Long,
    currentBvidCid: Long
): PortraitExitRestoreTarget? {
    val targetBvid = pendingMainReloadBvidAfterPortrait
        ?: portraitPendingSelectionBvid
        ?: portraitSyncSnapshotBvid
        ?: return null
    val targetCid = if (targetBvid == portraitSyncSnapshotBvid) {
        portraitSyncSnapshotCid
    } else {
        currentBvidCid
    }
    return PortraitExitRestoreTarget(
        bvid = targetBvid,
        cid = targetCid
    )
}

internal fun shouldResyncPortraitPagerOnUserSpaceReturn(
    pendingUserSpaceNavigation: Boolean,
    expectedBvid: String,
    currentPlayingBvid: String?,
    currentPlayerMediaId: String?
): Boolean {
    if (!pendingUserSpaceNavigation) return false
    if (expectedBvid.isBlank()) return false
    if (currentPlayingBvid != expectedBvid) return true
    val normalizedMediaId = currentPlayerMediaId?.trim().orEmpty()
    if (normalizedMediaId.isBlank()) return true
    return normalizedMediaId != expectedBvid
}
