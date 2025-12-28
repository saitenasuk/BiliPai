// 文件路径: feature/dynamic/DynamicViewModel.kt
package com.android.purebilibili.feature.dynamic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.FollowedLiveRoom
import com.android.purebilibili.data.repository.DynamicRepository
import com.android.purebilibili.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 🔥 动态页面 ViewModel
 * 支持：动态列表、侧边栏关注用户、在线状态
 */
class DynamicViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(DynamicUiState())
    val uiState: StateFlow<DynamicUiState> = _uiState.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    // 🔥 侧边栏相关状态
    private val _followedUsers = MutableStateFlow<List<SidebarUser>>(emptyList())
    val followedUsers: StateFlow<List<SidebarUser>> = _followedUsers.asStateFlow()
    
    private val _selectedUserId = MutableStateFlow<Long?>(null)
    val selectedUserId: StateFlow<Long?> = _selectedUserId.asStateFlow()
    
    private val _isSidebarExpanded = MutableStateFlow(true)
    val isSidebarExpanded: StateFlow<Boolean> = _isSidebarExpanded.asStateFlow()
    
    init {
        loadDynamicFeed(refresh = true)
        loadFollowedUsers()
    }
    
    /**
     * 🔥 加载关注用户列表及其直播状态
     */
    fun loadFollowedUsers() {
        viewModelScope.launch {
            // 获取关注的直播用户（有 liveStatus 字段）
            com.android.purebilibili.data.repository.LiveRepository.getFollowedLive(page = 1).onSuccess { liveRooms ->
                // 提取所有关注用户信息
                val users = extractUsersFromDynamics() + extractUsersFromLive(liveRooms)
                // 🔥🔥 [修复] 过滤无效用户数据，避免真机崩溃
                _followedUsers.value = users
                    .filter { it.uid > 0 && it.name.isNotBlank() }
                    .distinctBy { it.uid }
            }
        }
    }
    
    /**
     * 从动态列表提取用户
     */
    private fun extractUsersFromDynamics(): List<SidebarUser> {
        return _uiState.value.items
            .mapNotNull { it.modules.module_author }
            .map { author ->
                SidebarUser(
                    uid = author.mid,
                    name = author.name,
                    face = author.face,
                    isLive = false
                )
            }
    }
    
    /**
     * 从直播列表提取用户（包含在线状态）
     */
    private fun extractUsersFromLive(rooms: List<com.android.purebilibili.data.model.response.LiveRoom>): List<SidebarUser> {
        return rooms.map { room ->
            SidebarUser(
                uid = room.uid,
                name = room.uname,
                face = room.face,
                isLive = true  // 直播中
            )
        }
    }
    
    /**
     * 选择用户过滤动态
     */
    fun selectUser(uid: Long?) {
        _selectedUserId.value = uid
    }
    
    /**
     * 切换侧边栏展开/收起
     */
    fun toggleSidebar() {
        _isSidebarExpanded.value = !_isSidebarExpanded.value
    }
    
    /**
     * 加载动态列表
     */
    fun loadDynamicFeed(refresh: Boolean = false) {
        if (_uiState.value.isLoading && !refresh) return
        
        viewModelScope.launch {
            if (refresh) {
                _isRefreshing.value = true
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
            
            val result = DynamicRepository.getDynamicFeed(refresh)
            
            result.fold(
                onSuccess = { items ->
                    val currentItems = if (refresh) emptyList() else _uiState.value.items
                    _uiState.value = _uiState.value.copy(
                        items = currentItems + items,
                        isLoading = false,
                        error = null,
                        hasMore = DynamicRepository.hasMoreData()
                    )
                    // 刷新后更新关注用户列表
                    if (refresh) loadFollowedUsers()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "加载失败"
                    )
                }
            )
            
            _isRefreshing.value = false
        }
    }
    
    fun refresh() {
        loadDynamicFeed(refresh = true)
    }
    
    fun loadMore() {
        if (!_uiState.value.hasMore || _uiState.value.isLoading) return
        loadDynamicFeed(refresh = false)
    }
}

/**
 * 🔥 侧边栏用户数据
 */
data class SidebarUser(
    val uid: Long,
    val name: String,
    val face: String,
    val isLive: Boolean = false
)

/**
 * 动态页面 UI 状态
 */
data class DynamicUiState(
    val items: List<DynamicItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true
)
