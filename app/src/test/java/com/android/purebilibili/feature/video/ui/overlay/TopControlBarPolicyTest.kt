package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TopControlBarPolicyTest {

    @Test
    fun dislikeActionHiddenOnPhoneLandscape() {
        assertFalse(
            shouldShowDislikeInTopControlBar(
                widthDp = 780
            )
        )
    }

    @Test
    fun dislikeActionVisibleOnWideLandscape() {
        assertTrue(
            shouldShowDislikeInTopControlBar(
                widthDp = 1200
            )
        )
    }
}
