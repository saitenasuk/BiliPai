package com.android.purebilibili.feature.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchResultCardAppearancePolicyTest {

    @Test
    fun searchCardBlur_enabledWhenEitherHomeBlurToggleIsOn() {
        assertTrue(
            resolveSearchCardBlurEnabled(
                headerBlurEnabled = true,
                bottomBarBlurEnabled = false
            )
        )
        assertTrue(
            resolveSearchCardBlurEnabled(
                headerBlurEnabled = false,
                bottomBarBlurEnabled = true
            )
        )
        assertFalse(
            resolveSearchCardBlurEnabled(
                headerBlurEnabled = false,
                bottomBarBlurEnabled = false
            )
        )
    }

    @Test
    fun videoSearchAppearance_reusesHomeGlassAndBadgeInputs() {
        val appearance = resolveSearchVideoCardAppearance(
            liquidGlassEnabled = false,
            blurEnabled = true,
            showHomeCoverGlassBadges = false,
            showHomeInfoGlassBadges = true
        )

        assertFalse(appearance.glassEnabled)
        assertTrue(appearance.blurEnabled)
        assertFalse(appearance.showCoverGlassBadges)
        assertTrue(appearance.showInfoGlassBadges)
    }

    @Test
    fun genericSearchResultCard_switchesBetweenGlassAndPlainStyles() {
        val glass = resolveSearchResultCardAppearance(
            liquidGlassEnabled = true
        )
        val plain = resolveSearchResultCardAppearance(
            liquidGlassEnabled = false
        )

        assertEquals(SearchResultCardSurfaceStyle.GLASS, glass.surfaceStyle)
        assertEquals(0.92f, glass.containerAlpha)
        assertEquals(0.12f, glass.borderAlpha)
        assertEquals(0, glass.tonalElevationDp)

        assertEquals(SearchResultCardSurfaceStyle.PLAIN, plain.surfaceStyle)
        assertEquals(1f, plain.containerAlpha)
        assertEquals(0f, plain.borderAlpha)
        assertEquals(1, plain.shadowElevationDp)
    }
}
