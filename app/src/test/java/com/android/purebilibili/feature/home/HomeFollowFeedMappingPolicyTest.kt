package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeFollowFeedMappingPolicyTest {

    @Test
    fun resolveDynamicArchiveAid_parsesArchiveAidString() {
        assertEquals(1456400345L, resolveDynamicArchiveAid(archiveAid = "1456400345", fallbackId = 0L))
    }

    @Test
    fun resolveDynamicArchiveAid_fallsBackToExistingIdWhenArchiveAidInvalid() {
        assertEquals(9988L, resolveDynamicArchiveAid(archiveAid = "", fallbackId = 9988L))
    }
}

