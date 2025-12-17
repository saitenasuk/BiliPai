// 文件路径: feature/bangumi/BangumiPlayerViewModel.kt
package com.android.purebilibili.feature.bangumi

import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.android.purebilibili.data.model.response.*
import com.android.purebilibili.data.repository.BangumiRepository
import com.android.purebilibili.feature.player.BasePlayerViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * 番剧播放器 UI 状态
 */
sealed class BangumiPlayerState {
    object Loading : BangumiPlayerState()
    
    data class Success(
        val seasonDetail: BangumiDetail,
        val currentEpisode: BangumiEpisode,
        val currentEpisodeIndex: Int,
        val playUrl: String?,
        val audioUrl: String?,
        val quality: Int,
        val acceptQuality: List<Int>,
        val acceptDescription: List<String>
    ) : BangumiPlayerState()
    
    data class Error(
        val message: String,
        val isVipRequired: Boolean = false,
        val isLoginRequired: Boolean = false,
        val canRetry: Boolean = true
    ) : BangumiPlayerState()
}

/**
 * 番剧播放器 ViewModel
 * 
 * 🔥🔥 [重构] 继承 BasePlayerViewModel，复用空降助手、DASH 播放、弹幕等公共功能
 */
class BangumiPlayerViewModel : BasePlayerViewModel() {
    
    private val _uiState = MutableStateFlow<BangumiPlayerState>(BangumiPlayerState.Loading)
    val uiState = _uiState.asStateFlow()
    
    // 🔥 Toast 事件通道
    private val _toastEvent = Channel<String>()
    val toastEvent = _toastEvent.receiveAsFlow()
    
    private var currentSeasonId: Long = 0
    private var currentEpId: Long = 0
    
    // 🔥🔥 [重构] 覆盖基类的空降跳过回调，显示 toast
    override fun onSponsorSkipped(segment: SponsorSegment) {
        viewModelScope.launch {
            _toastEvent.send("已跳过: ${segment.categoryName}")
        }
    }
    
    /**
     * 绑定播放器
     */
    override fun attachPlayer(player: ExoPlayer) {
        super.attachPlayer(player)
    }
    
    /**
     * 加载番剧播放（从详情页进入）
     */
    fun loadBangumiPlay(seasonId: Long, epId: Long) {
        if (seasonId == currentSeasonId && epId == currentEpId && _uiState.value is BangumiPlayerState.Success) {
            return // 避免重复加载
        }
        
        currentSeasonId = seasonId
        currentEpId = epId
        
        viewModelScope.launch {
            _uiState.value = BangumiPlayerState.Loading
            
            // 1. 获取番剧详情（包含剧集列表）
            val detailResult = BangumiRepository.getSeasonDetail(seasonId)
            
            detailResult.onSuccess { detail ->
                // 找到当前剧集
                val episode = detail.episodes?.find { it.id == epId }
                    ?: detail.episodes?.firstOrNull()
                
                if (episode == null) {
                    _uiState.value = BangumiPlayerState.Error("未找到可播放的剧集")
                    return@onSuccess
                }
                
                val episodeIndex = detail.episodes?.indexOfFirst { it.id == episode.id } ?: 0
                
                // 2. 获取播放地址
                fetchPlayUrl(detail, episode, episodeIndex)
                
            }.onFailure { e ->
                _uiState.value = BangumiPlayerState.Error(
                    message = e.message ?: "加载失败",
                    canRetry = true
                )
            }
        }
    }
    
