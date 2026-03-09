package com.android.purebilibili.feature.space

import kotlin.test.Test
import kotlin.test.assertEquals

class SpaceDynamicLoadPolicyTest {

    @Test
    fun resolveSpaceDynamicPresentationState_treatsUntouchedEmptyStateAsLoading() {
        assertEquals(
            SpaceDynamicPresentationState.LOADING,
            resolveSpaceDynamicPresentationState(
                itemCount = 0,
                isLoading = false,
                hasLoadedOnce = false,
                lastLoadFailed = false
            )
        )
    }

    @Test
    fun resolveSpaceDynamicPresentationState_returnsEmptyAfterSuccessfulEmptyLoad() {
        assertEquals(
            SpaceDynamicPresentationState.EMPTY,
            resolveSpaceDynamicPresentationState(
                itemCount = 0,
                isLoading = false,
                hasLoadedOnce = true,
                lastLoadFailed = false
            )
        )
    }

    @Test
    fun resolveSpaceDynamicPresentationState_returnsErrorAfterFailedEmptyLoad() {
        assertEquals(
            SpaceDynamicPresentationState.ERROR,
            resolveSpaceDynamicPresentationState(
                itemCount = 0,
                isLoading = false,
                hasLoadedOnce = true,
                lastLoadFailed = true
            )
        )
    }

    @Test
    fun resolveSpaceDynamicPresentationState_prefersContentWhenItemsExist() {
        assertEquals(
            SpaceDynamicPresentationState.CONTENT,
            resolveSpaceDynamicPresentationState(
                itemCount = 3,
                isLoading = false,
                hasLoadedOnce = true,
                lastLoadFailed = false
            )
        )
    }
}
