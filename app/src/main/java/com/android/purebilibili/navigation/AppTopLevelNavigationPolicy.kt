package com.android.purebilibili.navigation

internal enum class TopLevelNavigationAction {
    SKIP,
    POP_EXISTING,
    NAVIGATE_WITH_RESTORE
}

internal fun resolveTopLevelNavigationAction(
    currentRoute: String?,
    targetRoute: String,
    hasTargetInBackStack: Boolean
): TopLevelNavigationAction {
    if (currentRoute == targetRoute) {
        return TopLevelNavigationAction.SKIP
    }

    if (hasTargetInBackStack) {
        return TopLevelNavigationAction.POP_EXISTING
    }

    return TopLevelNavigationAction.NAVIGATE_WITH_RESTORE
}
