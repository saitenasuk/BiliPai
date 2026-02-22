package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class MobileSettingsRootSectionOrderTest {

    @Test
    fun shouldUseFlatGroupedOrderForSettingsHome() {
        assertEquals(
            listOf(
                MobileSettingsRootSection.FOLLOW_AUTHOR,
                MobileSettingsRootSection.GENERAL,
                MobileSettingsRootSection.PRIVACY,
                MobileSettingsRootSection.STORAGE,
                MobileSettingsRootSection.DEVELOPER,
                MobileSettingsRootSection.FEED,
                MobileSettingsRootSection.ABOUT,
                MobileSettingsRootSection.SUPPORT
            ),
            resolveMobileSettingsRootSectionOrder()
        )
    }
}
