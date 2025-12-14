// 文件路径: feature/home/HomeViewModel.kt
package com.android.purebilibili.feature.home

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.model.response.LiveRoom
import com.android.purebilibili.data.repository.VideoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 🚀 性能优化：@Immutable 告诉 Compose 此类不可变，减少不必要的重组
@Immutable
data class UserState(
    val isLogin: Boolean = false,
    val face: String = "",
    val name: String = "",
    val mid: Long = 0,
    val level: Int = 0,
    val coin: Double = 0.0,
    val bcoin: Double = 0.0,
    val following: Int = 0,
    val follower: Int = 0,
    val dynamic: Int = 0,
    val isVip: Boolean = false,
    val vipLabel: String = ""
)

// 🔥🔥 [新增] 首页分类枚举（含 Bilibili 分区 ID）
enum class HomeCategory(val label: String, val tid: Int = 0) {
    RECOMMEND("推荐", 0),
    POPULAR("热门", 0),
    LIVE("直播", 0),
    ANIME("追番", 13),     // 番剧分区
    MOVIE("影视", 181),    // 影视分区
    // 🔥 新增分类
    GAME("游戏", 4),       // 游戏分区
    KNOWLEDGE("知识", 36), // 知识分区
    TECH("科技", 188)      // 科技分区
}

// 🔥🔥 [新增] 直播子分类
enum class LiveSubCategory(val label: String) {
    FOLLOWED("关注"),
    POPULAR("热门")
}

