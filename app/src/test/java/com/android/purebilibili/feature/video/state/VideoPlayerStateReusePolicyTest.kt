package com.android.purebilibili.feature.video.state

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoPlayerStateReusePolicyTest {

    @Test
    fun `does not reuse mini player when route cid mismatches current cid`() {
        assertFalse(
            shouldReuseMiniPlayerAtEntry(
                isMiniPlayerActive = true,
                miniPlayerBvid = "BV1same",
                miniPlayerCid = 1001L,
                hasMiniPlayerInstance = true,
                requestBvid = "BV1same",
                requestCid = 2002L
            )
        )
    }

    @Test
    fun `does not reuse when route cid not provided even if bvid matches`() {
        assertFalse(
            shouldReuseMiniPlayerAtEntry(
                isMiniPlayerActive = true,
                miniPlayerBvid = "BV1same",
                miniPlayerCid = 1001L,
                hasMiniPlayerInstance = true,
                requestBvid = "BV1same",
                requestCid = 0L
            )
        )
    }

    @Test
    fun `does not restore cached state when requested cid differs`() {
        assertFalse(
            shouldRestoreCachedUiState(
                cachedBvid = "BV1same",
                cachedCid = 1001L,
                requestBvid = "BV1same",
                requestCid = 2002L
            )
        )
    }

    @Test
    fun `does not restore cached state when request cid is unknown`() {
        assertFalse(
            shouldRestoreCachedUiState(
                cachedBvid = "BV1same",
                cachedCid = 1001L,
                requestBvid = "BV1same",
                requestCid = 0L
            )
        )
    }
}
