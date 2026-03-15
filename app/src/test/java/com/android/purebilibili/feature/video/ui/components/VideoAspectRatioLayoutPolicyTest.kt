package com.android.purebilibili.feature.video.ui.components

import androidx.media3.ui.AspectRatioFrameLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VideoAspectRatioLayoutPolicyTest {

    @Test
    fun `fixed 4 to 3 ratio should fit inside landscape container`() {
        val layout = resolveVideoViewportLayout(
            containerWidth = 2400,
            containerHeight = 1080,
            aspectRatio = VideoAspectRatio.RATIO_4_3
        )

        assertEquals(1440, layout.width)
        assertEquals(1080, layout.height)
    }

    @Test
    fun `fixed 16 to 9 ratio should fit inside landscape container`() {
        val layout = resolveVideoViewportLayout(
            containerWidth = 2400,
            containerHeight = 1080,
            aspectRatio = VideoAspectRatio.RATIO_16_9
        )

        assertEquals(1920, layout.width)
        assertEquals(1080, layout.height)
    }

    @Test
    fun `fit mode should keep fullscreen viewport`() {
        val layout = resolveVideoViewportLayout(
            containerWidth = 2400,
            containerHeight = 1080,
            aspectRatio = VideoAspectRatio.FIT
        )

        assertEquals(2400, layout.width)
        assertEquals(1080, layout.height)
        assertNull(VideoAspectRatio.FIT.targetAspectRatio)
    }

    @Test
    fun `fill and stretch should keep fullscreen viewport`() {
        val fillLayout = resolveVideoViewportLayout(
            containerWidth = 2400,
            containerHeight = 1080,
            aspectRatio = VideoAspectRatio.FILL
        )
        val stretchLayout = resolveVideoViewportLayout(
            containerWidth = 2400,
            containerHeight = 1080,
            aspectRatio = VideoAspectRatio.STRETCH
        )

        assertEquals(2400, fillLayout.width)
        assertEquals(1080, fillLayout.height)
        assertEquals(2400, stretchLayout.width)
        assertEquals(1080, stretchLayout.height)
        assertNull(VideoAspectRatio.FILL.targetAspectRatio)
        assertNull(VideoAspectRatio.STRETCH.targetAspectRatio)
    }

    @Test
    fun `fullscreen ratios should map to expected player resize modes`() {
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_FIT, VideoAspectRatio.FIT.playerResizeMode)
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, VideoAspectRatio.FILL.playerResizeMode)
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, VideoAspectRatio.RATIO_16_9.playerResizeMode)
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, VideoAspectRatio.RATIO_4_3.playerResizeMode)
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_FILL, VideoAspectRatio.STRETCH.playerResizeMode)
        assertEquals(16f / 9f, VideoAspectRatio.RATIO_16_9.targetAspectRatio)
        assertEquals(4f / 3f, VideoAspectRatio.RATIO_4_3.targetAspectRatio)
    }
}
