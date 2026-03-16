package com.android.purebilibili.core.network

import okhttp3.Protocol
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NetworkClientPolicyTest {

    @Test
    fun sharedClient_allowsHttpTwoForMultiplexing() {
        assertTrue(
            NetworkModule.okHttpClient.protocols.contains(Protocol.HTTP_2),
            "Expected shared client to support HTTP/2, actual=${NetworkModule.okHttpClient.protocols}"
        )
    }

    @Test
    fun sharedClient_expandsHttpCacheBudget() {
        val cache = NetworkModule.okHttpClient.cache
        val expectedBudget = 32L * 1024 * 1024

        requireNotNull(cache) { "Expected shared client to expose an HTTP cache" }
        assertEquals(expectedBudget, cache.maxSize())
    }
}
