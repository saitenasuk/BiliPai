// 文件路径: feature/bangumi/BangumiViewModel.kt
package com.android.purebilibili.feature.bangumi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.data.model.response.*
import com.android.purebilibili.data.repository.BangumiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 番剧列表 UI 状态
 */
sealed class BangumiListState {
    object Loading : BangumiListState()
    data class Success(
        val items: List<BangumiItem>,
        val hasMore: Boolean = true
    ) : BangumiListState()
    data class Error(val message: String) : BangumiListState()
}

/**
 * 番剧详情 UI 状态
 */
sealed class BangumiDetailState {
    object Loading : BangumiDetailState()
    data class Success(val detail: BangumiDetail) : BangumiDetailState()
    data class Error(val message: String) : BangumiDetailState()
}

/**
 * 时间表 UI 状态
 */
sealed class TimelineState {
    object Loading : TimelineState()
    data class Success(val days: List<TimelineDay>) : TimelineState()
    data class Error(val message: String) : TimelineState()
}

/**
 * 番剧/影视 ViewModel
 */
class BangumiViewModel : ViewModel() {
    
    // 当前选中的类型 (1=番剧 2=电影 3=纪录片 4=国创 5=电视剧 7=综艺)
    private val _selectedType = MutableStateFlow(1)
    val selectedType: StateFlow<Int> = _selectedType.asStateFlow()
    
    // 番剧列表状态
    private val _listState = MutableStateFlow<BangumiListState>(BangumiListState.Loading)
    val listState: StateFlow<BangumiListState> = _listState.asStateFlow()
    
    // 时间表状态
    private val _timelineState = MutableStateFlow<TimelineState>(TimelineState.Loading)
    val timelineState: StateFlow<TimelineState> = _timelineState.asStateFlow()
    
    // 番剧详情状态
    private val _detailState = MutableStateFlow<BangumiDetailState>(BangumiDetailState.Loading)
    val detailState: StateFlow<BangumiDetailState> = _detailState.asStateFlow()
    
    // 分页
    private var currentPage = 1
    private var isLoadingMore = false
    
    init {
        loadBangumiList()
    }
    
    /**
     * 切换番剧类型
     */
    fun selectType(type: Int) {
        if (_selectedType.value != type) {
            _selectedType.value = type
            currentPage = 1
            loadBangumiList()
        }
    }
    
    /**
     * 加载番剧列表
     */
    fun loadBangumiList() {
        viewModelScope.launch {
            _listState.value = BangumiListState.Loading
            currentPage = 1
            
            BangumiRepository.getBangumiIndex(
                seasonType = _selectedType.value,
                page = currentPage
            ).fold(
                onSuccess = { data ->
                    _listState.value = BangumiListState.Success(
                        items = data.list ?: emptyList(),
                        hasMore = data.hasNext == 1
                    )
                },
                onFailure = { error ->
                    _listState.value = BangumiListState.Error(error.message ?: "加载失败")
                }
            )
        }
    }
    
    /**
     * 加载更多
     */
    fun loadMore() {
        if (isLoadingMore) return
        val currentState = _listState.value
        if (currentState !is BangumiListState.Success || !currentState.hasMore) return
        
        isLoadingMore = true
        viewModelScope.launch {
            currentPage++
            
            BangumiRepository.getBangumiIndex(
                seasonType = _selectedType.value,
                page = currentPage
            ).fold(
                onSuccess = { data ->
                    val newItems = currentState.items + (data.list ?: emptyList())
                    _listState.value = BangumiListState.Success(
                        items = newItems,
                        hasMore = data.hasNext == 1
                    )
                },
                onFailure = {
                    currentPage--
                }
            )
            isLoadingMore = false
        }
    }
    
    /**
     * 加载时间表
     */
    fun loadTimeline(type: Int = 1) {
        viewModelScope.launch {
            _timelineState.value = TimelineState.Loading
            
            BangumiRepository.getTimeline(type).fold(
                onSuccess = { days ->
                    _timelineState.value = TimelineState.Success(days)
                },
                onFailure = { error ->
                    _timelineState.value = TimelineState.Error(error.message ?: "加载失败")
                }
            )
        }
    }
    
    /**
     * 加载番剧详情
     */
    fun loadSeasonDetail(seasonId: Long) {
        viewModelScope.launch {
            _detailState.value = BangumiDetailState.Loading
            
            BangumiRepository.getSeasonDetail(seasonId).fold(
                onSuccess = { detail ->
                    _detailState.value = BangumiDetailState.Success(detail)
                },
                onFailure = { error ->
                    _detailState.value = BangumiDetailState.Error(error.message ?: "加载失败")
                }
            )
        }
    }
    
    /**
     * 追番/取消追番
     */
    fun toggleFollow(seasonId: Long, isFollowing: Boolean) {
        viewModelScope.launch {
            val result = if (isFollowing) {
                BangumiRepository.unfollowBangumi(seasonId)
            } else {
                BangumiRepository.followBangumi(seasonId)
            }
            
            result.fold(
                onSuccess = {
                    // 重新加载详情以更新状态
                    loadSeasonDetail(seasonId)
                },
                onFailure = { error ->
                    android.util.Log.e("BangumiVM", "Toggle follow failed: ${error.message}")
                }
            )
        }
    }
}
