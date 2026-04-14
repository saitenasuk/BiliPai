package com.android.purebilibili.feature.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchScreenPolicyTest {

    @Test
    fun resetSearchScroll_onlyWhenShowingNonBlankResults() {
        assertTrue(
            shouldResetSearchResultScroll(
                searchSessionId = 1L,
                showResults = true
            )
        )
        assertFalse(
            shouldResetSearchResultScroll(
                searchSessionId = 0L,
                showResults = true
            )
        )
        assertFalse(
            shouldResetSearchResultScroll(
                searchSessionId = 2L,
                showResults = false
            )
        )
    }

    @Test
    fun submitKeyword_prefersTypedQuery_thenFallsBackToSuggestedKeyword() {
        assertEquals(
            "黑神话悟空",
            resolveSearchSubmitKeyword(
                query = "  黑神话悟空 ",
                suggestedKeyword = "睡羊妹妹m"
            )
        )
        assertEquals(
            "睡羊妹妹m",
            resolveSearchSubmitKeyword(
                query = " ",
                suggestedKeyword = " 睡羊妹妹m "
            )
        )
        assertEquals(
            "",
            resolveSearchSubmitKeyword(
                query = "",
                suggestedKeyword = " "
            )
        )
    }
}
