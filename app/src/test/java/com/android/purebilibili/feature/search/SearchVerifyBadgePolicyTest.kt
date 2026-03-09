package com.android.purebilibili.feature.search

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchVerifyBadgePolicyTest {

    @Test
    fun resolveSearchVerifyBadge_returnsNoneForMissingOrUnverifiedUsers() {
        assertEquals(SearchVerifyBadge.NONE, resolveSearchVerifyBadge(type = null, description = null))
        assertEquals(SearchVerifyBadge.NONE, resolveSearchVerifyBadge(type = -1, description = ""))
    }

    @Test
    fun resolveSearchVerifyBadge_mapsKnownPersonalVerificationTypesToPersonal() {
        assertEquals(SearchVerifyBadge.PERSONAL, resolveSearchVerifyBadge(type = 0, description = "个人认证：UP主"))
        assertEquals(SearchVerifyBadge.PERSONAL, resolveSearchVerifyBadge(type = 2, description = "个人认证：知名UP主"))
        assertEquals(SearchVerifyBadge.PERSONAL, resolveSearchVerifyBadge(type = 7, description = "个人认证"))
    }

    @Test
    fun resolveSearchVerifyBadge_mapsOrganizationTypesToOrganization() {
        assertEquals(SearchVerifyBadge.ORGANIZATION, resolveSearchVerifyBadge(type = 1, description = "机构认证：官方账号"))
        assertEquals(SearchVerifyBadge.ORGANIZATION, resolveSearchVerifyBadge(type = 3, description = "哔哩哔哩机构认证"))
    }

    @Test
    fun resolveSearchVerifyBadge_doesNotDefaultUnknownPositiveTypeToOrganization() {
        assertEquals(SearchVerifyBadge.NONE, resolveSearchVerifyBadge(type = 99, description = "未知认证"))
    }
}
