// 文件路径: feature/video/MiniPlayerManager.kt
package com.android.purebilibili.feature.video

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import com.android.purebilibili.core.util.Logger
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaSession
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import coil.transform.RoundedCornersTransformation
import com.android.purebilibili.R
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.util.FormatUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "MiniPlayerManager"
private const val NOTIFICATION_ID = 1002
private const val CHANNEL_ID = "mini_player_channel"
private const val THEME_COLOR = 0xFFFB7299.toInt()

/**
 * 🔥 全局小窗管理器
 * 
 * 负责管理跨导航的视频播放状态，支持：
 * 1. 在视频详情页和首页之间保持播放连续性
 * 2. 小窗模式下的播放控制
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MiniPlayerManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: MiniPlayerManager? = null

        fun getInstance(context: Context): MiniPlayerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MiniPlayerManager(context.applicationContext).also { 
                    INSTANCE = it 
                }
            }
        }
    }

    // --- 协程作用域 ---
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // --- 播放器状态 (可观察) ---
    var isActive by mutableStateOf(false)
        private set
    
    var isMiniMode by mutableStateOf(false)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var currentPosition by mutableLongStateOf(0L)
        private set

    var duration by mutableLongStateOf(0L)
        private set
    
    var progress by mutableFloatStateOf(0f)
        private set

    // --- 当前视频信息 ---
    var currentBvid by mutableStateOf<String?>(null)
        private set

    var currentTitle by mutableStateOf("")
        private set

    var currentCover by mutableStateOf("")
        private set

    var currentOwner by mutableStateOf("")
        private set
    
    // 🔥🔥 [新增] 当前视频的 cid，用于弹幕加载
    var currentCid by mutableLongStateOf(0L)
        private set
    
    // 🔥🔥 [新增] 缓存的视频详情页 UI 状态，用于从小窗返回时恢复
    var cachedUiState: PlayerUiState.Success? = null
        private set
    
    // 🔥🔥 [新增] 缓存 UI 状态
    fun cacheUiState(state: PlayerUiState.Success) {
        cachedUiState = state
        com.android.purebilibili.core.util.Logger.d(TAG, "✅ 缓存 UI 状态: ${state.info.title}")
    }
    
    // 🔥🔥 [新增] 获取并清除缓存的 UI 状态
    fun consumeCachedUiState(): PlayerUiState.Success? {
        val state = cachedUiState
        // 不清除缓存，允许多次复用
        return state
    }

    // --- ExoPlayer 实例 ---
    private var _player: ExoPlayer? = null
    // 🔥 外部播放器引用（来自 VideoDetailScreen 的 VideoPlayerState）
    private var _externalPlayer: ExoPlayer? = null
    // 🔥 优先使用外部播放器（如果存在）
    val player: ExoPlayer?
        get() = _externalPlayer ?: _player
    
    // 🔥🔥 [修复2] 检查是否有外部播放器
    val hasExternalPlayer: Boolean
        get() = _externalPlayer != null
    
    // 🔥🔥 [修复2] 清除外部播放器引用（从小窗返回全屏时调用）
    fun resetExternalPlayer() {
        Logger.d(TAG, "🔥 resetExternalPlayer: clearing external player reference")
        _externalPlayer = null
    }

    // --- MediaSession ---
    private var mediaSession: MediaSession? = null


    /**
     * 初始化播放器（如果尚未初始化）
     */
    fun ensurePlayer(): ExoPlayer {
        if (_player == null) {
            Logger.d(TAG, "Creating new ExoPlayer instance")
            
            val headers = mapOf(
                "Referer" to "https://www.bilibili.com",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            )
            val dataSourceFactory = OkHttpDataSource.Factory(NetworkModule.okHttpClient)
                .setDefaultRequestProperties(headers)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()

            _player = ExoPlayer.Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build()
                .apply {
                    addListener(playerListener)
                    prepare()
                }
            
            // 创建 MediaSession
            val sessionIntent = Intent(context, VideoActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, sessionIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            mediaSession = MediaSession.Builder(context, _player!!)
                .setSessionActivity(pendingIntent)
                .build()
        }
        return _player!!
    }


    /**
     * 开始播放新视频
     */
    fun startVideo(
        bvid: String,
        title: String,
        cover: String,
        owner: String,
        videoUrl: String,
        audioUrl: String?
    ) {
        Logger.d(TAG, "startVideo: bvid=$bvid, title=$title")
        
        ensurePlayer()
        
        // 如果是同一个视频，不重新加载
        if (currentBvid == bvid && _player?.isPlaying == true) {
            Logger.d(TAG, "Same video already playing, skip reload")
            return
        }

        currentBvid = bvid
        currentTitle = title
        currentCover = cover
        currentOwner = owner
        isActive = true
        isMiniMode = false

        // 构建媒体源
        val headers = mapOf(
            "Referer" to "https://www.bilibili.com",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        )
        val dataSourceFactory = OkHttpDataSource.Factory(NetworkModule.okHttpClient)
            .setDefaultRequestProperties(headers)

        val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoUrl))

        if (audioUrl != null) {
            val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(audioUrl))
            val mergedSource = MergingMediaSource(videoSource, audioSource)
            _player?.setMediaSource(mergedSource)
        } else {
            _player?.setMediaSource(videoSource)
        }

        _player?.prepare()
        _player?.playWhenReady = true

        // 更新媒体元数据
        updateMediaMetadata(title, owner, cover)
    }

    /**
     * 进入小窗模式
     */
    fun enterMiniMode() {
        Logger.d(TAG, "🔥 enterMiniMode called: isActive=$isActive, currentBvid=$currentBvid, isMiniMode=$isMiniMode")
        if (!isActive) {
            com.android.purebilibili.core.util.Logger.w(TAG, "⚠️ Cannot enter mini mode: isActive is false!")
            return
        }
        Logger.d(TAG, "✅ Entering mini mode for video: $currentTitle")
        isMiniMode = true
        // 继续播放
    }

    /**
     * 退出小窗模式（返回全屏详情页）
     */
    fun exitMiniMode() {
        Logger.d(TAG, "Exiting mini mode")
        isMiniMode = false
    }

    /**
     * 停止播放并关闭小窗
     */
    fun dismiss() {
        Logger.d(TAG, "Dismissing mini player")
        isMiniMode = false
        isActive = false
        // 🔥 不释放 player，因为它属于 VideoPlayerState
        _externalPlayer = null
        currentBvid = null
        
        // 清除通知
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    /**
     * 🔥 设置视频信息并关联外部播放器（用于小窗模式）
     * 这个方法不创建新播放器，而是使用 VideoDetailScreen 的播放器
     */
    fun setVideoInfo(
        bvid: String,
        title: String,
        cover: String,
        owner: String,
        cid: Long,  // 🔥🔥 [新增] cid 用于弹幕加载
        externalPlayer: ExoPlayer
    ) {
        Logger.d(TAG, "setVideoInfo: bvid=$bvid, title=$title, cid=$cid")
        currentBvid = bvid
        currentTitle = title
        currentCover = cover
        currentOwner = owner
        currentCid = cid  // 🔥🔥 保存 cid
        _externalPlayer = externalPlayer
        isActive = true
        isMiniMode = false
        
        // 同步播放状态
        isPlaying = externalPlayer.isPlaying
        duration = externalPlayer.duration.coerceAtLeast(0L)
    }

    /**
     * 暂停/播放切换
     */
    fun togglePlayPause() {
        val currentPlayer = player ?: return
        if (currentPlayer.isPlaying) {
            currentPlayer.pause()
        } else {
            currentPlayer.play()
        }
    }

    /**
     * Seek 到指定位置
     */
    fun seekTo(position: Long) {
        player?.seekTo(position)
    }


    /**
     * 释放所有资源
     */
    fun release() {
        Logger.d(TAG, "Releasing all resources")
        dismiss()
        mediaSession?.release()
        mediaSession = null
        _player?.removeListener(playerListener)
        _player?.release()
        _player = null
        INSTANCE = null
    }

    // --- 播放器监听器 ---
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing
            Logger.d(TAG, "isPlaying changed: $playing")
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    duration = _player?.duration ?: 0L
                    Logger.d(TAG, "Player ready, duration=$duration")
                }
                Player.STATE_ENDED -> {
                    Logger.d(TAG, "Playback ended")
                }
            }
        }
    }

    /**
     * 更新媒体元数据和通知
     */
    private fun updateMediaMetadata(title: String, artist: String, coverUrl: String) {
        val currentItem = _player?.currentMediaItem ?: return

        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(Uri.parse(FormatUtils.fixImageUrl(coverUrl)))
            .setDisplayTitle(title)
            .setIsPlayable(true)
            .build()

        val newItem = currentItem.buildUpon()
            .setMediaMetadata(metadata)
            .build()

        _player?.replaceMediaItem(_player?.currentMediaItemIndex ?: 0, newItem)

        // 异步加载封面并推送通知
        scope.launch(Dispatchers.IO) {
            val bitmap = loadBitmap(coverUrl)
            launch(Dispatchers.Main) {
                pushNotification(title, artist, bitmap)
            }
        }
    }

    private suspend fun loadBitmap(url: String): Bitmap? {
        return try {
            val loader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(FormatUtils.fixImageUrl(url))
                .allowHardware(false)
                .scale(Scale.FILL)
                .transformations(RoundedCornersTransformation(16f))
                .size(512, 512)
                .build()
            val result = loader.execute(request)
            (result as? SuccessResult)?.drawable?.let { (it as BitmapDrawable).bitmap }
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.e(TAG, "Failed to load bitmap", e)
            null
        }
    }

    private fun pushNotification(title: String, artist: String, bitmap: Bitmap?) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(CHANNEL_ID, "小窗播放", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "小窗播放控制"
                    setShowBadge(false)
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        val style = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession?.sessionCompatToken)
            .setShowActionsInCompactView(0)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(artist)
            .setLargeIcon(bitmap)
            .setStyle(style)
            .setColor(THEME_COLOR)
            .setColorized(true)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setContentIntent(mediaSession?.sessionActivity)

        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            com.android.purebilibili.core.util.Logger.e(TAG, "Failed to show notification", e)
        }
    }
}
