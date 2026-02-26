package com.android.purebilibili.feature.dynamic

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicUserRequestPolicyTest {

    @Test
    fun `user dynamic result applies only when uid and token still match`() {
        assertTrue(
            shouldApplyUserDynamicsResult(
                selectedUid = 123L,
                requestUid = 123L,
                activeRequestToken = 10L,
                requestToken = 10L
            )
        )
        assertFalse(
            shouldApplyUserDynamicsResult(
                selectedUid = 456L,
                requestUid = 123L,
                activeRequestToken = 10L,
                requestToken = 10L
            )
        )
        assertFalse(
            shouldApplyUserDynamicsResult(
                selectedUid = 123L,
                requestUid = 123L,
                activeRequestToken = 11L,
                requestToken = 10L
            )
        )
    }
}
