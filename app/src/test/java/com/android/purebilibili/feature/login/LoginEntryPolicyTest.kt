package com.android.purebilibili.feature.login

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoginEntryPolicyTest {

    @Test
    fun `login methods should only expose qr code entry`() {
        assertEquals(listOf(LoginMethod.QR_CODE), resolveAvailableLoginMethods())
    }

    @Test
    fun `qr login reason should explain why scan is required`() {
        val reason = resolveQrLoginReason()

        assertTrue(reason.contains("当前仅保留扫码登录"))
        assertTrue(reason.contains("高画质"))
    }
}
