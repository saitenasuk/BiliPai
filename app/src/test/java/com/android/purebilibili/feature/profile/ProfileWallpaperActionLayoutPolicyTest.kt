package com.android.purebilibili.feature.profile

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileWallpaperActionLayoutPolicyTest {

    @Test
    fun regularPhoneWidth_keepsWallpaperActionsSideBySide() {
        assertEquals(2, resolveProfileWallpaperActionColumnCount(screenWidthDp = 393))
    }

    @Test
    fun narrowPhoneWidth_stacksWallpaperActionsVertically() {
        assertEquals(1, resolveProfileWallpaperActionColumnCount(screenWidthDp = 320))
    }
}
