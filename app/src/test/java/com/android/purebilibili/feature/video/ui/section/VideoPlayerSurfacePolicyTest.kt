package com.android.purebilibili.feature.video.ui.section

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoPlayerSurfacePolicyTest {

    @Test
    fun `flip disabled keeps default surface type`() {
        assertFalse(
            shouldUseTextureSurfaceForFlip(
                isFlippedHorizontal = false,
                isFlippedVertical = false
            )
        )
    }

    @Test
    fun `horizontal flip requires texture surface`() {
        assertTrue(
            shouldUseTextureSurfaceForFlip(
                isFlippedHorizontal = true,
                isFlippedVertical = false
            )
        )
    }

    @Test
    fun `vertical flip requires texture surface`() {
        assertTrue(
            shouldUseTextureSurfaceForFlip(
                isFlippedHorizontal = false,
                isFlippedVertical = true
            )
        )
    }
}
