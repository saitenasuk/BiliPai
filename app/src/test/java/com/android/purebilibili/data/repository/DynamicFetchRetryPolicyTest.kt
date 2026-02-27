package com.android.purebilibili.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicFetchRetryPolicyTest {

    @Test
    fun `api error codes for risk control should be retryable`() {
        assertTrue(isRetryableDynamicApiError(code = -412, message = ""))
        assertTrue(isRetryableDynamicApiError(code = -352, message = ""))
        assertTrue(isRetryableDynamicApiError(code = -509, message = ""))
        assertTrue(isRetryableDynamicApiError(code = 22015, message = ""))
        assertFalse(isRetryableDynamicApiError(code = -101, message = ""))
    }

    @Test
    fun `api error message containing precondition should be retryable`() {
        assertTrue(isRetryableDynamicApiError(code = -999, message = "HTTP 412 Precondition Failed"))
        assertTrue(isRetryableDynamicApiError(code = -999, message = "触发风控"))
        assertFalse(isRetryableDynamicApiError(code = -999, message = "参数错误"))
    }

    @Test
    fun `friendly message maps 412 and risk`() {
        assertEquals(
            "请求过于频繁，请稍后重试",
            resolveDynamicFriendlyErrorMessage(code = -412, message = "")
        )
        assertEquals(
            "触发风控，请稍后重试",
            resolveDynamicFriendlyErrorMessage(code = -352, message = "")
        )
        assertEquals(
            "未登录，请先登录",
            resolveDynamicFriendlyErrorMessage(code = -101, message = "")
        )
    }

    @Test
    fun `retry delays should increase by attempts`() {
        assertEquals(250L, resolveDynamicRetryDelayMs(1))
        assertEquals(700L, resolveDynamicRetryDelayMs(2))
        assertEquals(1200L, resolveDynamicRetryDelayMs(3))
    }
}
