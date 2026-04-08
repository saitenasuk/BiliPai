package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.UiPreset
import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceUiPresetDescriptionPolicyTest {

    @Test
    fun `resolveAppearanceUiPresetDescription should return ios copy for ios preset`() {
        val description = resolveAppearanceUiPresetDescription(
            preset = UiPreset.IOS,
            iosTitle = "iOS Preset",
            iosSummary = "Keep stronger glass, roundness, and Cupertino-style details.",
            androidTitle = "Android Native Preset",
            androidSummary = "Use Material 3 structure while keeping blur and liquid glass."
        )

        assertEquals("iOS Preset", description.title)
        assertEquals(
            "Keep stronger glass, roundness, and Cupertino-style details.",
            description.summary
        )
    }

    @Test
    fun `resolveAppearanceUiPresetDescription should return android native copy for md3 preset`() {
        val description = resolveAppearanceUiPresetDescription(
            preset = UiPreset.MD3,
            iosTitle = "iOS Preset",
            iosSummary = "Keep stronger glass, roundness, and Cupertino-style details.",
            androidTitle = "Android Native Preset",
            androidSummary = "Use Material 3 structure while keeping blur and liquid glass."
        )

        assertEquals("Android Native Preset", description.title)
        assertEquals(
            "Use Material 3 structure while keeping blur and liquid glass.",
            description.summary
        )
    }
}
