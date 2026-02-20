package com.android.purebilibili.core.ui.animation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DissolveRenderPolicyTest {

    @Test
    fun `shouldWrapWithDissolveAnimation only when dissolving`() {
        assertTrue(shouldWrapWithDissolveAnimation(isDissolving = true))
        assertFalse(shouldWrapWithDissolveAnimation(isDissolving = false))
    }
}
