package com.android.purebilibili.feature.video.danmaku

import androidx.media3.common.Player
import kotlin.test.Test
import kotlin.test.assertEquals

class DanmakuSyncPolicyTest {

    @Test
    fun isPlayingChange_resumesWithHardResyncWhenDataReady() {
        assertEquals(
            DanmakuSyncAction.HardResync,
            resolveDanmakuActionForIsPlayingChange(
                isPlayerPlaying = true,
                danmakuEnabled = true,
                hasData = true
            )
        )
    }

    @Test
    fun isPlayingChange_pausesWhenPlaybackStops() {
        assertEquals(
            DanmakuSyncAction.PauseOnly,
            resolveDanmakuActionForIsPlayingChange(
                isPlayerPlaying = false,
                danmakuEnabled = true,
                hasData = true
            )
        )
    }

    @Test
    fun playbackState_readyAfterBuffering_usesHardResyncInsteadOfSoftStart() {
        assertEquals(
            DanmakuSyncAction.HardResync,
            resolveDanmakuActionForPlaybackState(
                playbackState = Player.STATE_READY,
                isPlayerPlaying = true,
                danmakuEnabled = true,
                hasData = true,
                resumedFromBuffering = true
            )
        )
    }

    @Test
    fun positionDiscontinuity_seekAlwaysForcesHardResync() {
        assertEquals(
            DanmakuSyncAction.HardResync,
            resolveDanmakuActionForPositionDiscontinuity(
                reason = Player.DISCONTINUITY_REASON_SEEK,
                hasData = true
            )
        )
    }

    @Test
    fun speedChange_forcesHardResyncWhenPlaybackRateActuallyChanges() {
        assertEquals(
            DanmakuSyncAction.HardResync,
            resolveDanmakuActionForPlaybackSpeedChange(
                previousSpeed = 1.0f,
                newSpeed = 1.5f,
                isPlayerPlaying = true,
                hasData = true
            )
        )
    }

    @Test
    fun driftGuard_staysIdleOnNormalTickButCorrectsPeriodicHealthChecks() {
        assertEquals(
            DanmakuSyncAction.None,
            resolveDanmakuGuardAction(
                videoSpeed = 1.0f,
                tickCount = 1,
                danmakuEnabled = true,
                isPlaying = true,
                hasData = true
            )
        )

        assertEquals(
            DanmakuSyncAction.HardResync,
            resolveDanmakuGuardAction(
                videoSpeed = 1.0f,
                tickCount = 6,
                danmakuEnabled = true,
                isPlaying = true,
                hasData = true
            )
        )
    }
}
