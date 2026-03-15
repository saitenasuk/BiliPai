package com.android.purebilibili.feature.dynamic

import org.junit.Assert.assertEquals
import org.junit.Test
import androidx.compose.ui.unit.dp

class DynamicLayoutPolicyTest {

    @Test
    fun `dynamic feed narrows large-screen content width slightly`() {
        assertEquals(760.dp, resolveDynamicFeedMaxWidth())
    }

    @Test
    fun `dynamic cards tighten outer and inner horizontal spacing`() {
        assertEquals(10.dp, resolveDynamicCardOuterPadding())
        assertEquals(14.dp, resolveDynamicCardContentPadding())
    }

    @Test
    fun `dynamic top areas tighten user list and tab spacing`() {
        assertEquals(10.dp, resolveDynamicHorizontalUserListHorizontalPadding())
        assertEquals(10.dp, resolveDynamicHorizontalUserListSpacing())
        assertEquals(14.dp, resolveDynamicTopBarHorizontalPadding())
    }

    @Test
    fun `dynamic sidebar trims width without crowding avatar affordances`() {
        assertEquals(68.dp, resolveDynamicSidebarWidth(isExpanded = true))
        assertEquals(60.dp, resolveDynamicSidebarWidth(isExpanded = false))
    }
}
