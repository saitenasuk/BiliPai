package com.android.purebilibili.core.util

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase

/**
 * 📊 Firebase Analytics 工具类
 * 封装 Firebase Analytics，提供统一的用户行为追踪接口
 * 
 * 追踪的事件类型：
 * - 屏幕浏览 (screen_view)
 * - 视频播放 (video_play, video_complete)
 * - 搜索行为 (search)
 * - 用户操作 (like, share, favorite, follow)
 * - 应用事件 (app_open, login)
 */
object AnalyticsHelper {
    
    private const val TAG = "AnalyticsHelper"
    
    private var analytics: FirebaseAnalytics? = null
    private var isEnabled: Boolean = true
    
    /**
     * 初始化 Analytics (在 Application 中调用)
     */
    fun init(context: Context) {
        try {
            analytics = Firebase.analytics
            Logger.d(TAG, "📊 Firebase Analytics initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init Firebase Analytics", e)
        }
    }
    
    /**
     * 启用/禁用 Analytics 收集
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        try {
            analytics?.setAnalyticsCollectionEnabled(enabled)
            Logger.d(TAG, "📊 Analytics collection ${if (enabled) "enabled" else "disabled"}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set Analytics enabled state", e)
        }
    }
    
    /**
     * 设置用户 ID (用于关联用户行为)
     * 注意：请勿设置可识别个人身份的信息
     */
    fun setUserId(userId: String?) {
        if (!isEnabled) return
        try {
            analytics?.setUserId(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set user ID", e)
        }
    }
    
    /**
     * 设置用户属性 (用于用户分群分析)
     */
    fun setUserProperty(name: String, value: String?) {
        if (!isEnabled) return
        try {
            analytics?.setUserProperty(name, value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set user property", e)
        }
    }
    
    // ========== 📱 屏幕浏览追踪 ==========
    
    /**
     * 记录屏幕浏览
     * @param screenName 屏幕名称 (如 "HomeScreen", "VideoDetailScreen")
     * @param screenClass 屏幕类名 (可选)
     */
    fun logScreenView(screenName: String, screenClass: String? = null) {
        if (!isEnabled) return
        try {
            analytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                screenClass?.let { param(FirebaseAnalytics.Param.SCREEN_CLASS, it) }
            }
            Logger.d(TAG, "📊 Screen view: $screenName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log screen view", e)
        }
    }
    
    // ========== 🎬 视频播放追踪 ==========
    
    /**
     * 记录视频播放开始
     * @param videoId 视频 BVID 或 AID
     * @param title 视频标题
     * @param author 视频作者
     * @param duration 视频时长 (秒)
     */
    fun logVideoPlay(
        videoId: String,
        title: String,
        author: String? = null,
        duration: Long? = null
    ) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("video_play") {
                param("video_id", videoId)
                param("video_title", title.take(100)) // 限制长度
                author?.let { param("video_author", it.take(50)) }
                duration?.let { param("video_duration_sec", it) }
            }
            Logger.d(TAG, "📊 Video play: $videoId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log video play", e)
        }
    }
    
    /**
     * 记录视频播放进度 (用于计算完播率)
     * @param videoId 视频 ID
     * @param progress 播放进度百分比 (0-100)
     * @param watchTime 实际观看时长 (秒)
     */
    fun logVideoProgress(
        videoId: String,
        progress: Int,
        watchTime: Long
    ) {
        if (!isEnabled) return
        // 只在关键节点记录: 25%, 50%, 75%, 100%
        if (progress !in listOf(25, 50, 75, 100)) return
        try {
            analytics?.logEvent("video_progress") {
                param("video_id", videoId)
                param("progress_percent", progress.toLong())
                param("watch_time_sec", watchTime)
            }
            Logger.d(TAG, "📊 Video progress: $videoId at $progress%")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log video progress", e)
        }
    }
    
    /**
     * 记录视频播放完成
     */
    fun logVideoComplete(videoId: String, totalWatchTime: Long) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("video_complete") {
                param("video_id", videoId)
                param("total_watch_time_sec", totalWatchTime)
            }
            Logger.d(TAG, "📊 Video complete: $videoId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log video complete", e)
        }
    }
    
    // ========== 🔍 搜索追踪 ==========
    
    /**
     * 记录搜索事件
     * @param query 搜索关键词
     */
    fun logSearch(query: String) {
        if (!isEnabled) return
        try {
            analytics?.logEvent(FirebaseAnalytics.Event.SEARCH) {
                param(FirebaseAnalytics.Param.SEARCH_TERM, query.take(100))
            }
            Logger.d(TAG, "📊 Search: $query")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log search", e)
        }
    }
    
    /**
     * 记录搜索结果点击
     */
    fun logSearchResultClick(query: String, videoId: String, position: Int) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("search_result_click") {
                param(FirebaseAnalytics.Param.SEARCH_TERM, query.take(100))
                param("video_id", videoId)
                param("position", position.toLong())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log search result click", e)
        }
    }
    
    // ========== ❤️ 用户互动追踪 ==========
    
    /**
     * 记录点赞事件
     */
    fun logLike(videoId: String, isLiked: Boolean) {
        if (!isEnabled) return
        try {
            analytics?.logEvent(if (isLiked) "video_like" else "video_unlike") {
                param("video_id", videoId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log like", e)
        }
    }
    
    /**
     * 记录收藏事件
     */
    fun logFavorite(videoId: String, isFavorited: Boolean) {
        if (!isEnabled) return
        try {
            analytics?.logEvent(if (isFavorited) "video_favorite" else "video_unfavorite") {
                param("video_id", videoId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log favorite", e)
        }
    }
    
    /**
     * 记录分享事件
     */
    fun logShare(videoId: String, method: String? = null) {
        if (!isEnabled) return
        try {
            analytics?.logEvent(FirebaseAnalytics.Event.SHARE) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "video")
                param(FirebaseAnalytics.Param.ITEM_ID, videoId)
                method?.let { param(FirebaseAnalytics.Param.METHOD, it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log share", e)
        }
    }
    
    /**
     * 记录关注用户事件
     */
    fun logFollow(userId: String, isFollowed: Boolean) {
        if (!isEnabled) return
        try {
            analytics?.logEvent(if (isFollowed) "user_follow" else "user_unfollow") {
                param("target_user_id", userId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log follow", e)
        }
    }
    
    /**
     * 记录投币事件
     */
    fun logCoin(videoId: String, coinCount: Int) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("video_coin") {
                param("video_id", videoId)
                param("coin_count", coinCount.toLong())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log coin", e)
        }
    }
    
    // ========== 🚀 应用事件追踪 ==========
    
    /**
     * 记录应用打开
     */
    fun logAppOpen() {
        if (!isEnabled) return
        try {
            analytics?.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
            Logger.d(TAG, "📊 App open")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log app open", e)
        }
    }
    
    /**
     * 记录登录事件
     */
    fun logLogin(method: String = "qrcode") {
        if (!isEnabled) return
        try {
            analytics?.logEvent(FirebaseAnalytics.Event.LOGIN) {
                param(FirebaseAnalytics.Param.METHOD, method)
            }
            Logger.d(TAG, "📊 Login: $method")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log login", e)
        }
    }
    
    /**
     * 记录登出事件
     */
    fun logLogout() {
        if (!isEnabled) return
        try {
            analytics?.logEvent("logout", null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log logout", e)
        }
    }
    
    // ========== 📂 分类/频道追踪 ==========
    
    /**
     * 记录分类切换
     */
    fun logCategoryView(categoryName: String, categoryId: Int? = null) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("category_view") {
                param("category_name", categoryName)
                categoryId?.let { param("category_id", it.toLong()) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log category view", e)
        }
    }
    
    /**
     * 记录番剧播放
     */
    fun logBangumiPlay(seasonId: String, episodeId: String, title: String) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("bangumi_play") {
                param("season_id", seasonId)
                param("episode_id", episodeId)
                param("title", title.take(100))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log bangumi play", e)
        }
    }
    
    // ========== ⚙️ 设置变更追踪 ==========
    
    /**
     * 记录设置变更 (用于了解用户偏好)
     */
    fun logSettingChange(settingName: String, value: String) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("setting_change") {
                param("setting_name", settingName)
                param("setting_value", value)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log setting change", e)
        }
    }
    
    // ========== 📺 直播追踪 ==========
    
    /**
     * 记录直播观看
     */
    fun logLivePlay(roomId: Long, title: String, upName: String? = null) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("live_play") {
                param("room_id", roomId.toString())
                param("title", title.take(100))
                upName?.let { param("up_name", it.take(50)) }
            }
            Logger.d(TAG, "📊 Live play: roomId=$roomId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log live play", e)
        }
    }
    
    /**
     * 记录直播观看时长
     */
    fun logLiveWatchTime(roomId: Long, watchTimeSeconds: Long) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("live_watch_time") {
                param("room_id", roomId.toString())
                param("watch_time_sec", watchTimeSeconds)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log live watch time", e)
        }
    }
    
    // ========== ❌ 错误事件追踪 (用于分析问题) ==========
    
    /**
     * 记录视频播放错误 (Analytics 层面，用于统计)
     */
    fun logVideoError(videoId: String, errorType: String) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("video_error") {
                param("video_id", videoId)
                param("error_type", errorType)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log video error", e)
        }
    }
    
    /**
     * 记录直播播放错误
     */
    fun logLiveError(roomId: Long, errorType: String) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("live_error") {
                param("room_id", roomId.toString())
                param("error_type", errorType)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log live error", e)
        }
    }
    
    // ========== 🎯 功能使用追踪 ==========
    
    /**
     * 记录空降助手使用 (SponsorBlock)
     */
    fun logSponsorBlockSkip(videoId: String, segmentType: String) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("sponsorblock_skip") {
                param("video_id", videoId)
                param("segment_type", segmentType)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log sponsorblock skip", e)
        }
    }
    
    /**
     * 记录画质切换
     */
    fun logQualityChange(videoId: String, fromQuality: Int, toQuality: Int) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("quality_change") {
                param("video_id", videoId)
                param("from_quality", fromQuality.toLong())
                param("to_quality", toQuality.toLong())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log quality change", e)
        }
    }
    
    /**
     * 记录弹幕开关
     */
    fun logDanmakuToggle(enabled: Boolean) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("danmaku_toggle") {
                param("enabled", if (enabled) "true" else "false")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log danmaku toggle", e)
        }
    }
}
