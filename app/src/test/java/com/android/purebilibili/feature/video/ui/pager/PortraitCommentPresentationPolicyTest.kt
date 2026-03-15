package com.android.purebilibili.feature.video.ui.pager

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitCommentPresentationPolicyTest {

    @Test
    fun `video sub reply expansion should stay inside embedded comment sheet`() {
        assertTrue(shouldUseEmbeddedVideoSubReplyPresentation())
    }

    @Test
    fun `video detail should not mount detached sub reply sheet when embedded path is enabled`() {
        assertFalse(shouldShowDetachedVideoSubReplySheet(useEmbeddedPresentation = true))
    }

    @Test
    fun `video sub reply sheet should fully cover parent comment layer`() {
        assertEquals(1f, resolveVideoSubReplySheetMaxHeightFraction())
        assertTrue(resolveVideoSubReplySheetScrimAlpha() < 0.5f)
    }
}
