package com.android.purebilibili.core.util

/**
 * 统一平板导航模式判定，避免不同页面断点不一致导致底栏/侧栏显示冲突。
 */
internal fun shouldUseSidebarNavigationForLayout(
    windowSizeClass: WindowSizeClass,
    tabletUseSidebar: Boolean
): Boolean {
    return tabletUseSidebar && windowSizeClass.shouldUseSideNavigation
}
