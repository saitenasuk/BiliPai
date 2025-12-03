// 文件路径: feature/video/PlayerViewModel.kt
package com.android.purebilibili.feature.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.model.response.ViewInfo
import com.android.purebilibili.data.repository.VideoRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.InputStream

// 二级评论状态
data class SubReplyUiState(
    val visible: Boolean = false,
    val rootReply: ReplyItem? = null,
    val items: List<ReplyItem> = emptyList(),
    val isLoading: Boolean = false,
    val page: Int = 1,
    val isEnd: Boolean = false,
    val error: String? = null
)

sealed class PlayerUiState {
    object Loading : PlayerUiState()
    data class Success(
        val info: ViewInfo,
        val playUrl: String,
        val related: List<RelatedVideo> = emptyList(),
        val danmakuStream: InputStream? = null,
        val currentQuality: Int = 64,
        val qualityLabels: List<String> = emptyList(),
        val qualityIds: List<Int> = emptyList(),
        val startPosition: Long = 0L,

        val replies: List<ReplyItem> = emptyList(),
        val isRepliesLoading: Boolean = false,
        val replyCount: Int = 0,
        val repliesError: String? = null,
        val isRepliesEnd: Boolean = false,
        val nextPage: Int = 1,

        val emoteMap: Map<String, String> = emptyMap()
    ) : PlayerUiState()
    data class Error(val msg: String) : PlayerUiState()
}

class PlayerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _subReplyState = MutableStateFlow(SubReplyUiState())
    val subReplyState = _subReplyState.asStateFlow()

    private val _toastEvent = Channel<String>()
    val toastEvent = _toastEvent.receiveAsFlow()

    private var currentBvid: String = ""
    private var currentCid: Long = 0
    private var exoPlayer: ExoPlayer? = null

    fun attachPlayer(player: ExoPlayer) {
        this.exoPlayer = player
        val currentState = _uiState.value
        if (currentState is PlayerUiState.Success) {
            playVideo(currentState.playUrl, currentState.startPosition)
        }
    }

    fun getPlayerCurrentPosition(): Long = exoPlayer?.currentPosition ?: 0L
    fun getPlayerDuration(): Long = if ((exoPlayer?.duration ?: 0L) < 0) 0L else exoPlayer?.duration ?: 0L
    fun seekTo(pos: Long) { exoPlayer?.seekTo(pos) }

    override fun onCleared() {
        super.onCleared()
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

    fun loadVideo(bvid: String) {
        if (bvid.isBlank()) return
        currentBvid = bvid
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading

            val detailDeferred = async { VideoRepository.getVideoDetails(bvid) }
            val relatedDeferred = async { VideoRepository.getRelatedVideos(bvid) }
            val emoteDeferred = async { VideoRepository.getEmoteMap() }

            val detailResult = detailDeferred.await()
            val relatedVideos = relatedDeferred.await()
            val emoteMap = emoteDeferred.await()

            detailResult.onSuccess { (info, playData) ->
                currentCid = info.cid
                val danmaku = VideoRepository.getDanmakuStream(info.cid)
                val url = playData.durl?.firstOrNull()?.url ?: ""
                val qualities = playData.accept_quality ?: emptyList()
                val labels = playData.accept_description ?: emptyList()
                val realQuality = playData.quality

                if (url.isNotEmpty()) {
                    playVideo(url)
                    _uiState.value = PlayerUiState.Success(
                        info = info,
                        playUrl = url,
                        related = relatedVideos,
                        danmakuStream = danmaku,
                        currentQuality = realQuality,
                        qualityIds = qualities,
                        qualityLabels = labels,
                        startPosition = 0L,
                        emoteMap = emoteMap
                    )
                    loadComments(info.aid)
                } else {
                    _uiState.value = PlayerUiState.Error("无法获取播放地址")
                }
            }.onFailure {
                _uiState.value = PlayerUiState.Error(it.message ?: "加载失败")
            }
        }
    }

    // --- 评论加载逻辑 ---
    fun loadComments(aid: Long) {
        val currentState = _uiState.value
        if (currentState is PlayerUiState.Success) {
            if (currentState.isRepliesEnd || currentState.isRepliesLoading) return

            _uiState.value = currentState.copy(isRepliesLoading = true, repliesError = null)

            viewModelScope.launch {
                val pageToLoad = currentState.nextPage
                val result = VideoRepository.getComments(aid, pageToLoad, 20)

                result.onSuccess { data ->
                    val current = _uiState.value
                    if (current is PlayerUiState.Success) {
                        val isEnd = data.cursor.isEnd || data.replies.isNullOrEmpty()
                        _uiState.value = current.copy(
                            replies = (current.replies + (data.replies ?: emptyList())).distinctBy { it.rpid },
                            replyCount = data.cursor.allCount,
                            isRepliesLoading = false,
                            repliesError = null,
                            isRepliesEnd = isEnd,
                            nextPage = pageToLoad + 1
                        )
                    }
                }.onFailure { e ->
                    val current = _uiState.value
                    if (current is PlayerUiState.Success) {
                        _uiState.value = current.copy(
                            isRepliesLoading = false,
                            repliesError = e.message ?: "加载评论失败"
                        )
                    }
                }
            }
        }
    }

    fun openSubReply(rootReply: ReplyItem) {
        _subReplyState.value = SubReplyUiState(
            visible = true,
            rootReply = rootReply,
            isLoading = true,
            page = 1
        )
        loadSubReplies(rootReply.oid, rootReply.rpid, 1)
    }

    fun closeSubReply() {
        _subReplyState.value = _subReplyState.value.copy(visible = false)
    }

    fun loadMoreSubReplies() {
        val state = _subReplyState.value
        if (state.isLoading || state.isEnd || state.rootReply == null) return
        val nextPage = state.page + 1
        _subReplyState.value = state.copy(isLoading = true)
        loadSubReplies(state.rootReply.oid, state.rootReply.rpid, nextPage)
    }

    private fun loadSubReplies(oid: Long, rootId: Long, page: Int) {
        viewModelScope.launch {
            val result = VideoRepository.getSubComments(oid, rootId, page)
            result.onSuccess { data ->
                val current = _subReplyState.value
                val newItems = data.replies ?: emptyList()
                val isEnd = data.cursor.isEnd || newItems.isEmpty()

                _subReplyState.value = current.copy(
                    items = if (page == 1) newItems else (current.items + newItems).distinctBy { it.rpid },
                    isLoading = false,
                    page = page,
                    isEnd = isEnd,
                    error = null
                )
            }.onFailure {
                _subReplyState.value = _subReplyState.value.copy(
                    isLoading = false,
                    error = it.message
                )
            }
        }
    }

    // --- 核心优化: 清晰度切换 ---
    fun changeQuality(qualityId: Int, currentPos: Long) {
        val currentState = _uiState.value
        if (currentState is PlayerUiState.Success) {
            viewModelScope.launch {
                try {
                    fetchAndPlay(
                        currentBvid, currentCid, qualityId,
                        currentState, currentPos
                    )
                } catch (e: Exception) {
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
        // 🔥 确保 VideoRepository.getPlayUrlData 已经接收 qn 参数
        val playUrlData = VideoRepository.getPlayUrlData(bvid, cid, qn)

        val url = playUrlData?.durl?.firstOrNull()?.url ?: ""
        val qualities = playUrlData?.accept_quality ?: emptyList()
        val labels = playUrlData?.accept_description ?: emptyList()
        val realQuality = playUrlData?.quality ?: qn

        if (url.isNotEmpty()) {
            // 修改 2] 传入 forceReset = true，强制 ExoPlayer 刷新
            playVideo(url, startPos, forceReset = true)

            _uiState.value = currentState.copy(
                playUrl = url,
                currentQuality = realQuality,
                qualityIds = qualities,
                qualityLabels = labels,
                startPosition = startPos
            )

            // 提示用户实际切换结果
            val targetLabel = labels.getOrNull(qualities.indexOf(qn)) ?: "$qn"
            val realLabel = labels.getOrNull(qualities.indexOf(realQuality)) ?: "$realQuality"

            if (realQuality != qn) {
                _toastEvent.send("尝试切换至 $targetLabel 失败，已降级至 $realLabel (可能需要登录)")
            } else {
                _toastEvent.send("已切换至 $realLabel")
            }
        } else {
            _toastEvent.send("该清晰度无法播放")
        }
    }
}