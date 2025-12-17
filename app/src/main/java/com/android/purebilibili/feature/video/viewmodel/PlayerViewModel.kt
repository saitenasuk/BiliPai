// File: feature/video/PlayerViewModel.kt
// 🔥🔥 [重构] 简化版 PlayerViewModel - 使用 UseCase 层
package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.feature.video.usecase.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.android.purebilibili.core.cache.PlayUrlCache
import com.android.purebilibili.core.util.AnalyticsHelper
import com.android.purebilibili.core.util.CrashReporter
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.model.VideoLoadError
import com.android.purebilibili.data.model.response.*
import com.android.purebilibili.data.repository.SponsorBlockRepository
import com.android.purebilibili.data.repository.VideoRepository
import com.android.purebilibili.feature.video.usecase.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

// ========== UI State ==========
sealed class PlayerUiState {
    data class Loading(
        val retryAttempt: Int = 0,
        val maxAttempts: Int = 4,
        val message: String = "\u52a0\u8f7d\u4e2d..."
    ) : PlayerUiState() {
        companion object { val Initial = Loading() }
    }
    
    data class Success(
        val info: ViewInfo,
        val playUrl: String,
        val audioUrl: String? = null,
        val related: List<RelatedVideo> = emptyList(),
        val currentQuality: Int = 64,
        val qualityLabels: List<String> = emptyList(),
        val qualityIds: List<Int> = emptyList(),
        val startPosition: Long = 0L,
        val cachedDashVideos: List<DashVideo> = emptyList(),
        val cachedDashAudios: List<DashAudio> = emptyList(),
        val isQualitySwitching: Boolean = false,
        val requestedQuality: Int? = null,
        val isLoggedIn: Boolean = false,
        val isVip: Boolean = false,
        val isFollowing: Boolean = false,
        val isFavorited: Boolean = false,
        val isLiked: Boolean = false,
        val coinCount: Int = 0,
        val emoteMap: Map<String, String> = emptyMap()
    ) : PlayerUiState()
    
    data class Error(
        val error: VideoLoadError,
        val canRetry: Boolean = true
    ) : PlayerUiState() {
        val msg: String get() = error.toUserMessage()
    }
}

// ========== ViewModel ==========
class PlayerViewModel : ViewModel() {
    // UseCases
    private val playbackUseCase = VideoPlaybackUseCase()
    private val interactionUseCase = VideoInteractionUseCase()
    private val sponsorBlockUseCase = SponsorBlockUseCase()
    
