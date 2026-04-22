package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.HotItem

private const val HISTORY_RECOMMEND_REASON = "与最近搜索相关"
private const val FOLLOWED_UP_RECOMMEND_REASON = "关注的 UP 主"

internal fun buildSearchRecommendItems(
    historySuggestionKeywords: List<String>,
    followedUpNames: List<String>,
    officialItems: List<HotItem>,
    trendingItems: List<HotItem>,
    fallbackKeywords: List<String>,
    limit: Int = 10
): List<HotItem> {
    val result = mutableListOf<HotItem>()
    val seen = mutableSetOf<String>()

    fun add(item: HotItem) {
        val keyword = item.keyword.trim().ifBlank { item.show_name.trim() }
        val title = item.show_name.trim().ifBlank { keyword }
        if (keyword.isBlank()) return
        val key = keyword.lowercase()
        if (!seen.add(key)) return
        result += item.copy(keyword = keyword, show_name = title)
    }

    historySuggestionKeywords.forEach { keyword ->
        val normalized = keyword.trim()
        add(HotItem(keyword = normalized, show_name = normalized, recommend_reason = HISTORY_RECOMMEND_REASON))
    }
    followedUpNames.forEach { name ->
        val normalized = name.trim()
        add(HotItem(keyword = normalized, show_name = normalized, recommend_reason = FOLLOWED_UP_RECOMMEND_REASON))
    }
    officialItems.forEach(::add)
    trendingItems.forEach(::add)
    fallbackKeywords.forEach { keyword ->
        val normalized = keyword.trim()
        add(HotItem(keyword = normalized, show_name = normalized))
    }

    return result.take(limit.coerceAtLeast(1))
}
