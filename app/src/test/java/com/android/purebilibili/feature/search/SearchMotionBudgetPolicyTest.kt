package com.android.purebilibili.feature.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchMotionBudgetPolicyTest {

    @Test
    fun activeSearchInteraction_reducesBudget() {
        assertEquals(
            SearchMotionBudget.REDUCED,
            resolveSearchMotionBudget(
                hasQuery = true,
                isSearching = true,
                isScrolling = false
            )
        )
        assertEquals(
            SearchMotionBudget.REDUCED,
            resolveSearchMotionBudget(
                hasQuery = true,
                isSearching = false,
                isScrolling = true
            )
        )
    }

    @Test
    fun idleSearchState_keepsFullBudgetAndHaze() {
        val budget = resolveSearchMotionBudget(
            hasQuery = false,
            isSearching = false,
            isScrolling = false
        )

        assertEquals(SearchMotionBudget.FULL, budget)
        assertTrue(shouldEnableSearchHazeSource(budget))
        assertFalse(shouldEnableSearchHazeSource(SearchMotionBudget.REDUCED))
    }
}
