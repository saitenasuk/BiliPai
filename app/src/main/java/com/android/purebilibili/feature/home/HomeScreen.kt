// 文件路径: feature/home/HomeScreen.kt
package com.android.purebilibili.feature.home

import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
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
import com.android.purebilibili.core.ui.LoadingAnimation
import com.android.purebilibili.core.ui.VideoCardSkeleton
import com.android.purebilibili.core.ui.ErrorState as ModernErrorState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.android.purebilibili.core.ui.shimmer

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
    // 🔥 新增：直播点击回调
    onLiveClick: (Long, String, String) -> Unit = { _, _, _ -> }  // roomId, title, uname
) {
    val state by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    val context = LocalContext.current
    val gridState = rememberLazyGridState()
    val hazeState = remember { HazeState() }



    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
        }
    }

    val density = LocalDensity.current
    val navBarHeight = WindowInsets.navigationBars.getBottom(density).let { with(density) { it.toDp() } }
    
    // 🔥 iOS 风格：BottomBar 悬浮，已包含 navigationBarsPadding
    val isBottomBarFloating by SettingsManager.getBottomBarFloating(context).collectAsState(initial = true)
    
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

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 4 && !state.isLoading && !isRefreshing
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadMore() }

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) { viewModel.refresh() }
    }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) pullRefreshState.startRefresh() else pullRefreshState.endRefresh()
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
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            // 🔥 判断是否需要显示骨架屏：加载中且当前分类对应的列表为空
            val showSkeleton = state.isLoading && when (state.currentCategory) {
                HomeCategory.LIVE -> state.liveRooms.isEmpty()
                else -> state.videos.isEmpty()
            }
            
            // 1. 底层：视频列表
            if (showSkeleton) {
                // 🔥 骨架屏加载动画（适用于视频和直播）- 包含完整的顶栏和分类栏
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = bottomBarHeight + 20.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 🔥 顶栏骨架
                    item(span = { GridItemSpan(2) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 头像骨架
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .shimmer()
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            // 搜索框骨架
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .shimmer()
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            // 设置按钮骨架
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .shimmer()
                            )
                        }
                    }
                    
                    // 🔥 分类标签栏骨架
                    item(span = { GridItemSpan(2) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            repeat(5) { index ->
                                Box(
                                    modifier = Modifier
                                        .width(if (index == 0) 48.dp else 40.dp)
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .shimmer(delayMillis = index * 50)
                                )
                            }
                        }
                    }
                    
                    // 🔥 视频卡片骨架
                    items(6) { index -> VideoCardSkeleton(index = index) }
                }
            } else if (state.error != null && state.videos.isEmpty() && state.liveRooms.isEmpty()) {
                // 🔥 使用现代化错误组件
                ModernErrorState(
                    message = state.error ?: "加载失败",
                    onRetry = { viewModel.refresh() }
                )
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = bottomBarHeight + 20.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(state = hazeState)
                ) {
                    // 🔥 1. 顶栏 (作为列表第一项)
                    item(span = { GridItemSpan(2) }) {
                        FluidHomeTopBar(
                            user = state.user,
                            onAvatarClick = { if (state.user.isLogin) onProfileClick() else onAvatarClick() },
                            onSettingsClick = onSettingsClick,
                            onSearchClick = onSearchClick
                        )
                    }
                    
                    // 🔥 2. 分类标签栏
                    item(span = { GridItemSpan(2) }) {
                        CategoryTabRow(
                            selectedIndex = categoryIndex,
                            onCategorySelected = { index ->
                                viewModel.switchCategory(HomeCategory.entries[index])
                            }
                        )
                    }

                    // 🔥 3. 内容列表 - 根据分类显示不同内容
                    if (state.currentCategory == HomeCategory.LIVE) {
                        // 🔥 直播子分类标签
                        item(span = { GridItemSpan(2) }) {
                            LiveSubCategoryRow(
                                selectedSubCategory = state.liveSubCategory,
                                onSubCategorySelected = { viewModel.switchLiveSubCategory(it) }
                            )
                        }
                        
                        // 直播卡片
                        itemsIndexed(
                            items = state.liveRooms,
                            key = { index, room -> "${state.liveSubCategory.name}_${room.roomid}_$index" }  // 🔥 添加 index 确保唯一
                        ) { index, room ->
                            LiveRoomCard(room, index) { roomId ->
                                // 🔥 使用应用内导航打开直播间
                                onLiveClick(roomId, room.title, room.uname)
                            }
                        }
                    } else {
                        // 视频卡片
                        itemsIndexed(
                            items = state.videos,
                            key = { _, video -> "${video.bvid}_${state.refreshKey}" }  // 🔥 key 包含 refreshKey
                        ) { index, video ->
                            ElegantVideoCard(video, index, state.refreshKey) { bvid, cid ->
                                onVideoClick(bvid, cid, video.pic)
                            }
                        }
                    }
                    
                    // 加载更多指示器
                    val hasContent = if (state.currentCategory == HomeCategory.LIVE) state.liveRooms.isNotEmpty() else state.videos.isNotEmpty()
                    if (hasContent && state.isLoading) {
                        item(span = { GridItemSpan(2) }) {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }

            // 2. 移除原有的悬浮顶栏
            // FluidHomeTopBar(...)

            // 3. 顶层：刷新指示器
            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )
            
            FrostedBottomBar(
                currentItem = currentNavItem,
                onItemClick = { item ->
                    currentNavItem = item
                    when (item) {
                        BottomNavItem.HOME -> { /* 已在首页 */ }
                        BottomNavItem.DYNAMIC -> onDynamicClick()
                        BottomNavItem.HISTORY -> onHistoryClick()
                        BottomNavItem.PROFILE -> onProfileClick()
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
                hazeState = hazeState,
                isFloating = isBottomBarFloating // 🔥 传递设置
            )

        }
    }
}