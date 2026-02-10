package com.android.purebilibili.feature.home.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TopTabIndicatorGeometryTest {

    @Test
    fun `indicator width follows ratio when within bounds`() {
        val width = resolveTopTabIndicatorWidthPx(
            itemWidthPx = 100f,
            widthRatio = 0.78f,
            minWidthPx = 48f,
            horizontalInsetPx = 8f
        )

        assertEquals(78f, width, 0.01f)
    }

    @Test
    fun `indicator width uses minimum width on narrow tabs`() {
        val width = resolveTopTabIndicatorWidthPx(
            itemWidthPx = 54f,
            widthRatio = 0.78f,
            minWidthPx = 48f,
            horizontalInsetPx = 8f
        )

        assertEquals(48f, width, 0.01f)
    }

    @Test
    fun `indicator width respects max width from inset`() {
        val width = resolveTopTabIndicatorWidthPx(
            itemWidthPx = 200f,
            widthRatio = 0.95f,
            minWidthPx = 48f,
            horizontalInsetPx = 16f
        )

        assertEquals(184f, width, 0.01f)
    }
}
