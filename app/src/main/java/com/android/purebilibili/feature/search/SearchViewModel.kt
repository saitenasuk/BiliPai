package com.android.purebilibili.feature.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.database.AppDatabase
import com.android.purebilibili.core.database.entity.SearchHistory
import com.android.purebilibili.data.model.response.HotItem
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.model.response.SearchUpItem
import com.android.purebilibili.data.model.response.SearchType
import com.android.purebilibili.data.repository.SearchRepository
import com.android.purebilibili.data.repository.SearchOrder
import com.android.purebilibili.data.repository.SearchDuration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val showResults: Boolean = false,
    // 🔥 搜索类型
    val searchType: SearchType = SearchType.VIDEO,
    // 视频结果
    val searchResults: List<VideoItem> = emptyList(),
    // 🔥 UP主 结果
    val upResults: List<SearchUpItem> = emptyList(),
    val hotList: List<HotItem> = emptyList(),
    val historyList: List<SearchHistory> = emptyList(),
    // 🔥 搜索建议
    val suggestions: List<String> = emptyList(),
    // 🔥 搜索发现 / 猜你想搜
    val discoverList: List<String> = listOf("黑神话悟空", "原神", "初音未来", "JOJO", "罗翔说刑法", "何同学", "毕业季", "猫咪", "我的世界", "战鹰"),
    val discoverTitle: String = "搜索发现",
    val error: String? = null,
    // 🔥 搜索过滤条件
    val searchOrder: SearchOrder = SearchOrder.TOTALRANK,
    val searchDuration: SearchDuration = SearchDuration.ALL
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private val searchDao = AppDatabase.getDatabase(application).searchHistoryDao()
    
    // 🔥 防抖任务
    private var suggestJob: Job? = null

    init {
        loadHotSearch()
        loadHistory()
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        if (newQuery.isEmpty()) {
            _uiState.update { it.copy(showResults = false, suggestions = emptyList(), error = null) }
        } else {
            // 🔥 触发搜索建议（防抖 300ms）
            loadSuggestions(newQuery)
        }
    }
    
    // 🔥 防抖加载搜索建议
    private fun loadSuggestions(keyword: String) {
        suggestJob?.cancel()
        suggestJob = viewModelScope.launch {
            delay(300) // 防抖 300ms
            val result = SearchRepository.getSuggest(keyword)
            result.onSuccess { suggestions ->
                _uiState.update { it.copy(suggestions = suggestions.take(8)) }
            }
        }
    }
    
    // 🔥 切换搜索类型
    fun setSearchType(type: SearchType) {
        _uiState.update { it.copy(searchType = type) }
        // 如果有查询内容，重新搜索
        if (_uiState.value.query.isNotBlank()) {
            search(_uiState.value.query)
        }
    }
    
    // 🔥 设置搜索排序
    fun setSearchOrder(order: SearchOrder) {
        _uiState.update { it.copy(searchOrder = order) }
        if (_uiState.value.query.isNotBlank() && _uiState.value.showResults) {
            search(_uiState.value.query)
        }
    }
    
    // 🔥 设置时长筛选
    fun setSearchDuration(duration: SearchDuration) {
        _uiState.update { it.copy(searchDuration = duration) }
        if (_uiState.value.query.isNotBlank() && _uiState.value.showResults) {
            search(_uiState.value.query)
        }
    }

    fun search(keyword: String) {
        if (keyword.isBlank()) return

        // 🔥 清空建议列表
        _uiState.update { it.copy(query = keyword, isSearching = true, showResults = true, suggestions = emptyList(), error = null) }
        saveHistory(keyword)
        
        // 📊 记录搜索事件
        com.android.purebilibili.core.util.AnalyticsHelper.logSearch(keyword)

        viewModelScope.launch {
            val searchType = _uiState.value.searchType
            
            when (searchType) {
                SearchType.VIDEO -> {
                    val order = _uiState.value.searchOrder
                    val duration = _uiState.value.searchDuration
                    val result = SearchRepository.search(keyword, order, duration)
                    result.onSuccess { videos ->
                        _uiState.update { it.copy(isSearching = false, searchResults = videos, upResults = emptyList()) }
                    }.onFailure { e ->
                        _uiState.update { it.copy(isSearching = false, error = e.message ?: "搜索失败") }
                    }
                }
                SearchType.UP -> {
                    val result = SearchRepository.searchUp(keyword)
                    result.onSuccess { ups ->
                        _uiState.update { it.copy(isSearching = false, upResults = ups, searchResults = emptyList()) }
                    }.onFailure { e ->
                        _uiState.update { it.copy(isSearching = false, error = e.message ?: "搜索失败") }
                    }
                }
                else -> {
                    // 其他类型暂未实现
                    _uiState.update { it.copy(isSearching = false, error = "该搜索类型暂未支持") }
                }
            }
        }
    }

    private fun loadHotSearch() {
        viewModelScope.launch {
            val result = SearchRepository.getHotSearch()
            result.onSuccess { items ->
                _uiState.update { it.copy(hotList = items) }
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            searchDao.getAll().collect { history ->
                _uiState.update { it.copy(historyList = history) }
                // 🔥 更新搜索发现
                updateDiscover(history)
            }
        }
    }

    // 🔥 生成个性化发现内容
    private fun updateDiscover(history: List<SearchHistory>) {
        viewModelScope.launch {
            val historyKeywords = history.map { it.keyword }
            // 使用 Repository 获取 (包含个性化逻辑 + 官方热搜兜底)
            val result = SearchRepository.getSearchDiscover(historyKeywords)
            
            result.onSuccess { (title, list) ->
                _uiState.update { 
                    it.copy(
                        discoverTitle = title,
                        discoverList = list
                    )
                }
            }
        }
    }

    private fun saveHistory(keyword: String) {
        viewModelScope.launch {
            // 🔥 使用 keyword 主键，重复搜索自动更新时间戳
            searchDao.insert(SearchHistory(keyword = keyword, timestamp = System.currentTimeMillis()))
        }
    }

    fun deleteHistory(history: SearchHistory) {
        viewModelScope.launch {
            searchDao.delete(history)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            searchDao.clearAll()
        }
    }
}