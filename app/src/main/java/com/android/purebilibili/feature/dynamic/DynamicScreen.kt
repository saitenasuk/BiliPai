// 文件路径: feature/dynamic/DynamicScreen.kt
package com.android.purebilibili.feature.dynamic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSPink
import com.android.purebilibili.core.ui.EmptyState
import com.android.purebilibili.core.ui.LoadingAnimation
import com.android.purebilibili.core.ui.BiliGradientButton
import com.android.purebilibili.data.model.response.*

/**
 * 🔥 动态页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicScreen(
    viewModel: DynamicViewModel = viewModel(),
    onVideoClick: (String) -> Unit,
    onUserClick: (Long) -> Unit = {},
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density).let { with(density) { it.toDp() } }

    // 触发刷新
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) { viewModel.refresh() }
    }
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) pullRefreshState.endRefresh()
    }
    
    // 加载更多
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 3 && !state.isLoading && state.hasMore
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadMore() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = statusBarHeight + 56.dp + 8.dp,
                    bottom = 80.dp,
                    start = 12.dp,
                    end = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 🔥 未登录提示 - 使用现代化空状态组件
                if (state.items.isEmpty() && !state.isLoading && state.error == null) {
                    item {
                        EmptyState(
                            message = "暂无动态",
                            actionText = "登录后查看关注 UP主 的动态",
                            modifier = Modifier.height(300.dp)
                        )
                    }
                }
                
                // 动态卡片列表
                items(state.items, key = { it.id_str }) { item ->
                    DynamicCard(
                        item = item,
                        onVideoClick = onVideoClick,
                        onUserClick = onUserClick
                    )
                }
                
                // 加载中
                if (state.isLoading && state.items.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingAnimation(size = 40.dp)
                        }
                    }
                }
                
                // 没有更多
                if (!state.hasMore && state.items.isNotEmpty()) {
                    item {
                        Text(
                            "没有更多了",
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
            
            // 顶栏
            DynamicTopBar(
                onBack = onBack,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // 刷新指示器
            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = statusBarHeight + 56.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )
            
            // 错误提示 - 使用现代化按钮
            if (state.error != null && state.items.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    BiliGradientButton(
                        text = "重试",
                        onClick = { viewModel.refresh() }
                    )
                }
            }
        }
    }
}

/**
 * 🔥 顶栏
 */
@Composable
fun DynamicTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density).let { with(density) { it.toDp() } }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 2.dp
    ) {
        Column {
            Spacer(modifier = Modifier.height(statusBarHeight))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "动态",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * 🔥 动态卡片
 */
@Composable
fun DynamicCard(
    item: DynamicItem,
    onVideoClick: (String) -> Unit,
    onUserClick: (Long) -> Unit
) {
    val author = item.modules.module_author
    val content = item.modules.module_dynamic
    val stat = item.modules.module_stat
    val type = DynamicType.fromApiValue(item.type)
    
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "scale")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 用户头部
            if (author != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUserClick(author.mid) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = author.face,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            author.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = if (author.vip?.status == 1) BiliPink else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            author.pub_time,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            
            // 动态内容文字
            content?.desc?.text?.takeIf { it.isNotEmpty() }?.let { text ->
                Text(
                    text,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // 🔥 视频类型动态
            content?.major?.archive?.let { archive ->
                VideoThumbnailCard(
                    archive = archive,
                    onClick = { onVideoClick(archive.bvid) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // 🔥 图片类型动态
            content?.major?.draw?.let { draw ->
                DrawGrid(items = draw.items)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // 🔥 交互统计 (彩色图标)
            if (stat != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    StatButton(icon = Icons.Default.Share, count = stat.forward.count, label = "转发", tintColor = iOSBlue)
                    StatButton(icon = Icons.Default.ChatBubbleOutline, count = stat.comment.count, label = "评论", tintColor = BiliPink)
                    StatButton(icon = Icons.Default.FavoriteBorder, count = stat.like.count, label = "点赞", tintColor = iOSPink)
                }
            }
        }
    }
}

/**
 * 视频缩略图卡片 (带 Referer 头支持)
 */
@Composable
fun VideoThumbnailCard(
    archive: ArchiveMajor,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    // 🔥 修复封面 URL
    val coverUrl = remember(archive.cover) {
        val raw = archive.cover.trim()
        when {
            raw.startsWith("https://") -> raw
            raw.startsWith("http://") -> raw.replace("http://", "https://")
            raw.startsWith("//") -> "https:$raw"
            raw.isNotEmpty() -> "https://$raw"
            else -> ""
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
    ) {
        Row(modifier = Modifier.height(80.dp)) {
            Box(modifier = Modifier.width(140.dp).fillMaxHeight()) {
                if (coverUrl.isNotEmpty()) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(context)
                            .data(coverUrl)
                            .addHeader("Referer", "https://www.bilibili.com/")
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                // 时长
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(archive.duration_text, fontSize = 10.sp, color = Color.White)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    archive.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
                    Text(archive.stat.play, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
                }
            }
        }
    }
}

/**
 * 🔥 图片九宫格 (修复 URL + Referer 头 + 动态比例)
 */
@Composable
fun DrawGrid(items: List<DrawItem>) {
    if (items.isEmpty()) return
    
    val displayItems = items.take(9)
    val columns = when {
        displayItems.size == 1 -> 1
        displayItems.size <= 4 -> 2
        else -> 3
    }
    
    // 单图时使用原始比例，多图时使用正方形
    val singleImageRatio = if (displayItems.size == 1 && displayItems[0].width > 0 && displayItems[0].height > 0) {
        displayItems[0].width.toFloat() / displayItems[0].height.toFloat()
    } else {
        1f
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        displayItems.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { item ->
                    val imageUrl = remember(item.src) {
                        val rawSrc = item.src.trim()
                        when {
                            rawSrc.startsWith("https://") -> rawSrc
                            rawSrc.startsWith("http://") -> rawSrc.replace("http://", "https://")
                            rawSrc.startsWith("//") -> "https:$rawSrc"
                            rawSrc.isNotEmpty() -> "https://$rawSrc"
                            else -> ""
                        }
                    }
                    
                    val context = LocalContext.current
                    val aspectRatio = if (displayItems.size == 1) singleImageRatio else 1f
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(aspectRatio.coerceIn(0.5f, 2f))
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUrl.isNotEmpty()) {
                            // 🔥 使用带 Referer 头的 ImageRequest
                            AsyncImage(
                                model = coil.request.ImageRequest.Builder(context)
                                    .data(imageUrl)
                                    .addHeader("Referer", "https://www.bilibili.com/")
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.BrokenImage,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = Color.Gray.copy(0.5f)
                            )
                        }
                    }
                }
                repeat(columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * 🔥 统计按钮 (彩色图标)
 */
@Composable
fun StatButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    label: String,
    tintColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = tintColor
        )
        if (count > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                if (count >= 10000) "${count / 10000}万" else count.toString(),
                fontSize = 12.sp,
                color = tintColor
            )
        }
    }
}
