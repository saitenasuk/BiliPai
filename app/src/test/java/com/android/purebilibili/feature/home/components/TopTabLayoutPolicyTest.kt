package com.android.purebilibili.feature.home.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopTabLayoutPolicyTest {

    @Test
    fun `visible slot count should stay in compact range`() {
        assertEquals(4, resolveTopTabVisibleSlots(1))
        assertEquals(4, resolveTopTabVisibleSlots(4))
        assertEquals(5, resolveTopTabVisibleSlots(5))
        assertEquals(5, resolveTopTabVisibleSlots(8))
    }

    @Test
    fun `floating style should enforce wider min width to avoid clipping`() {
        assertEquals(72f, resolveTopTabItemWidthDp(260f, 5, isFloatingStyle = true), 0.001f)
    }

    @Test
    fun `docked style should keep a denser minimum width`() {
        assertEquals(64f, resolveTopTabItemWidthDp(260f, 5, isFloatingStyle = false), 0.001f)
    }

    @Test
    fun `wide containers should use proportional width`() {
        assertEquals(100f, resolveTopTabItemWidthDp(500f, 5, isFloatingStyle = true), 0.001f)
    }

    @Test
    fun `live route decision should follow category label not fixed index`() {
        assertTrue(shouldRouteTopTabToLivePage("直播"))
        assertFalse(shouldRouteTopTabToLivePage("推荐"))
        assertFalse(shouldRouteTopTabToLivePage("LIVE"))
    }

    @Test
    fun `md3 top tabs keep four visible slots on every device width`() {
        assertEquals(4, resolveMd3TopTabVisibleSlots())
        assertEquals(80f, resolveMd3TopTabItemWidthDp(containerWidthDp = 320f), 0.001f)
        assertEquals(90f, resolveMd3TopTabItemWidthDp(containerWidthDp = 360f), 0.001f)
        assertEquals(160f, resolveMd3TopTabItemWidthDp(containerWidthDp = 640f), 0.001f)
    }

    @Test
    fun `md3 top tabs keep selected category within four visible slots`() {
        assertEquals(
            listOf(0, 1, 2, 3),
            resolveMd3VisibleTabIndices(totalCount = 5, selectedIndex = 0)
        )
        assertEquals(
            listOf(0, 1, 2, 4),
            resolveMd3VisibleTabIndices(totalCount = 5, selectedIndex = 4)
        )
        assertEquals(
            3,
            resolveMd3SelectedVisibleIndex(
                visibleIndices = listOf(0, 1, 2, 4),
                selectedIndex = 4
            )
        )
    }
}
