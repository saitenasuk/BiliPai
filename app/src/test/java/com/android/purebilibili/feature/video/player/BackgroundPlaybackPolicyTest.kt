package com.android.purebilibili.feature.video.player

import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.R
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackgroundPlaybackPolicyTest {

    @Test
    fun inAppMiniPlayerShownOnlyWhenEligible() {
        assertTrue(
            shouldShowInAppMiniPlayerByPolicy(
                mode = SettingsManager.MiniPlayerMode.IN_APP_ONLY,
                isActive = true,
                isNavigatingToVideo = false,
                stopPlaybackOnExit = false
            )
        )
        assertFalse(
            shouldShowInAppMiniPlayerByPolicy(
                mode = SettingsManager.MiniPlayerMode.IN_APP_ONLY,
                isActive = true,
                isNavigatingToVideo = true,
                stopPlaybackOnExit = false
            )
        )
    }

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
    fun systemPipRequiresActivePlayback() {
        assertTrue(
            shouldEnterPipByPolicy(
                mode = SettingsManager.MiniPlayerMode.SYSTEM_PIP,
                isActive = true,
                stopPlaybackOnExit = false
            )
        )
        assertFalse(
            shouldEnterPipByPolicy(
                mode = SettingsManager.MiniPlayerMode.SYSTEM_PIP,
                isActive = false,
                stopPlaybackOnExit = false
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

    @Test
    fun defaultModeStopsBackgroundAudioWhenLeavingByNavigation() {
        assertFalse(
            shouldContinueBackgroundAudioByPolicy(
                mode = SettingsManager.MiniPlayerMode.OFF,
                isActive = true,
                isLeavingByNavigation = true,
                stopPlaybackOnExit = false
            )
        )
    }

    @Test
    fun nonDefaultModesDoNotContinueBackgroundAudio() {
        assertFalse(
            shouldContinueBackgroundAudioByPolicy(
                mode = SettingsManager.MiniPlayerMode.IN_APP_ONLY,
                isActive = true,
                isLeavingByNavigation = false,
                stopPlaybackOnExit = false
            )
        )
        assertFalse(
            shouldContinueBackgroundAudioByPolicy(
                mode = SettingsManager.MiniPlayerMode.SYSTEM_PIP,
                isActive = true,
                isLeavingByNavigation = false,
                stopPlaybackOnExit = false
            )
        )
    }

    @Test
    fun navigationExitShouldClearPlaybackNotificationForOffAndPip() {
        assertTrue(
            shouldClearPlaybackNotificationOnNavigationExit(
                mode = SettingsManager.MiniPlayerMode.OFF,
                stopPlaybackOnExit = false
            )
        )
        assertTrue(
            shouldClearPlaybackNotificationOnNavigationExit(
                mode = SettingsManager.MiniPlayerMode.SYSTEM_PIP,
                stopPlaybackOnExit = false
            )
        )
        assertFalse(
            shouldClearPlaybackNotificationOnNavigationExit(
                mode = SettingsManager.MiniPlayerMode.IN_APP_ONLY,
                stopPlaybackOnExit = false
            )
        )
    }

    @Test
    fun notificationIconShouldFollowSelectedAppIconKey() {
        assertEquals(R.mipmap.ic_launcher_telegram_blue_round, resolveNotificationSmallIconRes("icon_telegram_blue"))
        assertEquals(R.mipmap.ic_launcher_neon_round, resolveNotificationSmallIconRes("Neon"))
        assertEquals(R.mipmap.ic_launcher_telegram_pink_round, resolveNotificationSmallIconRes("Pink"))
        assertEquals(R.mipmap.ic_launcher_telegram_dark_round, resolveNotificationSmallIconRes("Dark"))
        assertEquals(R.mipmap.ic_launcher_3d_round, resolveNotificationSmallIconRes("unknown_key"))
    }
}
