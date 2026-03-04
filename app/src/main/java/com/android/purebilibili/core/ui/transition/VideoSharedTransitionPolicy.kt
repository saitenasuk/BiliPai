package com.android.purebilibili.core.ui.transition

internal enum class VideoSharedTransitionProfile {
    COVER_ONLY,
    COVER_AND_METADATA
}

internal fun resolveVideoSharedTransitionProfile(): VideoSharedTransitionProfile {
    // Phase A: stability first. Keep only cover shared-element transitions.
    return VideoSharedTransitionProfile.COVER_ONLY
}

internal fun shouldEnableVideoCoverSharedTransition(
    transitionEnabled: Boolean,
    hasSharedTransitionScope: Boolean,
    hasAnimatedVisibilityScope: Boolean
): Boolean {
    return transitionEnabled &&
        hasSharedTransitionScope &&
        hasAnimatedVisibilityScope
}

internal fun shouldEnableVideoMetadataSharedTransition(
    coverSharedEnabled: Boolean,
    isQuickReturnLimited: Boolean,
    profile: VideoSharedTransitionProfile = resolveVideoSharedTransitionProfile()
): Boolean {
    if (!coverSharedEnabled) return false
    if (isQuickReturnLimited) return false
    return profile == VideoSharedTransitionProfile.COVER_AND_METADATA
}
