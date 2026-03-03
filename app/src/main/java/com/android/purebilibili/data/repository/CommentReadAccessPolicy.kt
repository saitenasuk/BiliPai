package com.android.purebilibili.data.repository

internal enum class CommentReadApiMode {
    AUTH,
    GUEST
}

internal data class CommentReadPlan(
    val primary: CommentReadApiMode,
    val fallback: CommentReadApiMode?
)

internal fun resolveCommentReadPlan(hasSession: Boolean): CommentReadPlan {
    return if (hasSession) {
        CommentReadPlan(
            primary = CommentReadApiMode.AUTH,
            fallback = CommentReadApiMode.GUEST
        )
    } else {
        CommentReadPlan(
            primary = CommentReadApiMode.GUEST,
            fallback = CommentReadApiMode.AUTH
        )
    }
}

internal fun shouldFallbackCommentRead(code: Int): Boolean {
    return code in setOf(-101, -111, -352, -412)
}

internal fun resolveCommentReadErrorMessage(code: Int): String {
    return when (code) {
        -352 -> "请求频率过高，请稍后再试"
        -111 -> "签名验证失败，请稍后重试"
        -101 -> "评论加载失败，请稍后重试或切换排序"
        -400 -> "请求参数错误"
        -412 -> "请求被拦截，请稍后再试"
        12002 -> "评论区已关闭"
        12009 -> "评论内容不存在"
        else -> "加载评论失败 ($code)"
    }
}
