package com.android.purebilibili.feature.dynamic.components

internal enum class DrawGridScaleMode {
    FIT,
    CROP
}

internal fun resolveSingleImageAspectRatio(
    width: Int,
    height: Int
): Float {
    if (width <= 0 || height <= 0) return 1.33f
    return (width.toFloat() / height.toFloat()).coerceIn(0.33f, 3f)
}

internal fun resolveDrawGridScaleMode(totalImages: Int): DrawGridScaleMode {
    return if (totalImages == 1) DrawGridScaleMode.FIT else DrawGridScaleMode.CROP
}