// 🚀 性能优化：@Stable 告诉 Compose 此类字段变化可被追踪，优化重组
@Stable
data class HomeUiState(
    val videos: List<VideoItem> = emptyList(),
    val liveRooms: List<LiveRoom> = emptyList(),  // 🔥 直播列表
    val isLoading: Boolean = false,
    val error: String? = null,
    val user: UserState = UserState(),
    val currentCategory: HomeCategory = HomeCategory.RECOMMEND,  // 🔥 当前分类
    val liveSubCategory: LiveSubCategory = LiveSubCategory.FOLLOWED,  // 🔥 直播子分类
    val refreshKey: Long = 0L  // 🔥 刷新标识符，用于强制重置动画
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private var refreshIdx = 0
    private var popularPage = 1  // 🔥 热门视频分页
    private var livePage = 1     // 🔥 直播分页
    private var hasMoreLiveData = true  // 🔥 是否还有更多直播数据

    init {
        loadData()
    }

    // 🔥🔥 [新增] 切换分类
    fun switchCategory(category: HomeCategory) {
        if (_uiState.value.currentCategory == category) return
        viewModelScope.launch {
            // 🔥🔥 [修复] 如果切换到直播分类，未登录用户默认显示热门
            val liveSubCategory = if (category == HomeCategory.LIVE) {
                val isLoggedIn = !com.android.purebilibili.core.store.TokenManager.sessDataCache.isNullOrEmpty()
                if (isLoggedIn) _uiState.value.liveSubCategory else LiveSubCategory.POPULAR
            } else {
                _uiState.value.liveSubCategory
            }
            
            _uiState.value = _uiState.value.copy(
                currentCategory = category,
                liveSubCategory = liveSubCategory,
                videos = emptyList(),
                liveRooms = emptyList(),  // 🔥 清空直播列表
                isLoading = true,
                error = null
            )
            refreshIdx = 0
            popularPage = 1
            livePage = 1
            hasMoreLiveData = true  // 🔥 重置分页标志
            fetchData(isLoadMore = false)
        }
    }
    
    // 🔥🔥 [新增] 切换直播子分类
    fun switchLiveSubCategory(subCategory: LiveSubCategory) {
        if (_uiState.value.liveSubCategory == subCategory) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                liveSubCategory = subCategory,
                liveRooms = emptyList(),
                isLoading = true,
                error = null
            )
            livePage = 1
            hasMoreLiveData = true  // 🔥 修复：切换分类时重置分页标志
            fetchLiveRooms(isLoadMore = false)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            fetchData(isLoadMore = false)
        }
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            refreshIdx = 0
            popularPage = 1
            livePage = 1  // 🔥 修复：刷新时也要重置直播分页
            hasMoreLiveData = true  // 🔥 修复：刷新时重置分页标志
            fetchData(isLoadMore = false)
            // 🔥 数据加载完成后再更新 refreshKey，避免闪烁
            _uiState.value = _uiState.value.copy(refreshKey = System.currentTimeMillis())
            _isRefreshing.value = false
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoading || _isRefreshing.value) return
        
        // 🔥 修复：如果是直播分类且没有更多数据，不再加载
        if (_uiState.value.currentCategory == HomeCategory.LIVE && !hasMoreLiveData) {
            com.android.purebilibili.core.util.Logger.d("HomeVM", "🔴 No more live data, skipping loadMore")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // 🔥 修复：先增加页码再获取数据（确保请求下一页）
            refreshIdx++
            popularPage++
            livePage++
            fetchData(isLoadMore = true)
        }
    }

    private suspend fun fetchData(isLoadMore: Boolean) {
        val currentCategory = _uiState.value.currentCategory
        
        // 🔥 直播分类单独处理
        if (currentCategory == HomeCategory.LIVE) {
            fetchLiveRooms(isLoadMore)
            return
        }
        
        // 🔥 视频类分类处理
        val videoResult = when (currentCategory) {
            HomeCategory.RECOMMEND -> VideoRepository.getHomeVideos(refreshIdx)
            HomeCategory.POPULAR -> VideoRepository.getPopularVideos(popularPage)
            else -> {
                // 🔥🔥 [修复] 未实现的分类显示错误，但保留 previousCategory 供返回使用
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "该分类暂未实现"
                )
                return
            }
        }
        
        // 仅在首次加载或刷新时获取用户信息
        if (!isLoadMore) {
            fetchUserInfo()
        }

        if (isLoadMore) delay(100)

        videoResult.onSuccess { videos ->
            val validVideos = videos.filter { it.bvid.isNotEmpty() && it.title.isNotEmpty() }
            if (validVideos.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    videos = if (isLoadMore) _uiState.value.videos + validVideos else validVideos,
                    liveRooms = emptyList(),  // 清空直播列表
                    isLoading = false,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (!isLoadMore && _uiState.value.videos.isEmpty()) "没有更多内容了" else null
                )
            }
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = if (!isLoadMore && _uiState.value.videos.isEmpty()) error.message ?: "网络错误" else null
            )
        }
    }
    
    // 🔥🔥 [新增] 获取直播间列表（支持关注/热门切换）
    private suspend fun fetchLiveRooms(isLoadMore: Boolean) {
        val page = if (isLoadMore) livePage else 1
        val subCategory = _uiState.value.liveSubCategory
        
        com.android.purebilibili.core.util.Logger.d("HomeVM", "🔴 fetchLiveRooms: isLoadMore=$isLoadMore, page=$page, livePage=$livePage, subCategory=$subCategory")
        
        // 🔥 根据子分类选择不同的 API
        val result = when (subCategory) {
            LiveSubCategory.FOLLOWED -> VideoRepository.getFollowedLive(page)
            LiveSubCategory.POPULAR -> VideoRepository.getLiveRooms(page)
        }
        
        if (!isLoadMore) fetchUserInfo()
        if (isLoadMore) delay(100)
        
        result.onSuccess { rooms ->
            com.android.purebilibili.core.util.Logger.d("HomeVM", "🔴 Fetched ${rooms.size} rooms for page $page")
            
            if (rooms.isNotEmpty()) {
                // 🔥 修复：过滤重复的直播间
                val existingRoomIds = _uiState.value.liveRooms.map { it.roomid }.toSet()
                val newRooms = if (isLoadMore) {
                    rooms.filter { it.roomid !in existingRoomIds }
                } else {
                    rooms
                }
                
                com.android.purebilibili.core.util.Logger.d("HomeVM", "🔴 New unique rooms: ${newRooms.size}")
                
                // 🔥 关键修复：如果没有新的唯一房间，标记为无更多数据
                if (isLoadMore && newRooms.isEmpty()) {
                    hasMoreLiveData = false
                    com.android.purebilibili.core.util.Logger.d("HomeVM", "🔴 No more unique live data, stopping pagination")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@onSuccess
                }
                
                _uiState.value = _uiState.value.copy(
                    liveRooms = if (isLoadMore) _uiState.value.liveRooms + newRooms else rooms,
                    videos = emptyList(),  // 清空视频列表
                    isLoading = false,
                    error = null
                )
            } else {
                // 🔥 没有更多数据时，不再触发加载更多
                val message = when (subCategory) {
                    LiveSubCategory.FOLLOWED -> "暂无关注的主播在直播"
                    LiveSubCategory.POPULAR -> "没有直播"
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (!isLoadMore && _uiState.value.liveRooms.isEmpty()) message else null
                )
            }
        }.onFailure { e ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = if (!isLoadMore && _uiState.value.liveRooms.isEmpty()) e.message ?: "网络错误" else null
            )
        }
    }
    
    // 🔥 提取用户信息获取逻辑
    private suspend fun fetchUserInfo() {
        val navResult = VideoRepository.getNavInfo()
        navResult.onSuccess { navData ->
            if (navData.isLogin) {
                val isVip = navData.vip.status == 1
                com.android.purebilibili.core.store.TokenManager.isVipCache = isVip
                com.android.purebilibili.core.store.TokenManager.midCache = navData.mid
                _uiState.value = _uiState.value.copy(
                    user = UserState(
                        isLogin = true,
                        face = navData.face,
                        name = navData.uname,
                        mid = navData.mid,
                        level = navData.level_info.current_level,
                        coin = navData.money,
                        bcoin = navData.wallet.bcoin_balance,
                        isVip = isVip
                    )
                )
            } else {
                com.android.purebilibili.core.store.TokenManager.isVipCache = false
                com.android.purebilibili.core.store.TokenManager.midCache = null
                _uiState.value = _uiState.value.copy(user = UserState(isLogin = false))
            }
        }
    }
}