package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.UiPreset
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IOSDialogComponentsPolicyTest {

    @Test
    fun `md3 dialog actions stay content sized inside material alert dialogs`() {
        val policy = resolveIosDialogActionLayoutPolicy(UiPreset.MD3)

        assertFalse(policy.expandToContainer)
    }

    @Test
    fun `ios preset dialog actions expand to keep full width tap targets`() {
        val policy = resolveIosDialogActionLayoutPolicy(UiPreset.IOS)

        assertTrue(policy.expandToContainer)
    }
}
