package com.android.purebilibili.feature.video.viewmodel

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerViewModelContextPolicyTest {

    @Test
    fun `should bootstrap player context when local context missing but app context exists`() {
        assertTrue(
            shouldBootstrapPlayerContext(
                hasBoundContext = false,
                hasGlobalContext = true
            )
        )
    }

    @Test
    fun `should not bootstrap when already bound or global context unavailable`() {
        assertFalse(
            shouldBootstrapPlayerContext(
                hasBoundContext = true,
                hasGlobalContext = true
            )
        )
        assertFalse(
            shouldBootstrapPlayerContext(
                hasBoundContext = false,
                hasGlobalContext = false
            )
        )
    }
}
