package com.android.purebilibili.feature.home.components

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.ui.blur.BlurIntensity
import kotlin.test.Test
import kotlin.test.assertEquals

class BottomBarSurfaceColorPolicyTest {

    @Test
    fun `blur enabled follows blur style alpha`() {
        val color = resolveBottomBarSurfaceColor(
            surfaceColor = Color.White,
            blurEnabled = true,
            blurIntensity = BlurIntensity.THIN
        )

        assertEquals(0.4f, color.alpha, 0.001f)
    }

    @Test
    fun `blur disabled keeps light theme surface color`() {
        val color = resolveBottomBarSurfaceColor(
            surfaceColor = Color.White,
            blurEnabled = false,
            blurIntensity = BlurIntensity.THIN
        )

        assertEquals(Color.White, color)
    }

    @Test
    fun `blur disabled keeps dark theme surface color`() {
        val color = resolveBottomBarSurfaceColor(
            surfaceColor = Color(0xFF121212),
            blurEnabled = false,
            blurIntensity = BlurIntensity.THIN
        )

        assertEquals(Color(0xFF121212), color)
    }
}
