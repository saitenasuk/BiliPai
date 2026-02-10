package com.android.purebilibili.feature.video.player

import com.android.purebilibili.core.store.SettingsManager
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackgroundPlaybackPolicyTest {

    @Test
    fun stopOnExitDisablesInAppMiniPlayer() {
        assertFalse(
            shouldShowInAppMiniPlayerByPolicy(
                mode = SettingsManager.MiniPlayerMode.IN_APP_ONLY,
                isActive = true,
                isNavigatingToVideo = false,
                stopPlaybackOnExit = true
            )
        )
    }

    @Test
    fun stopOnExitDisablesSystemPip() {
        assertFalse(
            shouldEnterPipByPolicy(
                mode = SettingsManager.MiniPlayerMode.SYSTEM_PIP,
                isActive = true,
                stopPlaybackOnExit = true
            )
        )
    }

    @Test
    fun stopOnExitDisablesBackgroundAudioEvenInDefaultMode() {
        assertFalse(
            shouldContinueBackgroundAudioByPolicy(
                mode = SettingsManager.MiniPlayerMode.OFF,
                isActive = true,
                isLeavingByNavigation = false,
                stopPlaybackOnExit = true
            )
        )
    }

    @Test
    fun defaultModeStillSupportsBackgroundAudioWhenOptionOff() {
        assertTrue(
            shouldContinueBackgroundAudioByPolicy(
                mode = SettingsManager.MiniPlayerMode.OFF,
                isActive = true,
                isLeavingByNavigation = false,
                stopPlaybackOnExit = false
            )
        )
    }
}
