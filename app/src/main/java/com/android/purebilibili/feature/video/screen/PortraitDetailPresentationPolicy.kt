package com.android.purebilibili.feature.video.screen

import kotlin.math.max
import kotlin.math.min

internal data class PortraitInlinePlayerLayoutSpec(
    val widthDp: Float,
    val heightDp: Float
)

internal enum class PortraitFullscreenButtonAction {
    ENTER_PORTRAIT_FULLSCREEN
}

internal fun shouldUseOfficialInlinePortraitDetailExperience(
    useTabletLayout: Boolean,
    isVerticalVideo: Boolean,
    portraitExperienceEnabled: Boolean
): Boolean {
    return portraitExperienceEnabled && !useTabletLayout && isVerticalVideo
}

internal fun shouldUseSharedPlayerForPortraitFullscreen(): Boolean {
    return false
}

internal fun shouldShowStandalonePortraitPager(
    portraitExperienceEnabled: Boolean,
    isPortraitFullscreen: Boolean,
    useOfficialInlinePortraitDetailExperience: Boolean,
    hasPlayableState: Boolean
): Boolean {
    return portraitExperienceEnabled &&
        isPortraitFullscreen &&
        hasPlayableState
}

internal fun shouldActivatePortraitFullscreenState(
    portraitExperienceEnabled: Boolean
): Boolean {
    return portraitExperienceEnabled
}

internal fun shouldEnableInlinePortraitScrollTransform(
    swipeHidePlayerEnabled: Boolean,
    useOfficialInlinePortraitDetailExperience: Boolean
): Boolean {
    return swipeHidePlayerEnabled || useOfficialInlinePortraitDetailExperience
}

internal fun resolvePortraitFullscreenButtonAction(
    useOfficialInlinePortraitDetailExperience: Boolean
): PortraitFullscreenButtonAction {
    return PortraitFullscreenButtonAction.ENTER_PORTRAIT_FULLSCREEN
}

internal fun resolvePortraitInlinePlayerLayoutSpec(
    screenWidthDp: Float,
    screenHeightDp: Float,
    isCollapsed: Boolean
): PortraitInlinePlayerLayoutSpec {
    if (isCollapsed) {
        val width = min(screenWidthDp * 0.34f, 168f)
        return PortraitInlinePlayerLayoutSpec(
            widthDp = width,
            heightDp = width * 16f / 9f
        )
    }

    val maxWidth = screenWidthDp * 0.9f
    val minWidth = screenWidthDp * 0.62f
    val maxHeight = screenHeightDp * 0.72f
    val height = min(maxWidth * 16f / 9f, maxHeight)
    val width = max(minWidth, min(maxWidth, height * 9f / 16f))
    return PortraitInlinePlayerLayoutSpec(
        widthDp = width,
        heightDp = height
    )
}
