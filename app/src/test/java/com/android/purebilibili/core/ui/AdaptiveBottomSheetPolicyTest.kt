package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.UiPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveBottomSheetPolicyTest {

    @Test
    fun `md3 preset should use material drag handle and larger corner radius`() {
        val spec = resolveAdaptiveBottomSheetVisualSpec(UiPreset.MD3)

        assertEquals(28, spec.cornerRadiusDp)
        assertTrue(spec.useMaterialDragHandle)
    }

    @Test
    fun `ios preset should preserve compact sheet chrome`() {
        val spec = resolveAdaptiveBottomSheetVisualSpec(UiPreset.IOS)

        assertEquals(14, spec.cornerRadiusDp)
        assertFalse(spec.useMaterialDragHandle)
    }
}
