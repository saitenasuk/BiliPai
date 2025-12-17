// File: feature/video/usecase/VideoPlaybackUseCase.kt
package com.android.purebilibili.feature.video.usecase

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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
     */
    suspend fun loadVideo(
        bvid: String,
        onProgress: (String) -> Unit = {}
    ): VideoLoadResult {
        try {
            onProgress("Loading video info...")
            
            val detailResult = VideoRepository.getVideoDetails(bvid)
            val relatedVideos = VideoRepository.getRelatedVideos(bvid)
            val emoteMap = VideoRepository.getEmoteMap()
            
            return detailResult.fold(
                onSuccess = { (info, playData) ->
                    val targetQn = playData.quality.takeIf { it > 0 } ?: 64
                    
                    val dashVideo = playData.dash?.getBestVideo(targetQn)
                    val dashAudio = playData.dash?.getBestAudio()
                    
                    val videoUrl = getValidVideoUrl(dashVideo, playData)
                    val audioUrl = dashAudio?.getValidUrl()?.takeIf { it.isNotEmpty() }
                    
                    if (videoUrl.isEmpty()) {
                        return@fold VideoLoadResult.Error(
                            error = VideoLoadError.UnknownError(Exception("Cannot get play URL")),
                            canRetry = true
                        )
                    }
                    
                    val isLogin = !com.android.purebilibili.core.store.TokenManager.sessDataCache.isNullOrEmpty()
                    val isVip = com.android.purebilibili.core.store.TokenManager.isVipCache
                    
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
                        qualityIds = playData.accept_quality ?: emptyList(),
                        qualityLabels = playData.accept_description ?: emptyList(),
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
                    VideoLoadResult.Error(
                        error = VideoLoadError.fromException(e),
                        canRetry = VideoLoadError.fromException(e).isRetryable()
                    )
                }
            )
        } catch (e: Exception) {
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
        if (cachedVideos.isEmpty()) return null
        
        val dashVideo = cachedVideos.find { it.id == qualityId }
            ?: cachedVideos.filter { it.id <= qualityId }.maxByOrNull { it.id }
            ?: cachedVideos.minByOrNull { it.id }
        
        val dashAudio = cachedAudios.firstOrNull()
        val videoUrl = dashVideo?.getValidUrl() ?: return null
        val audioUrl = dashAudio?.getValidUrl()
        
        if (videoUrl.isNotEmpty()) {
            playDashVideo(videoUrl, audioUrl, currentPos)
            return QualitySwitchResult(
                videoUrl = videoUrl,
                audioUrl = audioUrl,
                actualQuality = dashVideo.id,
                wasFallback = dashVideo.id != qualityId,
                cachedDashVideos = cachedVideos,
                cachedDashAudios = cachedAudios
            )
        }
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
        val playUrlData = VideoRepository.getPlayUrlData(bvid, cid, qualityId) ?: return null
        
        val dashVideo = playUrlData.dash?.getBestVideo(qualityId)
        val dashAudio = playUrlData.dash?.getBestAudio()
        
        val videoUrl = getValidVideoUrl(dashVideo, playUrlData)
        val audioUrl = dashAudio?.getValidUrl()
        
        if (videoUrl.isEmpty()) return null
        
        if (dashVideo != null) {
            playDashVideo(videoUrl, audioUrl, currentPos)
        } else {
            playVideo(videoUrl, currentPos)
        }
        
        return QualitySwitchResult(
            videoUrl = videoUrl,
            audioUrl = audioUrl,
            actualQuality = dashVideo?.id ?: playUrlData.quality ?: qualityId,
            wasFallback = (dashVideo?.id ?: qualityId) != qualityId,
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
