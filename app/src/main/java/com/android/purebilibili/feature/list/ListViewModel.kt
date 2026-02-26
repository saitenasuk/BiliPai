// 文件路径: feature/list/ListViewModel.kt
package com.android.purebilibili.feature.list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 通用的 UI 状态
data class ListUiState(
    val title: String = "",
    val items: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// 基类 ViewModel
abstract class BaseListViewModel(application: Application, private val pageTitle: String) : AndroidViewModel(application) {
    protected val _uiState = MutableStateFlow(ListUiState(title = pageTitle, isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val items = fetchItems()
                _uiState.value = _uiState.value.copy(isLoading = false, items = items)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "加载失败")
            }
        }
    }

    // 子类必须实现此方法来提供数据
    abstract suspend fun fetchItems(): List<VideoItem>
}

// --- 历史记录 ViewModel (支持游标分页加载) ---
class HistoryViewModel(application: Application) : BaseListViewModel(application, "历史记录") {
    
    private val progressManager by lazy {
        com.android.purebilibili.feature.video.controller.PlaybackProgressManager.getInstance(
            getApplication<Application>()
        )
    }
    
    // 游标分页状态
    private var cursorMax: Long = 0
    private var cursorViewAt: Long = 0
    private var cursorBusiness: String = ""
    private var hasMore = true
    private var isLoadingMore = false
    
    //  暴露加载更多状态
    private val _isLoadingMoreState = MutableStateFlow(false)
    val isLoadingMoreState = _isLoadingMoreState.asStateFlow()
    
    private val _hasMoreState = MutableStateFlow(true)
    val hasMoreState = _hasMoreState.asStateFlow()
    
    // [新增] 保存完整的历史记录项（包含导航信息）
    private val _historyItemsMap = mutableMapOf<String, com.android.purebilibili.data.model.response.HistoryItem>()
    private val _historyItemsByRenderKey = mutableMapOf<String, com.android.purebilibili.data.model.response.HistoryItem>()

    private val _dissolvingIds = MutableStateFlow<Set<String>>(emptySet())
    val dissolvingIds = _dissolvingIds.asStateFlow()
    
    /**
     * 根据 bvid 获取历史记录项的导航信息
     */
    fun getHistoryItem(bvid: String): com.android.purebilibili.data.model.response.HistoryItem? {
        return _historyItemsMap[bvid]
    }

    fun resolveHistoryRenderKey(video: VideoItem): String {
        val bvid = video.bvid.trim()
        if (bvid.isNotEmpty()) return bvid
        val matched = _historyItemsByRenderKey.entries.firstOrNull { (_, item) ->
            item.videoItem.id == video.id &&
                item.videoItem.cid == video.cid &&
                item.videoItem.title == video.title
        }?.key
        if (!matched.isNullOrBlank()) return matched
        return "unknown_${video.id.coerceAtLeast(0L)}"
    }

    fun startVideoDissolve(renderKey: String) {
        val key = renderKey.trim()
        if (key.isEmpty()) return
        _dissolvingIds.value = _dissolvingIds.value + key
    }

    fun startBatchVideoDissolve(renderKeys: Set<String>) {
        if (renderKeys.isEmpty()) return
        _dissolvingIds.value = _dissolvingIds.value + renderKeys
    }

    fun completeVideoDissolve(renderKey: String) {
        val key = renderKey.trim()
        if (key.isEmpty()) return
        _dissolvingIds.value = _dissolvingIds.value - key
        deleteHistoryItems(setOf(key))
    }

    private fun enrichHistoryProgress(
        historyItems: List<com.android.purebilibili.data.model.response.HistoryItem>
    ): List<com.android.purebilibili.data.model.response.HistoryItem> {
        return historyItems.map { item ->
            val video = item.videoItem
            if (video.bvid.isBlank()) return@map item

            val cid = (item.cid.takeIf { it > 0L } ?: video.cid).coerceAtLeast(0L)
            val localCachedMs = progressManager.getCachedPosition(video.bvid, cid)
            val resolvedProgress = resolveHistoryDisplayProgress(
                serverProgressSec = item.progress,
                durationSec = video.duration,
                localPositionMs = localCachedMs
            )

            if (resolvedProgress == item.progress && resolvedProgress == video.progress) {
                item
            } else {
                item.copy(
                    progress = resolvedProgress,
                    videoItem = video.copy(progress = resolvedProgress)
                )
            }
        }
    }
    
