package com.android.purebilibili.core.ui.adaptive

import com.android.purebilibili.core.util.WindowWidthSizeClass

enum class MotionTier {
    Reduced,
    Normal,
    Enhanced
}

data class DeviceUiProfile(
    val widthSizeClass: WindowWidthSizeClass,
    val isTablet: Boolean,
    val motionTier: MotionTier
)

fun resolveDeviceUiProfile(
    widthSizeClass: WindowWidthSizeClass
): DeviceUiProfile {
    val isTablet = widthSizeClass != WindowWidthSizeClass.Compact
    val motionTier = if (widthSizeClass == WindowWidthSizeClass.Expanded) {
        MotionTier.Enhanced
    } else {
        MotionTier.Normal
    }

    return DeviceUiProfile(
        widthSizeClass = widthSizeClass,
        isTablet = isTablet,
        motionTier = motionTier
    )
}
