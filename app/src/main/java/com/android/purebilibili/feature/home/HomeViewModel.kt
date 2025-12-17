// 文件路径: feature/home/HomeViewModel.kt
package com.android.purebilibili.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.data.repository.VideoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 状态类已移至 HomeUiState.kt

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
                
                // 🔥 获取关注列表（异步，不阻塞主流程）
                fetchFollowingList(navData.mid)
            } else {
                com.android.purebilibili.core.store.TokenManager.isVipCache = false
                com.android.purebilibili.core.store.TokenManager.midCache = null
                _uiState.value = _uiState.value.copy(
                    user = UserState(isLogin = false),
                    followingMids = emptySet()
                )
            }
        }
    }
    
    // 🔥 获取关注列表（并行分页获取，支持更多关注，带本地缓存）
    private suspend fun fetchFollowingList(mid: Long) {
        val context = getApplication<android.app.Application>()
        val prefs = context.getSharedPreferences("following_cache", android.content.Context.MODE_PRIVATE)
        val cacheKey = "following_mids_$mid"
        val cacheTimeKey = "following_time_$mid"
        
        // 🔥 检查缓存（1小时内有效）
        val cachedTime = prefs.getLong(cacheTimeKey, 0)
        val cacheValidDuration = 60 * 60 * 1000L  // 1小时
        if (System.currentTimeMillis() - cachedTime < cacheValidDuration) {
            val cachedMids = prefs.getStringSet(cacheKey, null)
            if (!cachedMids.isNullOrEmpty()) {
                val mids = cachedMids.mapNotNull { it.toLongOrNull() }.toSet()
                _uiState.value = _uiState.value.copy(followingMids = mids)
                com.android.purebilibili.core.util.Logger.d("HomeVM", "📋 Loaded ${mids.size} following mids from cache")
                return
            }
        }
        
        // 🔥 动态获取所有关注列表（无上限）
        try {
            val allMids = mutableSetOf<Long>()
            
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                var page = 1
                while (true) {  // 🔥 无限循环，直到获取完所有关注
                    try {
                        val result = com.android.purebilibili.core.network.NetworkModule.api.getFollowings(mid, page, 50)
                        if (result.code == 0 && result.data != null) {
                            val list = result.data.list ?: break
                            if (list.isEmpty()) break
                            
                            list.forEach { user -> allMids.add(user.mid) }
                            
                            // 如果这一页不满50，说明已经获取完所有关注
                            if (list.size < 50) {
                                com.android.purebilibili.core.util.Logger.d("HomeVM", "📋 Reached end at page $page, total: ${allMids.size}")
                                break
                            }
                            page++
                        } else {
                            break
                        }
                    } catch (e: Exception) {
                        com.android.purebilibili.core.util.Logger.e("HomeVM", "📋 Error at page $page", e)
                        break
                    }
                }
            }
            
            // 🔥 保存到本地缓存
            prefs.edit()
                .putStringSet(cacheKey, allMids.map { it.toString() }.toSet())
                .putLong(cacheTimeKey, System.currentTimeMillis())
                .apply()
            
            _uiState.value = _uiState.value.copy(followingMids = allMids.toSet())
            com.android.purebilibili.core.util.Logger.d("HomeVM", "📋 Total following mids fetched and cached: ${allMids.size}")
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.e("HomeVM", "📋 Error fetching following list", e)
        }
    }
}