    override suspend fun fetchItems(): List<VideoItem> {
        // 重置游标
        cursorMax = 0
        cursorViewAt = 0
        cursorBusiness = ""
        _historyItemsMap.clear()
        _historyItemsByRenderKey.clear()
        _dissolvingIds.value = emptySet()
        
        val result = com.android.purebilibili.data.repository.HistoryRepository.getHistoryList(
            ps = 30,
            max = 0,
            viewAt = 0
        )
        
        val historyResult = result.getOrNull()
        if (historyResult == null) {
            hasMore = false
            _hasMoreState.value = false
            return emptyList()
        }
        
        // 更新游标
        historyResult.cursor?.let { cursor ->
            cursorMax = cursor.max
            cursorViewAt = cursor.view_at
            cursorBusiness = cursor.business
        }
        
        // 判断是否还有更多
        hasMore = historyResult.list.isNotEmpty() && historyResult.cursor != null && historyResult.cursor.max > 0
        _hasMoreState.value = hasMore
        
        // 保存历史记录项并转换为 VideoItem
        val historyItems = enrichHistoryProgress(historyResult.list.map { it.toHistoryItem() })
        cacheHistoryItems(historyItems)
        
        com.android.purebilibili.core.util.Logger.d(
            "HistoryVM",
            " First page: ${historyResult.list.size} items, hasMore=$hasMore, nextMax=$cursorMax, nextViewAt=$cursorViewAt, nextBusiness=$cursorBusiness"
        )
        
        return historyItems.map { it.videoItem }
    }
    
