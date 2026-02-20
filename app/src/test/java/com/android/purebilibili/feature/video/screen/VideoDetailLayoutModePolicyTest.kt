package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailLayoutModePolicyTest {

    @Test
    fun expanded_usesTabletLayout() {
        assertTrue(
            shouldUseTabletVideoLayout(
                isExpandedScreen = true
            )
        )
    }

    @Test
    fun compact_doesNotUseTabletLayout() {
        assertFalse(
            shouldUseTabletVideoLayout(
                isExpandedScreen = false
            )
        )
    }

    @Test
    fun autoRotatePolicy_appliesOnlyOnPhoneLayout() {
        assertTrue(
            shouldApplyPhoneAutoRotatePolicy(
                useTabletLayout = false
            )
        )
        assertFalse(
            shouldApplyPhoneAutoRotatePolicy(
                useTabletLayout = true
            )
        )
        assertFalse(
            shouldApplyPhoneAutoRotatePolicy(
                useTabletLayout = true
            )
        )
    }

    @Test
    fun portraitAndInteractionUi_policiesReflectCurrentBehavior() {
        assertTrue(shouldEnablePortraitExperience())
        assertFalse(shouldShowVideoDetailBottomInteractionBar())
        assertFalse(shouldShowVideoDetailActionButtons())
    }

    @Test
    fun interactionUi_isHiddenOnPhoneToo() {
        assertFalse(shouldShowVideoDetailBottomInteractionBar())
        assertFalse(shouldShowVideoDetailActionButtons())
    }

    @Test
    fun orientationDrivenFullscreen_isPhoneOnly() {
        assertTrue(
            shouldUseOrientationDrivenFullscreen(
                useTabletLayout = false
            )
        )
        assertFalse(
            shouldUseOrientationDrivenFullscreen(
                useTabletLayout = true
            )
        )
        assertFalse(
            shouldUseOrientationDrivenFullscreen(
                useTabletLayout = true
            )
        )
    }

    @Test
    fun sharedCoverTransition_requiresSwitchAndBothScopes() {
        assertTrue(
            shouldEnableVideoCoverSharedTransition(
                transitionEnabled = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true
            )
        )
        assertFalse(
            shouldEnableVideoCoverSharedTransition(
                transitionEnabled = false,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true
            )
        )
        assertFalse(
            shouldEnableVideoCoverSharedTransition(
                transitionEnabled = true,
                hasSharedTransitionScope = false,
                hasAnimatedVisibilityScope = true
            )
        )
        assertFalse(
            shouldEnableVideoCoverSharedTransition(
                transitionEnabled = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = false
            )
        )
    }

    @Test
    fun highRefreshMode_prefersHighestRefreshThenResolution() {
        val selected = resolvePreferredHighRefreshModeId(
            currentModeId = 1,
            supportedModes = listOf(
                RefreshModeCandidate(modeId = 1, refreshRate = 60f, width = 2400, height = 1080),
                RefreshModeCandidate(modeId = 2, refreshRate = 120f, width = 1920, height = 1080),
                RefreshModeCandidate(modeId = 3, refreshRate = 120f, width = 2400, height = 1080)
            )
        )

        assertEquals(3, selected)
    }

    @Test
    fun highRefreshMode_returnsNullWhenNoEligibleHighRefresh() {
        val selected = resolvePreferredHighRefreshModeId(
            currentModeId = 1,
            supportedModes = listOf(
                RefreshModeCandidate(modeId = 1, refreshRate = 60f, width = 2400, height = 1080),
                RefreshModeCandidate(modeId = 2, refreshRate = 75f, width = 2400, height = 1080)
            )
        )

        assertEquals(null, selected)
    }
}
