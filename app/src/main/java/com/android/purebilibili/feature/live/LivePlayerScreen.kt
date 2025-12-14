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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
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
    
    // 🔥 创建带 Referer 的数据源
    val dataSourceFactory = remember {
        DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(mapOf(
                "Referer" to "https://live.bilibili.com",
                "User-Agent" to "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36"
            ))
    }
    
    // 🔥 ExoPlayer 实例 - 使用自定义数据源
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build().apply {
                playWhenReady = true
            }
    }
    
    // 🔥 播放直播流
    fun playLiveStream(url: String) {
        Logger.d(TAG, "Playing live stream: $url")
        
        // 🔥 根据 URL 后缀判断格式并创建合适的 MediaSource
        val mediaSource = if (url.contains(".m3u8") || url.contains("hls")) {
            // HLS 格式
            HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(url))
        } else {
            // FLV 或其他格式 - 让 ExoPlayer 自动识别
            DefaultMediaSourceFactory(dataSourceFactory)
                .createMediaSource(MediaItem.Builder()
                    .setUri(url)
                    .setMimeType("video/x-flv")  // 🔥 明确指定 FLV MIME 类型
                    .build())
        }
        
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
    }
    
    // 🔥 加载直播流 - 使用 ViewModel
    LaunchedEffect(roomId) {
        viewModel.loadLiveStream(roomId)
    }
    
    // 🔥 监听 ViewModel 状态变化，播放新 URL
    LaunchedEffect(uiState) {
        val state = uiState
        if (state is LivePlayerState.Success) {
            playLiveStream(state.playUrl)
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
    
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            // 🔥 恢复默认方向，避免离开直播后卡在横屏
            (context as? Activity)?.requestedOrientation = 
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    
    // 🔥 返回处理
    BackHandler { onBack() }
    
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
                        imageVector = Icons.Default.PlayArrow,
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
                    imageVector = Icons.Default.ArrowBack,
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
                if (uname.isNotEmpty()) {
                    Text(
                        text = uname,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
            
            // 🔥 画质选择按钮
            val successState = uiState as? LivePlayerState.Success
            if (successState != null && successState.qualityList.isNotEmpty()) {
                val currentQualityLabel = successState.qualityList.find { 
                    it.qn == successState.currentQuality 
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
            
            // 刷新按钮
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.clickable {
                    viewModel.retry()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "刷新",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
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
                                Icons.Default.Check,
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
