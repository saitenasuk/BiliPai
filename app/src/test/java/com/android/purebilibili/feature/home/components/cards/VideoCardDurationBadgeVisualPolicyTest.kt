package com.android.purebilibili.feature.home.components.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoCardDurationBadgeVisualPolicyTest {

    @Test
    fun `duration badge style should use transparent background to avoid black border`() {
        val style = resolveVideoCardDurationBadgeVisualStyle()

        assertEquals(0f, style.backgroundAlpha, 0.0001f)
        assertTrue(style.textShadowAlpha > 0f)
        assertTrue(style.textShadowBlurRadiusPx > 0f)
    }

    @Test
    fun `compact duration badge keeps enough width for mm ss text`() {
        val width = resolveVideoCardDurationBadgeMinWidthDp("02:57")

        assertEquals(40f, width, 0.0001f)
    }

    @Test
    fun `extended duration badge widens for hh mm ss text`() {
        val width = resolveVideoCardDurationBadgeMinWidthDp("1:25:10")

        assertEquals(52f, width, 0.0001f)
    }
}
