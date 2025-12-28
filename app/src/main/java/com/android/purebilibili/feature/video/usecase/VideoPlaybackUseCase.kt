// File: feature/video/usecase/VideoPlaybackUseCase.kt
package com.android.purebilibili.feature.video.usecase

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.android.purebilibili.core.cooldown.CooldownStatus
import com.android.purebilibili.core.cooldown.PlaybackCooldownManager
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.model.VideoLoadError
import com.android.purebilibili.data.model.response.*
import com.android.purebilibili.data.repository.ActionRepository
import com.android.purebilibili.data.repository.VideoRepository
import com.android.purebilibili.feature.video.controller.PlaybackProgressManager
import com.android.purebilibili.feature.video.controller.QualityManager

/**
 * Video Playback UseCase
 * 
 * Handles video loading, playback, quality switching, and page switching.
 * 
 * Requirement Reference: AC1.1 - Simplify PlayerViewModel
 */

/**
 * Video playback result
 */
sealed class VideoLoadResult {
    data class Success(
        val info: ViewInfo,
        val playUrl: String,
        val audioUrl: String?,
        val related: List<RelatedVideo>,
        val quality: Int,
        val qualityIds: List<Int>,
        val qualityLabels: List<String>,
        val cachedDashVideos: List<DashVideo>,
        val cachedDashAudios: List<DashAudio>,
        val emoteMap: Map<String, String>,
        val isLoggedIn: Boolean,
        val isVip: Boolean,
        val isFollowing: Boolean,
        val isFavorited: Boolean,
        val isLiked: Boolean,
        val coinCount: Int
    ) : VideoLoadResult()
    
    data class Error(
        val error: VideoLoadError,
        val canRetry: Boolean = true
    ) : VideoLoadResult()
}

/**
 * Quality switch result
 */
data class QualitySwitchResult(
    val videoUrl: String,
    val audioUrl: String?,
    val actualQuality: Int,
    val wasFallback: Boolean,
    val cachedDashVideos: List<DashVideo>,
    val cachedDashAudios: List<DashAudio>
)

