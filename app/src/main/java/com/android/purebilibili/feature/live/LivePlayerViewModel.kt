// 文件路径: feature/live/LivePlayerViewModel.kt
package com.android.purebilibili.feature.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.LiveQuality
import com.android.purebilibili.data.repository.LiveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 主播信息
 */
data class AnchorInfo(
    val uid: Long = 0,
    val uname: String = "",
    val face: String = "",
    val followers: Long = 0,
    val officialTitle: String = ""
)

/**
 * 直播间信息
 */
data class RoomInfo(
    val roomId: Long = 0,
    val title: String = "",
    val cover: String = "",
    val areaName: String = "",
    val parentAreaName: String = "",
    val online: Int = 0,
    val liveStatus: Int = 0,
    val liveStartTime: Long = 0,
    val description: String = "",
    val tags: String = ""
)

/**
 * 直播播放器 UI 状态
 */
sealed class LivePlayerState {
    object Loading : LivePlayerState()
    
    data class Success(
        val playUrl: String,
        val allPlayUrls: List<String> = emptyList(),  // 🔥🔥 [新增] 所有可用的 CDN URL（用于故障转移）
        val currentUrlIndex: Int = 0,  // 🔥🔥 [新增] 当前使用的 URL 索引
        val currentQuality: Int,
        val qualityList: List<LiveQuality>,
        val roomInfo: RoomInfo = RoomInfo(),
        val anchorInfo: AnchorInfo = AnchorInfo(),
        val isFollowing: Boolean = false
    ) : LivePlayerState()
    
    data class Error(
        val message: String
    ) : LivePlayerState()
}

/**
 * 直播播放器 ViewModel - 增强版
 */
class LivePlayerViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow<LivePlayerState>(LivePlayerState.Loading)
    val uiState = _uiState.asStateFlow()
    
    private var currentRoomId: Long = 0
    private var currentUid: Long = 0
    
    /**
     * 加载直播流和直播间详情
     */
    fun loadLiveStream(roomId: Long, qn: Int = 10000) {
        currentRoomId = roomId
        
        viewModelScope.launch {
            _uiState.value = LivePlayerState.Loading
            
            // 并行加载直播流和直播间详情
            val playUrlResult = LiveRepository.getLivePlayUrlWithQuality(roomId, qn)
            
            playUrlResult.onSuccess { data ->
                android.util.Log.d("LivePlayer", "🔴 === API Response Debug ===")
                android.util.Log.d("LivePlayer", "🔴 durl count: ${data.durl?.size ?: 0}")
                android.util.Log.d("LivePlayer", "🔴 quality_description: ${data.quality_description}")
                android.util.Log.d("LivePlayer", "🔴 current_quality: ${data.current_quality}")
                
                // 🔥🔥 [修复] 收集所有可用的 CDN URL
                val allUrls = data.durl?.mapNotNull { it.url } ?: emptyList()
                android.util.Log.d("LivePlayer", "🔴 All available URLs: ${allUrls.size}")
                allUrls.forEachIndexed { index, u ->
                    android.util.Log.d("LivePlayer", "🔴 URL[$index]: ${u.take(60)}...")
                }
                
                // 🔥🔥 [关键修复] 优先使用第二个 CDN（索引1），因为第一个 CDN 经常返回 403
                // 如果只有一个 URL，则使用第一个
                val preferredIndex = if (allUrls.size > 1) 1 else 0
                val url = allUrls.getOrNull(preferredIndex) ?: extractPlayUrl(data)
                
                android.util.Log.d("LivePlayer", "🔴 Selected URL (index=$preferredIndex): ${url?.take(100) ?: "NULL"}")
                
                if (url != null) {
                    val qualityList = data.quality_description?.takeIf { it.isNotEmpty() }
                        ?: data.playurl_info?.playurl?.gQnDesc
                        ?: emptyList()
                    
                    android.util.Log.d("LivePlayer", "🔴 Final qualityList: $qualityList (count: ${qualityList.size})")
                    
                    _uiState.value = LivePlayerState.Success(
                        playUrl = url,
                        allPlayUrls = allUrls,  // 🔥 保存所有 URL
                        currentUrlIndex = preferredIndex,
                        currentQuality = qn,  // 🔥🔥 [修复] 使用请求的 qn 值，而不是 API 返回的 current_quality
                        qualityList = qualityList
                    )
                    
                    // 异步加载直播间详情
                    loadRoomDetail(roomId)
                } else {
                    android.util.Log.e("LivePlayer", "❌ No playable URL found!")
                    _uiState.value = LivePlayerState.Error("无法获取直播流地址")
                }
            }.onFailure { e ->
                android.util.Log.e("LivePlayer", "❌ API call failed: ${e.message}", e)
                _uiState.value = LivePlayerState.Error(e.message ?: "加载失败")
            }
        }
    }
    
    /**
     * 加载直播间详情
     */
    private suspend fun loadRoomDetail(roomId: Long) {
        try {
            val api = NetworkModule.api
            val response = api.getLiveRoomDetail(roomId)
            
            if (response.code == 0 && response.data != null) {
                val roomData = response.data.roomInfo
                val anchorData = response.data.anchorInfo
                val watchedShow = response.data.watchedShow
                
                currentUid = roomData?.uid ?: 0
                
                val currentState = _uiState.value as? LivePlayerState.Success ?: return
                
                _uiState.value = currentState.copy(
                    roomInfo = RoomInfo(
                        roomId = roomData?.roomId ?: 0,
                        title = roomData?.title ?: "",
                        cover = roomData?.cover ?: "",
                        areaName = roomData?.areaName ?: "",
                        parentAreaName = roomData?.parentAreaName ?: "",
                        online = watchedShow?.num ?: roomData?.online ?: 0,
                        liveStatus = roomData?.liveStatus ?: 0,
                        liveStartTime = roomData?.liveStartTime ?: 0,
                        description = roomData?.description ?: "",
                        tags = roomData?.tags ?: ""
                    ),
                    anchorInfo = AnchorInfo(
                        uid = roomData?.uid ?: 0,
                        uname = anchorData?.baseInfo?.uname ?: "",
                        face = anchorData?.baseInfo?.face ?: "",
                        followers = anchorData?.relationInfo?.attention ?: 0,
                        officialTitle = anchorData?.baseInfo?.officialInfo?.title ?: ""
                    )
                )
                
                // 检查关注状态
                if (currentUid > 0) {
                    checkFollowStatus(currentUid)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 检查关注状态
     */
    private suspend fun checkFollowStatus(uid: Long) {
        try {
            val api = NetworkModule.api
            val response = api.getRelation(uid)
            
            if (response.code == 0 && response.data != null) {
                val currentState = _uiState.value as? LivePlayerState.Success ?: return
                _uiState.value = currentState.copy(
                    isFollowing = response.data.isFollowing
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 关注/取关主播
     */
    fun toggleFollow() {
        val currentState = _uiState.value as? LivePlayerState.Success ?: return
        if (currentUid <= 0) return
        
        viewModelScope.launch {
            try {
                val api = NetworkModule.api
                val csrf = TokenManager.csrfCache ?: return@launch
                
                val act = if (currentState.isFollowing) 2 else 1  // 2=取关, 1=关注
                val response = api.modifyRelation(currentUid, act, csrf)
                
                if (response.code == 0) {
                    _uiState.value = currentState.copy(
                        isFollowing = !currentState.isFollowing,
                        anchorInfo = currentState.anchorInfo.copy(
                            followers = if (currentState.isFollowing) {
                                currentState.anchorInfo.followers - 1
                            } else {
                                currentState.anchorInfo.followers + 1
                            }
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 切换画质
     */
    fun changeQuality(qn: Int) {
        val currentState = _uiState.value as? LivePlayerState.Success ?: return
        android.util.Log.d("LivePlayer", "🔴 changeQuality called: qn=$qn")
        
        viewModelScope.launch {
            val result = LiveRepository.getLivePlayUrlWithQuality(currentRoomId, qn)
            
            result.onSuccess { data ->
                android.util.Log.d("LivePlayer", "🔴 changeQuality success, durl count: ${data.durl?.size}")
                
                // 🔥🔥 [修复] 收集所有 URL 并优先使用备用 CDN
                val allUrls = data.durl?.mapNotNull { it.url } ?: emptyList()
                val preferredIndex = if (allUrls.size > 1) 1 else 0
                val url = allUrls.getOrNull(preferredIndex) ?: extractPlayUrl(data)
                
                android.util.Log.d("LivePlayer", "🔴 changeQuality selected URL: ${url?.take(80)}")
                
                if (url != null) {
                    val newQualityList = data.quality_description?.takeIf { it.isNotEmpty() }
                        ?: data.playurl_info?.playurl?.gQnDesc
                        ?: currentState.qualityList
                    
                    _uiState.value = currentState.copy(
                        playUrl = url,
                        allPlayUrls = allUrls,
                        currentUrlIndex = preferredIndex,
                        currentQuality = qn,  // 🔥🔥 [修复] 使用用户请求的 qn 值
                        qualityList = newQualityList
                    )
                } else {
                    android.util.Log.e("LivePlayer", "❌ changeQuality: No URL found")
                }
            }.onFailure { e ->
                android.util.Log.e("LivePlayer", "❌ changeQuality failed: ${e.message}")
            }
        }
    }
    
    /**
     * 🔥🔥 [新增] 尝试下一个 CDN URL（播放失败时调用）
     */
    fun tryNextUrl() {
        val currentState = _uiState.value as? LivePlayerState.Success ?: return
        
        val nextIndex = currentState.currentUrlIndex + 1
        if (nextIndex < currentState.allPlayUrls.size) {
            val nextUrl = currentState.allPlayUrls[nextIndex]
            android.util.Log.d("LivePlayer", "🔄 Trying next CDN URL (index=$nextIndex): ${nextUrl.take(80)}...")
            
            _uiState.value = currentState.copy(
                playUrl = nextUrl,
                currentUrlIndex = nextIndex
            )
        } else {
            android.util.Log.e("LivePlayer", "❌ No more CDN URLs to try (tried all ${currentState.allPlayUrls.size})")
            // 所有 URL 都失败了，显示错误
            _uiState.value = LivePlayerState.Error("所有 CDN 均无法连接，请稍后重试")
        }
    }
    
    /**
     * 从响应数据中提取播放 URL
     */
    private fun extractPlayUrl(data: com.android.purebilibili.data.model.response.LivePlayUrlData): String? {
        android.util.Log.d("LivePlayer", "🔴 === extractPlayUrl ===")
        
        // 尝试新 xlive API
        data.playurl_info?.playurl?.stream?.let { streams ->
            android.util.Log.d("LivePlayer", "🔴 Found ${streams.size} streams")
            streams.forEachIndexed { index, s ->
                android.util.Log.d("LivePlayer", "🔴 Stream[$index]: protocol=${s.protocolName}")
            }
            
            val stream = streams.find { it.protocolName == "http_hls" }
                ?: streams.find { it.protocolName == "http_stream" }
                ?: streams.firstOrNull()
            
            android.util.Log.d("LivePlayer", "🔴 Selected stream: ${stream?.protocolName}")
            
            val format = stream?.format?.firstOrNull()
            android.util.Log.d("LivePlayer", "🔴 Format: ${format?.formatName}")
            
            val codec = format?.codec?.firstOrNull()
            android.util.Log.d("LivePlayer", "🔴 Codec: ${codec?.codecName}, baseUrl=${codec?.baseUrl?.take(50)}")
            
            val urlInfo = codec?.url_info?.firstOrNull()
            android.util.Log.d("LivePlayer", "🔴 UrlInfo: host=${urlInfo?.host}, extra=${urlInfo?.extra?.take(30)}")
            
            if (codec != null && urlInfo != null) {
                val url = urlInfo.host + codec.baseUrl + urlInfo.extra
                android.util.Log.d("LivePlayer", "✅ Built URL from xlive API: ${url.take(100)}...")
                return url
            }
        }
        
        // 回退到旧 API
        android.util.Log.d("LivePlayer", "🔴 Trying durl fallback...")
        val durlUrl = data.durl?.firstOrNull()?.url
        if (durlUrl != null) {
            android.util.Log.d("LivePlayer", "✅ Using durl URL: ${durlUrl.take(100)}...")
            return durlUrl
        }
        
        android.util.Log.e("LivePlayer", "❌ No URL found in any structure!")
        return null
    }
    
    /**
     * 重试
     */
    fun retry() {
        loadLiveStream(currentRoomId)
    }
}