    /**
     * 获取播放地址
     */
    private suspend fun fetchPlayUrl(detail: BangumiDetail, episode: BangumiEpisode, episodeIndex: Int) {
        val playUrlResult = BangumiRepository.getBangumiPlayUrl(episode.id)
        
        playUrlResult.onSuccess { playData ->
            // 解析播放地址
            val videoUrl: String?
            val audioUrl: String?
            
            if (playData.dash != null) {
                // DASH 格式
                val dash = playData.dash
                val video = dash.getBestVideo(playData.quality)
                val audio = dash.getBestAudio()
                videoUrl = video?.getValidUrl()
                audioUrl = audio?.getValidUrl()
            } else if (!playData.durl.isNullOrEmpty()) {
                // FLV/MP4 格式
                videoUrl = playData.durl.firstOrNull()?.url
                audioUrl = null
            } else {
                _uiState.value = BangumiPlayerState.Error("无法获取播放地址")
                return
            }
            
            if (videoUrl.isNullOrEmpty()) {
                _uiState.value = BangumiPlayerState.Error("无法获取播放地址")
                return
            }
            
            _uiState.value = BangumiPlayerState.Success(
                seasonDetail = detail,
                currentEpisode = episode,
                currentEpisodeIndex = episodeIndex,
                playUrl = videoUrl,
                audioUrl = audioUrl,
                quality = playData.quality,
                acceptQuality = playData.acceptQuality ?: emptyList(),
                acceptDescription = playData.acceptDescription ?: emptyList()
            )
            
            // 🔥🔥 [重构] 使用基类方法播放视频
            playDashVideo(videoUrl, audioUrl)
            
            // 🔥🔥 [重构] 使用基类方法加载弹幕
            loadDanmaku(episode.cid)
            
            // 🔥 [重构] 使用基类方法加载空降片段
            episode.bvid?.let { loadSponsorSegments(it) }
            
        }.onFailure { e ->
            val isVip = e.message?.contains("大会员") == true
            val isLogin = e.message?.contains("登录") == true
            _uiState.value = BangumiPlayerState.Error(
                message = e.message ?: "获取播放地址失败",
                isVipRequired = isVip,
                isLoginRequired = isLogin,
                canRetry = !isVip && !isLogin
            )
        }
    }
    
    /**
     * 切换剧集
     */
    fun switchEpisode(episode: BangumiEpisode) {
        val currentState = _uiState.value as? BangumiPlayerState.Success ?: return
        
        if (episode.id == currentState.currentEpisode.id) return
        
        currentEpId = episode.id
        val newIndex = currentState.seasonDetail.episodes?.indexOfFirst { it.id == episode.id } ?: 0
        
        viewModelScope.launch {
            _uiState.value = BangumiPlayerState.Loading
            fetchPlayUrl(currentState.seasonDetail, episode, newIndex)
        }
    }
    
    /**
     * 切换清晰度
     */
    fun changeQuality(qualityId: Int) {
        val currentState = _uiState.value as? BangumiPlayerState.Success ?: return
        val currentPos = getPlayerCurrentPosition()
        
        viewModelScope.launch {
            val playUrlResult = BangumiRepository.getBangumiPlayUrl(currentState.currentEpisode.id, qualityId)
            
            playUrlResult.onSuccess { playData ->
                val videoUrl: String?
                val audioUrl: String?
                
                if (playData.dash != null) {
                    val video = playData.dash.getBestVideo(qualityId)
                    val audio = playData.dash.getBestAudio()
                    videoUrl = video?.getValidUrl()
                    audioUrl = audio?.getValidUrl()
                } else if (!playData.durl.isNullOrEmpty()) {
                    videoUrl = playData.durl.firstOrNull()?.url
                    audioUrl = null
                } else {
                    return@onSuccess
                }
                
                if (videoUrl.isNullOrEmpty()) return@onSuccess
                
                _uiState.value = currentState.copy(
                    playUrl = videoUrl,
                    audioUrl = audioUrl,
                    quality = playData.quality
                )
                
                // 🔥🔥 [修复] 切换清晰度时使用 resetPlayer=false 减少闪烁
                playDashVideo(videoUrl, audioUrl, currentPos, resetPlayer = false)
            }
        }
    }
    
    /**
     * 追番/取消追番
     */
    fun toggleFollow() {
        val currentState = _uiState.value as? BangumiPlayerState.Success ?: return
        val isFollowing = currentState.seasonDetail.userStatus?.follow == 1
        
        viewModelScope.launch {
            val result = if (isFollowing) {
                BangumiRepository.unfollowBangumi(currentState.seasonDetail.seasonId)
            } else {
                BangumiRepository.followBangumi(currentState.seasonDetail.seasonId)
            }
            
            if (result.isSuccess) {
                // 重新加载详情以更新追番状态
                val newDetail = BangumiRepository.getSeasonDetail(currentState.seasonDetail.seasonId).getOrNull()
                if (newDetail != null) {
                    _uiState.value = currentState.copy(seasonDetail = newDetail)
                }
            }
        }
    }
    
    /**
     * 重试
     */
    fun retry() {
        loadBangumiPlay(currentSeasonId, currentEpId)
    }
}
