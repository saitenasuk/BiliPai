package com.android.purebilibili.feature.profile

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileWallpaperActionLayoutPolicyTest {

    @Test
    fun regularPhoneWidth_usesCompactThreeUpWallpaperStrip() {
        assertEquals(3, resolveProfileWallpaperActionColumnCount(screenWidthDp = 393))
    }

    @Test
    fun narrowPhoneWidth_wrapsCompactWallpaperStripToTwoColumns() {
        assertEquals(2, resolveProfileWallpaperActionColumnCount(screenWidthDp = 320))
    }

    @Test
    fun wallpaperStripBlurFollowsSharedBlurToggle() {
        assertEquals(
            true,
            resolveProfileWallpaperActionBlurEnabled(
                headerBlurEnabled = false,
                bottomBarBlurEnabled = true
            )
        )
    }
}
