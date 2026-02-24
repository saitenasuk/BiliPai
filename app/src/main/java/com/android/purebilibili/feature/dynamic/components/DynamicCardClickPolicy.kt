package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.data.model.response.DynamicItem

internal sealed interface DynamicCardClickAction {
    data class OpenVideo(val bvid: String) : DynamicCardClickAction
    data class OpenDynamicDetail(val dynamicId: String) : DynamicCardClickAction
    data object None : DynamicCardClickAction
}

internal fun resolveDynamicCardClickAction(item: DynamicItem): DynamicCardClickAction {
    val bvid = item.modules.module_dynamic?.major?.archive?.bvid
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    if (bvid != null) {
        return DynamicCardClickAction.OpenVideo(bvid)
    }

    val dynamicId = item.id_str.trim().takeIf { it.isNotEmpty() } ?: return DynamicCardClickAction.None
    return DynamicCardClickAction.OpenDynamicDetail(dynamicId)
}
