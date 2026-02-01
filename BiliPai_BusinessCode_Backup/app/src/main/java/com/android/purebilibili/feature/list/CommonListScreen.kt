package com.android.purebilibili.feature.list

import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import com.android.purebilibili.core.ui.blur.unifiedBlur
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.flow.distinctUntilChanged // [Fix] Missing import

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.DisposableEffect // [Fix] Missing import
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.util.VideoGridItemSkeleton
import com.android.purebilibili.feature.home.components.cards.ElegantVideoCard
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import com.android.purebilibili.core.util.rememberAdaptiveGridColumns
import com.android.purebilibili.core.util.rememberResponsiveSpacing
import com.android.purebilibili.core.util.rememberResponsiveValue
import com.android.purebilibili.core.util.PinyinUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonListScreen(
    viewModel: BaseListViewModel,
    onBack: () -> Unit,
    onVideoClick: (String, Long) -> Unit,
    globalHazeState: HazeState? = null // [新增] 接收全局 HazeState
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()
    
    // 📱 响应式布局参数
    // Fix: 手机端(Compact)使用较小的最小宽度以保证2列显示 (360dp / 170dp = 2.1 -> 2列)
    // 平板端(Expanded)使用较大的最小宽度以避免卡片过小
    val minColWidth = rememberResponsiveValue(compact = 170.dp, medium = 170.dp, expanded = 240.dp)
    val columns = rememberAdaptiveGridColumns(minColumnWidth = minColWidth)
    val spacing = rememberResponsiveSpacing()
    
    //  [修复] 分页支持：收藏 + 历史记录
    val favoriteViewModel = viewModel as? FavoriteViewModel
    val historyViewModel = viewModel as? HistoryViewModel
    
    // 收藏分页状态
    val isLoadingMoreFav by favoriteViewModel?.isLoadingMoreState?.collectAsState() 
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val hasMoreFav by favoriteViewModel?.hasMoreState?.collectAsState() 
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    //  历史记录分页状态
    val isLoadingMoreHis by historyViewModel?.isLoadingMoreState?.collectAsState() 
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val hasMoreHis by historyViewModel?.hasMoreState?.collectAsState() 
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    //  统一分页状态
    val isLoadingMore = isLoadingMoreFav || isLoadingMoreHis
    val hasMore = hasMoreFav || hasMoreHis
    
    //  使用 derivedStateOf 来高效检测滚动位置
    val shouldLoadMore = androidx.compose.runtime.remember {
        androidx.compose.runtime.derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 4  // 提前4个item开始加载
        }
    }
    
    //  滚动到底部时加载更多
    LaunchedEffect(shouldLoadMore.value, hasMore, isLoadingMore) {
        if (shouldLoadMore.value && hasMore && !isLoadingMore) {
            favoriteViewModel?.loadMore()
            historyViewModel?.loadMore()  //  历史记录加载更多
        }
    }
    
    // [Feature] BottomBar Scroll Hiding for CommonListScreen (History/Favorite)
    val setBottomBarVisible = com.android.purebilibili.core.ui.LocalSetBottomBarVisible.current
    
    // 监听列表滚动实现底栏自动隐藏/显示
    var lastFirstVisibleItem by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var lastScrollOffset by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    
    LaunchedEffect(gridState) {
        snapshotFlow { 
            Pair(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) 
        }
        .distinctUntilChanged()
        .collect { (firstVisibleItem, scrollOffset) ->
             // 顶部始终显示
             if (firstVisibleItem == 0 && scrollOffset < 100) {
                 setBottomBarVisible(true)
             } else {
                 val isScrollingDown = when {
                     firstVisibleItem > lastFirstVisibleItem -> true
                     firstVisibleItem < lastFirstVisibleItem -> false
                     else -> scrollOffset > lastScrollOffset + 50
                 }
                 val isScrollingUp = when {
                     firstVisibleItem < lastFirstVisibleItem -> true
                     firstVisibleItem > lastFirstVisibleItem -> false
                     else -> scrollOffset < lastScrollOffset - 50
                 }
                 
                 if (isScrollingDown) setBottomBarVisible(false)
                 if (isScrollingUp) setBottomBarVisible(true)
             }
             lastFirstVisibleItem = firstVisibleItem
             lastScrollOffset = scrollOffset
        }
    }
    
    // 离开页面时恢复底栏显示
    DisposableEffect(Unit) {
        onDispose {
            setBottomBarVisible(true)
        }
    }

    // 📁 [新增] 收藏夹切换 Tab
    val foldersState by favoriteViewModel?.folders?.collectAsState() 
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyList()) }
    val selectedFolderIndex by favoriteViewModel?.selectedFolderIndex?.collectAsState() 
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    // [优化] Haze 性能优化：优先使用全局 HazeState，避免双重 Source 导致的过度绘制
    val activeHazeState = globalHazeState ?: androidx.compose.runtime.remember { HazeState() }
    
    // 🔍 搜索状态
    var searchQuery by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
             // 使用 Box 包裹实现毛玻璃背景
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .unifiedBlur(activeHazeState)
            ) {
                Column {
                    TopAppBar(
                        title = { Text(state.title) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(CupertinoIcons.Default.ChevronBackward, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent
                        ),
                        scrollBehavior = scrollBehavior
                    )
                    
                    // 🔍 搜索栏 (在 TopBar 内部)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        com.android.purebilibili.core.ui.components.IOSSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = "搜索视频",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) // 更加透明以适应模糊背景
                        )
                    }
                    
                    // 📁 [新增] 收藏夹 Tab 栏（仅显示多个收藏夹时）
                    if (foldersState.size > 1) {
                        ScrollableTabRow(
                            selectedTabIndex = selectedFolderIndex,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary,
                            edgePadding = 16.dp,
                            indicator = { tabPositions ->
                                if (selectedFolderIndex < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedFolderIndex]),
                                        color = MaterialTheme.colorScheme.primary // 使用主题色
                                    )
                                }
                            },
                            divider = {}
                        ) {
                            foldersState.forEachIndexed { index, folder ->
                                Tab(
                                    selected = selectedFolderIndex == index,
                                    onClick = { 
                                        favoriteViewModel?.switchFolder(index) 
                                        // 切换收藏夹时清空搜索，避免混淆
                                        searchQuery = ""
                                    },
                                    text = {
                                        Text(
                                            text = folder.title,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (selectedFolderIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = activeHazeState) // [优化] 仅使用统一的 activeHazeState
        ) {
            if (state.isLoading) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(
                        start = spacing.medium,
                        end = spacing.medium,
                        top = padding.calculateTopPadding() + spacing.medium,
                        bottom = padding.calculateBottomPadding() + spacing.medium
                    ),
                    horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(spacing.medium),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(columns * 4) { VideoGridItemSkeleton() } // 根据列数生成骨架屏数量
                }
            } else if (state.error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.error ?: "未知错误", color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadData() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                        Text("重试")
                    }
                }
            } else if (state.items.isEmpty()) {
                Box(modifier = Modifier.align(Alignment.Center)) {
                    Text("暂无数据", color = Color.Gray)
                }
            } else {
                // 🔍 过滤列表
                val filteredItems = androidx.compose.runtime.remember(state.items, searchQuery) {
                    if (searchQuery.isBlank()) state.items
                    else {
                        state.items.filter { 
                            PinyinUtils.matches(it.title, searchQuery) ||
                            PinyinUtils.matches(it.owner.name, searchQuery)
                        }
                    }
                }

                if (filteredItems.isEmpty() && searchQuery.isNotEmpty()) {
                     Box(Modifier.align(Alignment.Center)) {
                        Text("没有找到相关视频", color = Color.Gray)
                     }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        state = gridState,
                        contentPadding = PaddingValues(
                            start = spacing.medium,
                            end = spacing.medium,
                            top = padding.calculateTopPadding() + spacing.medium,
                            bottom = padding.calculateBottomPadding() + spacing.medium + 80.dp // [调整] 增加底部 padding 防止底栏遮挡
                        ),
                        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(spacing.medium),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = filteredItems,
                            key = { _, item -> item.bvid.ifEmpty { item.id.toString() } } // 确保 key 唯一且不为空
                        ) { index, video ->
                            ElegantVideoCard(
                                video = video,
                                index = index,
                                animationEnabled = true,
                                transitionEnabled = true, // 启用共享元素过渡
                                onClick = { bvid, cid -> onVideoClick(bvid, cid) }
                            )
                        }
                        
                        //  加载更多指示器 (仅在未搜索时显示)
                        if (searchQuery.isEmpty()) {
                            if (isLoadingMore) {
                                item(span = { GridItemSpan(columns) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CupertinoActivityIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