    //  加载更多
    fun loadMore() {
        if (isLoadingMore || !hasMore) return
        
        viewModelScope.launch {
            isLoadingMore = true
            _isLoadingMoreState.value = true
            
            try {
                com.android.purebilibili.core.util.Logger.d(
                    "HistoryVM",
                    " loadMore: max=$cursorMax, viewAt=$cursorViewAt, business=$cursorBusiness"
                )
                
                val result = com.android.purebilibili.data.repository.HistoryRepository.getHistoryList(
                    ps = 30,
                    max = cursorMax,
                    viewAt = cursorViewAt,
                    business = cursorBusiness
                )
                
                val historyResult = result.getOrNull()
                if (historyResult == null || historyResult.list.isEmpty()) {
                    hasMore = false
                    _hasMoreState.value = false
                    return@launch
                }
                
                // 更新游标
                historyResult.cursor?.let { cursor ->
                    cursorMax = cursor.max
                    cursorViewAt = cursor.view_at
                    cursorBusiness = cursor.business
                }
                
                // 判断是否还有更多
                hasMore = historyResult.cursor != null && historyResult.cursor.max > 0
                _hasMoreState.value = hasMore
                
                // 保存历史记录项并转换为 VideoItem
                val historyItems = enrichHistoryProgress(historyResult.list.map { it.toHistoryItem() })
                cacheHistoryItems(historyItems)
                
                val newItems = historyItems.map { it.videoItem }
                com.android.purebilibili.core.util.Logger.d("HistoryVM", " Loaded ${newItems.size} more items, hasMore=$hasMore")
                
                if (newItems.isNotEmpty()) {
                    // 追加到现有列表（过滤重复）
                    val currentItems = _uiState.value.items
                    val existingBvids = currentItems.map { it.bvid }.toSet()
                    val uniqueNewItems = newItems.filter { it.bvid !in existingBvids }
                    _uiState.value = _uiState.value.copy(items = currentItems + uniqueNewItems)
                    com.android.purebilibili.core.util.Logger.d("HistoryVM", " Total items: ${_uiState.value.items.size}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                com.android.purebilibili.core.util.Logger.e("HistoryVM", " loadMore failed", e)
            } finally {
                isLoadingMore = false
                _isLoadingMoreState.value = false
            }
        }
    }

    fun deleteHistoryItems(renderKeys: Set<String>) {
        if (renderKeys.isEmpty()) return
        val snapshotItems = _uiState.value.items
        val snapshotRenderMap = HashMap(_historyItemsByRenderKey)
        val snapshotBvidMap = HashMap(_historyItemsMap)
        val targetKeys = renderKeys.filter { it in snapshotRenderMap }.toSet()
        if (targetKeys.isEmpty()) return

        val optimisticList = snapshotItems.filter { video ->
            val key = resolveHistoryRenderKeyFromSnapshot(video, snapshotRenderMap)
            key !in targetKeys
        }
        _uiState.value = _uiState.value.copy(items = optimisticList)
        _historyItemsByRenderKey.keys.removeAll(targetKeys)
        targetKeys.forEach { key ->
            snapshotRenderMap[key]?.videoItem?.bvid?.takeIf { it.isNotBlank() }?.let { _historyItemsMap.remove(it) }
        }

        viewModelScope.launch {
            try {
                val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache.orEmpty()
                if (csrf.isBlank()) {
                    restoreHistorySnapshot(snapshotItems, snapshotRenderMap, snapshotBvidMap)
                    android.widget.Toast.makeText(getApplication(), "请先登录", android.widget.Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val targetEntries = targetKeys.mapNotNull { key ->
                    snapshotRenderMap[key]?.let { key to it }
                }
                val successKeys = mutableSetOf<String>()
                targetEntries.forEachIndexed { index, (key, item) ->
                    val kid = resolveHistoryDeleteKid(item)
                    if (!kid.isNullOrBlank()) {
                        val result = deleteHistoryItemWithRetry(kid = kid, csrf = csrf)
                        if (result.isSuccess) {
                            successKeys += key
                        }
                    }
                    if (index < targetEntries.lastIndex) {
                        kotlinx.coroutines.delay(HISTORY_DELETE_INTERVAL_MS)
                    }
                }

                if (successKeys.size == targetEntries.size) {
                    val count = successKeys.size
                    android.widget.Toast.makeText(
                        getApplication(),
                        if (count == 1) "已删除历史记录" else "已删除 $count 条历史记录",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val restoredItems = snapshotItems.filter { video ->
                        val key = resolveHistoryRenderKeyFromSnapshot(video, snapshotRenderMap)
                        key !in successKeys
                    }
                    val restoredRenderMap = HashMap(snapshotRenderMap).apply {
                        keys.removeAll(successKeys)
                    }
                    val restoredBvidMap = HashMap(snapshotBvidMap).apply {
                        entries.removeAll { (_, value) ->
                            resolveHistoryRenderKey(value) in successKeys
                        }
                    }
                    _uiState.value = _uiState.value.copy(items = restoredItems)
                    _historyItemsByRenderKey.clear()
                    _historyItemsByRenderKey.putAll(restoredRenderMap)
                    _historyItemsMap.clear()
                    _historyItemsMap.putAll(restoredBvidMap)
                    android.widget.Toast.makeText(
                        getApplication(),
                        "批量删除完成：成功 ${successKeys.size} / ${targetEntries.size}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                restoreHistorySnapshot(snapshotItems, snapshotRenderMap, snapshotBvidMap)
                android.widget.Toast.makeText(
                    getApplication(),
                    "删除历史失败: ${e.message ?: "请稍后重试"}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun cacheHistoryItems(historyItems: List<com.android.purebilibili.data.model.response.HistoryItem>) {
        historyItems.forEach { item ->
            val bvid = item.videoItem.bvid.trim()
            if (bvid.isNotEmpty()) {
                _historyItemsMap[bvid] = item
            }
            _historyItemsByRenderKey[resolveHistoryRenderKey(item)] = item
        }
    }

    private fun resolveHistoryRenderKeyFromSnapshot(
        video: VideoItem,
        historyMap: Map<String, com.android.purebilibili.data.model.response.HistoryItem>
    ): String {
        val bvid = video.bvid.trim()
        if (bvid.isNotEmpty()) return bvid
        val matched = historyMap.entries.firstOrNull { (_, item) ->
            item.videoItem.id == video.id &&
                item.videoItem.cid == video.cid &&
                item.videoItem.title == video.title
        }?.key
        if (!matched.isNullOrBlank()) return matched
        return "unknown_${video.id.coerceAtLeast(0L)}"
    }

    private fun restoreHistorySnapshot(
        items: List<VideoItem>,
        renderMap: Map<String, com.android.purebilibili.data.model.response.HistoryItem>,
        bvidMap: Map<String, com.android.purebilibili.data.model.response.HistoryItem>
    ) {
        _uiState.value = _uiState.value.copy(items = items)
        _historyItemsByRenderKey.clear()
        _historyItemsByRenderKey.putAll(renderMap)
        _historyItemsMap.clear()
        _historyItemsMap.putAll(bvidMap)
    }

    private suspend fun deleteHistoryItemWithRetry(
        kid: String,
        csrf: String
    ): Result<Unit> {
        repeat(HISTORY_DELETE_MAX_ATTEMPTS) { attempt ->
            val result = com.android.purebilibili.data.repository.HistoryRepository.deleteHistoryItem(
                kid = kid,
                csrf = csrf
            )
            if (result.isSuccess) return Result.success(Unit)
            if (attempt >= HISTORY_DELETE_MAX_ATTEMPTS - 1) {
                return result
            }
            val backoffMs = HISTORY_DELETE_RETRY_BASE_DELAY_MS * (attempt + 1)
            kotlinx.coroutines.delay(backoffMs)
        }
        return Result.failure(Exception("删除历史失败"))
    }

    companion object {
        private const val HISTORY_DELETE_MAX_ATTEMPTS = 3
        private const val HISTORY_DELETE_RETRY_BASE_DELAY_MS = 300L
        private const val HISTORY_DELETE_INTERVAL_MS = 150L
    }
}

// --- 收藏 ViewModel (支持分页加载所有收藏夹) ---
class FavoriteViewModel(application: Application) : BaseListViewModel(application, "我的收藏") {
    
    // 分页状态
    private var currentPage = 1
    private var hasMore = true
    private var allFolderIds: List<Long> = emptyList()  //  所有收藏夹 ID
    private var currentFolderIndex = 0  //  当前正在加载的收藏夹索引
    private var isLoadingMore = false
    
    //  暴露加载更多状态
    private val _isLoadingMoreState = MutableStateFlow(false)
    val isLoadingMoreState = _isLoadingMoreState.asStateFlow()
    
    private val _hasMoreState = MutableStateFlow(true)
    val hasMoreState = _hasMoreState.asStateFlow()
    
    // 📁 [新增] 收藏夹列表
    private val _folders = MutableStateFlow<List<com.android.purebilibili.data.model.response.FavFolder>>(emptyList())
    val folders = _folders.asStateFlow()
    
    // 📁 [新增] 当前选中的收藏夹索引
    private val _selectedFolderIndex = MutableStateFlow(0)
    val selectedFolderIndex = _selectedFolderIndex.asStateFlow()
    
    /**
     * 📁 [新增] 切换收藏夹
     */
    // 📁 [新增] 多文件夹状态管理
    private val _folderStates = mutableMapOf<Int, MutableStateFlow<ListUiState>>()
    // [Fix] Track active fetches to prevent infinite loading state or double fetching
    private val _fetchingIndices = mutableSetOf<Int>()
    
    /**
     * 获取指定文件夹的 UI 状态
     */
    fun getFolderUiState(index: Int): kotlinx.coroutines.flow.StateFlow<ListUiState> {
        return _folderStates.getOrPut(index) {
             // 默认状态: isLoading = true to show skeleton initially
            MutableStateFlow(ListUiState(title = "文件夹$index", isLoading = true))
        }.asStateFlow()
    }

    /**
     * 📁 切换收藏夹 (仅更新索引，不再强制刷新)
     */
    fun switchFolder(index: Int) {
        if (index < 0 || index >= allFolderIds.size) return
        currentFolderIndex = index
        _selectedFolderIndex.value = index
    }
    
    /**
     * 加载指定文件夹的数据
     */
    fun loadFolder(index: Int) {
        // [Fix] Do not validate index against allFolderIds.size here if it's 0, 
        // because allFolderIds might be empty initially and we need to fetch folders first.
        if (index < 0) return
        
        val stateFlow = _folderStates.getOrPut(index) { MutableStateFlow(ListUiState(isLoading = true)) }
        val currentState = stateFlow.value
        
        // 如果已经有数据，直接返回
        if (currentState.items.isNotEmpty()) return
        
        // 如果正在加载（通过 Set 追踪），则跳过
        if (_fetchingIndices.contains(index)) return
        
        _fetchingIndices.add(index)
        
        viewModelScope.launch {
            // Update state to loading (if not already)
            if (!currentState.isLoading) {
                 stateFlow.value = currentState.copy(isLoading = true, error = null)
            }
            
            try {
                // 确保第一次加载先获取文件夹列表（如果还未获取）
                if (allFolderIds.isEmpty()) {
                    fetchFolders()
                }
                
                // Double check index validity after fetchFolders
                if (index < allFolderIds.size) {
                    val listResult = com.android.purebilibili.data.repository.FavoriteRepository.getFavoriteList(
                        mediaId = allFolderIds[index], 
                        pn = 1
                    )
                    val resultData = listResult.getOrNull()
                    val items = resultData?.medias?.map { it.toVideoItem() } ?: emptyList()
                    
                     // Update Title if possible
                    val title = if (index < _folders.value.size) _folders.value[index].title else currentState.title

                    stateFlow.value = currentState.copy(isLoading = false, items = items, title = title)
                    com.android.purebilibili.core.util.Logger.d("FavoriteVM", "📁 Loaded folder $index ($title): ${items.size} items")
                } else {
                     // Index still out of bounds (maybe empty folders?)
                     if (allFolderIds.isEmpty()) {
                          // No folders found
                          stateFlow.value = currentState.copy(isLoading = false, error = "没有找到收藏夹")
                     }
                }
            } catch (e: Exception) {
                stateFlow.value = currentState.copy(isLoading = false, error = e.message)
            } finally {
                _fetchingIndices.remove(index)
            }
        }
    }
    
    private suspend fun fetchFolders() {
        val api = NetworkModule.api
        val navResp = api.getNavInfo()
        val mid = navResp.data?.mid
        if (mid != null && mid != 0L) {
             val foldersResult = com.android.purebilibili.data.repository.FavoriteRepository.getFavFolders(mid)
             val foldersList = foldersResult.getOrNull()
             if (!foldersList.isNullOrEmpty()) {
                 _folders.value = foldersList
                 allFolderIds = foldersList.map { it.id }
             }
        }
    }

    // 重写 loadMore 以支持当前文件夹 (简化版，暂不支持多 Tag 同时分页，主要针对当前 Tab)
    // 实际实现需要 Map<Int, PaginationState>
    private val folderPaginationStates = mutableMapOf<Int, PaginationState>()
    
    data class PaginationState(var currentPage: Int = 1, var hasMore: Boolean = true)
    
    fun loadMoreForFolder(index: Int) {
        if (index < 0 || index >= allFolderIds.size) return
        
        val pagination = folderPaginationStates.getOrPut(index) { PaginationState() }
        if (!pagination.hasMore || isLoadingMore) return
        
        viewModelScope.launch {
            // ... load more logic adapted for specific folder index
            // similar to existing loadMore but targetting _folderStates[index]
            isLoadingMore = true
            try {
                pagination.currentPage++
                val listResult = com.android.purebilibili.data.repository.FavoriteRepository.getFavoriteList(
                     mediaId = allFolderIds[index], 
                     pn = pagination.currentPage
                )
                val resultData = listResult.getOrNull()
                val newItems = resultData?.medias?.map { it.toVideoItem() } ?: emptyList()
                pagination.hasMore = resultData?.has_more == true
                
                val stateFlow = _folderStates[index]
                if (stateFlow != null) {
                    val currentItems = stateFlow.value.items
                    // Filter duplicates
                     val existingIds = currentItems.map { it.id }.toSet()
                     val uniqueNewItems = newItems.filter { it.id !in existingIds }
                    stateFlow.value = stateFlow.value.copy(items = currentItems + uniqueNewItems)
                }
            } catch (e: Exception) {
                pagination.currentPage--
            } finally {
                isLoadingMore = false
            }
        }
    }

    // 保持 BaseListViewModel 兼容性 (Redirect to current folder)
    override suspend fun fetchItems(): List<VideoItem> {
        // This is called by init -> loadData. 
        // We can use it to initialize everything.
        try {
            fetchFolders()
            if (allFolderIds.isNotEmpty()) {
                 loadFolder(0)
                 // Sync base UI state with first folder? 
                 // Actually CommonListScreen should observe getFolderUiState if it's FavoriteVM
                 return _folderStates[0]?.value?.items ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }
    
    //  加载更多
    //  加载更多 (重定向到当前文件夹)
    fun loadMore() {
        loadMoreForFolder(currentFolderIndex)
    }
    //  [新增] 移除收藏
    fun removeVideo(video: VideoItem) {
        // aid 作为 resourceId
        val resourceId = video.aid 
        if (resourceId == 0L || allFolderIds.isEmpty()) return

        val folderIndex = _selectedFolderIndex.value
        if (folderIndex < 0 || folderIndex >= allFolderIds.size) return
        currentFolderIndex = folderIndex

        val currentMediaId = allFolderIds[folderIndex]
        val stateFlow = _folderStates.getOrPut(folderIndex) {
            MutableStateFlow(ListUiState(isLoading = false))
        }
        
        viewModelScope.launch {
            val originalState = stateFlow.value
            try {
                // Optimistic update: remove from current folder state immediately.
                val updatedItems = originalState.items.filter { it.id != video.id }
                stateFlow.value = originalState.copy(items = updatedItems, error = null)
                if (_uiState.value.items.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(items = updatedItems)
                }
                
                val result = com.android.purebilibili.data.repository.FavoriteRepository.removeResource(currentMediaId, resourceId)
                if (result.isFailure) {
                    // Revert if failed
                    val error = "取消收藏失败: ${result.exceptionOrNull()?.message}"
                    stateFlow.value = originalState.copy(error = error)
                    _uiState.value = _uiState.value.copy(error = error)
                }
            } catch (e: Exception) {
                 e.printStackTrace()
                 val message = e.message ?: "取消收藏失败"
                 stateFlow.value = originalState.copy(error = message)
                 _uiState.value = _uiState.value.copy(error = message)
            }
        }
    }
}
