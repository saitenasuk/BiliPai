package com.android.purebilibili.feature.video.ui.pager

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.ViewInfo
import com.android.purebilibili.feature.video.ui.overlay.PlayerProgress
import com.android.purebilibili.feature.video.ui.components.VideoAspectRatio
import com.android.purebilibili.feature.video.ui.overlay.PortraitFullscreenOverlay
import com.android.purebilibili.feature.video.viewmodel.PlayerUiState
import com.android.purebilibili.feature.video.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 竖屏无缝滑动播放页面 (TikTok Style)
 * 
 * @param initialBvid 初始视频 BVID
 * @param initialInfo 初始视频详情
 * @param recommendations 推荐视频列表
 * @param onBack 返回回调
 * @param onVideoChange 切换视频回调 (当滑动到新视频时通知外部)
 */
import com.android.purebilibili.feature.video.viewmodel.VideoCommentViewModel

@UnstableApi
@Composable
fun PortraitVideoPager(
    initialBvid: String,
    initialInfo: ViewInfo,
    recommendations: List<RelatedVideo>,
    onBack: () -> Unit,
    onVideoChange: (String) -> Unit,
    viewModel: PlayerViewModel,
    commentViewModel: VideoCommentViewModel,
    initialStartPositionMs: Long = 0L,
    onProgressUpdate: (Long) -> Unit = {}
) {
    // 构造页面列表：第一个是当前视频，后续是推荐视频
    val pageItems = remember(initialBvid, recommendations) {
        val list = mutableListOf<Any>()
        // Page 0: Current Video Info
        list.add(initialInfo)
        // Page 1+: Recommended Videos
        list.addAll(recommendations)
        list
    }
    
    val pagerState = rememberPagerState(pageCount = { pageItems.size })
    
    // 监听页面变化，通过 callback 通知外部更新（可选，用于数据同步）
    LaunchedEffect(pagerState.currentPage) {
        val item = pageItems.getOrNull(pagerState.currentPage)
        if (item is RelatedVideo) {
            onVideoChange(item.bvid)
        } else if (item is ViewInfo && pagerState.currentPage == 0) {
            onVideoChange(item.bvid)
        }
    }

    VerticalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) { page ->
        val item = pageItems.getOrNull(page)
        
        if (item != null) {
            // 为每一页创建独立的播放内容
            VideoPageItem(
                item = item,
                isCurrentPage = page == pagerState.currentPage,
                onBack = onBack,
                viewModel = viewModel,
                commentViewModel = commentViewModel,
                startPositionMs = if (page == 0) initialStartPositionMs else 0L,
                onProgressUpdate = if (page == 0) onProgressUpdate else { _ -> } // 暂时只同步主视频，或者也可以同步所有
            )
        }
    }
}

