// 文件路径: feature/live/LivePlayerScreen.kt
package com.android.purebilibili.feature.live

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import com.android.purebilibili.core.util.Logger
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// 🍎 Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.android.purebilibili.data.model.response.LiveQuality
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import kotlinx.coroutines.launch

private const val TAG = "LivePlayerScreen"

// 🔥 辅助函数：格式化在线人数
private fun formatOnline(num: Int): String {
    return when {
        num >= 10000 -> String.format("%.1f万", num / 10000f)
        else -> num.toString()
    }
}

// 🔥 辅助函数：格式化粉丝数
private fun formatFollowers(num: Long): String {
    return when {
        num >= 10000 -> String.format("%.1f万", num / 10000f)
        else -> num.toString()
    }
}

@OptIn(UnstableApi::class)
@Composable
fun LivePlayerScreen(
    roomId: Long,
    title: String,
    uname: String,
    onBack: () -> Unit,
    viewModel: LivePlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    
    val uiState by viewModel.uiState.collectAsState()
    
    // 🔥 画质菜单状态
    var showQualityMenu by remember { mutableStateOf(false) }
    
    // 🔥 横屏状态
    var isFullscreen by remember { mutableStateOf(false) }
    
    // 🔥 切换横竖屏
    fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        activity?.requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
    
    // 🔥 返回处理 - 横屏时先退出横屏
    BackHandler {
        if (isFullscreen) {
            toggleFullscreen()
        } else {
            onBack()
        }
    }    
    // 🔥🔥 [修复] 创建带 Cookie 认证的数据源 - 解决 403 错误
    val dataSourceFactory = remember(roomId) {
        // 🔥 从 TokenManager 获取 Cookie 信息，构建完整的 Cookie 字符串
        val sessData = com.android.purebilibili.core.store.TokenManager.sessDataCache ?: ""
        val buvid3 = com.android.purebilibili.core.store.TokenManager.buvid3Cache ?: ""
        val cookies = buildString {
            if (sessData.isNotEmpty()) append("SESSDATA=$sessData; ")
            if (buvid3.isNotEmpty()) append("buvid3=$buvid3")
        }.trimEnd(';', ' ')
        Logger.d(TAG, "🔴 Creating dataSource with cookies: ${cookies.take(50)}...")
        
        DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(mapOf(
                "Referer" to "https://live.bilibili.com/$roomId",  // 🔥 使用完整直播间 URL
                "User-Agent" to "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
                "Cookie" to cookies,  // 🔥🔥 关键：添加 Cookie 认证
                "Origin" to "https://live.bilibili.com"
            ))
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
    }
    
    // 🔥 ExoPlayer 实例 - 使用自定义数据源（依赖 dataSourceFactory 重建）
    val exoPlayer = remember(dataSourceFactory) {
        Logger.d(TAG, "🔴 Creating new ExoPlayer instance")
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build().apply {
                playWhenReady = true
            }
    }
    
    // 🔥 播放直播流
    fun playLiveStream(url: String) {
        Logger.d(TAG, "🔴 === playLiveStream called ===")
        Logger.d(TAG, "🔴 URL: $url")
        Logger.d(TAG, "🔴 URL length: ${url.length}")
        Logger.d(TAG, "🔴 URL contains m3u8: ${url.contains(".m3u8")}")
        Logger.d(TAG, "🔴 URL contains hls: ${url.contains("hls")}")
        
        try {
            // 🔥 根据 URL 后缀判断格式并创建合适的 MediaSource
            val mediaSource = if (url.contains(".m3u8") || url.contains("hls")) {
                Logger.d(TAG, "🔴 Creating HLS MediaSource")
                // HLS 格式
                HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(url))
            } else {
                Logger.d(TAG, "🔴 Creating FLV/default MediaSource")
                // FLV 或其他格式 - 让 ExoPlayer 自动识别
                DefaultMediaSourceFactory(dataSourceFactory)
                    .createMediaSource(MediaItem.Builder()
                        .setUri(url)
                        .setMimeType("video/x-flv")  // 🔥 明确指定 FLV MIME 类型
                        .build())
            }
            
            Logger.d(TAG, "🔴 Setting media source...")
            exoPlayer.setMediaSource(mediaSource)
            Logger.d(TAG, "🔴 Calling prepare()...")
            exoPlayer.prepare()
            Logger.d(TAG, "✅ Player prepared successfully")
        } catch (e: Exception) {
            Logger.e(TAG, "❌ Error in playLiveStream: ${e.message}", e)
        }
    }
    
    // 🔥🔥 [改进] ExoPlayer 错误监听器 - 403 错误时自动切换 CDN
    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Logger.e(TAG, "❌ ExoPlayer Error: ${error.message}")
                Logger.e(TAG, "❌ Error code: ${error.errorCode}")
                Logger.e(TAG, "❌ Error cause: ${error.cause?.message}")
                
                // 🔥🔥 [关键修复] 403 错误时自动尝试下一个 CDN
                val cause = error.cause
                if (cause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
                    if (cause.responseCode == 403) {
                        Logger.d(TAG, "🔄 Got 403, trying next CDN...")
                        viewModel.tryNextUrl()
                    }
                }
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateName = when (playbackState) {
                    androidx.media3.common.Player.STATE_IDLE -> "IDLE"
                    androidx.media3.common.Player.STATE_BUFFERING -> "BUFFERING"
                    androidx.media3.common.Player.STATE_READY -> "READY"
                    androidx.media3.common.Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN($playbackState)"
                }
                Logger.d(TAG, "🔴 Player state changed: $stateName")
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Logger.d(TAG, "🔴 isPlaying changed: $isPlaying")
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }
    
    // 🔥 加载直播流 - 使用 ViewModel
    LaunchedEffect(roomId) {
        Logger.d(TAG, "🔴 LaunchedEffect: Loading live stream for roomId=$roomId")
        viewModel.loadLiveStream(roomId)
    }
    
    // 🔥 监听 ViewModel 状态变化，播放新 URL
    LaunchedEffect(uiState) {
        val state = uiState
        Logger.d(TAG, "🔴 uiState changed: ${state::class.simpleName}")
        if (state is LivePlayerState.Success) {
            Logger.d(TAG, "🔴 Success state, playUrl: ${state.playUrl.take(80)}...")
            Logger.d(TAG, "🔴 Current quality: ${state.currentQuality}")
            Logger.d(TAG, "🔴 Quality list count: ${state.qualityList.size}")
            playLiveStream(state.playUrl)
        } else if (state is LivePlayerState.Error) {
            Logger.e(TAG, "❌ Error state: ${state.message}")
        }
    }
    
    // 🔥🔥 [性能优化] 生命周期感知：进入后台时暂停播放，返回前台时继续
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    Logger.d(TAG, "🔴 App entering background, pausing player")
                    exoPlayer.pause()
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    Logger.d(TAG, "🟢 App returning to foreground, resuming player")
                    exoPlayer.play()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // 🔥 清理播放器 + 屏幕常亮管理
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        
        // 🔥🔥 [修复] 进入直播间时保持屏幕常亮，防止自动熄屏
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        onDispose {
            exoPlayer.release()
            // 🔥 恢复默认方向，避免离开直播后卡在横屏
            (context as? Activity)?.requestedOrientation = 
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            
            // 🔥🔥 [修复] 离开直播间时取消屏幕常亮
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 播放器 - 🔥 禁用默认控制器，使用自定义覆盖层
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false  // 🔥 隐藏默认控制器（包含进度条）
                    keepScreenOn = true  // 🔥 确保屏幕常亮
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // 🔥 中心播放/暂停按钮 - 点击切换
        var isPlaying by remember { mutableStateOf(true) }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                        isPlaying = false
                    } else {
                        exoPlayer.play()
                        isPlaying = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // 🔥 只有暂停时显示播放按钮
            if (!isPlaying) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Icon(
                        imageVector = CupertinoIcons.Default.Play,
                        contentDescription = "播放",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(20.dp)
                            .size(48.dp)
                    )
                }
            }
        }
        
        // 🔥 顶部信息
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.clickable { onBack() }
            ) {
                Icon(
                    imageVector = CupertinoIcons.Default.ChevronBackward,
                    contentDescription = "返回",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.ifEmpty { "直播间 $roomId" },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                // 🔥 显示主播名和在线人数
                val successState = uiState as? LivePlayerState.Success
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uname.isNotEmpty()) {
                        Text(
                            text = uname,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                    // 🔥 在线人数
                    if (successState != null && successState.roomInfo.online > 0) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            CupertinoIcons.Default.Eye,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = formatOnline(successState.roomInfo.online),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            // 🔥 画质选择按钮
            val successStateForQuality = uiState as? LivePlayerState.Success
            if (successStateForQuality != null && successStateForQuality.qualityList.isNotEmpty()) {
                val currentQualityLabel = successStateForQuality.qualityList.find { 
                    it.qn == successStateForQuality.currentQuality 
                }?.desc ?: "自动"
                
                Surface(
                    onClick = { showQualityMenu = true },
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = currentQualityLabel,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            
            // 🔥 横屏/全屏按钮
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clickable { toggleFullscreen() }
            ) {
                Icon(
                    imageVector = if (isFullscreen) CupertinoIcons.Default.ArrowDownRightAndArrowUpLeft else CupertinoIcons.Default.ArrowUpLeftAndArrowDownRight,
                    contentDescription = if (isFullscreen) "退出全屏" else "全屏",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
            
            // 刷新按钮
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.clickable {
                    viewModel.retry()
                }
            ) {
                Icon(
                    imageVector = CupertinoIcons.Default.ArrowClockwise,
                    contentDescription = "刷新",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
        
        // 🔥🔥 [新增] 底部主播信息卡片 (竖屏模式)
        if (!isFullscreen) {
            val successState = uiState as? LivePlayerState.Success
            if (successState != null && successState.anchorInfo.uname.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 主播头像
                        AsyncImage(
                            model = successState.anchorInfo.face,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        )
                        
                        Spacer(Modifier.width(12.dp))
                        
                        // 主播信息
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = successState.anchorInfo.uname,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${formatFollowers(successState.anchorInfo.followers)} 粉丝",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                        
                        // 🔥 关注按钮
                        Surface(
                            onClick = { viewModel.toggleFollow() },
                            shape = RoundedCornerShape(18.dp),
                            color = if (successState.isFollowing) 
                                Color.White.copy(alpha = 0.2f) 
                            else 
                                MaterialTheme.colorScheme.primary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (!successState.isFollowing) {
                                    Icon(
                                        CupertinoIcons.Default.Plus,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White
                                    )
                                }
                                Text(
                                    text = if (successState.isFollowing) "已关注" else "关注",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // 🔥 加载中状态
        if (uiState is LivePlayerState.Loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CupertinoActivityIndicator()
            }
        }
        
        // 🔥 错误状态
        if (uiState is LivePlayerState.Error) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = (uiState as LivePlayerState.Error).message,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Row {
                        Button(onClick = { viewModel.retry() }) {
                            Text("重试")
                        }
                        Spacer(Modifier.width(16.dp))
                        OutlinedButton(onClick = onBack) {
                            Text("返回")
                        }
                    }
                }
            }
        }
        
        // 🔥 画质选择菜单
        if (showQualityMenu) {
            val successState = uiState as? LivePlayerState.Success
            if (successState != null) {
                LiveQualityMenu(
                    qualityList = successState.qualityList,
                    currentQuality = successState.currentQuality,
                    onQualitySelected = { qn ->
                        viewModel.changeQuality(qn)
                        showQualityMenu = false
                    },
                    onDismiss = { showQualityMenu = false }
                )
            }
        }
    }
}

/**
 * 🔥 直播画质选择菜单
 */
@Composable
private fun LiveQualityMenu(
    qualityList: List<LiveQuality>,
    currentQuality: Int,
    onQualitySelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 200.dp, max = 280.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = false) {},
            color = Color(0xFF2B2B2B),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "画质选择",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                HorizontalDivider(color = Color.White.copy(0.1f))
                
                qualityList.forEach { quality ->
                    val isSelected = quality.qn == currentQuality
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onQualitySelected(quality.qn) }
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = quality.desc,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(0.9f),
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        if (isSelected) {
                            Icon(
                                CupertinoIcons.Default.Checkmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
