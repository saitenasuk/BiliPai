// 文件路径: feature/live/LivePlayerViewModel.kt
package com.android.purebilibili.feature.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.data.model.response.LiveQuality
import com.android.purebilibili.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 直播播放器 UI 状态
 */
sealed class LivePlayerState {
    object Loading : LivePlayerState()
    
    data class Success(
        val playUrl: String,
        val currentQuality: Int,
        val qualityList: List<LiveQuality>
    ) : LivePlayerState()
    
    data class Error(
        val message: String
    ) : LivePlayerState()
}

/**
 * 直播播放器 ViewModel
 */
class LivePlayerViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow<LivePlayerState>(LivePlayerState.Loading)
    val uiState = _uiState.asStateFlow()
    
    private var currentRoomId: Long = 0
    
    /**
     * 加载直播流
     */
    fun loadLiveStream(roomId: Long, qn: Int = 10000) {
        currentRoomId = roomId
        
        viewModelScope.launch {
            _uiState.value = LivePlayerState.Loading
            
            val result = VideoRepository.getLivePlayUrlWithQuality(roomId, qn)
            
            result.onSuccess { data ->
                val url = extractPlayUrl(data)
                if (url != null) {
                    _uiState.value = LivePlayerState.Success(
                        playUrl = url,
                        currentQuality = data.current_quality,
                        qualityList = data.quality_description ?: emptyList()
                    )
                } else {
                    _uiState.value = LivePlayerState.Error("无法获取直播流地址")
                }
            }.onFailure { e ->
                _uiState.value = LivePlayerState.Error(e.message ?: "加载失败")
            }
        }
    }
    
    /**
     * 切换画质
     */
    fun changeQuality(qn: Int) {
        val currentState = _uiState.value as? LivePlayerState.Success ?: return
        
        viewModelScope.launch {
            val result = VideoRepository.getLivePlayUrlWithQuality(currentRoomId, qn)
            
            result.onSuccess { data ->
                val url = extractPlayUrl(data)
                if (url != null) {
                    _uiState.value = currentState.copy(
                        playUrl = url,
                        currentQuality = data.current_quality,
                        qualityList = data.quality_description ?: currentState.qualityList
                    )
                }
            }
        }
    }
    
    /**
     * 从响应数据中提取播放 URL
     */
    private fun extractPlayUrl(data: com.android.purebilibili.data.model.response.LivePlayUrlData): String? {
        // 尝试新 xlive API
        data.playurl_info?.playurl?.stream?.let { streams ->
            val stream = streams.find { it.protocolName == "http_hls" }
                ?: streams.find { it.protocolName == "http_stream" }
                ?: streams.firstOrNull()
            
            val format = stream?.format?.firstOrNull()
            val codec = format?.codec?.firstOrNull()
            val urlInfo = codec?.url_info?.firstOrNull()
            
            if (codec != null && urlInfo != null) {
                return urlInfo.host + codec.baseUrl + urlInfo.extra
            }
        }
        
        // 回退到旧 API
        return data.durl?.firstOrNull()?.url
    }
    
    /**
     * 重试
     */
    fun retry() {
        loadLiveStream(currentRoomId)
    }
}
