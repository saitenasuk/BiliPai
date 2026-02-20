package com.android.purebilibili.navigation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppNavigationTransitionPolicyTest {

    @Test
    fun tabletBackToHomeFromVideo_usesSeamlessTransition() {
        assertTrue(
            shouldUseTabletSeamlessBackTransition(
                isTabletLayout = true,
                cardTransitionEnabled = true,
                fromRoute = VideoRoute.route,
                toRoute = ScreenRoutes.Home.route
            )
        )
    }

    @Test
    fun phoneBackToHomeFromVideo_keepsDefaultTransition() {
        assertFalse(
            shouldUseTabletSeamlessBackTransition(
                isTabletLayout = false,
                cardTransitionEnabled = true,
                fromRoute = VideoRoute.route,
                toRoute = ScreenRoutes.Home.route
            )
        )
    }

    @Test
    fun tabletBackToHomeWithoutCardTransition_keepsDefaultTransition() {
        assertFalse(
            shouldUseTabletSeamlessBackTransition(
                isTabletLayout = true,
                cardTransitionEnabled = false,
                fromRoute = VideoRoute.route,
                toRoute = ScreenRoutes.Home.route
            )
        )
    }

    @Test
    fun tabletBackToHomeFromNonVideoRoute_keepsDefaultTransition() {
        assertFalse(
            shouldUseTabletSeamlessBackTransition(
                isTabletLayout = true,
                cardTransitionEnabled = true,
                fromRoute = ScreenRoutes.Search.route,
                toRoute = ScreenRoutes.Home.route
            )
        )
    }
}
