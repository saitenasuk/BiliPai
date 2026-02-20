package com.android.purebilibili.navigation

import com.android.purebilibili.feature.home.HomeVideoClickRequest
import com.android.purebilibili.feature.home.HomeVideoClickSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeVideoNavigationPolicyTest {

    @Test
    fun resolveIntent_returnsNull_whenBvidBlank() {
        val request = HomeVideoClickRequest(
            bvid = "   ",
            cid = 123L,
            coverUrl = "https://i0.hdslb.com/test.jpg",
            source = HomeVideoClickSource.GRID
        )

        val intent = resolveHomeVideoNavigationIntent(request)

        assertNull(intent)
    }

    @Test
    fun resolveIntent_normalizesCid_whenNonPositive() {
        val request = HomeVideoClickRequest(
            bvid = "BV1abc",
            cid = -9L,
            coverUrl = "",
            source = HomeVideoClickSource.TODAY_WATCH
        )

        val intent = resolveHomeVideoNavigationIntent(request)

        assertEquals(0L, intent?.cid)
    }

    @Test
    fun resolveIntent_preservesSourceMetadata() {
        val request = HomeVideoClickRequest(
            bvid = "BV1xyz",
            cid = 100L,
            coverUrl = "cover",
            source = HomeVideoClickSource.PREVIEW
        )

        val intent = resolveHomeVideoNavigationIntent(request)

        assertEquals(HomeVideoClickSource.PREVIEW, intent?.source)
    }

    @Test
    fun resolveRoute_buildsVideoRouteFromRequest() {
        val request = HomeVideoClickRequest(
            bvid = "BV1route",
            cid = 88L,
            coverUrl = "https://img.test.com/a b.jpg",
            source = HomeVideoClickSource.GRID
        )

        val route = resolveHomeVideoRoute(request)

        assertEquals(
            "video/BV1route?cid=88&cover=https%3A%2F%2Fimg.test.com%2Fa+b.jpg",
            route
        )
    }
}
