package com.android.purebilibili.feature.search

import kotlin.test.Test
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
}
