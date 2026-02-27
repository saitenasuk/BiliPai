package com.android.purebilibili.data.repository

import retrofit2.HttpException

internal const val DYNAMIC_FETCH_MAX_ATTEMPTS = 3

internal fun isRetryableDynamicApiError(code: Int, message: String): Boolean {
    if (code in setOf(-412, -352, -509, 22015, 34004)) return true
    val text = message.lowercase()
    return text.contains("412") ||
        text.contains("precondition") ||
        text.contains("风控") ||
        text.contains("risk")
}

internal fun isRetryableDynamicException(error: Throwable): Boolean {
    return when (error) {
        is HttpException -> error.code() in setOf(412, 429, 500, 502, 503, 504)
        else -> {
            val text = error.message.orEmpty().lowercase()
            text.contains("412") ||
                text.contains("precondition") ||
                text.contains("timeout") ||
                text.contains("reset")
        }
    }
}

internal fun resolveDynamicRetryDelayMs(attempt: Int): Long {
    return when (attempt) {
        1 -> 250L
        2 -> 700L
        else -> 1200L
    }
}

internal fun resolveDynamicFriendlyErrorMessage(code: Int, message: String): String {
    return when {
        code == -101 -> "未登录，请先登录"
        code == -412 || message.contains("412", ignoreCase = true) -> "请求过于频繁，请稍后重试"
        code == -352 || message.contains("风控") || message.contains("risk", ignoreCase = true) ->
            "触发风控，请稍后重试"
        message.isBlank() -> "加载失败，请稍后重试"
        else -> message
    }
}
