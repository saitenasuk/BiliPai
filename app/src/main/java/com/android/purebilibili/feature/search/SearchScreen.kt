// 文件路径: feature/search/SearchScreen.kt
package com.android.purebilibili.feature.search

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.database.entity.SearchHistory
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.ui.LoadingAnimation
import com.android.purebilibili.feature.home.components.cards.ElegantVideoCard  // 🔥 使用首页卡片
import com.android.purebilibili.core.store.SettingsManager  // 🔥 读取动画设置
import com.android.purebilibili.data.repository.SearchOrder
import com.android.purebilibili.data.repository.SearchDuration
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.core.util.FormatUtils
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.KeyboardArrowDown

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(),
    userFace: String = "",
    onBack: () -> Unit,
    onVideoClick: (String, Long) -> Unit,
    onAvatarClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // 1. 滚动状态监听 (用于列表)
    val historyListState = rememberLazyListState()
    val resultGridState = rememberLazyGridState()

    // 2. 顶部避让高度计算
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density).let { with(density) { it.toDp() } }
    val topBarHeight = 64.dp // 搜索栏高度
    val contentTopPadding = statusBarHeight + topBarHeight
    
    // 🔥 读取动画设置开关
    val context = LocalContext.current
    val cardAnimationEnabled by SettingsManager.getCardAnimationEnabled(context).collectAsState(initial = true)
    val cardTransitionEnabled by SettingsManager.getCardTransitionEnabled(context).collectAsState(initial = false)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        // 🔥 移除 bottomBar，搜索栏现在位于顶部 Box 中
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            // --- 列表内容层 ---
            if (state.showResults) {
                if (state.isSearching) {
                    // 🔥 使用 Lottie 加载动画
                    LoadingAnimation(
                        modifier = Modifier.align(Alignment.Center),
                        size = 80.dp,
                        text = "搜索中..."
                    )
                } else if (state.error != null) {
                    Text(
                        text = state.error ?: "未知错误",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 🔥 筛选条件栏
                        Spacer(modifier = Modifier.height(contentTopPadding + 4.dp))
                        SearchFilterBar(
                            currentOrder = state.searchOrder,
                            currentDuration = state.searchDuration,
                            onOrderChange = { viewModel.setSearchOrder(it) },
                            onDurationChange = { viewModel.setSearchDuration(it) }
                        )
                        
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = resultGridState,
                            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp, start = 8.dp, end = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(state.searchResults) { index, video ->
                                // 🔥🔥 使用首页卡片组件，支持动画和过渡
                                ElegantVideoCard(
                                    video = video,
                                    index = index,
                                    animationEnabled = cardAnimationEnabled,
                                    transitionEnabled = cardTransitionEnabled,
                                    showPublishTime = true,  // 搜索结果显示发布时间
                                    onClick = { bvid, _ -> onVideoClick(bvid, 0) }
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = historyListState,
                    // 🔥 contentPadding 顶部避让搜索栏
                    contentPadding = PaddingValues(top = contentTopPadding + 16.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                ) {

                    
                    // 🔥 搜索发现 (恢复此板块)
                    item {
                            // 🔥 搜索发现 / 个性化推荐
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "💎",
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        state.discoverTitle, // 🔥 使用动态标题
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                // 刷新按钮
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { /* TODO: Refresh logic */ }
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "换一换",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            // 动态发现内容
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                state.discoverList.forEach { keyword -> // 🔥 使用动态列表
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.clickable { 
                                            viewModel.search(keyword)
                                            keyboardController?.hide() 
                                        }
                                    ) {
                                        Text(
                                            keyword,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                    }

                    if (state.hotList.isNotEmpty()) {
                        item {
                            // 🔥 热搜标题
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "🔥",
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "热门搜索",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 🔥 热搜列表 (双列布局)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                state.hotList.take(10).chunked(2).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowItems.forEachIndexed { indexInRow, hotItem ->
                                            // 计算全局索引
                                            val globalIndex = state.hotList.indexOf(hotItem)
                                            val isTop3 = globalIndex < 3
                                            
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { 
                                                        viewModel.search(hotItem.keyword)
                                                        keyboardController?.hide() 
                                                    },
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // 排名序号
                                                Text(
                                                    text = "${globalIndex + 1}",
                                                    fontSize = 14.sp,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                    color = if (isTop3) BiliPink else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                    modifier = Modifier.width(24.dp)
                                                )
                                                
                                                // 标题
                                                Text(
                                                    text = hotItem.show_name,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                                
                                                // "新"/"热" 标签 (如果有 icon 字段可以判断，这里简化)
                                            }
                                        }
                                        // 如果是奇数个，补一个空位占位
                                        if (rowItems.size < 2) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                    
                    if (state.historyList.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "历史记录",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                TextButton(onClick = { viewModel.clearHistory() }) {
                                    Text("清空", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // 🔥 气泡化历史记录
                        item {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                state.historyList.forEach { history ->
                                    HistoryChip(
                                        keyword = history.keyword,
                                        onClick = { viewModel.search(history.keyword); keyboardController?.hide() },
                                        onDelete = { viewModel.deleteHistory(history) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 🔥 顶部搜索栏 (常驻顶部) ---
            SearchTopBar(
                query = state.query,
                onBack = onBack,
                onQueryChange = { viewModel.onQueryChange(it) },
                onSearch = {
                    viewModel.search(it)
                    keyboardController?.hide()
                },
                onClearQuery = { viewModel.onQueryChange("") },
                modifier = Modifier.align(Alignment.TopCenter)
            )
            
            // --- 🔥 搜索建议下拉列表 ---
            if (state.suggestions.isNotEmpty() && state.query.isNotEmpty() && !state.showResults) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = contentTopPadding + 4.dp)
                        .padding(horizontal = 16.dp)
                        .align(Alignment.TopCenter),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        state.suggestions.forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.search(suggestion)
                                        keyboardController?.hide()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = suggestion,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 🔥 新设计的顶部搜索栏 (含 Focus 高亮动画)
@Composable
fun SearchTopBar(
    query: String,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 🔥 Focus 状态追踪
    var isFocused by remember { mutableStateOf(false) }
    
    // 🔥 边框宽度动画
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "borderWidth"
    )
    
    // 🔥 搜索图标颜色动画
    val searchIconColor by animateColorAsState(
        targetValue = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 200),
        label = "iconColor"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp
    ) {
        Column {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 🔥 搜索输入框 (带 Focus 边框动画)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(50))
                        .border(
                            width = borderWidth,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50)
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        null,
                        tint = searchIconColor,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { isFocused = it.isFocused },
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text(
                                        "搜索视频、UP主...",
                                        style = TextStyle(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                                            fontSize = 15.sp
                                        )
                                    )
                                }
                                inner()
                            }
                        }
                    )

                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = onClearQuery,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                TextButton(
                    onClick = { onSearch(query) },
                    enabled = query.isNotEmpty()
                ) {
                    Text(
                        "搜索",
                        color = if (query.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

// 🔥 气泡化历史记录组件
@Composable
fun HistoryChip(
    keyword: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .height(36.dp)
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = keyword,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                maxLines = 1
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// 保留旧版 HistoryItem 用于兼容 (可选保留)
@Composable
fun HistoryItem(
    history: SearchHistory,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = history.keyword, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f), modifier = Modifier.size(16.dp))
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
}

/**
 * 🔥 快捷分类入口
 */
@Composable
fun QuickCategory(
    emoji: String,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 22.sp)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 🔥 搜索筛选条件栏
 */
@Composable
fun SearchFilterBar(
    currentOrder: SearchOrder,
    currentDuration: SearchDuration,
    onOrderChange: (SearchOrder) -> Unit,
    onDurationChange: (SearchDuration) -> Unit
) {
    var showOrderMenu by remember { mutableStateOf(false) }
    var showDurationMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 排序选择
        Box {
            Surface(
                onClick = { showOrderMenu = true },
                color = if (currentOrder != SearchOrder.TOTALRANK) 
                    BiliPink.copy(alpha = 0.1f) 
                else 
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentOrder.displayName,
                        fontSize = 13.sp,
                        color = if (currentOrder != SearchOrder.TOTALRANK) 
                            BiliPink 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (currentOrder != SearchOrder.TOTALRANK) 
                            BiliPink 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            DropdownMenu(
                expanded = showOrderMenu,
                onDismissRequest = { showOrderMenu = false }
            ) {
                SearchOrder.entries.forEach { order ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                order.displayName,
                                color = if (order == currentOrder) BiliPink else MaterialTheme.colorScheme.onSurface
                            ) 
                        },
                        onClick = {
                            onOrderChange(order)
                            showOrderMenu = false
                        }
                    )
                }
            }
        }
        
        // 时长选择
        Box {
            Surface(
                onClick = { showDurationMenu = true },
                color = if (currentDuration != SearchDuration.ALL) 
                    BiliPink.copy(alpha = 0.1f) 
                else 
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentDuration.displayName,
                        fontSize = 13.sp,
                        color = if (currentDuration != SearchDuration.ALL) 
                            BiliPink 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (currentDuration != SearchDuration.ALL) 
                            BiliPink 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            DropdownMenu(
                expanded = showDurationMenu,
                onDismissRequest = { showDurationMenu = false }
            ) {
                SearchDuration.entries.forEach { duration ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                duration.displayName,
                                color = if (duration == currentDuration) BiliPink else MaterialTheme.colorScheme.onSurface
                            ) 
                        },
                        onClick = {
                            onDurationChange(duration)
                            showDurationMenu = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * 🔥 搜索结果卡片 (显示发布时间)
 */
@Composable
fun SearchResultCard(
    video: VideoItem,
    index: Int,
    onClick: (String) -> Unit
) {
    val coverUrl = remember(video.bvid) {
        FormatUtils.fixImageUrl(if (video.pic.startsWith("//")) "https:${video.pic}" else video.pic)
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick(video.bvid) }
            .padding(bottom = 8.dp)
    ) {
        // 封面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .crossfade(150)
                    .size(480, 300)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // 底部渐变
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
            
            // 时长标签
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    text = FormatUtils.formatDuration(video.duration),
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
            
            // 播放量
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "▶ ${FormatUtils.formatStat(video.stat.view.toLong())}",
                    color = Color.White,
                    fontSize = 11.sp
                )
                if (video.stat.danmaku > 0) {
                    Text(
                        text = "  💬 ${FormatUtils.formatStat(video.stat.danmaku.toLong())}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 标题
        Text(
            text = video.title,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // UP主 + 发布时间
        Row(
            modifier = Modifier.padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = video.owner.name,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            
            // 🔥 显示发布时间
            if (video.pubdate > 0) {
                Text(
                    text = " · ${FormatUtils.formatPublishTime(video.pubdate)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}