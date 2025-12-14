// 文件路径: feature/video/PlayerViewModel.kt
package com.android.purebilibili.feature.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.android.purebilibili.data.model.VideoLoadError
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.model.response.ViewInfo
import com.android.purebilibili.data.model.response.DashVideo
import com.android.purebilibili.data.model.response.DashAudio
import com.android.purebilibili.data.model.response.getBestVideo
import com.android.purebilibili.data.model.response.getBestAudio
import com.android.purebilibili.data.repository.VideoRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.InputStream

sealed class PlayerUiState {
    // 🔥 增强 Loading 状态：包含重试进度信息
    data class Loading(
        val retryAttempt: Int = 0,
        val maxAttempts: Int = 4,
        val message: String = "加载中..."
    ) : PlayerUiState() {
        companion object {
            val Initial = Loading()
        }
    }
    
    data class Success(
        val info: ViewInfo,
        val playUrl: String,
        val audioUrl: String? = null,  // 🔥 添加音频 URL
        val related: List<RelatedVideo> = emptyList(),
        val currentQuality: Int = 64,
        val qualityLabels: List<String> = emptyList(),
        val qualityIds: List<Int> = emptyList(),
        val startPosition: Long = 0L,
        // 🔥🔥 [新增] 缓存的 DASH 流数据，用于切换清晰度
        val cachedDashVideos: List<DashVideo> = emptyList(),
        val cachedDashAudios: List<DashAudio> = emptyList(),
        // 🔥 新增：清晰度切换状态
        val isQualitySwitching: Boolean = false,
        val requestedQuality: Int? = null, // 用户请求的清晰度，用于显示降级提示
        // 🔥 登录与大会员状态
        val isLoggedIn: Boolean = false,
        val isVip: Boolean = false,  // 🔥 新增：大会员状态
        // 🔥 新增：关注/收藏状态
        val isFollowing: Boolean = false,
        val isFavorited: Boolean = false,
        // 🔥🔥 [新增] 点赞/投币状态
        val isLiked: Boolean = false,
        val coinCount: Int = 0,  // 已投币数量 (0/1/2)

        // 移除评论相关状态: replies, isRepliesLoading, replyCount, repliesError, isRepliesEnd, nextPage

        val emoteMap: Map<String, String> = emptyMap()
    ) : PlayerUiState()
    
    // 🔥 增强 Error 状态：使用 VideoLoadError 类型
    data class Error(
        val error: VideoLoadError,
        val canRetry: Boolean = true
    ) : PlayerUiState() {
        // 兼容旧代码的便捷属性
        val msg: String get() = error.toUserMessage()
    }
}

class PlayerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading())
    val uiState = _uiState.asStateFlow()

    // 移除 subReplyState

    private val _toastEvent = Channel<String>()
    val toastEvent = _toastEvent.receiveAsFlow()
    
    // 🎉 庆祝动画状态
    private val _likeBurstVisible = kotlinx.coroutines.flow.MutableStateFlow(false)
    val likeBurstVisible = _likeBurstVisible.asStateFlow()
    
    private val _tripleCelebrationVisible = kotlinx.coroutines.flow.MutableStateFlow(false)
    val tripleCelebrationVisible = _tripleCelebrationVisible.asStateFlow()
    
    fun dismissLikeBurst() { _likeBurstVisible.value = false }
    fun dismissTripleCelebration() { _tripleCelebrationVisible.value = false }

    private var currentBvid: String = ""
    private var currentCid: Long = 0
    private var exoPlayer: ExoPlayer? = null
    
    // 🔥🔥 [修复1] 心跳上报 Job，每 30 秒上报一次播放进度
    private var heartbeatJob: kotlinx.coroutines.Job? = null
    
    private fun startHeartbeat() {
        stopHeartbeat() // 确保没有重复的 Job
        heartbeatJob = viewModelScope.launch {
            while (true) {  // Job.cancel() 会在 delay 时抛出 CancellationException 终止循环
                kotlinx.coroutines.delay(30_000) // 每 30 秒
                val player = exoPlayer ?: continue
                if (player.isPlaying && currentBvid.isNotEmpty() && currentCid > 0) {
                    val positionSec = player.currentPosition / 1000
                    com.android.purebilibili.core.util.Logger.d("PlayerVM", "💓 Heartbeat: bvid=$currentBvid, cid=$currentCid, pos=$positionSec")
                    try {
                        VideoRepository.reportPlayHeartbeat(currentBvid, currentCid, positionSec)
                    } catch (e: Exception) {
                        com.android.purebilibili.core.util.Logger.w("PlayerVM", "Heartbeat failed: ${e.message}")
                    }
                }
            }
        }
    }
    
    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }


    fun attachPlayer(player: ExoPlayer) {
        this.exoPlayer = player
        val currentState = _uiState.value
        if (currentState is PlayerUiState.Success) {
            playVideo(currentState.playUrl, currentState.startPosition)
        }
    }

    fun getPlayerCurrentPosition(): Long = exoPlayer?.currentPosition ?: 0L
    fun getPlayerDuration(): Long = if ((exoPlayer?.duration ?: 0L) < 0) 0L else exoPlayer?.duration ?: 0L
    
    // 🔥🔥 新增：关注/取关 UP 主
    fun toggleFollow() {
        com.android.purebilibili.core.util.Logger.d("PlayerViewModel", "🔥 toggleFollow() called")
        val current = _uiState.value as? PlayerUiState.Success
        if (current == null) {
            android.util.Log.e("PlayerViewModel", "❌ toggleFollow: uiState is not Success")
            return
        }
        val mid = current.info.owner.mid
        val newFollowing = !current.isFollowing
        com.android.purebilibili.core.util.Logger.d("PlayerViewModel", "🔥 toggleFollow: mid=$mid, newFollowing=$newFollowing")
        
        viewModelScope.launch {
            val result = com.android.purebilibili.data.repository.ActionRepository.followUser(mid, newFollowing)
            result.onSuccess {
                com.android.purebilibili.core.util.Logger.d("PlayerViewModel", "✅ toggleFollow success: $it")
                _uiState.value = current.copy(isFollowing = it)
                _toastEvent.send(if (it) "关注成功" else "已取消关注")
            }.onFailure {
                android.util.Log.e("PlayerViewModel", "❌ toggleFollow failed: ${it.message}")
                _toastEvent.send(it.message ?: "操作失败")
            }
        }
    }
    
    // 🔥🔥 新增：收藏/取消收藏视频
    fun toggleFavorite() {
        com.android.purebilibili.core.util.Logger.d("PlayerViewModel", "🔥 toggleFavorite() called")
        val current = _uiState.value as? PlayerUiState.Success
        if (current == null) {
            android.util.Log.e("PlayerViewModel", "❌ toggleFavorite: uiState is not Success")
            return
        }
        val aid = current.info.aid
        val newFavorited = !current.isFavorited
        com.android.purebilibili.core.util.Logger.d("PlayerViewModel", "🔥 toggleFavorite: aid=$aid, newFavorited=$newFavorited")
        
        viewModelScope.launch {
            val result = com.android.purebilibili.data.repository.ActionRepository.favoriteVideo(aid, newFavorited)
            result.onSuccess {
                com.android.purebilibili.core.util.Logger.d("PlayerViewModel", "✅ toggleFavorite success: $it")
                // 🔥 更新收藏状态和计数
                val newStat = current.info.stat.copy(
                    favorite = current.info.stat.favorite + (if (it) 1 else -1)
                )
                val newInfo = current.info.copy(stat = newStat)
                _uiState.value = current.copy(info = newInfo, isFavorited = it)
                _toastEvent.send(if (it) "已收藏" else "已取消收藏")
            }.onFailure {
                android.util.Log.e("PlayerViewModel", "❌ toggleFavorite failed: ${it.message}")
                _toastEvent.send(it.message ?: "操作失败")
            }
        }
    }
    
    // 🔥🔥 [新增] 点赞/取消点赞
    fun toggleLike() {
        com.android.purebilibili.core.util.Logger.d("PlayerViewModel", "🔥 toggleLike() called")
        val current = _uiState.value as? PlayerUiState.Success ?: return
        val aid = current.info.aid
        val newLiked = !current.isLiked
        
        viewModelScope.launch {
            val result = com.android.purebilibili.data.repository.ActionRepository.likeVideo(aid, newLiked)
            result.onSuccess {
                // 🔥 更新点赞状态和计数
                val newStat = current.info.stat.copy(
                    like = current.info.stat.like + (if (it) 1 else -1)
                )
                val newInfo = current.info.copy(stat = newStat)
                _uiState.value = current.copy(info = newInfo, isLiked = it)
                // 🎉 点赞成功时触发庆祝动画
                if (it) _likeBurstVisible.value = true
                _toastEvent.send(if (it) "点赞成功" else "已取消点赞")
            }.onFailure {
                _toastEvent.send(it.message ?: "操作失败")
            }
        }
    }
    
    // 🔥🔥 [新增] 投币对话框状态
    private val _coinDialogVisible = kotlinx.coroutines.flow.MutableStateFlow(false)
    val coinDialogVisible = _coinDialogVisible.asStateFlow()
    
    fun openCoinDialog() {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        if (current.coinCount >= 2) {
            viewModelScope.launch { _toastEvent.send("已投满2个硬币") }
            return
        }
        _coinDialogVisible.value = true
    }
    
    fun closeCoinDialog() {
        _coinDialogVisible.value = false
    }
    
    // 🔥🔥 [新增] 执行投币
    fun doCoin(count: Int, alsoLike: Boolean) {
        com.android.purebilibili.core.util.Logger.d("PlayerViewModel", "🔥 doCoin: count=$count, alsoLike=$alsoLike")
        val current = _uiState.value as? PlayerUiState.Success ?: return
        val aid = current.info.aid
        
        _coinDialogVisible.value = false
        
        viewModelScope.launch {
            val result = com.android.purebilibili.data.repository.ActionRepository.coinVideo(aid, count, alsoLike)
            result.onSuccess {
                val newCoinCount = minOf(current.coinCount + count, 2)
                var newState = current.copy(coinCount = newCoinCount)
                if (alsoLike && !current.isLiked) {
                    newState = newState.copy(isLiked = true)
                }
                _uiState.value = newState
                _toastEvent.send("投币成功")
            }.onFailure {
                _toastEvent.send(it.message ?: "投币失败")
            }
        }
    }
    
    // 🔥🔥 [新增] 一键三连
    fun doTripleAction() {
        com.android.purebilibili.core.util.Logger.d("PlayerViewModel", "🔥 doTripleAction() called")
        val current = _uiState.value as? PlayerUiState.Success ?: return
        val aid = current.info.aid
        
        viewModelScope.launch {
            _toastEvent.send("正在三连...")
            val result = com.android.purebilibili.data.repository.ActionRepository.tripleAction(aid)
            result.onSuccess { tripleResult ->
                // 更新状态
                var newState = current
                if (tripleResult.likeSuccess) newState = newState.copy(isLiked = true)
                if (tripleResult.coinSuccess) newState = newState.copy(coinCount = 2)
                if (tripleResult.favoriteSuccess) newState = newState.copy(isFavorited = true)
                _uiState.value = newState
                
                // 构建反馈消息
                val parts = mutableListOf<String>()
                if (tripleResult.likeSuccess) parts.add("点赞✓")
                if (tripleResult.coinSuccess) parts.add("投币✓")
                else if (tripleResult.coinMessage != null) parts.add("投币:${tripleResult.coinMessage}")
                if (tripleResult.favoriteSuccess) parts.add("收藏✓")
                
                val allSuccess = tripleResult.likeSuccess && tripleResult.coinSuccess && tripleResult.favoriteSuccess
                // 🎉 三连成功时触发庆祝动画
                if (allSuccess) _tripleCelebrationVisible.value = true
                _toastEvent.send(if (allSuccess) "三连成功！" else parts.joinToString(" "))
            }.onFailure {
                _toastEvent.send(it.message ?: "三连失败")
            }
        }
    }
    
    // 🔥🔥 [新增] 视频分P切换
    fun switchPage(pageIndex: Int) {
        val current = _uiState.value as? PlayerUiState.Success ?: return
        val pages = current.info.pages
        if (pageIndex < 0 || pageIndex >= pages.size) return
        
        val page = pages[pageIndex]
        if (page.cid == currentCid) {
            viewModelScope.launch { _toastEvent.send("已是当前分P") }
            return
        }
        
        com.android.purebilibili.core.util.Logger.d("PlayerVM", "🔥 switchPage: index=$pageIndex, cid=${page.cid}, part=${page.part}")
        currentCid = page.cid
        
        viewModelScope.launch {
            _uiState.value = current.copy(isQualitySwitching = true)
            
            try {
                val playUrlData = VideoRepository.getPlayUrlData(currentBvid, page.cid, current.currentQuality)
                
                if (playUrlData != null) {
                    val dashVideo = playUrlData.dash?.getBestVideo(current.currentQuality)
                    val dashAudio = playUrlData.dash?.getBestAudio()
                    val videoUrl = dashVideo?.getValidUrl() 
                        ?: playUrlData.durl?.firstOrNull()?.url ?: ""
                    val audioUrl = dashAudio?.getValidUrl()
                    
                    if (videoUrl.isNotEmpty()) {
                        if (dashVideo != null) {
                            playDashVideo(videoUrl, audioUrl, 0L)
                        } else {
                            playVideo(videoUrl, 0L, forceReset = true)
                        }
                        
                        // 更新 info 中的 cid
                        val newInfo = current.info.copy(cid = page.cid)
                        _uiState.value = current.copy(
                            info = newInfo,
                            playUrl = videoUrl,
                            audioUrl = audioUrl,
                            startPosition = 0L,
                            isQualitySwitching = false,
                            cachedDashVideos = playUrlData.dash?.video ?: emptyList(),
                            cachedDashAudios = playUrlData.dash?.audio ?: emptyList()
                        )
                        _toastEvent.send("已切换至 P${pageIndex + 1}")
                        return@launch
                    }
                }
                
                _uiState.value = current.copy(isQualitySwitching = false)
                _toastEvent.send("分P切换失败")
            } catch (e: Exception) {
                _uiState.value = current.copy(isQualitySwitching = false)
                _toastEvent.send("分P切换失败: ${e.message}")
            }
        }
    }
    
    fun seekTo(pos: Long) { exoPlayer?.seekTo(pos) }

    override fun onCleared() {
        super.onCleared()
        stopHeartbeat()  // 🔥 停止心跳上报
        exoPlayer = null
    }

    // 🔥🔥🔥 [修改 1] 增加 forceReset 参数，默认 false
    private fun playVideo(url: String, seekTo: Long = 0L, forceReset: Boolean = false) {
        val player = exoPlayer ?: return

        val currentUri = player.currentMediaItem?.localConfiguration?.uri.toString()

        // 如果不是强制重置，且 URL 相同，且正在播放，则跳过（避免重复加载）
        // 但如果是切换画质，即使 URL 看起来一样（有时 B 站返回相同 URL），我们也要强制重置
        if (!forceReset && currentUri == url && player.playbackState != Player.STATE_IDLE) {
            return
        }

        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        if (seekTo > 0) {
            player.seekTo(seekTo)
        }
        player.prepare()
        player.playWhenReady = true
    }

    // 🔥🔥 [新增] DASH 格式播放：合并视频和音频流
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun playDashVideo(videoUrl: String, audioUrl: String?, seekTo: Long = 0L) {
        val player = exoPlayer ?: return
        com.android.purebilibili.core.util.Logger.d("PlayerVM", "🔥 playDashVideo: video=${videoUrl.take(50)}..., audio=${audioUrl?.take(50) ?: "null"}")
        
        val headers = mapOf(
            "Referer" to "https://www.bilibili.com",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        )
        val dataSourceFactory = androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(
            com.android.purebilibili.core.network.NetworkModule.okHttpClient
        ).setDefaultRequestProperties(headers)
        
        val mediaSourceFactory = androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSourceFactory)
        
        val videoSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(videoUrl))
        
        val finalSource = if (audioUrl != null) {
            val audioSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(audioUrl))
            // 🔥 使用 MergingMediaSource 合并视频和音频
            androidx.media3.exoplayer.source.MergingMediaSource(videoSource, audioSource)
        } else {
            videoSource
        }
        
        player.setMediaSource(finalSource)
        if (seekTo > 0) {
            player.seekTo(seekTo)
        }
        player.prepare()
        player.playWhenReady = true
    }
    
    // 🔥🔥 [新增] 从缓存恢复 UI 状态，避免网络重载
    fun restoreFromCache(cachedState: PlayerUiState.Success, startPosition: Long = -1L) {
        com.android.purebilibili.core.util.Logger.d("PlayerVM", "🔥 Restoring from cache: ${cachedState.info.title}, position=$startPosition")
        currentBvid = cachedState.info.bvid
        currentCid = cachedState.info.cid
        
        // 更新状态，保持播放进度
        val restoredState = if (startPosition >= 0) {
            cachedState.copy(startPosition = startPosition)
        } else {
            cachedState
        }
        _uiState.value = restoredState
    }

    fun loadVideo(bvid: String) {
        if (bvid.isBlank()) return
        
        // 🔥 如果已经加载过相同的视频，跳过重载（保持进度）
        val currentState = _uiState.value
        if (currentBvid == bvid && currentState is PlayerUiState.Success) {
            com.android.purebilibili.core.util.Logger.d("PlayerVM", "🔥 Same video already loaded, skip reload: $bvid")
            return
        }
        
        currentBvid = bvid
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading()

            val detailDeferred = async { VideoRepository.getVideoDetails(bvid) }
            val relatedDeferred = async { VideoRepository.getRelatedVideos(bvid) }
            val emoteDeferred = async { VideoRepository.getEmoteMap() }

            val detailResult = detailDeferred.await()
            val relatedVideos = relatedDeferred.await()
            val emoteMap = emoteDeferred.await()

            detailResult.onSuccess { (info, playData) ->
                currentCid = info.cid
                // 弹幕功能已移除，后续开发
                
                // 🔥🔥 [修复] 使用扩展函数选择最佳视频和音频流，增加更多 fallback
                val targetQn = playData.quality.takeIf { it > 0 } ?: 64
                com.android.purebilibili.core.util.Logger.d("PlayerVM", "🔍 loadVideo: targetQn=$targetQn, dash=${playData.dash != null}, dashVideoCount=${playData.dash?.video?.size ?: 0}, durlCount=${playData.durl?.size ?: 0}")
                
                val dashVideo = playData.dash?.getBestVideo(targetQn)
                val dashAudio = playData.dash?.getBestAudio()
                
                // 🔥🔥 [修复] 多层 fallback 确保能获取视频 URL
                val videoUrl = dashVideo?.getValidUrl()?.takeIf { it.isNotEmpty() }
                    ?: playData.dash?.video?.firstOrNull()?.baseUrl?.takeIf { it.isNotEmpty() }  // 直接访问第一个视频
                    ?: playData.dash?.video?.firstOrNull()?.backupUrl?.firstOrNull()?.takeIf { it.isNotEmpty() }  // 第一个视频的备用 URL
                    ?: playData.durl?.firstOrNull()?.url?.takeIf { it.isNotEmpty() }  // durl 格式
                    ?: playData.durl?.firstOrNull()?.backup_url?.firstOrNull()  // durl 备用
                    ?: ""
                    
                val audioUrl = dashAudio?.getValidUrl()?.takeIf { it.isNotEmpty() }
                    ?: playData.dash?.audio?.firstOrNull()?.baseUrl?.takeIf { it.isNotEmpty() }  // 直接访问第一个音频
                
                com.android.purebilibili.core.util.Logger.d("PlayerVM", "🔥 VideoUrl: ${if (videoUrl.isNotEmpty()) "${videoUrl.take(60)}..." else "EMPTY!"}")
                com.android.purebilibili.core.util.Logger.d("PlayerVM", "🔥 AudioUrl: ${if (audioUrl?.isNotEmpty() == true) "${audioUrl.take(60)}..." else "null"}")
                
                val qualities = playData.accept_quality ?: emptyList()
                val labels = playData.accept_description ?: emptyList()
                // 🔥 使用正在播放的 DASH 视频画质，而不是 durl 画质
                val realQuality = dashVideo?.id ?: playData.dash?.video?.firstOrNull()?.id ?: playData.quality

                if (videoUrl.isNotEmpty()) {
                    // 🔥 根据是否有音频流选择播放方式
                    if (playData.dash != null) {
                        playDashVideo(videoUrl, audioUrl, 0L)
                    } else {
                        playVideo(videoUrl)
                    }
                    // 🔥 获取登录状态和大会员状态
                    val isLogin = !com.android.purebilibili.core.store.TokenManager.sessDataCache.isNullOrEmpty()
                    val isVip = com.android.purebilibili.core.store.TokenManager.isVipCache
                    
                    // 🔥🔥 [新增] 异步检查关注和收藏状态
                    val isFollowingDeferred = async { 
                        if (isLogin) com.android.purebilibili.data.repository.ActionRepository.checkFollowStatus(info.owner.mid) 
                        else false 
                    }
                    val isFavoritedDeferred = async { 
                        if (isLogin) com.android.purebilibili.data.repository.ActionRepository.checkFavoriteStatus(info.aid) 
                        else false 
                    }
                    // 🔥🔥 [新增] 异步检查点赞和投币状态
                    val isLikedDeferred = async {
                        if (isLogin) com.android.purebilibili.data.repository.ActionRepository.checkLikeStatus(info.aid)
                        else false
                    }
                    val coinCountDeferred = async {
                        if (isLogin) com.android.purebilibili.data.repository.ActionRepository.checkCoinStatus(info.aid)
                        else 0
                    }
                    
                    val isFollowing = isFollowingDeferred.await()
                    val isFavorited = isFavoritedDeferred.await()
                    val isLiked = isLikedDeferred.await()
                    val coinCount = coinCountDeferred.await()
                    
                    _uiState.value = PlayerUiState.Success(
                        info = info,
                        playUrl = videoUrl,
                        audioUrl = audioUrl,  // 🔥 保存音频 URL
                        related = relatedVideos,

                        currentQuality = realQuality,
                        qualityIds = qualities,
                        qualityLabels = labels,
                        startPosition = 0L,
                        // 🔥🔥 缓存 DASH 流，用于切换清晰度时不需要再请求 API
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
                    
                    // 🔥🔥 [修复1] 上报播放心跳并启动定时心跳
                    launch {
                        VideoRepository.reportPlayHeartbeat(bvid, info.cid, 0)
                    }
                    startHeartbeat()  // 🔥 启动定时心跳上报
                    
                    // 移除 loadComments 调用
                } else {
                    _uiState.value = PlayerUiState.Error(
                        error = VideoLoadError.UnknownError(Exception("无法获取播放地址")),
                        canRetry = true
                    )
                }
            }.onFailure { e ->
                _uiState.value = PlayerUiState.Error(
                    error = VideoLoadError.fromException(e),
                    canRetry = VideoLoadError.fromException(e).isRetryable()
                )
            }
        }
    }
    
    // 🔥🔥 [新增] 重试功能
    fun retry() {
        val bvid = currentBvid
        if (bvid.isBlank()) return
        
        com.android.purebilibili.core.util.Logger.d("PlayerVM", "🔄 Retrying video load: $bvid")
        
        // 清除可能过期的缓存
        com.android.purebilibili.core.cache.PlayUrlCache.invalidate(bvid, currentCid)
        
        // 重置状态并重新加载
        currentBvid = "" // 允许重新加载
        loadVideo(bvid)
    }
    
    // 移除 loadComments, openSubReply, closeSubReply, loadMoreSubReplies, loadSubReplies

    // --- 核心优化: 清晰度切换 ---
    fun changeQuality(qualityId: Int, currentPos: Long) {
        val currentState = _uiState.value
        if (currentState is PlayerUiState.Success) {
            // 🔥 防止重复切换：如果正在切换中或已是目标画质，则跳过
            if (currentState.isQualitySwitching) {
                viewModelScope.launch { _toastEvent.send("正在切换中，请稍候...") }
                return
            }
            if (currentState.currentQuality == qualityId) {
                viewModelScope.launch { _toastEvent.send("已是当前清晰度") }
                return
            }

            viewModelScope.launch {
                // 🔥 进入切换状态
                _uiState.value = currentState.copy(
                    isQualitySwitching = true,
                    requestedQuality = qualityId
                )

                try {
                    // 🔥🔥 [优化] 优先使用缓存的 DASH 流，避免重复 API 请求导致 412
                    val cachedVideos = currentState.cachedDashVideos
                    val cachedAudios = currentState.cachedDashAudios
                    
                    com.android.purebilibili.core.util.Logger.d("PlayerVM", "🔥 changeQuality: requested=$qualityId, cachedVideos=${cachedVideos.map { it.id }}")
                    
                    if (cachedVideos.isNotEmpty()) {
                        // 从缓存中查找目标画质
                        val dashVideo = cachedVideos.find { it.id == qualityId }
                            ?: cachedVideos.filter { it.id <= qualityId }.maxByOrNull { it.id }
                            ?: cachedVideos.minByOrNull { it.id }
                        
                        val dashAudio = cachedAudios.firstOrNull()
                        val videoUrl = dashVideo?.getValidUrl() ?: ""
                        val audioUrl = dashAudio?.getValidUrl()
                        val realQuality = dashVideo?.id ?: qualityId
                        
                        com.android.purebilibili.core.util.Logger.d("PlayerVM", "🔥 Using cached DASH: found=$realQuality, url=${videoUrl.take(50)}...")
                        
                        if (videoUrl.isNotEmpty()) {
                            playDashVideo(videoUrl, audioUrl, currentPos)
                            
                            _uiState.value = currentState.copy(
                                playUrl = videoUrl,
                                audioUrl = audioUrl,
                                currentQuality = realQuality,
                                startPosition = currentPos,
                                isQualitySwitching = false,
                                requestedQuality = null
                            )
                            
                            val labels = currentState.qualityLabels
                            val qualities = currentState.qualityIds
                            val targetLabel = labels.getOrNull(qualities.indexOf(qualityId)) ?: "$qualityId"
                            val realLabel = labels.getOrNull(qualities.indexOf(realQuality)) ?: "$realQuality"
                            
                            if (realQuality != qualityId) {
                                _toastEvent.send("⚠️ $targetLabel 不可用，已切换至 $realLabel")
                            } else {
                                _toastEvent.send("✓ 已切换至 $realLabel")
                            }
                            return@launch
                        }
                    }
                    
                    // 🔥 缓存中没有，fallback 到 API 请求
                    com.android.purebilibili.core.util.Logger.d("PlayerVM", "🔥 Cache miss, falling back to API request")
                    fetchAndPlay(currentBvid, currentCid, qualityId, currentState, currentPos)
                    
                } catch (e: Exception) {
                    // 🔥 切换失败，恢复状态
                    _uiState.value = currentState.copy(
                        isQualitySwitching = false,
                        requestedQuality = null
                    )
                    _toastEvent.send("清晰度切换失败: ${e.message}")
                }
            }
        }
    }

    private suspend fun fetchAndPlay(
        bvid: String, cid: Long, qn: Int,
        currentState: PlayerUiState.Success,
        startPos: Long
    ) {
        // 调用 Repository 获取新画质链接
        val playUrlData = VideoRepository.getPlayUrlData(bvid, cid, qn)
        
        // 🔥 添加调试日志
        com.android.purebilibili.core.util.Logger.d("PlayerVM", "🔥 fetchAndPlay: playUrlData=${if (playUrlData != null) "OK" else "NULL"}")
        
        if (playUrlData == null) {
            android.util.Log.e("PlayerVM", "❌ getPlayUrlData returned null for bvid=$bvid, cid=$cid, qn=$qn")
            _uiState.value = currentState.copy(
                isQualitySwitching = false,
                requestedQuality = null
            )
            _toastEvent.send("获取播放地址失败，请重试")
            return
        }
        
        // 🔥🔥 [优化] 使用扩展函数选择最佳视频流（支持备用 URL 和编码优先级）
        val dashVideo = playUrlData.dash?.getBestVideo(qn)
        val dashAudio = playUrlData.dash?.getBestAudio()
        
        com.android.purebilibili.core.util.Logger.d("PlayerVM", "🔥 fetchAndPlay: requested=$qn, found=${dashVideo?.id ?: "none"}, codec=${dashVideo?.codecs ?: "none"}")
        
        // 🔥 使用 getValidUrl 扩展函数，自动 fallback 到备用 URL
        val videoUrl = dashVideo?.getValidUrl() 
            ?: playUrlData.durl?.firstOrNull()?.url?.takeIf { it.isNotEmpty() }
            ?: playUrlData.durl?.firstOrNull()?.backup_url?.firstOrNull()
            ?: ""
        val audioUrl = dashAudio?.getValidUrl()
        com.android.purebilibili.core.util.Logger.d("PlayerVM", "🔥 fetchAndPlay: videoUrl=${videoUrl.take(50)}...")
        
        val qualities = playUrlData.accept_quality ?: emptyList()
        val labels = playUrlData.accept_description ?: emptyList()
        // 🔥 使用正在播放的 DASH 视频画质
        val realQuality = dashVideo?.id ?: playUrlData.quality ?: qn

        if (videoUrl.isNotEmpty()) {
            // 🔥 使用 DASH 播放（如果有音频流）或普通播放
            if (dashVideo != null) {
                playDashVideo(videoUrl, audioUrl, startPos)
            } else {
                playVideo(videoUrl, startPos, forceReset = true)
            }

            // 🔥 切换完成，更新状态并清除切换标志
            _uiState.value = currentState.copy(
                playUrl = videoUrl,
                currentQuality = realQuality,
                qualityIds = qualities,
                qualityLabels = labels,
                startPosition = startPos,
                isQualitySwitching = false,
                requestedQuality = null
            )

            // 🔥 提示用户实际切换结果
            val targetLabel = labels.getOrNull(qualities.indexOf(qn)) ?: "$qn"
            val realLabel = labels.getOrNull(qualities.indexOf(realQuality)) ?: "$realQuality"

            if (realQuality != qn) {
                _toastEvent.send("⚠️ $targetLabel 不可用，已切换至 $realLabel")
            } else {
                _toastEvent.send("✓ 已切换至 $realLabel")
            }
        } else {
            // 🔥 切换失败，恢复状态
            _uiState.value = currentState.copy(
                isQualitySwitching = false,
                requestedQuality = null
            )
            _toastEvent.send("该清晰度无法播放")
        }
    }
}