class VideoPlaybackUseCase(
    private var progressManager: PlaybackProgressManager = PlaybackProgressManager(),
    private val qualityManager: QualityManager = QualityManager()
) {
    
    private var exoPlayer: ExoPlayer? = null
    
    /**
     * Initialize with context for persistent progress storage
     */
    fun initWithContext(context: android.content.Context) {
        progressManager = PlaybackProgressManager.getInstance(context)
    }
    
    /**
     * Attach ExoPlayer instance
     */
    fun attachPlayer(player: ExoPlayer) {
        exoPlayer = player
        player.volume = 1.0f
    }
    
    /**
     * Load video data
     * 
     * @param defaultQuality 网络感知的默认清晰度 (WiFi=80/1080P, Mobile=64/720P)
     */
    suspend fun loadVideo(
        bvid: String,
        defaultQuality: Int = 64,
        onProgress: (String) -> Unit = {}
    ): VideoLoadResult {
        try {
            // 🔥🔥 [风控冷却] 检查是否处于冷却期
            when (val cooldownStatus = PlaybackCooldownManager.getCooldownStatus(bvid)) {
                is CooldownStatus.GlobalCooldown -> {
                    Logger.w("VideoPlaybackUseCase", "⏳ 全局冷却中，跳过请求: ${cooldownStatus.remainingMinutes}分${cooldownStatus.remainingSeconds}秒")
                    return VideoLoadResult.Error(
                        error = VideoLoadError.GlobalCooldown(
                            cooldownStatus.remainingMs, 
                            PlaybackCooldownManager.getConsecutiveFailures()
                        ),
                        canRetry = false
                    )
                }
                is CooldownStatus.VideoCooldown -> {
                    Logger.w("VideoPlaybackUseCase", "⏳ 视频冷却中: $bvid，剩余 ${cooldownStatus.remainingMinutes}分${cooldownStatus.remainingSeconds}秒")
                    return VideoLoadResult.Error(
                        error = VideoLoadError.RateLimited(cooldownStatus.remainingMs, bvid),
                        canRetry = false
                    )
                }
                is CooldownStatus.Ready -> {
                    // 可以继续请求
                }
            }
            
            onProgress("Loading video info...")
            
            // 🔥🔥 [关键修复] 将用户画质设置传递给 Repository
            val detailResult = VideoRepository.getVideoDetails(bvid, defaultQuality)
            val relatedVideos = VideoRepository.getRelatedVideos(bvid)
            val emoteMap = com.android.purebilibili.data.repository.CommentRepository.getEmoteMap()
            
            return detailResult.fold(
                onSuccess = { (info, playData) ->
                    // 🔥🔥 [网络感知] 使用 API 返回的画质或传入的默认画质
                    val targetQn = playData.quality.takeIf { it > 0 } ?: defaultQuality
                    
                    val dashVideo = playData.dash?.getBestVideo(targetQn)
                    val dashAudio = playData.dash?.getBestAudio()
                    
                    val videoUrl = getValidVideoUrl(dashVideo, playData)
                    val audioUrl = dashAudio?.getValidUrl()?.takeIf { it.isNotEmpty() }
                    
                    if (videoUrl.isEmpty()) {
                        // 🔥🔥 [风控冷却] 播放地址为空，记录失败
                        PlaybackCooldownManager.recordFailure(bvid, "播放地址为空")
                        return@fold VideoLoadResult.Error(
                            error = VideoLoadError.PlayUrlEmpty,
                            canRetry = true
                        )
                    }
                    
                    // 🔥🔥 [风控冷却] 加载成功，重置失败计数
                    PlaybackCooldownManager.recordSuccess()
                    
                    val isLogin = !com.android.purebilibili.core.store.TokenManager.sessDataCache.isNullOrEmpty()
                    
                    // 🔥🔥 [修复] 主动获取最新VIP状态，避免缓存过期导致高画质不可用
                    var isVip = com.android.purebilibili.core.store.TokenManager.isVipCache
                    if (isLogin && !isVip) {
                        // 用户已登录但VIP状态为false时，主动刷新一次
                        try {
                            val navResult = VideoRepository.getNavInfo()
                            navResult.onSuccess { navData ->
                                isVip = navData.vip.status == 1
                                com.android.purebilibili.core.store.TokenManager.isVipCache = isVip
                                Logger.d("VideoPlaybackUseCase", "🔥 Refreshed VIP status: $isVip")
                            }
                        } catch (e: Exception) {
                            Logger.d("VideoPlaybackUseCase", "⚠️ Failed to refresh VIP status: ${e.message}")
                        }
                    }
                    
                    // 🔥🔥 [修复] 合成完整画质列表：API 返回的 accept_quality + DASH 视频流中的实际画质
                    val apiQualities = playData.accept_quality ?: emptyList()
                    val dashVideoIds = playData.dash?.video?.map { it.id }?.distinct() ?: emptyList()
                    
                    // 🔥🔥 [新增] 确保包含所有标准画质选项，用户可以切换到低画质以省流量
                    // 即使 DASH 流中没有这些画质，也可以通过 API 请求获取
                    val standardLowQualities = listOf(32, 16) // 480P, 360P
                    val mergedQualityIds = (apiQualities + dashVideoIds + standardLowQualities)
                        .distinct()
                        .sortedDescending()
                    
                    // 🔥🔥 [修复] 生成对应的画质标签 - 使用更短的名称确保竖屏显示完整
                    val qualityLabelMap = mapOf(
                        127 to "8K",
                        126 to "杜比",
                        125 to "HDR",
                        120 to "4K",
                        116 to "60帧",   // 🔥 "1080P60" 改为 "60帧"
                        112 to "高码",   // 🔥 "1080P+" 改为 "高码"
                        80 to "1080P",
                        74 to "720P60",
                        64 to "720P",
                        32 to "480P",
                        16 to "360P"
                    )
                    val mergedQualityLabels = mergedQualityIds.map { qn ->
                        qualityLabelMap[qn] ?: "${qn}P"
                    }
                    
                    Logger.d("VideoPlaybackUseCase", "🔥 Quality merge: api=$apiQualities, dash=$dashVideoIds, merged=$mergedQualityIds")
                    
                    // Check user interaction status
                    val isFollowing = if (isLogin) ActionRepository.checkFollowStatus(info.owner.mid) else false
                    val isFavorited = if (isLogin) ActionRepository.checkFavoriteStatus(info.aid) else false
                    val isLiked = if (isLogin) ActionRepository.checkLikeStatus(info.aid) else false
                    val coinCount = if (isLogin) ActionRepository.checkCoinStatus(info.aid) else 0
                    
                    VideoLoadResult.Success(
                        info = info,
                        playUrl = videoUrl,
                        audioUrl = audioUrl,
                        related = relatedVideos,
                        quality = dashVideo?.id ?: playData.quality,
                        qualityIds = mergedQualityIds,
                        qualityLabels = mergedQualityLabels,
                        cachedDashVideos = playData.dash?.video ?: emptyList(),
                        cachedDashAudios = playData.dash?.audio ?: emptyList(),
                        emoteMap = emoteMap,
                        isLoggedIn = isLogin,
                        isVip = isVip,
                        isFollowing = isFollowing,
                        isFavorited = isFavorited,
                        isLiked = isLiked,
                        coinCount = coinCount
                    )
                },
                onFailure = { e ->
                    // 🔥🔥 [风控冷却] 加载失败，记录失败
                    PlaybackCooldownManager.recordFailure(bvid, e.message ?: "unknown")
                    VideoLoadResult.Error(
                        error = VideoLoadError.fromException(e),
                        canRetry = VideoLoadError.fromException(e).isRetryable()
                    )
                }
            )
        } catch (e: Exception) {
            // 🔥🔥 [风控冷却] 异常失败，记录
            PlaybackCooldownManager.recordFailure(bvid, e.message ?: "exception")
            return VideoLoadResult.Error(
                error = VideoLoadError.fromException(e),
                canRetry = true
            )
        }
    }
    
    /**
     * Get cached position for video
     */
    fun getCachedPosition(bvid: String): Long {
        return progressManager.getCachedPosition(bvid)
    }
    
    /**
     * Save current playback position
     */
    fun savePosition(bvid: String) {
        val player = exoPlayer ?: return
        if (bvid.isNotEmpty() && player.currentPosition > 0) {
            progressManager.savePosition(bvid, player.currentPosition)
        }
    }
    
    /**
     * Play video with DASH format
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun playDashVideo(videoUrl: String, audioUrl: String?, seekTo: Long = 0L) {
        val player = exoPlayer ?: return
        player.volume = 1.0f
        
        val headers = mapOf(
            "Referer" to "https://www.bilibili.com",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        )
        val dataSourceFactory = androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(
            NetworkModule.okHttpClient
        ).setDefaultRequestProperties(headers)
        
        val mediaSourceFactory = androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSourceFactory)
        val videoSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(videoUrl))
        
        val finalSource = if (audioUrl != null) {
            val audioSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(audioUrl))
            androidx.media3.exoplayer.source.MergingMediaSource(videoSource, audioSource)
        } else {
            videoSource
        }
        
        player.setMediaSource(finalSource)
        player.prepare()
        if (seekTo > 0) {
            player.seekTo(seekTo)
        }
        player.playWhenReady = true
    }
    
    /**
     * Play simple video URL
     */
    fun playVideo(url: String, seekTo: Long = 0L) {
        val player = exoPlayer ?: return
        player.volume = 1.0f
        
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.prepare()
        if (seekTo > 0) {
            player.seekTo(seekTo)
        }
        player.playWhenReady = true
    }
    
    /**
     * Change quality using cached DASH streams
     */
    fun changeQualityFromCache(
        qualityId: Int,
        cachedVideos: List<DashVideo>,
        cachedAudios: List<DashAudio>,
        currentPos: Long
    ): QualitySwitchResult? {
        if (cachedVideos.isEmpty()) {
            Logger.d("VideoPlaybackUseCase", "🔥 changeQualityFromCache: cache is EMPTY, returning null")
            return null
        }
        
        // 🔥🔥 [调试] 输出缓存中的所有画质
        val availableIds = cachedVideos.map { it.id }.distinct().sortedDescending()
        Logger.d("VideoPlaybackUseCase", "🔥 changeQualityFromCache: target=$qualityId, available=$availableIds")
        
        // 🔥🔥 [优先精确匹配] 先找精确匹配
        val exactMatch = cachedVideos.find { it.id == qualityId }
        if (exactMatch != null) {
            Logger.d("VideoPlaybackUseCase", "✅ Exact match found: ${exactMatch.id}")
            val videoUrl = exactMatch.getValidUrl()
            val dashAudio = cachedAudios.firstOrNull()
            val audioUrl = dashAudio?.getValidUrl()
            if (videoUrl.isNotEmpty()) {
                playDashVideo(videoUrl, audioUrl, currentPos)
                return QualitySwitchResult(
                    videoUrl = videoUrl,
                    audioUrl = audioUrl,
                    actualQuality = exactMatch.id,
                    wasFallback = false,
                    cachedDashVideos = cachedVideos,
                    cachedDashAudios = cachedAudios
                )
            }
        }
        
        // 🔥🔥 [降级逻辑] 缓存中没有目标画质，需要返回 null 让调用者请求 API
        Logger.d("VideoPlaybackUseCase", "⚠️ Target quality $qualityId not in cache, returning null to trigger API request")
        return null
    }
    
    /**
     * Change quality via API request
     */
    suspend fun changeQualityFromApi(
        bvid: String,
        cid: Long,
        qualityId: Int,
        currentPos: Long
    ): QualitySwitchResult? {
        Logger.d("VideoPlaybackUseCase", "🔥 changeQualityFromApi: bvid=$bvid, cid=$cid, target=$qualityId")
        
        val playUrlData = VideoRepository.getPlayUrlData(bvid, cid, qualityId) ?: run {
            Logger.d("VideoPlaybackUseCase", "❌ getPlayUrlData returned null")
            return null
        }
        
        // 🔥🔥 [调试] 输出 API 返回的画质信息
        val returnedQuality = playUrlData.quality
        val acceptQualities = playUrlData.accept_quality
        val dashVideoIds = playUrlData.dash?.video?.map { it.id }?.distinct()?.sortedDescending()
        Logger.d("VideoPlaybackUseCase", "🔥 API returned: quality=$returnedQuality, accept_quality=$acceptQualities")
        Logger.d("VideoPlaybackUseCase", "🔥 DASH videos available: $dashVideoIds")
        
        val dashVideo = playUrlData.dash?.getBestVideo(qualityId)
        val dashAudio = playUrlData.dash?.getBestAudio()
        
        Logger.d("VideoPlaybackUseCase", "🔥 getBestVideo selected: ${dashVideo?.id}")
        
        val videoUrl = getValidVideoUrl(dashVideo, playUrlData)
        val audioUrl = dashAudio?.getValidUrl()
        
        if (videoUrl.isEmpty()) {
            Logger.d("VideoPlaybackUseCase", "❌ Video URL is empty")
            return null
        }
        
        if (dashVideo != null) {
            playDashVideo(videoUrl, audioUrl, currentPos)
        } else {
            playVideo(videoUrl, currentPos)
        }
        
        val actualQuality = dashVideo?.id ?: playUrlData.quality ?: qualityId
        Logger.d("VideoPlaybackUseCase", "✅ Quality switch result: target=$qualityId, actual=$actualQuality")
        
        return QualitySwitchResult(
            videoUrl = videoUrl,
            audioUrl = audioUrl,
            actualQuality = actualQuality,
            wasFallback = actualQuality != qualityId,
            cachedDashVideos = playUrlData.dash?.video ?: emptyList(),
            cachedDashAudios = playUrlData.dash?.audio ?: emptyList()
        )
    }
    
    /**
     * Get player current position
     */
    fun getCurrentPosition(): Long = exoPlayer?.currentPosition ?: 0L
    
    /**
     * Get player duration
     */
    fun getDuration(): Long {
        val duration = exoPlayer?.duration ?: 0L
        return if (duration < 0) 0L else duration
    }
    
    /**
     * Seek to position
     */
    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
    }
    
    private fun getValidVideoUrl(dashVideo: DashVideo?, playData: PlayUrlData): String {
        return dashVideo?.getValidUrl()?.takeIf { it.isNotEmpty() }
            ?: playData.dash?.video?.firstOrNull()?.baseUrl?.takeIf { it.isNotEmpty() }
            ?: playData.dash?.video?.firstOrNull()?.backupUrl?.firstOrNull()?.takeIf { it.isNotEmpty() }
            ?: playData.durl?.firstOrNull()?.url?.takeIf { it.isNotEmpty() }
            ?: playData.durl?.firstOrNull()?.backupUrl?.firstOrNull()
            ?: ""
    }
}
