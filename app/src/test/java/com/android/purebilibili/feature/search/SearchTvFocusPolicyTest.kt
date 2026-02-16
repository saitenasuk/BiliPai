package com.android.purebilibili.feature.search

import android.view.KeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchTvFocusPolicyTest {

    @Test
    fun initialFocus_defaultsToTopBarOnTv() {
        assertEquals(SearchTvFocusZone.TOP_BAR, resolveInitialSearchTvFocusZone(isTv = true))
    }

    @Test
    fun downFromTopMovesToSuggestionsWhenVisible() {
        val transition = resolveSearchTvFocusTransition(
            currentZone = SearchTvFocusZone.TOP_BAR,
            keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
            action = KeyEvent.ACTION_UP,
            showResults = false,
            hasSuggestions = true,
            hasHistory = true
        )

        assertEquals(SearchTvFocusZone.SUGGESTIONS, transition.nextZone)
        assertEquals(true, transition.consumeEvent)
    }

    @Test
    fun downFromTopMovesToResultsWhenResultsVisible() {
        val transition = resolveSearchTvFocusTransition(
            currentZone = SearchTvFocusZone.TOP_BAR,
            keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
            action = KeyEvent.ACTION_UP,
            showResults = true,
            hasSuggestions = false,
            hasHistory = false
        )

        assertEquals(SearchTvFocusZone.RESULTS, transition.nextZone)
        assertEquals(true, transition.consumeEvent)
    }

    @Test
    fun upFromSuggestionsReturnsTop() {
        val transition = resolveSearchTvFocusTransition(
            currentZone = SearchTvFocusZone.SUGGESTIONS,
            keyCode = KeyEvent.KEYCODE_DPAD_UP,
            action = KeyEvent.ACTION_UP,
            showResults = false,
            hasSuggestions = true,
            hasHistory = true
        )

        assertEquals(SearchTvFocusZone.TOP_BAR, transition.nextZone)
        assertEquals(true, transition.consumeEvent)
    }

    @Test
    fun upFromResultsReturnsTop() {
        val transition = resolveSearchTvFocusTransition(
            currentZone = SearchTvFocusZone.RESULTS,
            keyCode = KeyEvent.KEYCODE_DPAD_UP,
            action = KeyEvent.ACTION_UP,
            showResults = true,
            hasSuggestions = false,
            hasHistory = false
        )

        assertEquals(SearchTvFocusZone.TOP_BAR, transition.nextZone)
        assertEquals(true, transition.consumeEvent)
    }

    @Test
    fun backFromResultsReturnsTopInsteadOfLeavingPage() {
        val transition = resolveSearchTvFocusTransition(
            currentZone = SearchTvFocusZone.RESULTS,
            keyCode = KeyEvent.KEYCODE_BACK,
            action = KeyEvent.ACTION_UP,
            showResults = true,
            hasSuggestions = false,
            hasHistory = false
        )

        assertEquals(SearchTvFocusZone.TOP_BAR, transition.nextZone)
        assertEquals(true, transition.consumeEvent)
    }
}
