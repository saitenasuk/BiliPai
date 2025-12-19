// 文件路径: feature/home/HomeScreen.kt
package com.android.purebilibili.feature.home

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.staggeredgrid.*  // 🌊 瀑布流布局
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.luminance  // 🔥 状态栏亮度计算
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.feature.settings.GITHUB_URL
import com.android.purebilibili.core.store.SettingsManager // 🔥 引入 SettingsManager
// 🔥 从 components 包导入拆分后的组件
import com.android.purebilibili.feature.home.components.BottomNavItem
import com.android.purebilibili.feature.home.components.FluidHomeTopBar
import com.android.purebilibili.feature.home.components.FrostedBottomBar
import com.android.purebilibili.feature.home.components.CategoryTabRow
import com.android.purebilibili.feature.home.components.iOSHomeHeader  // 🍎 iOS 大标题头部
import com.android.purebilibili.feature.home.components.iOSRefreshIndicator  // 🍎 iOS 下拉刷新指示器
// 🔥 从 cards 子包导入卡片组件
import com.android.purebilibili.feature.home.components.cards.ElegantVideoCard
import com.android.purebilibili.feature.home.components.cards.LiveRoomCard
import com.android.purebilibili.feature.home.components.cards.StoryVideoCard   // 🎬 故事卡片
import com.android.purebilibili.feature.home.components.cards.GlassVideoCard   // 🍎 玻璃拟态
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
    val staggeredGridState = rememberLazyStaggeredGridState()  // 🌊 瀑布流状态
    val hazeState = remember { HazeState() }
    val coroutineScope = rememberCoroutineScope()  // 🍎 用于双击回顶动画
    
    // 🔥🔥 [修复] 确保首页显示时 WindowInsets 配置正确，防止从视频页返回时布局跳动
    val view = androidx.compose.ui.platform.LocalView.current
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        // 保持边到边显示（与 VideoDetailScreen 一致）
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    // �� [性能优化] 合并首页设置为单一 Flow，减少 6 个 collectAsState → 1 个
    val homeSettings by SettingsManager.getHomeSettings(context).collectAsState(
        initial = com.android.purebilibili.core.store.HomeSettings()
    )
    
    // 解构设置值（避免每次访问都触发重组）
    val displayMode = homeSettings.displayMode
    val isBottomBarFloating = homeSettings.isBottomBarFloating
    val bottomBarLabelMode = homeSettings.bottomBarLabelMode
    val isHeaderBlurEnabled = homeSettings.isHeaderBlurEnabled
    val isBottomBarBlurEnabled = homeSettings.isBottomBarBlurEnabled
    val crashTrackingConsentShown = homeSettings.crashTrackingConsentShown
    val cardAnimationEnabled = homeSettings.cardAnimationEnabled      // 🔥 卡片进场动画开关
    val cardTransitionEnabled = homeSettings.cardTransitionEnabled    // 🔥 卡片过渡动画开关
    
    // 🔥🔥 [修复] 根据展示模式动态设置网格列数
    // 故事卡片需要单列全宽，网格和玻璃使用双列
    val gridColumns = if (displayMode == 1) 1 else 2

    // 🔥🔥 [修复] 恢复状态栏样式：确保从视频详情页返回后状态栏正确
    // 当使用滑动动画时，Theme.kt 的 SideEffect 可能不会重新执行
    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightBackground = remember(backgroundColor) { backgroundColor.luminance() > 0.5f }
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as? android.app.Activity)?.window ?: return@SideEffect
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
            // 🔥 根据背景亮度设置状态栏图标颜色
            insetsController.isAppearanceLightStatusBars = isLightBackground
            // 🔥 确保状态栏可见且透明
            insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
    }

    val density = LocalDensity.current
    val navBarHeight = WindowInsets.navigationBars.getBottom(density).let { with(density) { it.toDp() } }
    
    // 🔥 动态计算底部避让高度
    val bottomBarHeight = if (isBottomBarFloating) {
        84.dp + navBarHeight  // 72dp(栏高度) + 12dp(底部边距)
    } else {
        64.dp + navBarHeight  // 64dp(Docked模式)
    }

    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    
    // 🔥 当前选中的导航项
    var currentNavItem by remember { mutableStateOf(BottomNavItem.HOME) }
    
    // 🔥🔥 [新增] 底栏可见性状态（初始隐藏，进入后显示触发动画）
    var bottomBarVisible by remember { mutableStateOf(false) }
    
    // 🔥🔥 包装 onVideoClick：点击视频时先隐藏底栏再导航
    val wrappedOnVideoClick: (String, Long, String) -> Unit = remember(onVideoClick) {
        { bvid, cid, cover ->
            bottomBarVisible = false  // 🔥 触发底栏下滑动画
            onVideoClick(bvid, cid, cover)
        }
    }
    
    // 🔥🔥 进入/返回首页时触发底栏上滑动画
    LaunchedEffect(Unit) {
        // 短暂延迟确保组合完成后再显示，触发滑入动画
        kotlinx.coroutines.delay(50)
        bottomBarVisible = true
    }
    
    // 🔥🔥 [修复] 使用 ViewModel 中的标签页显示索引（跨导航保持）
    // 当用户滑动到特殊分类时，标签页位置更新，但内容分类保持不变
    val displayedTabIndex = state.displayedTabIndex
    
    // 🔥🔥 [修复] 使用 rememberSaveable 记住本次会话中是否已处理过弹窗（防止导航后重新显示）
    var consentDialogHandled by rememberSaveable { mutableStateOf(false) }
    var showConsentDialog by remember { mutableStateOf(false) }
    
    // 🔥🔥 检查欢迎弹窗是否已显示过（确保弹窗顺序显示，不会同时出现）
    val welcomePrefs = remember { context.getSharedPreferences("app_welcome", Context.MODE_PRIVATE) }
    val welcomeAlreadyShown = welcomePrefs.getBoolean("first_launch_shown", false)
    
    // 检查是否需要显示弹窗（欢迎弹窗已显示过 且 同意弹窗尚未显示过 且 本次会话未处理过）
    LaunchedEffect(crashTrackingConsentShown) {
        if (welcomeAlreadyShown && !crashTrackingConsentShown && !consentDialogHandled) {
            showConsentDialog = true
        }
    }
    
    // 显示弹窗
    if (showConsentDialog) {
        com.android.purebilibili.feature.home.components.CrashTrackingConsentDialog(
            onDismiss = { 
                showConsentDialog = false
                consentDialogHandled = true  // 标记为已处理
            }
        )
    }
    
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


    // 🔥🔥 PullToRefreshBox 自动处理下拉刷新逻辑
    
    // 🔥🔥 [已移除] 特殊分类（ANIME, MOVIE等）不再在首页切换，直接导航到独立页面
    
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
    
    // 🔥 记录滑动方向用于动画 (true = 向右/上一个分类, false = 向左/下一个分类)
    var swipeDirection by remember { mutableStateOf(true) }
    
    // 🔥🔥 [修复] 特殊分类列表（有独立页面，不在首页显示内容）
    val specialCategories = listOf(
        HomeCategory.ANIME, 
        HomeCategory.MOVIE, 
        HomeCategory.GAME, 
        HomeCategory.KNOWLEDGE, 
        HomeCategory.TECH
    )
    
    // 🔥 水平滑动切换分类的回调
    val switchToPreviousCategory: () -> Unit = remember(displayedTabIndex) {
        {
            swipeDirection = true  // 右滑
            // 🔥🔥 [修复] 使用 ViewModel 中的标签页索引
            if (displayedTabIndex > 0) {
                val prevIndex = displayedTabIndex - 1
                val prevCategory = HomeCategory.entries[prevIndex]
                // 更新标签页显示位置（通过 ViewModel）
                viewModel.updateDisplayedTabIndex(prevIndex)
                // 🔥🔥 [修复] 对于特殊分类，只导航到独立页面；普通分类更新内容
                when (prevCategory) {
                    HomeCategory.ANIME -> onBangumiClick(1)
                    HomeCategory.MOVIE -> onBangumiClick(2)
                    HomeCategory.GAME, HomeCategory.KNOWLEDGE, HomeCategory.TECH -> 
                        onCategoryClick(prevCategory.tid, prevCategory.label)
                    else -> viewModel.switchCategory(prevCategory)
                }
            }
        }
    }
    
    val switchToNextCategory: () -> Unit = remember(displayedTabIndex) {
        {
            swipeDirection = false  // 左滑
            // 🔥🔥 [修复] 使用 ViewModel 中的标签页索引
            if (displayedTabIndex < HomeCategory.entries.size - 1) {
                val nextIndex = displayedTabIndex + 1
                val nextCategory = HomeCategory.entries[nextIndex]
                // 更新标签页显示位置（通过 ViewModel）
                viewModel.updateDisplayedTabIndex(nextIndex)
                // 🔥🔥 [修复] 对于特殊分类，只导航到独立页面；普通分类更新内容
                when (nextCategory) {
                    HomeCategory.ANIME -> onBangumiClick(1)
                    HomeCategory.MOVIE -> onBangumiClick(2)
                    HomeCategory.GAME, HomeCategory.KNOWLEDGE, HomeCategory.TECH -> 
                        onCategoryClick(nextCategory.tid, nextCategory.label)
                    else -> viewModel.switchCategory(nextCategory)
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            // 🔥🔥 底栏进入/退出动画：进入首页时从底部滑入，离开首页时向底部滑出
            AnimatedVisibility(
                visible = bottomBarVisible,  // 🔥 受状态控制
                enter = slideInVertically(
                    initialOffsetY = { it },  // 从底部滑入
                    animationSpec = tween(350)
                ) + fadeIn(animationSpec = tween(250)),
                exit = slideOutVertically(
                    targetOffsetY = { it },   // 向底部滑出
                    animationSpec = tween(250)
                ) + fadeOut(animationSpec = tween(200))
            ) {
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
                            isFloating = true,
                            labelMode = bottomBarLabelMode
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
                        isFloating = false,
                        labelMode = bottomBarLabelMode
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                // 🚀 [性能优化] 移除 AnimatedContent 包裹，减少分类切换时的重组开销
                // 原：AnimatedContent 对整个 Grid 做动画，成本很高
                // 新：直接渲染，分类切换瞬间完成
                val targetCategory = state.currentCategory
                
                // 🔥 使用 PullToRefreshBox 包裹内容
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    state = pullRefreshState,
                    modifier = Modifier.fillMaxSize(),
                    // 🍎 iOS 风格下拉刷新指示器
                    indicator = {
                        iOSRefreshIndicator(
                            state = pullRefreshState,
                            isRefreshing = isRefreshing,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 120.dp)  // 🔥 在 Header 下方显示
                        )
                    }
                ) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(gridColumns),
                    contentPadding = PaddingValues(
                        top = 156.dp,  // 🔥 Header 高度
                        bottom = if (isBottomBarFloating) 100.dp else padding.calculateBottomPadding() + 20.dp,
                        start = 8.dp, 
                        end = 8.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (isBottomBarFloating) 0.dp else navBarHeight)
                        // 🔥 水平滑动手势切换分类
                        .pointerInput(targetCategory) {
                            var totalDragX = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalDragX = 0f },
                                onDragEnd = {
                                    // 滑动阈值：120px
                                    if (totalDragX > 120f) {
                                        // 右滑：切换到上一个分类
                                        switchToPreviousCategory()
                                    } else if (totalDragX < -120f) {
                                        // 左滑：切换到下一个分类
                                        switchToNextCategory()
                                    }
                                },
                                onDragCancel = { totalDragX = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragX += dragAmount
                                }
                            )
                        }
                ) {
                    if (targetCategory == HomeCategory.LIVE) {
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
                                contentType = { _, _ -> "live_room" }
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
                                contentType = { _, _ -> "video" }
                            ) { index, video ->
                                // 🔥🔥 [新增] 根据展示模式选择卡片样式
                                when (displayMode) {
                                    1 -> {
                                        // 🎬 故事卡片 (Apple TV+ 风格)
                                        StoryVideoCard(
                                            video = video,
                                            index = index,  // 🔥 动画索引
                                            animationEnabled = cardAnimationEnabled,  // 🔥 动画开关
                                            transitionEnabled = cardTransitionEnabled, // 🔥 过渡动画开关
                                            onClick = { bvid, cid -> wrappedOnVideoClick(bvid, cid, video.pic) }
                                        )
                                    }
                                    2 -> {
                                        // 🍎 玻璃拟态 (Vision Pro 风格)
                                        GlassVideoCard(
                                            video = video,
                                            index = index,  // 🔥 动画索引
                                            animationEnabled = cardAnimationEnabled,  // 🔥 动画开关
                                            transitionEnabled = cardTransitionEnabled, // 🔥 过渡动画开关
                                            onClick = { bvid, cid -> wrappedOnVideoClick(bvid, cid, video.pic) }
                                        )
                                    }
                                    else -> {
                                        // 🔥 默认网格卡片
                                        ElegantVideoCard(
                                            video = video,
                                            index = index,
                                            isFollowing = video.owner.mid in state.followingMids,  // 🔥 判断是否已关注
                                            animationEnabled = cardAnimationEnabled,    // 🔥 进场动画开关
                                            transitionEnabled = cardTransitionEnabled,  // 🔥 过渡动画开关
                                            onClick = { bvid, cid -> wrappedOnVideoClick(bvid, cid, video.pic) }
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
            }

            // 🍎 iOS 风格 Header (带滚动隐藏/显示动画)
            // 使用 zIndex 确保 header 始终在列表内容之上
            Box(modifier = Modifier.zIndex(1f)) {
                iOSHomeHeader(
                    scrollOffset = scrollOffset,
                    user = state.user,
                    onAvatarClick = { if (state.user.isLogin) onProfileClick() else onAvatarClick() },
                    onSettingsClick = onSettingsClick,
                    onSearchClick = onSearchClick,
                    categoryIndex = displayedTabIndex,  // 🔥🔥 [修复] 使用 ViewModel 中的标签页索引
                    onCategorySelected = { index ->
                        // 🔥🔥 [修复] 通过 ViewModel 更新标签页显示位置
                        viewModel.updateDisplayedTabIndex(index)
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
                    },
                    // 🍎 [新增] 下拉刷新时收起标签页
                    isRefreshing = isRefreshing,
                    pullProgress = pullRefreshState.distanceFraction
                )
            }
        }
    }
}