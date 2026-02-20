package com.android.purebilibili.data.repository

internal enum class PlayUrlSource {
    APP,
    DASH,
    HTML5,
    LEGACY,
    GUEST
}

internal fun resolveInitialStartQuality(
    targetQuality: Int?,
    isAutoHighestQuality: Boolean,
    isLogin: Boolean,
    isVip: Boolean,
    auto1080pEnabled: Boolean
): Int {
    return when {
        isAutoHighestQuality && isVip -> 120
        isAutoHighestQuality && isLogin -> 80
        isAutoHighestQuality -> 64
        targetQuality != null -> targetQuality
        isVip -> 116
        isLogin && auto1080pEnabled -> 80
        isLogin -> 64
        else -> 32
    }
}

internal fun shouldSkipPlayUrlCache(
    isAutoHighestQuality: Boolean,
    isVip: Boolean,
    audioLang: String?
): Boolean {
    return audioLang != null || (isAutoHighestQuality && isVip)
}

internal fun buildDashAttemptQualities(targetQn: Int): List<Int> {
    val fallback = if (targetQn > 80) listOf(targetQn, 80) else listOf(targetQn)
    return fallback.distinct()
}

internal fun resolveDashRetryDelays(targetQn: Int): List<Long> {
    // 首帧优先：低画质冷启动不做二次重试，直接进入后备链路。
    return if (targetQn >= 112) listOf(0L) else listOf(0L)
}

internal fun shouldCallAccessTokenApi(
    nowMs: Long,
    cooldownUntilMs: Long,
    hasAccessToken: Boolean
): Boolean {
    return hasAccessToken && nowMs >= cooldownUntilMs
}

internal fun buildGuestFallbackQualities(): List<Int> {
    return listOf(80, 64, 32)
}

internal fun shouldCachePlayUrlResult(
    source: PlayUrlSource,
    audioLang: String?
): Boolean {
    if (audioLang != null) return false
    return source != PlayUrlSource.GUEST
}

internal fun shouldFetchCommentEmoteMapOnVideoLoad(): Boolean {
    return false
}

internal fun shouldRefreshVipStatusOnVideoLoad(): Boolean {
    return false
}

internal fun shouldFetchInteractionStatusOnVideoLoad(): Boolean {
    return false
}
