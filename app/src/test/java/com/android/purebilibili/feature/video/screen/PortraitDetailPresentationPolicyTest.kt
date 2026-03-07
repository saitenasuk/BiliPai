package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitDetailPresentationPolicyTest {

    @Test
    fun portraitFullscreenPlayback_usesDedicatedPlayerToAvoidSharedSurfaceGhosting() {
        assertFalse(shouldUseSharedPlayerForPortraitFullscreen())
    }

    @Test
    fun officialInlinePortraitMode_enabledForPhoneVerticalVideo() {
        assertTrue(
            shouldUseOfficialInlinePortraitDetailExperience(
                useTabletLayout = false,
                isVerticalVideo = true,
                portraitExperienceEnabled = true
            )
        )
    }

    @Test
    fun officialInlinePortraitMode_disabledForTabletLayout() {
        assertFalse(
            shouldUseOfficialInlinePortraitDetailExperience(
                useTabletLayout = true,
                isVerticalVideo = true,
                portraitExperienceEnabled = true
            )
        )
    }

    @Test
    fun standalonePortraitPager_showsWhenPortraitFullscreenRequestedEvenInInlineMode() {
        assertTrue(
            shouldShowStandalonePortraitPager(
                portraitExperienceEnabled = true,
                isPortraitFullscreen = true,
                useOfficialInlinePortraitDetailExperience = true,
                hasPlayableState = true
            )
        )
    }

    @Test
    fun portraitFullscreenRequest_isAllowedWhenPortraitExperienceEnabled() {
        assertTrue(
            shouldActivatePortraitFullscreenState(
                portraitExperienceEnabled = true
            )
        )
        assertFalse(
            shouldActivatePortraitFullscreenState(
                portraitExperienceEnabled = false
            )
        )
    }

    @Test
    fun inlinePortraitPlayerLayout_keepsExpandedViewportPortraitAndCentered() {
        val spec = resolvePortraitInlinePlayerLayoutSpec(
            screenWidthDp = 412f,
            screenHeightDp = 915f,
            isCollapsed = false
        )

        assertTrue(spec.widthDp < 412f)
        assertTrue(spec.widthDp > 240f)
        assertTrue(spec.heightDp > spec.widthDp)
    }

    @Test
    fun inlinePortraitPlayerLayout_shrinksViewportWhenCollapsed() {
        val expanded = resolvePortraitInlinePlayerLayoutSpec(
            screenWidthDp = 412f,
            screenHeightDp = 915f,
            isCollapsed = false
        )
        val collapsed = resolvePortraitInlinePlayerLayoutSpec(
            screenWidthDp = 412f,
            screenHeightDp = 915f,
            isCollapsed = true
        )

        assertTrue(collapsed.widthDp < expanded.widthDp)
        assertTrue(collapsed.heightDp < expanded.heightDp)
        assertTrue(collapsed.heightDp > collapsed.widthDp)
    }

    @Test
    fun inlinePortraitScrollTransform_enabledForOfficialModeEvenWhenSettingIsOff() {
        assertTrue(
            shouldEnableInlinePortraitScrollTransform(
                swipeHidePlayerEnabled = false,
                useOfficialInlinePortraitDetailExperience = true
            )
        )
    }

    @Test
    fun portraitButton_entersPortraitFullscreenInOfficialInlineMode() {
        assertEquals(
            PortraitFullscreenButtonAction.ENTER_PORTRAIT_FULLSCREEN,
            resolvePortraitFullscreenButtonAction(
                useOfficialInlinePortraitDetailExperience = true
            )
        )
    }

    @Test
    fun portraitButton_entersPortraitFullscreenInRegularModeToo() {
        assertEquals(
            PortraitFullscreenButtonAction.ENTER_PORTRAIT_FULLSCREEN,
            resolvePortraitFullscreenButtonAction(
                useOfficialInlinePortraitDetailExperience = false
            )
        )
    }
}
