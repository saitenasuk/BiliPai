package com.android.purebilibili.feature.dynamic.components

import kotlin.test.Test
import kotlin.test.assertEquals

class DrawGridLayoutPolicyTest {

    @Test
    fun resolveSingleImageAspectRatio_allowsTallerImagesThanLegacyClamp() {
        // Legacy clamp minimum was 0.6f, which cropped tall images too aggressively.
        assertEquals(0.33f, resolveSingleImageAspectRatio(width = 900, height = 3000))
    }

    @Test
    fun resolveDrawGridScaleMode_usesFitForSingleImage() {
        assertEquals(DrawGridScaleMode.FIT, resolveDrawGridScaleMode(totalImages = 1))
    }

    @Test
    fun resolveDrawGridScaleMode_keepsCropForMultiImageGrid() {
        assertEquals(DrawGridScaleMode.CROP, resolveDrawGridScaleMode(totalImages = 4))
    }
}

