package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class SponsorBlockClientPolicyTest {

    @Test
    fun sponsorBlockClient_reusesSharedNetworkStack() {
        val sharedClient = NetworkModule.okHttpClient
        val sponsorClient = buildSponsorBlockHttpClient(sharedClient)

        assertEquals(sharedClient.protocols, sponsorClient.protocols)
        assertSame(sharedClient.cache, sponsorClient.cache)
        assertEquals(5_000, sponsorClient.connectTimeoutMillis.toLong())
        assertEquals(5_000, sponsorClient.readTimeoutMillis.toLong())
    }
}
