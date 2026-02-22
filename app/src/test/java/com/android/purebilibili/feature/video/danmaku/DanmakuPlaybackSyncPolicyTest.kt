package com.android.purebilibili.feature.video.danmaku

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DanmakuPlaybackSyncPolicyTest {

    @Test
    fun `drift sync interval should be aggressive for high speed`() {
        assertEquals(900L, resolveDanmakuDriftSyncIntervalMs(2.0f))
        assertEquals(1200L, resolveDanmakuDriftSyncIntervalMs(1.5f))
        assertEquals(1600L, resolveDanmakuDriftSyncIntervalMs(1.1f))
    }

    @Test
    fun `drift sync interval should keep low frequency around normal speed`() {
        assertEquals(5000L, resolveDanmakuDriftSyncIntervalMs(1.0f))
        assertEquals(5000L, resolveDanmakuDriftSyncIntervalMs(1.01f))
        assertEquals(5000L, resolveDanmakuDriftSyncIntervalMs(0.99f))
    }

    @Test
    fun `force resync should only trigger every third tick outside normal speed`() {
        assertFalse(shouldForceDanmakuDataResync(1.0f, 3))
        assertFalse(shouldForceDanmakuDataResync(1.3f, 1))
        assertFalse(shouldForceDanmakuDataResync(1.3f, 2))
        assertTrue(shouldForceDanmakuDataResync(1.3f, 3))
        assertTrue(shouldForceDanmakuDataResync(0.8f, 6))
    }
}
