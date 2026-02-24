package com.android.purebilibili.feature.home

enum class HomeVideoClickSource {
    GRID,
    TODAY_WATCH,
    PREVIEW
}

data class HomeVideoClickRequest(
    val bvid: String,
    val dynamicId: String = "",
    val cid: Long = 0L,
    val coverUrl: String = "",
    val source: HomeVideoClickSource = HomeVideoClickSource.GRID
)
