package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.core.util.appendDistinctByKey
import com.android.purebilibili.data.model.response.DynamicItem

internal fun shouldApplyUserDynamicsResult(
    selectedUid: Long?,
    requestUid: Long,
    activeRequestToken: Long,
    requestToken: Long
): Boolean {
    return selectedUid == requestUid && activeRequestToken == requestToken
}

internal fun resolveSelectedUserVisibleItems(
    timelineItems: List<DynamicItem>,
    remoteUserItems: List<DynamicItem>,
    selectedUid: Long?
): List<DynamicItem> {
    if (selectedUid == null) return timelineItems

    val localMatches = timelineItems.filter { item ->
        item.modules.module_author?.mid == selectedUid
    }
    if (remoteUserItems.isEmpty()) {
        return localMatches
    }
    return appendDistinctByKey(
        existing = localMatches,
        incoming = remoteUserItems,
        keySelector = ::dynamicFeedItemKey
    )
}

internal fun shouldAutoLoadSelectedUserDynamics(
    previousUid: Long?,
    nextUid: Long,
    currentItems: List<DynamicItem>,
    userError: String?,
    localMatchCount: Int
): Boolean {
    if (nextUid != previousUid) {
        return localMatchCount == 0
    }
    if (!userError.isNullOrBlank()) return true
    return currentItems.isEmpty() && localMatchCount == 0
}

internal fun shouldReloadSelectedUserDynamics(
    previousUid: Long?,
    nextUid: Long,
    currentItems: List<DynamicItem>,
    userError: String?,
    localMatchCount: Int
): Boolean {
    return shouldAutoLoadSelectedUserDynamics(
        previousUid = previousUid,
        nextUid = nextUid,
        currentItems = currentItems,
        userError = userError,
        localMatchCount = localMatchCount
    )
}
