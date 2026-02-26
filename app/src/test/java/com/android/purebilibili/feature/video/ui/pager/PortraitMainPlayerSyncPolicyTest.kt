package com.android.purebilibili.feature.video.ui.pager

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitMainPlayerSyncPolicyTest {

    @Test
    fun noReloadWhenSnapshotBvidBlank() {
        assertFalse(
            shouldReloadMainPlayerAfterPortraitExit(
                snapshotBvid = " ",
                snapshotCid = 0L,
                currentBvid = "BV1xx411c7mD",
                currentCid = 123L
            )
        )
    }

    @Test
    fun reloadWhenCurrentBvidMissingButSnapshotExists() {
        assertTrue(
            shouldReloadMainPlayerAfterPortraitExit(
                snapshotBvid = "BV1xx411c7mD",
                snapshotCid = 123L,
                currentBvid = null,
                currentCid = 0L
            )
        )
    }

    @Test
    fun noReloadWhenSnapshotMatchesCurrent_bvidAndCid() {
        assertFalse(
            shouldReloadMainPlayerAfterPortraitExit(
                snapshotBvid = "BV1xx411c7mD",
                snapshotCid = 100L,
                currentBvid = "BV1xx411c7mD",
                currentCid = 100L
            )
        )
    }

    @Test
    fun reloadWhenSnapshotDiffersFromCurrent() {
        assertTrue(
            shouldReloadMainPlayerAfterPortraitExit(
                snapshotBvid = "BV17x411w7KC",
                snapshotCid = 200L,
                currentBvid = "BV1xx411c7mD",
                currentCid = 100L
            )
        )
    }

    @Test
    fun reloadWhenBvidSameButCidDiffers() {
        assertTrue(
            shouldReloadMainPlayerAfterPortraitExit(
                snapshotBvid = "BV1xx411c7mD",
                snapshotCid = 222L,
                currentBvid = "BV1xx411c7mD",
                currentCid = 111L
            )
        )
    }

    @Test
    fun sharedPlayerMode_shouldNotPauseMainPlayerOnPortraitEnter() {
        assertFalse(
            shouldPauseMainPlayerOnPortraitEnter(useSharedPlayer = true)
        )
        assertTrue(
            shouldPauseMainPlayerOnPortraitEnter(useSharedPlayer = false)
        )
    }

    @Test
    fun resolvePortraitInitialPlayingBvid_usesInitialBvidOnlyWhenShared() {
        assertEquals(
            "BV1xx411c7mD",
            resolvePortraitInitialPlayingBvid(
                useSharedPlayer = true,
                initialBvid = "BV1xx411c7mD"
            )
        )
        assertEquals(
            null,
            resolvePortraitInitialPlayingBvid(
                useSharedPlayer = false,
                initialBvid = "BV1xx411c7mD"
            )
        )
    }

    @Test
    fun sharedPlayerMode_shouldNotMirrorPortraitProgressBackToMainPlayer() {
        assertFalse(shouldMirrorPortraitProgressToMainPlayer(useSharedPlayer = true))
        assertTrue(shouldMirrorPortraitProgressToMainPlayer(useSharedPlayer = false))
    }

    @Test
    fun externalNavigation_shouldExitPortraitBeforeJumping() {
        assertTrue(shouldExitPortraitForExternalNavigation(isPortraitFullscreen = true))
        assertFalse(shouldExitPortraitForExternalNavigation(isPortraitFullscreen = false))
    }

    @Test
    fun userSpaceNavigation_shouldExitPortraitBeforeJumping() {
        assertTrue(shouldExitPortraitForUserSpaceNavigation(isPortraitFullscreen = true))
        assertFalse(shouldExitPortraitForUserSpaceNavigation(isPortraitFullscreen = false))
    }

    @Test
    fun userSpaceReturn_shouldResync_whenCurrentPlayingBvidDiffers() {
        assertTrue(
            shouldResyncPortraitPagerOnUserSpaceReturn(
                pendingUserSpaceNavigation = true,
                expectedBvid = "BV17x411w7KC",
                currentPlayingBvid = "BV1xx411c7mD",
                currentPlayerMediaId = "BV1xx411c7mD"
            )
        )
    }

    @Test
    fun userSpaceReturn_shouldResync_whenPlayerMediaIdMissing() {
        assertTrue(
            shouldResyncPortraitPagerOnUserSpaceReturn(
                pendingUserSpaceNavigation = true,
                expectedBvid = "BV17x411w7KC",
                currentPlayingBvid = "BV17x411w7KC",
                currentPlayerMediaId = " "
            )
        )
    }

    @Test
    fun userSpaceReturn_shouldNotResync_whenAlreadyAligned() {
        assertFalse(
            shouldResyncPortraitPagerOnUserSpaceReturn(
                pendingUserSpaceNavigation = true,
                expectedBvid = "BV17x411w7KC",
                currentPlayingBvid = "BV17x411w7KC",
                currentPlayerMediaId = "BV17x411w7KC"
            )
        )
    }

    @Test
    fun userSpaceReturn_shouldNotResync_whenNoPendingNavigation() {
        assertFalse(
            shouldResyncPortraitPagerOnUserSpaceReturn(
                pendingUserSpaceNavigation = false,
                expectedBvid = "BV17x411w7KC",
                currentPlayingBvid = "BV17x411w7KC",
                currentPlayerMediaId = ""
            )
        )
    }
}
