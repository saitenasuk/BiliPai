package com.android.purebilibili.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CuteLoadingIndicatorPolicyTest {

    @Test
    fun `resolveMascotBounceWave keeps periodic endpoints equal`() {
        val start = resolveMascotBounceWave(0f)
        val end = resolveMascotBounceWave(1f)
        assertEquals(start, end, absoluteTolerance = 0.0001f)
    }

    @Test
    fun `resolveMascotBounceWave peaks around quarter cycle`() {
        assertTrue(resolveMascotBounceWave(0.25f) > 0.95f)
    }

    @Test
    fun `resolveMascotDotAlpha stays inside visual range`() {
        repeat(3) { index ->
            val alpha = resolveMascotDotAlpha(phase = 0.35f, index = index)
            assertTrue(alpha in 0.18f..1f)
        }
    }
}
