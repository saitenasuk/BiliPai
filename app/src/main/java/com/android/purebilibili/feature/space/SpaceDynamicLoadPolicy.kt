package com.android.purebilibili.feature.space

enum class SpaceDynamicPresentationState {
    LOADING,
    CONTENT,
    EMPTY,
    ERROR
}

fun resolveSpaceDynamicPresentationState(
    itemCount: Int,
    isLoading: Boolean,
    hasLoadedOnce: Boolean,
    lastLoadFailed: Boolean
): SpaceDynamicPresentationState {
    if (itemCount > 0) return SpaceDynamicPresentationState.CONTENT
    if (isLoading || !hasLoadedOnce) return SpaceDynamicPresentationState.LOADING
    if (lastLoadFailed) return SpaceDynamicPresentationState.ERROR
    return SpaceDynamicPresentationState.EMPTY
}
