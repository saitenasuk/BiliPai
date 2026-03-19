package com.android.purebilibili.feature.live

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class LiveListTabColorPolicyTest {

    @Test
    fun `resolveLiveListTabColors should use theme accent for selected tab`() {
        val colors = resolveLiveListTabColors(
            primary = Color(0xFF6750A4),
            onPrimary = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFE7E0EC),
            onSurfaceVariant = Color(0xFF49454F)
        )

        assertEquals(Color(0xFF6750A4), colors.selectedContainerColor)
        assertEquals(Color(0xFFFFFFFF), colors.selectedContentColor)
        assertEquals(Color(0xFFE7E0EC), colors.unselectedContainerColor)
        assertEquals(Color(0xFF49454F), colors.unselectedContentColor)
    }
}
