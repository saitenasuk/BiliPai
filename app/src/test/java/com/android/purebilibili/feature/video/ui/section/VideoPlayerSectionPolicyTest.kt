package com.android.purebilibili.feature.video.ui.section

import androidx.media3.common.PlaybackParameters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoPlayerSectionPolicyTest {

    @Test
    fun inlinePlayerTakeover_disablesKeepingLastFrame_whenPortraitFullscreenOwnsPlayback() {
        assertFalse(
            shouldKeepInlinePlayerContentOnReset(
                isPortraitFullscreen = true
            )
        )
        assertTrue(
            shouldKeepInlinePlayerContentOnReset(
                isPortraitFullscreen = false
            )
        )
    }

    @Test
    fun inlinePlayerTakeover_hidesInlinePlayerView_whenPortraitFullscreenOwnsPlayback() {
        assertFalse(
            shouldShowInlinePlayerView(
                isPortraitFullscreen = true
            )
        )
        assertTrue(
            shouldShowInlinePlayerView(
                isPortraitFullscreen = false
            )
        )
    }

    @Test
    fun livePlayerSharedElement_enabledOnlyWhenAllGuardsPass() {
        assertTrue(
            shouldEnableLivePlayerSharedElement(
                transitionEnabled = true,
                allowLivePlayerSharedElement = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true
            )
        )
    }

    @Test
    fun livePlayerSharedElement_disabledWhenPredictiveBackRequiresStability() {
        assertFalse(
            shouldEnableLivePlayerSharedElement(
                transitionEnabled = true,
                allowLivePlayerSharedElement = false,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true
            )
        )
    }

    @Test
    fun livePlayerSharedElement_disabledWhenTransitionSwitchOff() {
        assertFalse(
            shouldEnableLivePlayerSharedElement(
                transitionEnabled = false,
                allowLivePlayerSharedElement = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true
            )
        )
    }

    @Test
    fun playerSurfaceRebind_onlyWhenForegroundVideoSurfaceCanRender() {
        assertTrue(
            shouldRebindPlayerSurfaceOnForeground(
                hasPlayerView = true,
                isInPipMode = false,
                videoWidth = 1920,
                videoHeight = 1080
            )
        )
    }

    @Test
    fun playerSurfaceRebind_skipsWhenPipOrVideoSizeMissing() {
        assertFalse(
            shouldRebindPlayerSurfaceOnForeground(
                hasPlayerView = true,
                isInPipMode = true,
                videoWidth = 1920,
                videoHeight = 1080
            )
        )
        assertFalse(
            shouldRebindPlayerSurfaceOnForeground(
                hasPlayerView = true,
                isInPipMode = false,
                videoWidth = 0,
                videoHeight = 1080
            )
        )
        assertFalse(
            shouldRebindPlayerSurfaceOnForeground(
                hasPlayerView = false,
                isInPipMode = false,
                videoWidth = 1920,
                videoHeight = 1080
            )
        )
    }

    @Test
    fun longPressSpeed_clampsHiResToCompatibilityLimit() {
        val effective = resolveEffectiveLongPressSpeed(
            requestedSpeed = 3.0f,
            currentAudioQuality = 30251
        )

        assertTrue(effective == 1.5f)
    }

    @Test
    fun longPressSpeed_keepsStandardAudioAtRequestedHighSpeed() {
        val effective = resolveEffectiveLongPressSpeed(
            requestedSpeed = 3.0f,
            currentAudioQuality = 30280
        )

        assertTrue(effective == 3.0f)
    }

    @Test
    fun longPressSpeed_keepsRequestedSpeedWithinCompatibilityLimit() {
        val effective = resolveEffectiveLongPressSpeed(
            requestedSpeed = 1.25f,
            currentAudioQuality = 30280
        )

        assertTrue(effective == 1.25f)
    }

    @Test
    fun longPressPlaybackParameters_forceNaturalPitch() {
        val parameters = resolveLongPressPlaybackParameters(
            requestedSpeed = 1.25f,
            currentAudioQuality = 30280
        )

        assertEquals(1.25f, parameters.speed)
        assertEquals(1.0f, parameters.pitch)
    }

    @Test
    fun longPressPlaybackParameters_clampHiResSpeedBeforeApplyingPitchSafePlayback() {
        val parameters = resolveLongPressPlaybackParameters(
            requestedSpeed = 3.0f,
            currentAudioQuality = 30251
        )

        assertEquals(PlaybackParameters(1.5f, 1.0f), parameters)
    }

    @Test
    fun longPressPlaybackParameters_keepStandardAudioRequestedHighSpeed() {
        val parameters = resolveLongPressPlaybackParameters(
            requestedSpeed = 3.0f,
            currentAudioQuality = 30280
        )

        assertEquals(PlaybackParameters(3.0f, 1.0f), parameters)
    }

    @Test
    fun playbackReadyAutoFullscreen_enabledForPhonesInOrientationDrivenMode() {
        assertTrue(
            shouldAllowPlaybackStateAutoFullscreen(
                smallestScreenWidthDp = 411
            )
        )
    }

    @Test
    fun playbackReadyAutoFullscreen_disabledForTabletsEvenWhenUsingCompactLayout() {
        assertFalse(
            shouldAllowPlaybackStateAutoFullscreen(
                smallestScreenWidthDp = 600
            )
        )
    }
}
