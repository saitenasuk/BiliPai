package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.util.HapticType
import kotlin.test.Test
import kotlin.test.assertEquals

class BottomBarInteractionPolicyTest {

    @Test
    fun materialBottomBarTap_triggersLightHapticBeforeNavigation() {
        val events = mutableListOf<String>()

        performMaterialBottomBarTap(
            haptic = { type: HapticType ->
                events += "haptic:${type.name}"
            },
            onClick = {
                events += "navigate"
            }
        )

        assertEquals(listOf("haptic:${HapticType.LIGHT.name}", "navigate"), events)
    }

    @Test
    fun primaryTap_navigate_triggersLightHapticBeforeNavigation() {
        val events = mutableListOf<String>()

        performBottomBarPrimaryTap(
            item = BottomNavItem.DYNAMIC,
            isSelected = false,
            haptic = { type ->
                events += "haptic:${type.name}"
            },
            onNavigate = {
                events += "navigate"
            },
            onHomeReselect = {
                events += "home_reselect"
            }
        )

        assertEquals(listOf("haptic:${HapticType.LIGHT.name}", "navigate"), events)
    }

    @Test
    fun primaryTap_homeReselect_triggersLightHapticBeforeRefreshAction() {
        val events = mutableListOf<String>()

        performBottomBarPrimaryTap(
            item = BottomNavItem.HOME,
            isSelected = true,
            haptic = { type ->
                events += "haptic:${type.name}"
            },
            onNavigate = {
                events += "navigate"
            },
            onHomeReselect = {
                events += "home_reselect"
            }
        )

        assertEquals(listOf("haptic:${HapticType.LIGHT.name}", "home_reselect"), events)
    }
}
