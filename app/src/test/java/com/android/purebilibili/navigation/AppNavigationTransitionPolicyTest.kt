package com.android.purebilibili.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun leavingVideoToHome_shouldStopPlaybackEagerly() {
        assertTrue(
            shouldStopPlaybackEagerlyOnVideoRouteExit(
                fromRoute = VideoRoute.route,
                toRoute = ScreenRoutes.Home.route
            )
        )
    }

    @Test
    fun leavingVideoToAudioMode_shouldNotStopPlaybackEagerly() {
        assertFalse(
            shouldStopPlaybackEagerlyOnVideoRouteExit(
                fromRoute = VideoRoute.route,
                toRoute = ScreenRoutes.AudioMode.route
            )
        )
    }

    @Test
    fun switchingBetweenVideoRoutes_shouldNotStopPlaybackEagerly() {
        assertFalse(
            shouldStopPlaybackEagerlyOnVideoRouteExit(
                fromRoute = VideoRoute.route,
                toRoute = VideoRoute.route
            )
        )
    }

    @Test
    fun nonSharedReturnToHome_leftCard_slidesRightToLeft() {
        assertEquals(
            VideoPopExitDirection.LEFT,
            resolveVideoPopExitDirection(
                targetRoute = ScreenRoutes.Home.route,
                isSingleColumnCard = false,
                lastClickedCardCenterX = 0.2f
            )
        )
    }

    @Test
    fun nonSharedReturnToHome_rightCard_slidesLeftToRight() {
        assertEquals(
            VideoPopExitDirection.RIGHT,
            resolveVideoPopExitDirection(
                targetRoute = ScreenRoutes.Home.route,
                isSingleColumnCard = false,
                lastClickedCardCenterX = 0.8f
            )
        )
    }

    @Test
    fun nonSharedReturnToNonHome_singleColumn_slidesDown() {
        assertEquals(
            VideoPopExitDirection.DOWN,
            resolveVideoPopExitDirection(
                targetRoute = ScreenRoutes.Search.route,
                isSingleColumnCard = true,
                lastClickedCardCenterX = 0.2f
            )
        )
    }
}
