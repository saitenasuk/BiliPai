// 文件路径: feature/category/CategoryScreen.kt
package com.android.purebilibili.feature.category

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.repository.VideoRepository
import com.android.purebilibili.feature.home.components.GlassVideoCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 🔥 分类视频 ViewModel
 */
class CategoryViewModel : ViewModel() {
    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos = _videos.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    
    private var currentTid: Int = 0
    private var currentPage: Int = 1
    private var hasMore: Boolean = true
    
    fun loadCategory(tid: Int) {
        if (currentTid == tid && _videos.value.isNotEmpty()) return
        currentTid = tid
        currentPage = 1
        hasMore = true
        _videos.value = emptyList()
        loadVideos()
    }
    
    private fun loadVideos() {
        if (_isLoading.value || !hasMore) return
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            VideoRepository.getRegionVideos(tid = currentTid, page = currentPage)
                .onSuccess { newVideos ->
                    if (newVideos.isEmpty()) {
                        hasMore = false
                    } else {
                        _videos.value = _videos.value + newVideos
                        currentPage++
                    }
                }
                .onFailure { e ->
                    _error.value = e.message ?: "加载失败"
                }
            
            _isLoading.value = false
        }
    }
    
    fun loadMore() {
        loadVideos()
    }
}

/**
 * 🔥 分类详情页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    tid: Int,
    name: String,
    onBack: () -> Unit,
    onVideoClick: (String, Long, String) -> Unit = { _, _, _ -> },
    viewModel: CategoryViewModel = viewModel()
) {
    val videos by viewModel.videos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val gridState = rememberLazyGridState()
    
    // 首次加载
    LaunchedEffect(tid) {
        viewModel.loadCategory(tid)
    }
    
    // 滚动到底部时加载更多
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= videos.size - 4
        }
    }
    
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isLoading) {
            viewModel.loadMore()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (videos.isEmpty() && isLoading) {
                // 首次加载
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (videos.isEmpty() && error != null) {
                // 错误状态
                Text(
                    text = error ?: "加载失败",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                // 视频网格
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(videos, key = { it.bvid }) { video ->
                        GlassVideoCard(
                            video = video,
                            onClick = { bvid, _ -> onVideoClick(bvid, video.id, video.pic) }
                        )
                    }
                    
                    // 加载更多指示器
                    if (isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
