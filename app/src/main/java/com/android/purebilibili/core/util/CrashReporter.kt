package com.android.purebilibili.core.util

import android.util.Log
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

/**
 * 🔥 崩溃报告工具类
 * 封装 Firebase Crashlytics，提供统一的错误上报接口
 */
object CrashReporter {
    
    private const val TAG = "CrashReporter"
    
    /**
     * 启用/禁用 Crashlytics 收集
     */
    fun setEnabled(enabled: Boolean) {
        try {
            Firebase.crashlytics.setCrashlyticsCollectionEnabled(enabled)
            Logger.d(TAG, "📊 Crashlytics collection ${if (enabled) "enabled" else "disabled"}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set Crashlytics enabled state", e)
        }
    }
    
    /**
     * 记录非致命异常
     * 用于捕获的异常，不会导致崩溃但需要追踪
     */
    fun logException(e: Throwable, message: String? = null) {
        try {
            message?.let { Firebase.crashlytics.log(it) }
            Firebase.crashlytics.recordException(e)
            Logger.e(TAG, "📊 Exception logged: ${e.message}", e)
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to log exception", ex)
        }
    }
    
    /**
     * 记录自定义日志
     * 这些日志会在崩溃报告中显示，帮助定位问题
     */
    fun log(message: String) {
        try {
            Firebase.crashlytics.log(message)
            Logger.d(TAG, "📊 Log: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log message", e)
        }
    }
    
    /**
     * 设置用户标识符（用于追踪特定用户的问题）
     * 注意：请勿设置可识别个人身份的信息
     */
    fun setUserId(userId: String) {
        try {
            Firebase.crashlytics.setUserId(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set user ID", e)
        }
    }
    
    /**
     * 设置自定义键值对（崩溃时会附带这些信息）
     */
    fun setCustomKey(key: String, value: String) {
        try {
            Firebase.crashlytics.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set custom key", e)
        }
    }
    
    /**
     * 设置 Boolean 类型的自定义键
     */
    fun setCustomKey(key: String, value: Boolean) {
        try {
            Firebase.crashlytics.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set custom key", e)
        }
    }
    
    /**
     * 设置 Int 类型的自定义键
     */
    fun setCustomKey(key: String, value: Int) {
        try {
            Firebase.crashlytics.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set custom key", e)
        }
    }
    
    // ========== 🔥🔥 视频播放错误上报 ==========
    
    /**
     * 🎬 上报视频播放错误
     * @param bvid 视频 BV 号
     * @param errorType 错误类型 (如 "no_play_url", "network_error", "decode_error")
     * @param errorMessage 错误详情
     * @param exception 可选的异常对象
     */
    fun reportVideoError(
        bvid: String,
        errorType: String,
        errorMessage: String,
        exception: Throwable? = null
    ) {
        try {
            // 设置上下文信息
            Firebase.crashlytics.setCustomKey("video_bvid", bvid)
            Firebase.crashlytics.setCustomKey("video_error_type", errorType)
            
            // 记录详细日志
            Firebase.crashlytics.log("🎬 Video Error: [$errorType] $bvid - $errorMessage")
            
            // 上报异常
            val wrappedException = exception ?: VideoPlaybackException(errorType, errorMessage)
            Firebase.crashlytics.recordException(wrappedException)
            
            Logger.e(TAG, "📊 Video error reported: [$errorType] $bvid - $errorMessage", exception)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report video error", e)
        }
    }
    
    /**
     * 🌐 上报 API/网络错误
     * @param endpoint API 端点 (如 "playurl", "video_info", "danmaku")
     * @param httpCode HTTP 状态码 (如 412, 403, 500)
     * @param errorMessage 错误详情
     * @param bvid 可选的视频 BV 号
     */
    fun reportApiError(
        endpoint: String,
        httpCode: Int,
        errorMessage: String,
        bvid: String? = null
    ) {
        try {
            Firebase.crashlytics.setCustomKey("api_endpoint", endpoint)
            Firebase.crashlytics.setCustomKey("api_http_code", httpCode)
            bvid?.let { Firebase.crashlytics.setCustomKey("api_bvid", it) }
            
            Firebase.crashlytics.log("🌐 API Error: [$httpCode] $endpoint - $errorMessage")
            Firebase.crashlytics.recordException(ApiException(endpoint, httpCode, errorMessage))
            
            Logger.e(TAG, "📊 API error reported: [$httpCode] $endpoint - $errorMessage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report API error", e)
        }
    }
    
    /**
     * 📺 上报弹幕加载错误
     */
    fun reportDanmakuError(cid: Long, errorMessage: String, exception: Throwable? = null) {
        try {
            Firebase.crashlytics.setCustomKey("danmaku_cid", cid.toString())
            Firebase.crashlytics.log("📺 Danmaku Error: cid=$cid - $errorMessage")
            Firebase.crashlytics.recordException(exception ?: DanmakuException(cid, errorMessage))
            
            Logger.e(TAG, "📊 Danmaku error reported: cid=$cid - $errorMessage", exception)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report danmaku error", e)
        }
    }
    
    /**
     * 🔴 上报直播播放错误
     * @param roomId 直播间 ID
     * @param errorType 错误类型 (如 "no_stream", "network_error", "room_not_found")
     * @param errorMessage 错误详情
     */
    fun reportLiveError(
        roomId: Long,
        errorType: String,
        errorMessage: String,
        exception: Throwable? = null
    ) {
        try {
            Firebase.crashlytics.setCustomKey("live_room_id", roomId.toString())
            Firebase.crashlytics.setCustomKey("live_error_type", errorType)
            
            Firebase.crashlytics.log("🔴 Live Error: [$errorType] roomId=$roomId - $errorMessage")
            Firebase.crashlytics.recordException(exception ?: LiveStreamException(roomId, errorType, errorMessage))
            
            Logger.e(TAG, "📊 Live error reported: [$errorType] roomId=$roomId - $errorMessage", exception)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report live error", e)
        }
    }
    
    /**
     * 手动触发崩溃（仅用于测试）
     */
    fun testCrash() {
        throw RuntimeException("CrashReporter Test Crash")
    }
}

// ========== 🔥 自定义异常类（用于 Crashlytics 分类） ==========

/**
 * 视频播放异常
 */
class VideoPlaybackException(
    val errorType: String,
    override val message: String
) : Exception("[$errorType] $message")

/**
 * API 请求异常
 */
class ApiException(
    val endpoint: String,
    val httpCode: Int,
    override val message: String
) : Exception("[$httpCode] $endpoint: $message")

/**
 * 弹幕加载异常
 */
class DanmakuException(
    val cid: Long,
    override val message: String
) : Exception("Danmaku cid=$cid: $message")

/**
 * 直播播放异常
 */
class LiveStreamException(
    val roomId: Long,
    val errorType: String,
    override val message: String
) : Exception("[$errorType] Live roomId=$roomId: $message")
