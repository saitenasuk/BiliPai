package com.android.purebilibili.core.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UiPresetPolicyTest {

    @Test
    fun unknownPresetValue_fallsBackToIos() {
        assertEquals(UiPreset.IOS, UiPreset.fromValue(99))
    }

    @Test
    fun iosPreset_usesExistingIosChromeAndMotion() {
        val profile = resolveUiRenderingProfile(UiPreset.IOS)

        assertFalse(profile.useMaterialChrome)
        assertFalse(profile.useMaterialMotion)
        assertFalse(profile.useMaterialIcons)
    }

    @Test
    fun md3Preset_usesMaterialChromeAndMotion() {
        val profile = resolveUiRenderingProfile(UiPreset.MD3)

        assertTrue(profile.useMaterialChrome)
        assertTrue(profile.useMaterialMotion)
        assertTrue(profile.useMaterialIcons)
    }
}
