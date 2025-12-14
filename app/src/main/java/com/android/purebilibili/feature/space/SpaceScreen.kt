package com.android.purebilibili.feature.space

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.*
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import com.android.purebilibili.core.util.iOSTapEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceScreen(
    mid: Long,
    onBack: () -> Unit,
    onVideoClick: (String) -> Unit,
    viewModel: SpaceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(mid) {
        viewModel.loadSpaceInfo(mid)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("空间", maxLines = 1, overflow = TextOverflow.Ellipsis)  // 🔥 简化标题，避免重复
                },
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
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is SpaceUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CupertinoActivityIndicator()
                    }
                }
                
                is SpaceUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("😢", fontSize = 48.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadSpaceInfo(mid) }) {
                                Text("重试")
                            }
                        }
                    }
                }
                
                is SpaceUiState.Success -> {
                    SpaceContent(
                        state = state,
                        onVideoClick = onVideoClick,
                        onLoadMore = { viewModel.loadMoreVideos() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SpaceContent(
    state: SpaceUiState.Success,
    onVideoClick: (String) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // 用户头部信息
        item {
            SpaceHeader(
                userInfo = state.userInfo,
                relationStat = state.relationStat,
                upStat = state.upStat
            )
        }
        
        // 投稿视频标题
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "投稿视频",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "(${state.totalVideos})",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        
        // 视频列表
        items(state.videos, key = { it.bvid }) { video ->
            SpaceVideoItem(video = video, onClick = { onVideoClick(video.bvid) })
        }
        
        // 加载更多
        if (state.hasMoreVideos || state.isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !state.isLoadingMore) { onLoadMore() }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isLoadingMore) {
                        CupertinoActivityIndicator()
                    } else {
                        Text("加载更多", color = BiliPink, fontSize = 14.sp)
                    }
                }
            }
        } else if (state.videos.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("—— 没有更多了 ——", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SpaceHeader(
    userInfo: SpaceUserInfo,
    relationStat: RelationStatData?,
    upStat: UpStatData?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 头图 + 头像
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            // 头图
            if (userInfo.topPhoto.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(FormatUtils.fixImageUrl(userInfo.topPhoto))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // 默认渐变背景
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(BiliPink.copy(alpha = 0.8f), BiliPink.copy(alpha = 0.4f))
                            )
                        )
                )
            }
            
            // 渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface)
                        )
                    )
            )
        }
        
        // 头像和信息
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-30).dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // 头像
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(FormatUtils.fixImageUrl(userInfo.face))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            
            Spacer(Modifier.width(12.dp))
            
            // 用户名和认证
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = userInfo.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // VIP 标签
                    if (userInfo.vip.status == 1 && userInfo.vip.label.text.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = BiliPink,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = userInfo.vip.label.text,
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                
                // 等级
                Text(
                    text = "LV${userInfo.level}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        
        // 签名
        if (userInfo.sign.isNotEmpty()) {
            Text(
                text = userInfo.sign,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-16).dp)
                    .padding(horizontal = 16.dp)
            )
        }
        
        // 数据统计
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = if (userInfo.sign.isNotEmpty()) (-8).dp else (-16).dp)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 关注
            StatItem(label = "关注", value = relationStat?.following ?: 0)
            // 粉丝
            StatItem(label = "粉丝", value = relationStat?.follower ?: 0)
            // 获赞
            StatItem(label = "获赞", value = (upStat?.likes ?: 0).toInt())
            // 播放
            StatItem(label = "播放", value = (upStat?.archive?.view ?: 0).toInt())
        }
        
        HorizontalDivider(
            modifier = Modifier.offset(y = if (userInfo.sign.isNotEmpty()) 0.dp else (-8).dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun StatItem(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = FormatUtils.formatStat(value.toLong()),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SpaceVideoItem(video: SpaceVideoItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .iOSTapEffect(scale = 0.98f) { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 封面
        Box(
            modifier = Modifier
                .width(150.dp)
                .height(94.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(FormatUtils.fixImageUrl(video.pic))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // 时长标签
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = video.length,
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        
        Spacer(Modifier.width(12.dp))
        
        // 信息
        Column(
            modifier = Modifier
                .weight(1f)
                .height(94.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = video.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = FormatUtils.formatStat(video.play.toLong()),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                
                Spacer(Modifier.width(12.dp))
                
                Icon(
                    Icons.Filled.Comment,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = FormatUtils.formatStat(video.comment.toLong()),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
