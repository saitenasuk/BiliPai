// 文件路径: feature/bangumi/BangumiScreen.kt
package com.android.purebilibili.feature.bangumi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.purebilibili.core.theme.iOSYellow
import com.android.purebilibili.core.util.FormatUtils  // 🔥🔥 [修复] 导入图片 URL 修复工具
import com.android.purebilibili.data.model.response.BangumiItem
import com.android.purebilibili.data.model.response.BangumiType

/**
 * 番剧主页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BangumiScreen(
    onBack: () -> Unit,
    onBangumiClick: (Long) -> Unit,  // 点击番剧 -> seasonId
    initialType: Int = 1,  // 🔥🔥 [修复] 初始类型：1=番剧 2=电影 等
    viewModel: BangumiViewModel = viewModel()
) {
    val selectedType by viewModel.selectedType.collectAsState()
    
    // 🔥🔥 [修复] 如果初始类型不是番剧，自动切换
    LaunchedEffect(initialType) {
        if (initialType != 1) {
            viewModel.selectType(initialType)
        }
    }
    val listState by viewModel.listState.collectAsState()
    
    // 番剧类型列表
    val types = listOf(
        BangumiType.ANIME,
        BangumiType.GUOCHUANG,
        BangumiType.MOVIE,
        BangumiType.TV_SHOW,
        BangumiType.DOCUMENTARY,
        BangumiType.VARIETY
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("番剧影视") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 类型选择 Tabs
            ScrollableTabRow(
                selectedTabIndex = types.indexOfFirst { it.value == selectedType }.coerceAtLeast(0),
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                divider = {}
            ) {
                types.forEach { type ->
                    Tab(
                        selected = type.value == selectedType,
                        onClick = { viewModel.selectType(type.value) },
                        text = {
                            Text(
                                text = type.label,
                                fontWeight = if (type.value == selectedType) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
            
            // 内容区域
            when (val state = listState) {
                is BangumiListState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is BangumiListState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadBangumiList() }) {
                                Text("重试")
                            }
                        }
                    }
                }
                is BangumiListState.Success -> {
                    BangumiGrid(
                        items = state.items,
                        hasMore = state.hasMore,
                        onLoadMore = { viewModel.loadMore() },
                        onItemClick = { onBangumiClick(it.seasonId) }
                    )
                }
            }
        }
    }
}

/**
 * 番剧网格列表
 */
@Composable
private fun BangumiGrid(
    items: List<BangumiItem>,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onItemClick: (BangumiItem) -> Unit
) {
    val gridState = rememberLazyGridState()
    
    // 加载更多检测
    LaunchedEffect(gridState) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= layoutInfo.totalItemsCount - 6
        }.collect { shouldLoad ->
            if (shouldLoad && hasMore) {
                onLoadMore()
            }
        }
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items) { item ->
            BangumiCard(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
        
        // 加载更多指示器
        if (hasMore) {
            item(span = { GridItemSpan(3) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

/**
 * 番剧卡片
 */
@Composable
private fun BangumiCard(
    item: BangumiItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        // 封面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)  // 3:4 比例
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = FormatUtils.fixImageUrl(item.cover),  // 🔥🔥 [修复] 处理图片 URL
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // 渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 100f
                        )
                    )
            )
            
            // 角标（会员专享等）
            if (item.badge.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    color = when {
                        item.badge.contains("会员") -> MaterialTheme.colorScheme.primary
                        item.badge.contains("独家") -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = item.badge,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
            
            // 底部信息
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                // 评分
                if (item.score.isNotEmpty() && item.score != "0") {
                    Text(
                        text = item.score,
                        color = iOSYellow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // 更新状态
                item.newEp?.indexShow?.let { indexShow ->
                    Text(
                        text = indexShow,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
        }
        
        // 标题
        Text(
            text = item.title,
            modifier = Modifier.padding(top = 6.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}
