package com.android.purebilibili.feature.list

import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import com.android.purebilibili.core.ui.blur.unifiedBlur
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.platform.LocalContext // [New]
import androidx.compose.ui.platform.LocalDensity // [New]
import androidx.compose.ui.zIndex // [New]
import androidx.compose.ui.layout.onGloballyPositioned // [New]
import com.android.purebilibili.core.store.SettingsManager // [New]
import com.android.purebilibili.core.ui.blur.BlurStyles // [New]
import com.android.purebilibili.core.ui.blur.currentUnifiedBlurIntensity
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.adaptive.resolveDeviceUiProfile
import com.android.purebilibili.core.ui.adaptive.resolveEffectiveMotionTier

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.DisposableEffect // [Fix] Missing import
import kotlinx.coroutines.launch // [Fix] Import
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.ui.animation.DissolveAnimationPreset
import com.android.purebilibili.core.ui.animation.DissolvableVideoCard
import com.android.purebilibili.core.ui.animation.jiggleOnDissolve
import com.android.purebilibili.core.util.VideoGridItemSkeleton
import com.android.purebilibili.feature.home.components.cards.ElegantVideoCard
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.util.rememberAdaptiveGridColumns
import com.android.purebilibili.core.util.rememberResponsiveSpacing
import com.android.purebilibili.core.util.rememberResponsiveValue
import com.android.purebilibili.core.util.PinyinUtils
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.feature.video.player.ExternalPlaylistSource
import com.android.purebilibili.feature.video.player.PlayMode
import com.android.purebilibili.feature.video.player.PlaylistManager

internal enum class FavoriteContentMode {
    BASE_LIST,
    SINGLE_FOLDER,
    PAGER
}

internal fun resolveFavoriteContentMode(
    isFavoritePage: Boolean,
    folderCount: Int
): FavoriteContentMode {
    if (!isFavoritePage) return FavoriteContentMode.BASE_LIST
    return when {
        folderCount > 1 -> FavoriteContentMode.PAGER
        folderCount == 1 -> FavoriteContentMode.SINGLE_FOLDER
        else -> FavoriteContentMode.BASE_LIST
    }
}

