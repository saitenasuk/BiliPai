// 文件路径: feature/home/HomeScreen.kt
package com.android.purebilibili.feature.home

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.feature.settings.GITHUB_URL
import com.android.purebilibili.core.store.SettingsManager // 🔥 引入 SettingsManager
// 🔥 从 components 包导入拆分后的组件
import com.android.purebilibili.feature.home.components.BottomNavItem
import com.android.purebilibili.feature.home.components.ElegantVideoCard
import com.android.purebilibili.feature.home.components.FluidHomeTopBar
import com.android.purebilibili.feature.home.components.FrostedBottomBar
import com.android.purebilibili.feature.home.components.CategoryTabRow
import com.android.purebilibili.feature.home.components.LiveRoomCard
import com.android.purebilibili.feature.home.components.StoryVideoCard   // 🎬 故事卡片
import com.android.purebilibili.feature.home.components.GlassVideoCard   // 🍎 玻璃拟态
import com.android.purebilibili.feature.home.components.iOSHomeHeader  // 🍎 iOS 大标题头部
import com.android.purebilibili.core.ui.LoadingAnimation
import com.android.purebilibili.core.ui.VideoCardSkeleton
import com.android.purebilibili.core.ui.ErrorState as ModernErrorState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.android.purebilibili.core.ui.shimmer
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import coil.imageLoader
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged  // 🚀 性能优化：防止重复触发

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onVideoClick: (String, Long, String) -> Unit,
    onAvatarClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    // 🔥 新增：动态页面回调
    onDynamicClick: () -> Unit = {},
    // 🔥 新增：历史记录回调
    onHistoryClick: () -> Unit = {},
    // 🔥 新增：分区回调
    onPartitionClick: () -> Unit = {},
    // 🔥 新增：直播点击回调
    onLiveClick: (Long, String, String) -> Unit = { _, _, _ -> },  // roomId, title, uname
    // 🔥🔥 [修复] 番剧/影视回调，接受类型参数 (1=番剧 2=电影 等)
    onBangumiClick: (Int) -> Unit = {},
    // 🔥 新增：分类点击回调（用于游戏、知识、科技等分类，传入 tid 和 name）
    onCategoryClick: (Int, String) -> Unit = { _, _ -> }
) {
    val state by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    val context = LocalContext.current
    val gridState = rememberLazyGridState()
    val hazeState = remember { HazeState() }
    val coroutineScope = rememberCoroutineScope()  // 🍎 用于双击回顶动画

    // 🔥🔥 [新增] 首页展示模式 (0=网格, 1=故事卡片, 2=玻璃拟态)
    val displayMode by SettingsManager.getDisplayMode(context).collectAsState(initial = 0)
    
    // 🔥🔥 [修复] 根据展示模式动态设置网格列数
    // 故事卡片需要单列全宽，网格和玻璃使用双列
    val gridColumns = if (displayMode == 1) 1 else 2

    // 🔥 状态栏样式由 MainActivity.enableEdgeToEdge() 根据主题自动管理
    // 不再在这里手动设置，避免覆盖主题感知的状态栏配置

    val density = LocalDensity.current
    val navBarHeight = WindowInsets.navigationBars.getBottom(density).let { with(density) { it.toDp() } }
    
    // 🔥 iOS 风格：BottomBar 悬浮，已包含 navigationBarsPadding
    val isBottomBarFloating by SettingsManager.getBottomBarFloating(context).collectAsState(initial = true)
    
    // 🔥 [新增] 模糊效果设置
    val isHeaderBlurEnabled by SettingsManager.getHeaderBlurEnabled(context).collectAsState(initial = true)
    val isBottomBarBlurEnabled by SettingsManager.getBottomBarBlurEnabled(context).collectAsState(initial = true)
    
    // 🔥 动态计算底部避让高度
    val bottomBarHeight = if (isBottomBarFloating) {
        84.dp + navBarHeight  // 72dp(栏高度) + 12dp(底部边距)
    } else {
        64.dp + navBarHeight  // 64dp(Docked模式)
    }

    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    
    // 🔥 当前选中的导航项
    var currentNavItem by remember { mutableStateOf(BottomNavItem.HOME) }
    
    // 🔥 分类标签索引由 ViewModel 状态计算
    val categoryIndex = state.currentCategory.ordinal
    
    // 🍎 计算滚动偏移量用于头部动画 - 🚀 优化：量化减少重组
    val scrollOffset by remember {
        derivedStateOf {
            val firstVisibleItem = gridState.firstVisibleItemIndex
            if (firstVisibleItem == 0) {
                // 🚀 量化到 50px 单位，减少重组频率
                val raw = gridState.firstVisibleItemScrollOffset
                (raw / 50) * 50f
            } else 1000f
        }
    }
    
    // 🍎 滚动方向（简化版 - 不再需要复杂检测，因为标签页只在顶部显示）
    val isScrollingUp = true  // 保留参数兼容性

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 4 && !state.isLoading && !isRefreshing
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadMore() }
    
    // 🚀🚀 [性能优化] 图片预加载 - 提前加载即将显示的视频封面
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()  // 🚀 只在索引变化时触发
            .collect { lastVisibleIndex ->
                val videos = state.videos
                val preloadStart = (lastVisibleIndex + 1).coerceAtMost(videos.size)
                val preloadEnd = (lastVisibleIndex + 6).coerceAtMost(videos.size)  // 🚀 减少预加载数量
                
                if (preloadStart < preloadEnd) {
                    for (i in preloadStart until preloadEnd) {
                        val imageUrl = videos.getOrNull(i)?.pic ?: continue
                        val request = coil.request.ImageRequest.Builder(context)
                            .data(com.android.purebilibili.core.util.FormatUtils.fixImageUrl(imageUrl))
                            .size(480, 300)  // 🚀 预加载也使用限制尺寸
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                            .build()
                        context.imageLoader.enqueue(request)
                    }
                }
            }
    }


    // 🔥 下拉刷新触发逻辑
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) { viewModel.refresh() }
    }
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) pullRefreshState.endRefresh()
    }
    
    // 🔥🔥 [修复] 如果当前在未实现的分类上，手势返回切换到推荐分类而不是退出应用
    val isUnimplementedCategory = state.currentCategory in listOf(HomeCategory.ANIME, HomeCategory.MOVIE)
    androidx.activity.compose.BackHandler(enabled = isUnimplementedCategory) {
        viewModel.switchCategory(HomeCategory.RECOMMEND)
    }
    
    // 🔥🔥 [修复] 如果当前在直播-关注分类且列表为空，返回时先切换到热门，再切换到推荐
    val isEmptyLiveFollowed = state.currentCategory == HomeCategory.LIVE && 
                               state.liveSubCategory == LiveSubCategory.FOLLOWED &&
                               state.liveRooms.isEmpty() && 
                               !state.isLoading
    androidx.activity.compose.BackHandler(enabled = isEmptyLiveFollowed) {
        // 切换到热门直播
        viewModel.switchLiveSubCategory(LiveSubCategory.POPULAR)
    }

    // 🔥🔥 [修复] 如果当前在直播分类（非关注空列表情况），返回时切换到推荐
    val isLiveCategoryNotHome = state.currentCategory == HomeCategory.LIVE && !isEmptyLiveFollowed
    androidx.activity.compose.BackHandler(enabled = isLiveCategoryNotHome) {
        viewModel.switchCategory(HomeCategory.RECOMMEND)
    }

    Scaffold(
        bottomBar = {
            if (isBottomBarFloating) {
                // 悬浮式底栏
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp), // 悬浮距离
                    contentAlignment = Alignment.Center
                ) {
                    FrostedBottomBar(
                        currentItem = currentNavItem,
                        onItemClick = { item ->
                            currentNavItem = item
                            when(item) {
                                BottomNavItem.HOME -> {
                                    coroutineScope.launch { gridState.animateScrollToItem(0) }
                                }
                                BottomNavItem.DYNAMIC -> onDynamicClick()
                                BottomNavItem.HISTORY -> onHistoryClick()
                                BottomNavItem.PROFILE -> onProfileClick()
                            }
                        },
                        onHomeDoubleTap = {
                            coroutineScope.launch { gridState.animateScrollToItem(0) }
                        },
                        hazeState = if (isBottomBarBlurEnabled) hazeState else null,
                        isFloating = true
                    )
                }
            } else {
                // 贴底式底栏
                FrostedBottomBar(
                    currentItem = currentNavItem,
                    onItemClick = { item ->
                        currentNavItem = item
                        when(item) {
                            BottomNavItem.HOME -> {
                                coroutineScope.launch { gridState.animateScrollToItem(0) }
                            }
                            BottomNavItem.DYNAMIC -> onDynamicClick()
                            BottomNavItem.HISTORY -> onHistoryClick()
                            BottomNavItem.PROFILE -> onProfileClick()
                        }
                    },
                    onHomeDoubleTap = {
                        coroutineScope.launch { gridState.animateScrollToItem(0) }
                    },
                    hazeState = if (isBottomBarBlurEnabled) hazeState else null,
                    isFloating = false
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullRefreshState.nestedScrollConnection)
                .haze(state = hazeState)  // 🔥 Haze 源：整个内容区域
        ) {
            if (state.isLoading && state.videos.isEmpty() && state.liveRooms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CupertinoActivityIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (state.isLoading && state.videos.isEmpty()) {
                 // 骨架屏 - 使用 LazyVerticalGrid 显示多个骨架卡片
                 LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    contentPadding = PaddingValues(
                        top = 156.dp,  // 🔥 与主内容保持一致
                        bottom = if (isBottomBarFloating) 100.dp else padding.calculateBottomPadding() + 20.dp,
                        start = 8.dp,
                        end = 8.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(8) { index ->
                        VideoCardSkeleton(index = index)
                    }
                }
            } else if (state.error != null && state.videos.isEmpty()) {
                ModernErrorState(
                    message = state.error ?: "未知错误",
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (isBottomBarFloating) 100.dp else padding.calculateBottomPadding() + 20.dp)
                )
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(gridColumns),
                    contentPadding = PaddingValues(
                        top = 0.dp, 
                        bottom = if (isBottomBarFloating) 100.dp else padding.calculateBottomPadding() + 20.dp,
                        start = 8.dp, 
                        end = 8.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (isBottomBarFloating) 0.dp else navBarHeight)
                ) {
                    item(span = { GridItemSpan(gridColumns) }) {
                        Spacer(modifier = Modifier.height(156.dp))  // 🔥 Header 高度：状态栏 + 搜索栏(52dp) + 分类标签(~52dp) + 阴影缓冲
                    }
                    
                    if (state.currentCategory == HomeCategory.LIVE) {
                        item(span = { GridItemSpan(gridColumns) }) {
                            LiveSubCategoryRow(
                                selectedSubCategory = state.liveSubCategory,
                                onSubCategorySelected = { viewModel.switchLiveSubCategory(it) }
                            )
                        }

                        if (state.liveRooms.isNotEmpty()) {
                            itemsIndexed(
                                items = state.liveRooms,
                                key = { _, room -> room.roomid },
                                contentType = { _, _ -> "live_room" }  // 🚀 性能优化
                            ) { index, room ->
                                LiveRoomCard(
                                    room = room,
                                    index = index,
                                    onClick = { onLiveClick(room.roomid, room.title, room.uname) } 
                                )
                            }
                        }
                    } else {
                        if (state.videos.isNotEmpty()) {
                            itemsIndexed(
                                items = state.videos,
                                key = { _, video -> video.bvid },
                                contentType = { _, _ -> "video" }  // 🚀 性能优化
                            ) { index, video ->
                                // 🔥🔥 [新增] 根据展示模式选择卡片样式
                                when (displayMode) {
                                    1 -> {
                                        // 🎬 故事卡片 (Apple TV+ 风格)
                                        StoryVideoCard(
                                            video = video,
                                            onClick = { bvid, cid -> onVideoClick(bvid, cid, video.pic) }
                                        )
                                    }
                                    2 -> {
                                        // 🍎 玻璃拟态 (Vision Pro 风格)
                                        GlassVideoCard(
                                            video = video,
                                            onClick = { bvid, cid -> onVideoClick(bvid, cid, video.pic) }
                                        )
                                    }
                                    else -> {
                                        // 🔥 默认网格卡片
                                        ElegantVideoCard(
                                            video = video,
                                            index = index,
                                            onClick = { bvid, cid -> onVideoClick(bvid, cid, video.pic) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (!state.isLoading && state.error == null) {
                        item(span = { GridItemSpan(gridColumns) }) {
                            LaunchedEffect(Unit) {
                                viewModel.loadMore()
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.isLoading) {
                                    CupertinoActivityIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                    
                    item(span = { GridItemSpan(gridColumns) }) {
                        Box(modifier = Modifier.fillMaxWidth().height(20.dp))
                    }
                }
            }

            // 🔥 下拉刷新指示器
            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )

            // 🍎 iOS 风格 Header (带滚动隐藏/显示动画)
            // 使用 zIndex 确保 header 始终在列表内容之上
            Box(modifier = Modifier.zIndex(1f)) {
                iOSHomeHeader(
                    scrollOffset = scrollOffset,
                    user = state.user,
                    onAvatarClick = { if (state.user.isLogin) onProfileClick() else onAvatarClick() },
                    onSettingsClick = onSettingsClick,
                    onSearchClick = onSearchClick,
                    categoryIndex = HomeCategory.entries.indexOf(state.currentCategory),
                    onCategorySelected = { index ->
                        val category = HomeCategory.entries[index]
                        // 🔥🔥 分类跳转逻辑
                        when (category) {
                            HomeCategory.ANIME -> onBangumiClick(1)   // 番剧
                            HomeCategory.MOVIE -> onBangumiClick(2)   // 电影
                            // 🔥 新增分类：跳转到分类详情页面
                            HomeCategory.GAME,
                            HomeCategory.KNOWLEDGE,
                            HomeCategory.TECH -> onCategoryClick(category.tid, category.label)
                            // 其他分类正常切换
                            else -> viewModel.switchCategory(category)
                        }
                    },
                    onPartitionClick = onPartitionClick,  // 🔥 分区按钮点击
                    isScrollingUp = isScrollingUp,
                    hazeState = if (isHeaderBlurEnabled) hazeState else null,  // 🔥 恢复 header 模糊
                    onStatusBarDoubleTap = {
                        // 🍎 双击状态栏，平滑滚动回顶部
                        coroutineScope.launch {
                            gridState.animateScrollToItem(0)
                        }
                    }
                )
            }
        }
    }
}