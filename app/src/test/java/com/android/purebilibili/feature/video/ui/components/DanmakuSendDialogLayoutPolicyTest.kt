package com.android.purebilibili.feature.video.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DanmakuSendDialogLayoutPolicyTest {

    @Test
    fun `danmaku dialog should use bottom sheet style to avoid blocking video center`() {
        val policy = resolveDanmakuSendDialogLayoutPolicy()

        assertTrue(policy.bottomAligned)
        assertEquals(1f, policy.fillMaxWidthFraction)
        assertEquals(14, policy.bottomLiftDp)
    }

    @Test
    fun `ime visible should remove extra bottom lift to avoid gap`() {
        val liftWhenImeVisible = resolveDanmakuDialogBottomLiftDp(
            defaultBottomLiftDp = 14,
            imeBottomPx = 320
        )
        val liftWhenImeHidden = resolveDanmakuDialogBottomLiftDp(
            defaultBottomLiftDp = 14,
            imeBottomPx = 0
        )

        assertEquals(0, liftWhenImeVisible)
        assertEquals(14, liftWhenImeHidden)
    }
}