@UnstableApi
@Composable
private fun VideoPageItem(
    item: Any, // ViewInfo or RelatedVideo
    isCurrentPage: Boolean,
    onBack: () -> Unit,
    viewModel: PlayerViewModel,
    commentViewModel: VideoCommentViewModel,
    startPositionMs: Long,
    onProgressUpdate: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 提取统一的视频信息
    val bvid = if (item is ViewInfo) item.bvid else (item as RelatedVideo).bvid
    val aid = if (item is ViewInfo) item.aid else (item as RelatedVideo).aid.toLong() // RelatedVideo aid is Long? No, Video info uses Long. Need to check type. RelatedVideo aid might be Int or Long. Let's cast safely or assume compatibility. Checking view_file output...
    // RelatedVideo likely has 'aid'. ViewInfo has 'aid'.
    
    // 互动显示状态
    var showCommentSheet by remember { mutableStateOf(false) }
    // RelatedVideo 没有 cid，需要后续获取
    val initialCid = if (item is ViewInfo) item.cid else 0L
    
    val title = if (item is ViewInfo) item.title else (item as RelatedVideo).title
    val cover = if (item is ViewInfo) item.pic else (item as RelatedVideo).pic
    val authorName = if (item is ViewInfo) item.owner.name else (item as RelatedVideo).owner.name
    val authorFace = if (item is ViewInfo) item.owner.face else (item as RelatedVideo).owner.face
    val authorMid = if (item is ViewInfo) item.owner.mid else (item as RelatedVideo).owner.mid
    
    // 播放状态
    var isPlaying by remember { mutableStateOf(isCurrentPage) } // 默认进来如果是当前页就播放
    var playUrl by remember { mutableStateOf<String?>(null) }
    var audioUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // 互动状态 (简化版)
    var isLiked by remember { mutableStateOf(false) }
    var isCoined by remember { mutableStateOf(false) }
    var isFavorited by remember { mutableStateOf(false) }
    
    // 创建 ExoPlayer
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 1.0f
            }
    }
    
    // 加载视频地址
    LaunchedEffect(isCurrentPage, bvid) {
        if (!isCurrentPage) {
            exoPlayer.pause()
            return@LaunchedEffect
        }
        
        if (playUrl != null) {
            exoPlayer.play()
            return@LaunchedEffect
        }
        
        isLoading = true
        try {
            // 调用 Repository 获取视频详情和播放流
            // 注意：getVideoDetails 返回 Pair<ViewInfo, PlayUrlData>
            val result = com.android.purebilibili.data.repository.VideoRepository.getVideoDetails(
                 bvid = bvid,
                 aid = aid,
                 targetQuality = 64 // 优先高清
            )
             
            result.fold(
                 onSuccess = { (_, playData) ->
                     // 提取视频流
                     val videoUrl = playData.dash?.video?.firstOrNull()?.baseUrl 
                         ?: playData.durl?.firstOrNull()?.url
                         
                     val audioUrlResult = playData.dash?.audio?.firstOrNull()?.baseUrl
                     
                     if (!videoUrl.isNullOrEmpty()) {
                         playUrl = videoUrl
                         audioUrl = audioUrlResult
                         isLoading = false
                         
                     // 设置 MediaSource
                     // 关键修复：添加 Referer 和 User-Agent 防止 403 Forbidden
                     val headers = mapOf(
                         "Referer" to "https://www.bilibili.com",
                         "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                     )
                     
                     val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                         .setUserAgent(headers["User-Agent"])
                         .setDefaultRequestProperties(headers)
                         
                     val mediaSourceFactory = DefaultMediaSourceFactory(context)
                         .setDataSourceFactory(dataSourceFactory)
                     
                     val videoSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(videoUrl))
                     val finalSource = if (!audioUrlResult.isNullOrEmpty()) {
                         val audioSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(audioUrlResult))
                         MergingMediaSource(videoSource, audioSource)
                     } else {
                         videoSource
                     }
                     
                     exoPlayer.setMediaSource(finalSource)
                     exoPlayer.prepare()
                     
                     // [新增] 应用初始进度 (仅第一次)
                     if (startPositionMs > 0) {
                         com.android.purebilibili.core.util.Logger.d("PortraitVideoPager", "Seeking to start position: ${startPositionMs}ms")
                         exoPlayer.seekTo(startPositionMs)
                     } else {
                         com.android.purebilibili.core.util.Logger.d("PortraitVideoPager", "Start position is 0 or invalid: $startPositionMs")
                     }
                     
                     exoPlayer.play()
                     } else {
                         isLoading = false
                         // Failed to get valid url
                     }
                 },
                 onFailure = {
                     // Error handling
                     isLoading = false
                 }
            )
             
        } catch (e: Exception) {
            isLoading = false
            e.printStackTrace()
        }
    }
    
    // 释放播放器
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    // 提取时长 (如果有)
    // 提取时长 (如果有)
    val initialDuration = if (item is RelatedVideo) {
        item.duration * 1000L
    } else if (item is ViewInfo) {
        (item.pages.firstOrNull()?.duration ?: 0L) * 1000L
    } else {
        0L
    }
    com.android.purebilibili.core.util.Logger.d("PortraitVideoPager", "Initial duration for $bvid: $initialDuration ms (Item type: ${item::class.simpleName})")

    // 提取 Stats
    val stat = if (item is ViewInfo) item.stat else (item as RelatedVideo).stat

    // [新增] 详情页显示状态
    var showDetailSheet by remember { mutableStateOf(false) }

    // [新增] 覆盖层显示状态，默认为显示
    var isOverlayVisible by remember { mutableStateOf(true) }

    // 进度监听
    var progressState by remember { 
        mutableStateOf(PlayerProgress(0, initialDuration, 0)) 
    }
    LaunchedEffect(exoPlayer) {
        var logCount = 0
        while (true) {
            val realDuration = exoPlayer.duration
            var displayDuration = if (realDuration > 0 && realDuration != androidx.media3.common.C.TIME_UNSET) realDuration else initialDuration
            
            if (displayDuration <= 0 && item is ViewInfo) {
                val mainDuration = viewModel.currentPlayer?.duration ?: 0L
                if (mainDuration > 0) {
                     displayDuration = mainDuration
                }
            }
            
            if (logCount % 25 == 0 || (displayDuration > 0 && progressState.duration <= 0)) {
                com.android.purebilibili.core.util.Logger.d("PortraitVideoPager", "Duration update: real=$realDuration, initial=$initialDuration, display=$displayDuration, bvid=$bvid")
            }
            logCount++

            progressState = PlayerProgress(
                current = exoPlayer.currentPosition,
                duration = displayDuration,
                buffered = exoPlayer.bufferedPosition
            )
            
            if (isCurrentPage && exoPlayer.isPlaying) {
                onProgressUpdate(exoPlayer.currentPosition)
            }
            
            delay(200)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { 
                        isOverlayVisible = !isOverlayVisible
                    },
                    onDoubleTap = {
                        if (isPlaying) {
                             exoPlayer.pause()
                             isPlaying = false
                        } else {
                             exoPlayer.play()
                             isPlaying = true
                        }
                    }
                )
            }
    ) {
        // 背景图 (封面)
        if (playUrl == null || isLoading) {
            AsyncImage(
                model = FormatUtils.fixImageUrl(cover),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            if (isLoading && isCurrentPage) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
        } else {
            // 播放器视图
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // 暂停图标
        if (!isPlaying && !isLoading && playUrl != null) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Pause",
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(60.dp),
                tint = Color.White.copy(alpha = 0.8f)
            )
        }
    
        // 覆盖层 (Overlay)
        // 判断当前 Item 是否是 ViewModel 正在持有的视频 (用于获取实时状态)
        val currentUiState = viewModel.uiState.collectAsState().value
        val isCurrentModelVideo = (currentUiState as? PlayerUiState.Success)?.info?.bvid == bvid
        val currentSuccess = currentUiState as? PlayerUiState.Success

        PortraitFullscreenOverlay(
            title = title,
            authorName = authorName,
            authorFace = authorFace,
            isPlaying = isPlaying,
            progress = progressState,
            
            // 互动数据
            statView = if(isCurrentModelVideo && currentSuccess != null) currentSuccess.info.stat.view else stat.view,
            statLike = if(isCurrentModelVideo && currentSuccess != null) currentSuccess.info.stat.like else stat.like,
            statDanmaku = if(isCurrentModelVideo && currentSuccess != null) currentSuccess.info.stat.danmaku else stat.danmaku,
            statReply = if(isCurrentModelVideo && currentSuccess != null) currentSuccess.info.stat.reply else stat.reply,
            statFavorite = if(isCurrentModelVideo && currentSuccess != null) currentSuccess.info.stat.favorite else stat.favorite,
            statShare = if(isCurrentModelVideo && currentSuccess != null) currentSuccess.info.stat.share else stat.share,
            
            // 交互状态
            isLiked = if(isCurrentModelVideo) currentSuccess?.isLiked == true else false,
            isCoined = false,
            isFavorited = if(isCurrentModelVideo) currentSuccess?.isFavorited == true else false,
            
            // 关注状态
            isFollowing = (currentUiState as? PlayerUiState.Success)?.followingMids?.contains(authorMid) == true,
            onFollowClick = { 
                viewModel.toggleFollow(authorMid, (currentUiState as? PlayerUiState.Success)?.followingMids?.contains(authorMid) == true)
            },
            
            // [新增] 详情点击
            onDetailClick = {
                showDetailSheet = true
            },
            
            onLikeClick = { if (isCurrentModelVideo) viewModel.toggleLike() },
            onCoinClick = { },
            onFavoriteClick = { if (isCurrentModelVideo) viewModel.showFavoriteFolderDialog() },
            onCommentClick = { showCommentSheet = true },
            onShareClick = { 
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, "Check out this video: https://www.bilibili.com/video/$bvid")
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share too..."))
            },
            
            // 状态
            currentSpeed = 1.0f,
            currentQualityLabel = "高清",
            currentRatio = VideoAspectRatio.FIT,
            danmakuEnabled = true,
            isStatusBarHidden = true,
            
            // 回调
            onBack = onBack,
            onPlayPause = {
                if (isPlaying) { exoPlayer.pause(); isPlaying = false } 
                else { exoPlayer.play(); isPlaying = true }
            },
            onSeek = { pos -> exoPlayer.seekTo(pos) },
            onSeekStart = { },
            onSpeedClick = { },
            onQualityClick = { },
            onRatioClick = { },
            onDanmakuToggle = { },
            onDanmakuInputClick = { 
                android.widget.Toast.makeText(context, "暂不可用，后续更新", android.widget.Toast.LENGTH_SHORT).show()
            },
            onToggleStatusBar = { },
            
            // [修改] 结合 isOverlayVisible 控制，且这几个 sheet 开启时隐藏 controls
            showControls = isOverlayVisible && !showCommentSheet && !showDetailSheet
        )
        
        // 评论底栏 Sheet
        PortraitCommentSheet(
            visible = showCommentSheet,
            onDismiss = { showCommentSheet = false },
            commentViewModel = commentViewModel,
            aid = aid
        )
        
        // [新增] 简介 Sheet
        PortraitDetailSheet(
            visible = showDetailSheet,
            onDismiss = { showDetailSheet = false },
            info = currentSuccess?.info // 传递完整 info 用于显示简介
        )
    }
}
