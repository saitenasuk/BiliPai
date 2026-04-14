package com.android.purebilibili.feature.search

import com.android.purebilibili.data.model.response.HotItem
import com.android.purebilibili.data.model.response.SearchSuggestTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchUiModelsTest {

    @Test
    fun hotItemMapping_normalizesProtocolRelativeIcon() {
        val model = HotItem(
            keyword = "Vitality G2",
            show_name = "Vitality G2",
            icon = "//i0.hdslb.com/hot.png",
            show_live_icon = true,
            recommend_reason = "热"
        ).toSearchKeywordUiModel()

        assertEquals("Vitality G2", model.keyword)
        assertEquals("https://i0.hdslb.com/hot.png", model.iconUrl)
        assertTrue(model.showLiveBadge)
        assertEquals("热", model.subtitle)
    }

    @Test
    fun searchSuggestMapping_prefersTermAndStripsFallbackMarkup() {
        val model = SearchSuggestTag(
            term = "黑神话",
            value = "",
            name = "<suggest_high_light>黑神话</suggest_high_light>悟空"
        ).toSearchSuggestionUiModel()

        assertEquals("黑神话", model.keyword)
        assertEquals("<suggest_high_light>黑神话</suggest_high_light>悟空", model.richText)
        assertEquals("黑神话悟空", "<suggest_high_light>黑神话</suggest_high_light>悟空".stripSearchMarkup())
    }
}
