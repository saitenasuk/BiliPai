package com.android.purebilibili.navigation

import com.android.purebilibili.feature.home.components.BottomNavItem
import kotlin.test.Test
import kotlin.test.assertEquals

class AppTopLevelNavigationPolicyTest {

    @Test
    fun returnsSkip_whenCurrentRouteAlreadyMatchesTarget() {
        val action = resolveTopLevelNavigationAction(
            currentRoute = ScreenRoutes.Profile.route,
            targetRoute = ScreenRoutes.Profile.route,
            hasTargetInBackStack = true
        )

        assertEquals(TopLevelNavigationAction.SKIP, action)
    }

    @Test
    fun returnsPopExisting_whenTargetExistsInBackStack() {
        val action = resolveTopLevelNavigationAction(
            currentRoute = ScreenRoutes.History.route,
            targetRoute = ScreenRoutes.Profile.route,
            hasTargetInBackStack = true
        )

        assertEquals(TopLevelNavigationAction.POP_EXISTING, action)
    }

    @Test
    fun returnsNavigateWithRestore_whenTargetNotInBackStack() {
        val action = resolveTopLevelNavigationAction(
            currentRoute = ScreenRoutes.History.route,
            targetRoute = ScreenRoutes.Profile.route,
            hasTargetInBackStack = false
        )

        assertEquals(TopLevelNavigationAction.NAVIGATE_WITH_RESTORE, action)
    }

    @Test
    fun selectedHomeBottomBarTap_requestsScrollToTop_insteadOfNavigate() {
        val action = resolveBottomBarSelectionAction(
            currentItem = BottomNavItem.HOME,
            tappedItem = BottomNavItem.HOME
        )

        assertEquals(BottomBarSelectionAction.HOME_RESELECT, action)
    }

    @Test
    fun nonHomeOrNonReselectBottomBarTap_keepsNavigateAction() {
        assertEquals(
            BottomBarSelectionAction.NAVIGATE,
            resolveBottomBarSelectionAction(
                currentItem = BottomNavItem.HISTORY,
                tappedItem = BottomNavItem.HOME
            )
        )
        assertEquals(
            BottomBarSelectionAction.NAVIGATE,
            resolveBottomBarSelectionAction(
                currentItem = BottomNavItem.HOME,
                tappedItem = BottomNavItem.DYNAMIC
            )
        )
    }
}
