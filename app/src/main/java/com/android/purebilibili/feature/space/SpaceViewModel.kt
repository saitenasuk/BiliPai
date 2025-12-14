package com.android.purebilibili.feature.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiUtils
import com.android.purebilibili.data.model.response.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// UI 状态
sealed class SpaceUiState {
    object Loading : SpaceUiState()
    data class Success(
        val userInfo: SpaceUserInfo,
        val relationStat: RelationStatData? = null,
        val upStat: UpStatData? = null,
        val videos: List<SpaceVideoItem> = emptyList(),
        val totalVideos: Int = 0,
        val isLoadingMore: Boolean = false,
        val hasMoreVideos: Boolean = true
    ) : SpaceUiState()
    data class Error(val message: String) : SpaceUiState()
}

class SpaceViewModel : ViewModel() {
    
    private val spaceApi = NetworkModule.spaceApi
    
    private val _uiState = MutableStateFlow<SpaceUiState>(SpaceUiState.Loading)
    val uiState = _uiState.asStateFlow()
    
    private var currentMid: Long = 0
    private var currentPage = 1
    private val pageSize = 30
    
    // 🔥 缓存 WBI keys 避免重复请求
    private var cachedImgKey: String = ""
    private var cachedSubKey: String = ""
    
    fun loadSpaceInfo(mid: Long) {
        if (mid <= 0) return
        currentMid = mid
        currentPage = 1
        
        viewModelScope.launch {
            _uiState.value = SpaceUiState.Loading
            
            try {
                // 🔥 首先获取 WBI keys（只获取一次）
                val keys = fetchWbiKeys()
                if (keys == null) {
                    _uiState.value = SpaceUiState.Error("获取签名失败，请重试")
                    return@launch
                }
                cachedImgKey = keys.first
                cachedSubKey = keys.second
                
                // 并行请求用户信息、关注数、播放量统计
                val infoDeferred = async { fetchSpaceInfo(mid, cachedImgKey, cachedSubKey) }
                val relationDeferred = async { fetchRelationStat(mid) }
                val upStatDeferred = async { fetchUpStat(mid) }
                val videosDeferred = async { fetchSpaceVideos(mid, 1, cachedImgKey, cachedSubKey) }
                
                val userInfo = infoDeferred.await()
                val relationStat = relationDeferred.await()
                val upStat = upStatDeferred.await()
                val videosResult = videosDeferred.await()
                
                if (userInfo != null) {
                    _uiState.value = SpaceUiState.Success(
                        userInfo = userInfo,
                        relationStat = relationStat,
                        upStat = upStat,
                        videos = videosResult?.list?.vlist ?: emptyList(),
                        totalVideos = videosResult?.page?.count ?: 0,
                        hasMoreVideos = (videosResult?.list?.vlist?.size ?: 0) >= pageSize
                    )
                } else {
                    _uiState.value = SpaceUiState.Error("获取用户信息失败")
                }
            } catch (e: Exception) {
                android.util.Log.e("SpaceVM", "Error loading space: ${e.message}", e)
                _uiState.value = SpaceUiState.Error(e.message ?: "加载失败")
            }
        }
    }
    
    fun loadMoreVideos() {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        if (current.isLoadingMore || !current.hasMoreVideos) return
        
        viewModelScope.launch {
            _uiState.value = current.copy(isLoadingMore = true)
            
            try {
                val nextPage = currentPage + 1
                val result = fetchSpaceVideos(currentMid, nextPage, cachedImgKey, cachedSubKey)
                
                if (result != null) {
                    currentPage = nextPage
                    val newVideos = current.videos + (result.list.vlist)
                    _uiState.value = current.copy(
                        videos = newVideos,
                        isLoadingMore = false,
                        hasMoreVideos = result.list.vlist.size >= pageSize
                    )
                } else {
                    _uiState.value = current.copy(isLoadingMore = false)
                }
            } catch (e: Exception) {
                _uiState.value = current.copy(isLoadingMore = false)
            }
        }
    }
    
    // 🔥 获取 WBI 签名 keys
    private suspend fun fetchWbiKeys(): Pair<String, String>? {
        return try {
            val navResp = NetworkModule.api.getNavInfo()
            val wbiImg = navResp.data?.wbi_img ?: return null
            val imgKey = wbiImg.img_url.substringAfterLast("/").substringBefore(".")
            val subKey = wbiImg.sub_url.substringAfterLast("/").substringBefore(".")
            Pair(imgKey, subKey)
        } catch (e: Exception) {
            android.util.Log.e("SpaceVM", "fetchWbiKeys error: ${e.message}")
            null
        }
    }
    
    private suspend fun fetchSpaceInfo(mid: Long, imgKey: String, subKey: String): SpaceUserInfo? {
        return try {
            val params = WbiUtils.sign(mapOf("mid" to mid.toString()), imgKey, subKey)
            com.android.purebilibili.core.util.Logger.d("SpaceVM", "🔍 fetchSpaceInfo params: $params")
            val response = spaceApi.getSpaceInfo(params)
            com.android.purebilibili.core.util.Logger.d("SpaceVM", "📦 fetchSpaceInfo response: code=${response.code}, message=${response.message}")
            if (response.code == 0) response.data else {
                android.util.Log.e("SpaceVM", "❌ fetchSpaceInfo failed: code=${response.code}, message=${response.message}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("SpaceVM", "fetchSpaceInfo error: ${e.message}", e)
            null
        }
    }
    
    private suspend fun fetchRelationStat(mid: Long): RelationStatData? {
        return try {
            val response = spaceApi.getRelationStat(mid)
            if (response.code == 0) response.data else null
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun fetchUpStat(mid: Long): UpStatData? {
        return try {
            val response = spaceApi.getUpStat(mid)
            if (response.code == 0) response.data else null
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun fetchSpaceVideos(mid: Long, page: Int, imgKey: String, subKey: String): SpaceVideoData? {
        return try {
            val params = WbiUtils.sign(mapOf(
                "mid" to mid.toString(),
                "pn" to page.toString(),
                "ps" to pageSize.toString(),
                "order" to "pubdate"  // 按发布时间排序
            ), imgKey, subKey)
            val response = spaceApi.getSpaceVideos(params)
            if (response.code == 0) response.data else null
        } catch (e: Exception) {
            android.util.Log.e("SpaceVM", "fetchSpaceVideos error: ${e.message}")
            null
        }
    }
}

