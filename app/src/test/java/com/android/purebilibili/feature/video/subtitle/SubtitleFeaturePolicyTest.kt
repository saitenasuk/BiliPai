package com.android.purebilibili.feature.video.subtitle

import kotlin.test.Test
import kotlin.test.assertFalse

class SubtitleFeaturePolicyTest {

    @Test
    fun `subtitle feature is disabled for users by default`() {
        assertFalse(isSubtitleFeatureEnabledForUser())
    }
}
