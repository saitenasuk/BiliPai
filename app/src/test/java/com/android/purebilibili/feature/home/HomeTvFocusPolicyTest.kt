package com.android.purebilibili.feature.home

import android.view.KeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeTvFocusPolicyTest {

    @Test
    fun initialFocus_usesSidebarWhenAvailableOnTv() {
        assertEquals(
            HomeTvFocusZone.SIDEBAR,
            resolveInitialHomeTvFocusZone(isTv = true, hasSidebar = true)
        )
    }

    @Test
    fun initialFocus_fallsBackToPagerWhenNoSidebarOnTv() {
        assertEquals(
            HomeTvFocusZone.PAGER,
            resolveInitialHomeTvFocusZone(isTv = true, hasSidebar = false)
        )
    }

    @Test
    fun downFromPagerMovesToGrid() {
        val transition = resolveHomeTvFocusTransition(
            currentZone = HomeTvFocusZone.PAGER,
            keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
            action = KeyEvent.ACTION_UP,
            hasSidebar = true,
            isGridFirstRow = false,
            isGridFirstColumn = false
        )

        assertEquals(HomeTvFocusZone.GRID, transition.nextZone)
        assertEquals(true, transition.consumeEvent)
    }

    @Test
    fun leftFromPagerMovesToSidebarWhenSidebarVisible() {
        val transition = resolveHomeTvFocusTransition(
            currentZone = HomeTvFocusZone.PAGER,
            keyCode = KeyEvent.KEYCODE_DPAD_LEFT,
            action = KeyEvent.ACTION_UP,
            hasSidebar = true,
            isGridFirstRow = false,
            isGridFirstColumn = false
        )

        assertEquals(HomeTvFocusZone.SIDEBAR, transition.nextZone)
        assertEquals(true, transition.consumeEvent)
    }

    @Test
    fun upFromGridFirstRowMovesBackToPager() {
        val transition = resolveHomeTvFocusTransition(
            currentZone = HomeTvFocusZone.GRID,
            keyCode = KeyEvent.KEYCODE_DPAD_UP,
            action = KeyEvent.ACTION_UP,
            hasSidebar = true,
            isGridFirstRow = true,
            isGridFirstColumn = false
        )

        assertEquals(HomeTvFocusZone.PAGER, transition.nextZone)
        assertEquals(true, transition.consumeEvent)
    }

    @Test
    fun leftFromGridFirstColumnMovesToSidebar() {
        val transition = resolveHomeTvFocusTransition(
            currentZone = HomeTvFocusZone.GRID,
            keyCode = KeyEvent.KEYCODE_DPAD_LEFT,
            action = KeyEvent.ACTION_UP,
            hasSidebar = true,
            isGridFirstRow = false,
            isGridFirstColumn = true
        )

        assertEquals(HomeTvFocusZone.SIDEBAR, transition.nextZone)
        assertEquals(true, transition.consumeEvent)
    }

    @Test
    fun rightFromSidebarReturnsToPager() {
        val transition = resolveHomeTvFocusTransition(
            currentZone = HomeTvFocusZone.SIDEBAR,
            keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
            action = KeyEvent.ACTION_UP,
            hasSidebar = true,
            isGridFirstRow = false,
            isGridFirstColumn = false
        )

        assertEquals(HomeTvFocusZone.PAGER, transition.nextZone)
        assertEquals(true, transition.consumeEvent)
    }

    @Test
    fun pagerLeftRightWithoutSidebarTurnsPages() {
        val left = resolveHomeTvFocusTransition(
            currentZone = HomeTvFocusZone.PAGER,
            keyCode = KeyEvent.KEYCODE_DPAD_LEFT,
            action = KeyEvent.ACTION_UP,
            hasSidebar = false,
            isGridFirstRow = false,
            isGridFirstColumn = false
        )
        val right = resolveHomeTvFocusTransition(
            currentZone = HomeTvFocusZone.PAGER,
            keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
            action = KeyEvent.ACTION_UP,
            hasSidebar = false,
            isGridFirstRow = false,
            isGridFirstColumn = false
        )

        assertEquals(-1, left.pageDelta)
        assertEquals(1, right.pageDelta)
        assertEquals(true, left.consumeEvent)
        assertEquals(true, right.consumeEvent)
    }
}
