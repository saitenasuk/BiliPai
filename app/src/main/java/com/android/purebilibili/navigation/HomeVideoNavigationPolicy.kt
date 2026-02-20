package com.android.purebilibili.navigation

import com.android.purebilibili.feature.home.HomeVideoClickRequest
import com.android.purebilibili.feature.home.HomeVideoClickSource
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal data class HomeVideoNavigationIntent(
    val bvid: String,
    val cid: Long,
    val coverUrl: String,
    val source: HomeVideoClickSource
)

internal fun resolveHomeVideoNavigationIntent(
    request: HomeVideoClickRequest
): HomeVideoNavigationIntent? {
    val normalizedBvid = request.bvid.trim()
    if (normalizedBvid.isEmpty()) return null

    return HomeVideoNavigationIntent(
        bvid = normalizedBvid,
        cid = request.cid.takeIf { it > 0L } ?: 0L,
        coverUrl = request.coverUrl,
        source = request.source
    )
}

internal fun resolveHomeVideoRoute(request: HomeVideoClickRequest): String? {
    val intent = resolveHomeVideoNavigationIntent(request) ?: return null
    val encodedCover = URLEncoder.encode(intent.coverUrl, StandardCharsets.UTF_8.toString())
    return "${VideoRoute.base}/${intent.bvid}?cid=${intent.cid}&cover=$encodedCover"
}