    // State
    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading.Initial)
    val uiState = _uiState.asStateFlow()
    
    private val _toastEvent = Channel<String>()
    val toastEvent = _toastEvent.receiveAsFlow()
    
    // Celebration animations
    private val _likeBurstVisible = MutableStateFlow(false)
    val likeBurstVisible = _likeBurstVisible.asStateFlow()
    
    private val _tripleCelebrationVisible = MutableStateFlow(false)
    val tripleCelebrationVisible = _tripleCelebrationVisible.asStateFlow()
    
    // Coin dialog
    private val _coinDialogVisible = MutableStateFlow(false)
    val coinDialogVisible = _coinDialogVisible.asStateFlow()
    
    // SponsorBlock
    val sponsorSegments = sponsorBlockUseCase.segments
    val currentSponsorSegment = sponsorBlockUseCase.currentSegment
    val showSkipButton = sponsorBlockUseCase.showSkipButton
    
    // Internal state
    private var currentBvid = ""
    private var currentCid = 0L
    private var exoPlayer: ExoPlayer? = null
    private var heartbeatJob: Job? = null
    
    // ========== Public API ==========
    
    /**
     * 初始化持久化存储（需要在使用前调用一次）
     */
    fun initWithContext(context: android.content.Context) {
        playbackUseCase.initWithContext(context)
    }
    
    fun attachPlayer(player: ExoPlayer) {
        val changed = exoPlayer !== player
        if (changed && exoPlayer != null) saveCurrentPosition()
        exoPlayer = player
        playbackUseCase.attachPlayer(player)
        player.volume = 1.0f
    }
    
    fun loadVideo(bvid: String) {
        if (bvid.isBlank()) return
        
        // 🔥 防止重复加载：只有在正在加载同一视频时才跳过
        if (currentBvid == bvid && _uiState.value is PlayerUiState.Loading) {
            Logger.d("PlayerVM", "⚠️ Already loading $bvid, skip")
            return
        }
        
        // 🔥🔥 [修复] 更智能的重复检测：只有播放器真正在播放同一视频时才跳过
        // 如果播放器已停止、出错或处于空闲状态，应该重新加载
        val player = exoPlayer
        val isPlayerHealthy = player != null && 
            player.playbackState in listOf(Player.STATE_READY, Player.STATE_BUFFERING) &&
            player.playerError == null // 没有播放错误
        
        val currentSuccess = _uiState.value as? PlayerUiState.Success
        if (currentSuccess != null && currentBvid == bvid && isPlayerHealthy && player != null) {
            Logger.d("PlayerVM", "⚠️ $bvid already playing healthy, skip reload")
            // 🔥 确保音量正常
            player.volume = 1.0f
            if (!player.isPlaying) {
                player.play()
            }
            return
        }
        
        if (currentBvid.isNotEmpty() && currentBvid != bvid) saveCurrentPosition()
        
        val cachedPosition = playbackUseCase.getCachedPosition(bvid)
        currentBvid = bvid
        
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading.Initial
            
            when (val result = playbackUseCase.loadVideo(bvid)) {
                is VideoLoadResult.Success -> {
                    currentCid = result.info.cid
                    
                    // Play video
                    if (result.audioUrl != null) {
                        playbackUseCase.playDashVideo(result.playUrl, result.audioUrl, cachedPosition)
                    } else {
                        playbackUseCase.playVideo(result.playUrl, cachedPosition)
                    }
                    
                    _uiState.value = PlayerUiState.Success(
                        info = result.info,
                        playUrl = result.playUrl,
                        audioUrl = result.audioUrl,
                        related = result.related,
                        currentQuality = result.quality,
                        qualityIds = result.qualityIds,
                        qualityLabels = result.qualityLabels,
                        cachedDashVideos = result.cachedDashVideos,
                        cachedDashAudios = result.cachedDashAudios,
                        emoteMap = result.emoteMap,
                        isLoggedIn = result.isLoggedIn,
                        isVip = result.isVip,
                        isFollowing = result.isFollowing,
                        isFavorited = result.isFavorited,
                        isLiked = result.isLiked,
                        coinCount = result.coinCount
                    )
                    
                    startHeartbeat()
                    sponsorBlockUseCase.loadSegments(bvid)
                    AnalyticsHelper.logVideoPlay(bvid, result.info.title, result.info.owner.name)
                }
                is VideoLoadResult.Error -> {
                    CrashReporter.reportVideoError(bvid, "load_failed", result.error.toUserMessage())
                    _uiState.value = PlayerUiState.Error(result.error, result.canRetry)
                }
            }
        }
    }
    
    fun retry() {
        val bvid = currentBvid.takeIf { it.isNotBlank() } ?: return
        PlayUrlCache.invalidate(bvid, currentCid)
        currentBvid = ""
        loadVideo(bvid)
    }
    
    // ========== Interaction ==========
    
    fun toggleFollow() {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        viewModelScope.launch {
            interactionUseCase.toggleFollow(current.info.owner.mid, current.isFollowing)
                .onSuccess { _uiState.value = current.copy(isFollowing = it); toast(if (it) "\u5173\u6ce8\u6210\u529f" else "\u5df2\u53d6\u6d88\u5173\u6ce8") }
                .onFailure { toast(it.message ?: "\u64cd\u4f5c\u5931\u8d25") }
        }
    }
    
    fun toggleFavorite() {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        viewModelScope.launch {
            interactionUseCase.toggleFavorite(current.info.aid, current.isFavorited, currentBvid)
                .onSuccess { 
                    val newStat = current.info.stat.copy(favorite = current.info.stat.favorite + if (it) 1 else -1)
                    _uiState.value = current.copy(info = current.info.copy(stat = newStat), isFavorited = it)
                    toast(if (it) "\u5df2\u6536\u85cf" else "\u5df2\u53d6\u6d88\u6536\u85cf")
                }
                .onFailure { toast(it.message ?: "\u64cd\u4f5c\u5931\u8d25") }
        }
    }
    
    fun toggleLike() {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        viewModelScope.launch {
            interactionUseCase.toggleLike(current.info.aid, current.isLiked, currentBvid)
                .onSuccess { 
                    val newStat = current.info.stat.copy(like = current.info.stat.like + if (it) 1 else -1)
                    _uiState.value = current.copy(info = current.info.copy(stat = newStat), isLiked = it)
                    if (it) _likeBurstVisible.value = true
                    toast(if (it) "\u70b9\u8d5e\u6210\u529f" else "\u5df2\u53d6\u6d88\u70b9\u8d5e")
                }
                .onFailure { toast(it.message ?: "\u64cd\u4f5c\u5931\u8d25") }
        }
    }
    
    fun openCoinDialog() {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        if (current.coinCount >= 2) { toast("\u5df2\u6295\u6ee12\u4e2a\u786c\u5e01"); return }
        _coinDialogVisible.value = true
    }
    
    fun closeCoinDialog() { _coinDialogVisible.value = false }
    
    fun doCoin(count: Int, alsoLike: Boolean) {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        _coinDialogVisible.value = false
        viewModelScope.launch {
            interactionUseCase.doCoin(current.info.aid, count, alsoLike, currentBvid)
                .onSuccess { 
                    var newState = current.copy(coinCount = minOf(current.coinCount + count, 2))
                    if (alsoLike && !current.isLiked) newState = newState.copy(isLiked = true)
                    _uiState.value = newState
                    toast("\u6295\u5e01\u6210\u529f")
                }
                .onFailure { toast(it.message ?: "\u6295\u5e01\u5931\u8d25") }
        }
    }
    
    fun doTripleAction() {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        viewModelScope.launch {
            toast("\u6b63\u5728\u4e09\u8fde...")
            interactionUseCase.doTripleAction(current.info.aid)
                .onSuccess { result ->
                    var newState = current
                    if (result.likeSuccess) newState = newState.copy(isLiked = true)
                    if (result.coinSuccess) newState = newState.copy(coinCount = 2)
                    if (result.favoriteSuccess) newState = newState.copy(isFavorited = true)
                    _uiState.value = newState
                    if (result.allSuccess) _tripleCelebrationVisible.value = true
                    toast(result.toSummaryMessage())
                }
                .onFailure { toast(it.message ?: "\u4e09\u8fde\u5931\u8d25") }
        }
    }
    
    fun dismissLikeBurst() { _likeBurstVisible.value = false }
    fun dismissTripleCelebration() { _tripleCelebrationVisible.value = false }
    
    // ========== Quality ==========
    
    fun changeQuality(qualityId: Int, currentPos: Long) {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        if (current.isQualitySwitching) { toast("\u6b63\u5728\u5207\u6362\u4e2d..."); return }
        if (current.currentQuality == qualityId) { toast("\u5df2\u662f\u5f53\u524d\u6e05\u6670\u5ea6"); return }
        
        _uiState.value = current.copy(isQualitySwitching = true, requestedQuality = qualityId)
        
        viewModelScope.launch {
            val result = playbackUseCase.changeQualityFromCache(qualityId, current.cachedDashVideos, current.cachedDashAudios, currentPos)
                ?: playbackUseCase.changeQualityFromApi(currentBvid, currentCid, qualityId, currentPos)
            
            if (result != null) {
                _uiState.value = current.copy(
                    playUrl = result.videoUrl, audioUrl = result.audioUrl,
                    currentQuality = result.actualQuality, isQualitySwitching = false, requestedQuality = null
                )
                val label = current.qualityLabels.getOrNull(current.qualityIds.indexOf(result.actualQuality)) ?: "${result.actualQuality}"
                toast(if (result.wasFallback) "\u26a0\ufe0f \u5df2\u5207\u6362\u81f3 $label" else "\u2713 \u5df2\u5207\u6362\u81f3 $label")
            } else {
                _uiState.value = current.copy(isQualitySwitching = false, requestedQuality = null)
                toast("\u6e05\u6670\u5ea6\u5207\u6362\u5931\u8d25")
            }
        }
    }
    
    // ========== Page Switch ==========
    
    fun switchPage(pageIndex: Int) {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        val page = current.info.pages.getOrNull(pageIndex) ?: return
        if (page.cid == currentCid) { toast("\u5df2\u662f\u5f53\u524d\u5206P"); return }
        
        currentCid = page.cid
        _uiState.value = current.copy(isQualitySwitching = true)
        
        viewModelScope.launch {
            try {
                val playUrlData = VideoRepository.getPlayUrlData(currentBvid, page.cid, current.currentQuality)
                if (playUrlData != null) {
                    val dashVideo = playUrlData.dash?.getBestVideo(current.currentQuality)
                    val dashAudio = playUrlData.dash?.getBestAudio()
                    val videoUrl = dashVideo?.getValidUrl() ?: playUrlData.durl?.firstOrNull()?.url ?: ""
                    val audioUrl = dashAudio?.getValidUrl()
                    
                    if (videoUrl.isNotEmpty()) {
                        if (dashVideo != null) playbackUseCase.playDashVideo(videoUrl, audioUrl, 0L)
                        else playbackUseCase.playVideo(videoUrl, 0L)
                        
                        _uiState.value = current.copy(
                            info = current.info.copy(cid = page.cid), playUrl = videoUrl, audioUrl = audioUrl,
                            startPosition = 0L, isQualitySwitching = false,
                            cachedDashVideos = playUrlData.dash?.video ?: emptyList(),
                            cachedDashAudios = playUrlData.dash?.audio ?: emptyList()
                        )
                        toast("\u5df2\u5207\u6362\u81f3 P${pageIndex + 1}")
                        return@launch
                    }
                }
                _uiState.value = current.copy(isQualitySwitching = false)
                toast("\u5206P\u5207\u6362\u5931\u8d25")
            } catch (e: Exception) {
                _uiState.value = current.copy(isQualitySwitching = false)
                toast("\u5206P\u5207\u6362\u5931\u8d25: ${e.message}")
            }
        }
    }
    
    // ========== SponsorBlock ==========
    
    suspend fun checkAndSkipSponsor(context: android.content.Context): Boolean {
        val pos = playbackUseCase.getCurrentPosition()
        val result = sponsorBlockUseCase.checkAndSkip(context, pos) { playbackUseCase.seekTo(it) }
        if (result == SkipResult.SKIPPED) toast("\u5df2\u8df3\u8fc7\u7a7a\u964d\u7247\u6bb5")
        return result == SkipResult.SKIPPED
    }
    
    fun skipCurrentSponsorSegment() {
        sponsorBlockUseCase.skipCurrent { playbackUseCase.seekTo(it) }
        viewModelScope.launch { toast("\u5df2\u8df3\u8fc7\u7a7a\u964d\u7247\u6bb5") }
    }
    
    fun dismissSponsorSkipButton() { sponsorBlockUseCase.dismiss() }
    
    // ========== Playback Control ==========
    
    fun seekTo(pos: Long) { playbackUseCase.seekTo(pos) }
    fun getPlayerCurrentPosition() = playbackUseCase.getCurrentPosition()
    fun getPlayerDuration() = playbackUseCase.getDuration()
    fun saveCurrentPosition() { playbackUseCase.savePosition(currentBvid) }
    
    fun restoreFromCache(cachedState: PlayerUiState.Success, startPosition: Long = -1L) {
        currentBvid = cachedState.info.bvid
        currentCid = cachedState.info.cid
        _uiState.value = if (startPosition >= 0) cachedState.copy(startPosition = startPosition) else cachedState
    }
    
    // ========== Private ==========
    
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (true) {
                delay(30_000)
                if (exoPlayer?.isPlaying == true && currentBvid.isNotEmpty() && currentCid > 0) {
                    try { VideoRepository.reportPlayHeartbeat(currentBvid, currentCid, playbackUseCase.getCurrentPosition() / 1000) }
                    catch (_: Exception) {}
                }
            }
        }
    }
    
    private fun toast(msg: String) { viewModelScope.launch { _toastEvent.send(msg) } }
    
    override fun onCleared() {
        super.onCleared()
        heartbeatJob?.cancel()
        exoPlayer = null
    }
}