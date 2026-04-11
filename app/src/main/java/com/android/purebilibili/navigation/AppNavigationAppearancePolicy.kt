package com.android.purebilibili.navigation

import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset

internal data class AppNavigationAppearance(
    val cardTransitionEnabled: Boolean,
    val predictiveBackAnimationEnabled: Boolean,
    val bottomBarBlurEnabled: Boolean,
    val bottomBarLabelMode: Int,
    val bottomBarFloating: Boolean
)

private fun usesDefaultBottomBarShellSettings(homeSettings: HomeSettings): Boolean {
    return homeSettings.isBottomBarFloating &&
        homeSettings.bottomBarLabelMode == 0 &&
        homeSettings.isBottomBarBlurEnabled
}

internal fun resolveAppNavigationAppearance(
    homeSettings: HomeSettings,
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
): AppNavigationAppearance {
    val shouldUseDockedMd3Shell =
        uiPreset == UiPreset.MD3 &&
            androidNativeVariant == AndroidNativeVariant.MATERIAL3 &&
            usesDefaultBottomBarShellSettings(homeSettings)

    return AppNavigationAppearance(
        cardTransitionEnabled = homeSettings.cardTransitionEnabled,
        predictiveBackAnimationEnabled = homeSettings.predictiveBackAnimationEnabled,
        bottomBarBlurEnabled = homeSettings.isBottomBarBlurEnabled,
        bottomBarLabelMode = homeSettings.bottomBarLabelMode,
        bottomBarFloating = if (shouldUseDockedMd3Shell) {
            false
        } else {
            homeSettings.isBottomBarFloating
        }
    )
}