internal fun resolveFavoritePlayAllItems(
    mode: FavoriteContentMode,
    baseItems: List<VideoItem>,
    selectedFolderItems: List<VideoItem>,
    singleFolderItems: List<VideoItem>
): List<VideoItem> {
    return when (mode) {
        FavoriteContentMode.PAGER -> selectedFolderItems.ifEmpty { baseItems }
        FavoriteContentMode.SINGLE_FOLDER -> singleFolderItems.ifEmpty { baseItems }
        FavoriteContentMode.BASE_LIST -> baseItems
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonListScreen(
    viewModel: BaseListViewModel,
    onBack: () -> Unit,
    onVideoClick: (String, Long) -> Unit,
    onPlayAllAudioClick: ((String, Long) -> Unit)? = null,
    globalHazeState: HazeState? = null // [新增] 接收全局 HazeState
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()
    
    // 📱 响应式布局参数
    // Fix: 手机端(Compact)使用较小的最小宽度以保证2列显示 (360dp / 170dp = 2.1 -> 2列)
    // 平板端(Expanded)使用较大的最小宽度以避免卡片过小
    val context = LocalContext.current
    val homeSettings by SettingsManager.getHomeSettings(context).collectAsState(initial = com.android.purebilibili.core.store.HomeSettings())
    val windowSizeClass = LocalWindowSizeClass.current
    val deviceUiProfile = remember(windowSizeClass.widthSizeClass) {
        resolveDeviceUiProfile(
            widthSizeClass = windowSizeClass.widthSizeClass
        )
    }
    val cardMotionTier = resolveEffectiveMotionTier(
        baseTier = deviceUiProfile.motionTier,
        animationEnabled = homeSettings.cardAnimationEnabled
    )
    
    val minColWidth = rememberResponsiveValue(compact = 170.dp, medium = 170.dp, expanded = 240.dp)
    val adaptiveColumns = rememberAdaptiveGridColumns(minColumnWidth = minColWidth)
    
    // [新增] 优先使用用户设置的列数
    val columns = if (homeSettings.gridColumnCount > 0) homeSettings.gridColumnCount else adaptiveColumns
    val spacing = rememberResponsiveSpacing()
    
    //  [修复] 分页支持：收藏 + 历史记录
    val favoriteViewModel = viewModel as? FavoriteViewModel
    val historyViewModel = viewModel as? HistoryViewModel
    val historyDissolvingIds by historyViewModel?.dissolvingIds?.collectAsState()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptySet()) }
    var isHistoryBatchMode by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var selectedHistoryKeys by rememberSaveable { androidx.compose.runtime.mutableStateOf(setOf<String>()) }
    var showHistoryBatchDeleteConfirm by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var pendingHistorySingleDeleteKey by rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }

    LaunchedEffect(state.items, historyViewModel, isHistoryBatchMode) {
        if (historyViewModel == null) return@LaunchedEffect
        val validKeys = state.items
            .map(historyViewModel::resolveHistoryRenderKey)
            .filter { it.isNotBlank() }
            .toSet()
        selectedHistoryKeys = selectedHistoryKeys.filter { it in validKeys }.toSet()
        if (isHistoryBatchMode && state.items.isEmpty()) {
            isHistoryBatchMode = false
            selectedHistoryKeys = emptySet()
        }
    }
    
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
    
    // [Fix] Import for launch
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // 📁 [新增] 收藏夹切换 Tab
    val foldersState by favoriteViewModel?.folders?.collectAsState() 
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyList()) }
    val selectedFolderIndex by favoriteViewModel?.selectedFolderIndex?.collectAsState() 
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val favoriteContentMode = resolveFavoriteContentMode(
        isFavoritePage = favoriteViewModel != null,
        folderCount = foldersState.size
    )
    val selectedFolderUiState by favoriteViewModel
        ?.getFolderUiState(selectedFolderIndex)
        ?.collectAsState()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(ListUiState()) }
    val singleFolderUiState by favoriteViewModel
        ?.getFolderUiState(0)
        ?.collectAsState()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(ListUiState()) }
    val activeFavoriteItems = resolveFavoritePlayAllItems(
        mode = favoriteContentMode,
        baseItems = state.items,
        selectedFolderItems = selectedFolderUiState.items,
        singleFolderItems = singleFolderUiState.items
    )
    
    // [新增] Pager State (仅当有多个文件夹时使用)
    // 尽管 compose 会自动处理 rememberKey，但这里用 foldersState.size 作为 key 确保变化时重置
    val pagerState = rememberPagerState(initialPage = 0) {
        if (favoriteViewModel != null && foldersState.size > 1) foldersState.size else 0
    }
    
    // [Fix] 协程作用域 (用于 UI 事件触发的滚动)
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    
    // [Fix] 这里的模糊冲突核心：顶栏需要自己的独立 HazeState
    val localHazeState = androidx.compose.runtime.remember { HazeState() }
    
    // 🔍 搜索状态
    var searchQuery by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    // [New] 动态顶栏高度测量 (最准确的方式)
    var headerHeightPx by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val headerHeightDp = with(LocalDensity.current) { headerHeightPx.toDp() }
    
    // [Feature] Header Blur Optimization
    val isHeaderBlurEnabled by SettingsManager.getHeaderBlurEnabled(context).collectAsState(initial = true)
    val blurIntensity = currentUnifiedBlurIntensity()
    val backgroundAlpha = BlurStyles.getBackgroundAlpha(blurIntensity)
    
    // 决定顶栏背景 (使用私有的 localHazeState)
    val topBarBackgroundModifier = if (isHeaderBlurEnabled) {
        Modifier
            .fillMaxWidth()
            .unifiedBlur(localHazeState)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = backgroundAlpha))
    } else {
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    }

    val playFavoriteVideo: (List<VideoItem>, String, Long) -> Unit =
        { items, bvid, cid ->
            val externalPlaylist = buildExternalPlaylistFromFavorite(
                items = items,
                clickedBvid = bvid
            )
            if (externalPlaylist != null) {
                PlaylistManager.setExternalPlaylist(
                    externalPlaylist.playlistItems,
                    externalPlaylist.startIndex,
                    source = ExternalPlaylistSource.FAVORITE
                )
                PlaylistManager.setPlayMode(PlayMode.SEQUENTIAL)
            }
            onVideoClick(bvid, cid)
        }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. 底层：内容区域
            // [Haze Audit] 全局源已在 AppNavigation 根层提供，这里仅保留本地源
            val contentModifier = Modifier
                .fillMaxSize()
                .hazeSource(state = localHazeState)

            Box(modifier = contentModifier) {
                when (favoriteContentMode) {
                    FavoriteContentMode.PAGER -> {
                        val favoriteVm = requireNotNull(favoriteViewModel)
                        // [Feature] 联动 Pager -> ViewModel
                        // 仅当 isUserAction 为 true 时才允许 Pager 驱动 ViewModel 变更
                        var isUserAction by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                        LaunchedEffect(pagerState) {
                            pagerState.interactionSource.interactions.collect { interaction ->
                                if (interaction is androidx.compose.foundation.interaction.DragInteraction.Start) {
                                    isUserAction = true
                                }
                            }
                        }

                        LaunchedEffect(pagerState) {
                            snapshotFlow { pagerState.settledPage }
                                .collect { page ->
                                    if (isUserAction) {
                                        favoriteVm.switchFolder(page)
                                        isUserAction = false
                                    }
                                }
                        }

                        // 联动 ViewModel -> Pager (Tab click)
                        LaunchedEffect(selectedFolderIndex) {
                            if (pagerState.currentPage != selectedFolderIndex) {
                                pagerState.animateScrollToPage(selectedFolderIndex)
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1 // 预加载
                        ) { page ->
                            // 获取当前页面的状态
                            val folderUiState by favoriteVm.getFolderUiState(page).collectAsState()

                            // 确保数据加载
                            LaunchedEffect(page) {
                                favoriteVm.loadFolder(page)
                            }

                            // 渲染通用列表内容 (复用下方逻辑，提取为组件)
                            CommonListContent(
                                items = folderUiState.items,
                                isLoading = folderUiState.isLoading,
                                error = folderUiState.error,
                                searchQuery = searchQuery,
                                columns = columns,
                                spacing = spacing.medium,
                                padding = PaddingValues(top = headerHeightDp, bottom = scaffoldPadding.calculateBottomPadding()),
                                cardAnimationEnabled = homeSettings.cardAnimationEnabled,
                                cardTransitionEnabled = homeSettings.cardTransitionEnabled,
                                cardMotionTier = cardMotionTier,
                                onVideoClick = { bvid, cid ->
                                    playFavoriteVideo(folderUiState.items, bvid, cid)
                                },
                                onLoadMore = { favoriteVm.loadMoreForFolder(page) },
                                onUnfavorite = { video -> favoriteVm.removeVideo(video) }
                            )
                        }
                    }

                    FavoriteContentMode.SINGLE_FOLDER -> {
                        val favoriteVm = requireNotNull(favoriteViewModel)
                        val folderUiState by favoriteVm.getFolderUiState(0).collectAsState()
                        LaunchedEffect(favoriteVm) {
                            favoriteVm.loadFolder(0)
                        }
                        CommonListContent(
                            items = folderUiState.items,
                            isLoading = folderUiState.isLoading,
                            error = folderUiState.error,
                            searchQuery = searchQuery,
                            columns = columns,
                            spacing = spacing.medium,
                            padding = PaddingValues(top = headerHeightDp, bottom = scaffoldPadding.calculateBottomPadding()),
                            cardAnimationEnabled = homeSettings.cardAnimationEnabled,
                            cardTransitionEnabled = homeSettings.cardTransitionEnabled,
                            cardMotionTier = cardMotionTier,
                            onVideoClick = { bvid, cid ->
                                playFavoriteVideo(folderUiState.items, bvid, cid)
                            },
                            onLoadMore = { favoriteVm.loadMoreForFolder(0) },
                            onUnfavorite = { video -> favoriteVm.removeVideo(video) }
                        )
                    }

                    FavoriteContentMode.BASE_LIST -> CommonListContent(
                        items = state.items,
                        isLoading = state.isLoading,
                        error = state.error,
                        searchQuery = searchQuery,
                        columns = columns,
                        spacing = spacing.medium,
                        padding = PaddingValues(top = headerHeightDp, bottom = scaffoldPadding.calculateBottomPadding()),
                        cardAnimationEnabled = homeSettings.cardAnimationEnabled,
                        cardTransitionEnabled = homeSettings.cardTransitionEnabled,
                        cardMotionTier = cardMotionTier,
                        onVideoClick = { bvid, cid ->
                            if (favoriteViewModel != null) {
                                playFavoriteVideo(state.items, bvid, cid)
                            } else {
                                onVideoClick(bvid, cid)
                            }
                        },
                        onLoadMore = { 
                            favoriteViewModel?.loadMore()
                            historyViewModel?.loadMore()
                        },
                        onUnfavorite = if (favoriteViewModel != null) { 
                            { favoriteViewModel.removeVideo(it) } 
                        } else null,
                        historyDissolvingIds = historyDissolvingIds,
                        historyBatchMode = historyViewModel != null && isHistoryBatchMode,
                        historySelectedKeys = selectedHistoryKeys,
                        resolveHistoryItemKey = if (historyViewModel != null) {
                            { video -> historyViewModel.resolveHistoryRenderKey(video) }
                        } else {
                            { video -> video.bvid.ifBlank { video.id.toString() } }
                        },
                        onHistoryLongDelete = if (historyViewModel != null) {
                            { key ->
                                if (!isHistoryBatchMode) {
                                    pendingHistorySingleDeleteKey = key.takeIf { it.isNotBlank() }
                                }
                            }
                        } else null,
                        onHistoryDissolveComplete = if (historyViewModel != null) {
                            { key -> historyViewModel.completeVideoDissolve(key) }
                        } else null,
                        onHistoryToggleSelect = if (historyViewModel != null) {
                            { key ->
                                if (key.isNotBlank()) {
                                    selectedHistoryKeys = if (key in selectedHistoryKeys) {
                                        selectedHistoryKeys - key
                                    } else {
                                        selectedHistoryKeys + key
                                    }
                                }
                            }
                        } else null
                    )
                }
            }

            // 2. 顶层：悬浮顶栏 (使用 onGloballyPositioned 测量高度)
            Box(
                modifier = topBarBackgroundModifier
                    .zIndex(1f)
                    .align(Alignment.TopCenter)
                    .onGloballyPositioned { coordinates ->
                        headerHeightPx = coordinates.size.height
                    }
            ) {
                Column {
                    TopAppBar(
                        title = { Text(state.title) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(CupertinoIcons.Default.ChevronBackward, contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (favoriteViewModel != null) {
                                IconButton(
                                    enabled = activeFavoriteItems.isNotEmpty(),
                                    onClick = {
                                        val externalPlaylist = buildExternalPlaylistFromFavorite(
                                            items = activeFavoriteItems,
                                            clickedBvid = activeFavoriteItems.firstOrNull()?.bvid
                                        ) ?: return@IconButton

                                        PlaylistManager.setExternalPlaylist(
                                            externalPlaylist.playlistItems,
                                            externalPlaylist.startIndex,
                                            source = ExternalPlaylistSource.FAVORITE
                                        )
                                        PlaylistManager.setPlayMode(PlayMode.SEQUENTIAL)

                                        val startItem = activeFavoriteItems
                                            .getOrNull(externalPlaylist.startIndex)
                                            ?: return@IconButton
                                        onPlayAllAudioClick?.invoke(startItem.bvid, startItem.cid)
                                            ?: onVideoClick(startItem.bvid, startItem.cid)
                                    }
                                ) {
                                    Icon(
                                        imageVector = CupertinoIcons.Outlined.Headphones,
                                        contentDescription = "全部听"
                                    )
                                }
                            }

                            if (historyViewModel != null && state.items.isNotEmpty()) {
                                if (isHistoryBatchMode) {
                                    val allSelected = selectedHistoryKeys.size == state.items.size
                                    TextButton(
                                        onClick = {
                                            selectedHistoryKeys = if (allSelected) {
                                                emptySet()
                                            } else {
                                                state.items.map(historyViewModel::resolveHistoryRenderKey).toSet()
                                            }
                                        }
                                    ) {
                                        Text(if (allSelected) "取消全选" else "全选")
                                    }
                                    TextButton(
                                        enabled = selectedHistoryKeys.isNotEmpty(),
                                        onClick = { showHistoryBatchDeleteConfirm = true }
                                    ) {
                                        Text("删除(${selectedHistoryKeys.size})")
                                    }
                                    TextButton(
                                        onClick = {
                                            isHistoryBatchMode = false
                                            selectedHistoryKeys = emptySet()
                                        }
                                    ) {
                                        Text("完成")
                                    }
                                } else {
                                    TextButton(
                                        onClick = {
                                            isHistoryBatchMode = true
                                            selectedHistoryKeys = emptySet()
                                        }
                                    ) {
                                        Text("批量删除")
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent
                        ),
                        scrollBehavior = scrollBehavior
                    )
                    
                    // 🔍 搜索栏
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        com.android.purebilibili.core.ui.components.IOSSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = "搜索视频",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
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
                                        // 
                                        scope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
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
        }
    }

    if (showHistoryBatchDeleteConfirm && historyViewModel != null) {
        AlertDialog(
            onDismissRequest = { showHistoryBatchDeleteConfirm = false },
            title = { Text("批量删除历史") },
            text = { Text("确认删除已选择的 ${selectedHistoryKeys.size} 条历史记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        historyViewModel.startBatchVideoDissolve(selectedHistoryKeys)
                        selectedHistoryKeys = emptySet()
                        isHistoryBatchMode = false
                        showHistoryBatchDeleteConfirm = false
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHistoryBatchDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (pendingHistorySingleDeleteKey != null && historyViewModel != null) {
        AlertDialog(
            onDismissRequest = { pendingHistorySingleDeleteKey = null },
            title = { Text("删除历史记录") },
            text = { Text("确认删除这条历史记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingHistorySingleDeleteKey?.let { historyViewModel.startVideoDissolve(it) }
                        pendingHistorySingleDeleteKey = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingHistorySingleDeleteKey = null }) {
                    Text("取消")
                }
            }
        )
    }
}

// 提取通用列表内容组件
@Composable
fun CommonListContent(
    items: List<com.android.purebilibili.data.model.response.VideoItem>,
    isLoading: Boolean,
    error: String?,
    searchQuery: String,
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp,
    padding: PaddingValues,
    cardAnimationEnabled: Boolean,
    cardTransitionEnabled: Boolean,
    cardMotionTier: MotionTier,
    onVideoClick: (String, Long) -> Unit,
    onLoadMore: () -> Unit,
    onUnfavorite: ((com.android.purebilibili.data.model.response.VideoItem) -> Unit)?,
    historyDissolvingIds: Set<String> = emptySet(),
    historyBatchMode: Boolean = false,
    historySelectedKeys: Set<String> = emptySet(),
    resolveHistoryItemKey: (com.android.purebilibili.data.model.response.VideoItem) -> String = { video ->
        video.bvid.ifBlank { video.id.toString() }
    },
    onHistoryLongDelete: ((String) -> Unit)? = null,
    onHistoryDissolveComplete: ((String) -> Unit)? = null,
    onHistoryToggleSelect: ((String) -> Unit)? = null
) {
    if (isLoading && items.isEmpty()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(
                start = spacing,
                end = spacing,
                top = padding.calculateTopPadding() + spacing,
                bottom = padding.calculateBottomPadding() + spacing
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
            modifier = Modifier.fillMaxSize()
        ) {
            items(columns * 4) { VideoGridItemSkeleton() }
        }
    } else if (error != null && items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = error, color = Color.Gray)
        }
    } else if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
             Text("暂无数据", color = Color.Gray)
        }
    } else {
        val filteredItems = androidx.compose.runtime.remember(items, searchQuery) {
            if (searchQuery.isBlank()) items
            else {
                items.filter { 
                    PinyinUtils.matches(it.title, searchQuery) ||
                    PinyinUtils.matches(it.owner.name, searchQuery)
                }
            }
        }

        if (filteredItems.isEmpty() && searchQuery.isNotEmpty()) {
             Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("没有找到相关视频", color = Color.Gray)
             }
        } else {
            val gridState = rememberLazyGridState()
            
            // 自动加载更多
            val shouldLoadMore = androidx.compose.runtime.remember {
                androidx.compose.runtime.derivedStateOf {
                    val layoutInfo = gridState.layoutInfo
                    val total = layoutInfo.totalItemsCount
                    val last = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    total > 0 && last >= total - 4
                }
            }
            LaunchedEffect(shouldLoadMore.value) {
                if (shouldLoadMore.value) onLoadMore()
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = gridState,
                contentPadding = PaddingValues(
                    start = spacing,
                    end = spacing,
                    top = padding.calculateTopPadding() + spacing,
                    bottom = padding.calculateBottomPadding() + spacing + 80.dp 
                ),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalArrangement = Arrangement.spacedBy(spacing),
                modifier = Modifier.fillMaxSize()
            ) {
                 itemsIndexed(
                    items = filteredItems,
                    key = { _, item -> resolveHistoryItemKey(item) }
                ) { index, video ->
                    val historyKey = resolveHistoryItemKey(video)
                    val supportsHistoryDissolve = onHistoryLongDelete != null && onHistoryDissolveComplete != null
                    val isDissolving = supportsHistoryDissolve && historyKey in historyDissolvingIds
                    val isSelected = historyBatchMode && historyKey in historySelectedKeys

                    val cardContent: @Composable () -> Unit = {
                        Box {
                            ElegantVideoCard(
                                video = video,
                                index = index,
                                animationEnabled = cardAnimationEnabled,
                                motionTier = cardMotionTier,
                                transitionEnabled = cardTransitionEnabled,
                                onClick = { bvid, cid ->
                                    if (historyBatchMode) {
                                        onHistoryToggleSelect?.invoke(historyKey)
                                    } else {
                                        onVideoClick(bvid, cid)
                                    }
                                },
                                onUnfavorite = if (onUnfavorite != null) { { onUnfavorite(video) } } else null,
                                onLongClick = if (!historyBatchMode && supportsHistoryDissolve) {
                                    { onHistoryLongDelete?.invoke(historyKey) }
                                } else null
                            )

                            if (historyBatchMode) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .background(
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                            } else {
                                                Color.Transparent
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                )
                                Icon(
                                    imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = if (isSelected) "已选择" else "未选择",
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                )
                            }
                        }
                    }

                    if (supportsHistoryDissolve) {
                        DissolvableVideoCard(
                            isDissolving = isDissolving,
                            onDissolveComplete = { onHistoryDissolveComplete?.invoke(historyKey) },
                            cardId = historyKey,
                            preset = DissolveAnimationPreset.TELEGRAM_FAST,
                            modifier = Modifier.jiggleOnDissolve(historyKey)
                        ) {
                            cardContent()
                        }
                    } else {
                        cardContent()
                    }
                }
            }
        }
    }
}
