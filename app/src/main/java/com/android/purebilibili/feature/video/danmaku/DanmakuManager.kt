// 文件路径: feature/video/danmaku/DanmakuManager.kt
package com.android.purebilibili.feature.video.danmaku

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.android.purebilibili.data.repository.VideoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import master.flame.danmaku.controller.DrawHandler
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.DanmakuTimer
import master.flame.danmaku.danmaku.model.IDanmakus
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.danmaku.model.android.Danmakus
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser
import master.flame.danmaku.ui.widget.DanmakuView

/**
 * 弹幕管理器（单例模式）
 * 
 * 负责：
 * 1. 加载和解析弹幕数据
 * 2. 与 ExoPlayer 同步弹幕播放
 * 3. 管理弹幕视图生命周期
 * 
 * 使用单例模式确保横竖屏切换时保持弹幕状态
 */
class DanmakuManager private constructor(
    private val context: Context,
    private var scope: CoroutineScope
) {
    companion object {
        private const val TAG = "DanmakuManager"
        
        @Volatile
        private var instance: DanmakuManager? = null
        
        /**
         * 获取单例实例
         */
        fun getInstance(context: Context, scope: CoroutineScope): DanmakuManager {
            return instance ?: synchronized(this) {
                instance ?: DanmakuManager(context.applicationContext, scope).also { 
                    instance = it 
                    Log.d(TAG, "🆕 DanmakuManager instance created")
                }
            }
        }
        
        /**
         * 更新 CoroutineScope（用于配置变化时）
         */
        fun updateScope(scope: CoroutineScope) {
            instance?.scope = scope
        }
        
        /**
         * 释放单例实例
         */
        fun clearInstance() {
            instance?.release()
            instance = null
            Log.d(TAG, "🗑️ DanmakuManager instance cleared")
        }
    }
    
    // 视图和上下文
    private var danmakuView: DanmakuView? = null
    private var danmakuContext: DanmakuContext? = null
    private var player: ExoPlayer? = null
    private var playerListener: Player.Listener? = null
    private var loadJob: Job? = null
    
    // 弹幕状态
    private var isReady = false
    private var isPrepared = false
    private var isLoading = false  // 🔥 防止重复加载
    
    // 缓存弹幕数据
    private var cachedDanmakuData: ByteArray? = null
    private var cachedCid: Long = 0L
    private var isDanmakuLoaded = false  // 🔥 标记弹幕是否已加载到视图
    
    // 配置
    val config = DanmakuConfig()
    
    // 便捷属性访问器
    var isEnabled: Boolean
        get() = config.isEnabled
        set(value) {
            config.isEnabled = value
            if (value) show() else hide()
        }
    
    var opacity: Float
        get() = config.opacity
        set(value) = config.updateOpacity(danmakuContext, value)
    
    var fontScale: Float
        get() = config.fontScale
        set(value) = config.updateFontScale(danmakuContext, value)
    
    var speedFactor: Float
        get() = config.speedFactor
        set(value) = config.updateSpeedFactor(danmakuContext, value)
    
    var topMarginPx: Int
        get() = config.topMarginPx
        set(value) = config.updateTopMargin(danmakuContext, value)
    
    /**
     * 初始化弹幕上下文
     */
    private fun initDanmakuContext() {
        if (danmakuContext != null) return
        
        danmakuContext = DanmakuContext.create().also { ctx ->
            config.applyTo(ctx, context)
        }
        Log.d(TAG, "✅ DanmakuContext initialized")
    }
    
    /**
     * 绑定 DanmakuView
     */
    fun attachView(view: DanmakuView) {
        // 如果是同一个视图，跳过
        if (danmakuView === view) {
            Log.d(TAG, "📎 attachView: Same view, skipping")
            return
        }
        
        Log.d(TAG, "📎 attachView: new view, old=${danmakuView != null}")
        
        // 先解绑旧视图
        detachView()
        
        danmakuView = view
        initDanmakuContext()
        
        view.setCallback(object : DrawHandler.Callback {
            override fun prepared() {
                Log.d(TAG, "✅ DanmakuView prepared")
                isPrepared = true
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    player?.let { syncToPosition(it.currentPosition) }
                }
            }
            override fun updateTimer(timer: DanmakuTimer?) {}
            override fun danmakuShown(danmaku: BaseDanmaku?) {}
            override fun drawingFinished() {}
        })
        
        view.enableDanmakuDrawingCache(true)
        
        // 如果有缓存数据，立即解析
        cachedDanmakuData?.let { data ->
            Log.d(TAG, "📎 Found cached danmaku data (${data.size} bytes), parsing...")
            parseDanmaku(data)
        }
    }
    
    /**
     * 解绑 DanmakuView（不释放弹幕数据）
     */
    fun detachView() {
        danmakuView?.let { view ->
            Log.d(TAG, "📎 detachView: Pausing and hiding")
            try {
                view.pause()
                view.hide()
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Error detaching view: ${e.message}")
            }
        }
        danmakuView = null
        isPrepared = false
        isDanmakuLoaded = false  // 🔥 重置标记，新视图需要重新加载
    }
    
    /**
     * 绑定 ExoPlayer
     */
    fun attachPlayer(exoPlayer: ExoPlayer) {
        Log.d(TAG, "🎬 attachPlayer")
        
        // 移除旧监听器
        playerListener?.let { player?.removeListener(it) }
        
        player = exoPlayer
        
        playerListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d(TAG, "🎬 isPlaying=$isPlaying, isPrepared=$isPrepared, isEnabled=${config.isEnabled}")
                if (isPlaying && isPrepared && config.isEnabled) {
                    startDanmaku()
                } else {
                    danmakuView?.pause()
                }
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        if (exoPlayer.isPlaying && isPrepared && config.isEnabled) {
                            startDanmaku()
                        }
                    }
                    Player.STATE_ENDED, Player.STATE_BUFFERING -> {
                        danmakuView?.pause()
                    }
                }
            }
            
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                    Log.d(TAG, "🎬 Seek to ${newPosition.positionMs}ms")
                    danmakuView?.seekTo(newPosition.positionMs)
                }
            }
        }
        
        exoPlayer.addListener(playerListener!!)
    }
    
    /**
     * 加载弹幕数据
     */
    fun loadDanmaku(cid: Long) {
        Log.d(TAG, "📥 loadDanmaku: cid=$cid, cached=$cachedCid, isLoading=$isLoading")
        
        // 🔥 如果正在加载，跳过
        if (isLoading) {
            Log.d(TAG, "📥 Already loading, skipping")
            return
        }
        
        // 🔥 如果是同一个 cid 且弹幕已加载，不需要重新解析
        if (cid == cachedCid && isDanmakuLoaded && danmakuView != null) {
            Log.d(TAG, "📥 Danmaku already loaded for cid=$cid, just sync position")
            // 只需同步位置
            player?.let { syncToPosition(it.currentPosition) }
            return
        }
        
        // 使用缓存数据（新视图绑定时）
        if (cid == cachedCid && cachedDanmakuData != null && danmakuView != null) {
            Log.d(TAG, "📥 Using cached danmaku data")
            scope.launch(Dispatchers.Main) {
                parseDanmaku(cachedDanmakuData!!)
            }
            return
        }
        
        // 需要从网络加载
        isLoading = true
        isDanmakuLoaded = false
        
        loadJob?.cancel()
        loadJob = scope.launch {
            try {
                val rawData = VideoRepository.getDanmakuRawData(cid)
                if (rawData == null || rawData.isEmpty()) {
                    Log.w(TAG, "⚠️ Danmaku data is empty")
                    isLoading = false
                    return@launch
                }
                
                Log.d(TAG, "📥 Raw data loaded: ${rawData.size} bytes")
                
                cachedDanmakuData = rawData
                cachedCid = cid
                
                withContext(Dispatchers.Main) {
                    if (danmakuView != null) {
                        parseDanmaku(rawData)
                    } else {
                        Log.d(TAG, "📥 View not attached, data cached")
                    }
                    isLoading = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load danmaku: ${e.message}", e)
                isLoading = false
            }
        }
    }
    
    private fun parseDanmaku(rawData: ByteArray) {
        val view = danmakuView ?: return
        val ctx = danmakuContext ?: return
        
        Log.d(TAG, "🎯 Parsing danmaku...")
        
        val startTime = System.currentTimeMillis()
        
        // 使用空解析器初始化
        val emptyParser = object : BaseDanmakuParser() {
            override fun parse(): IDanmakus = Danmakus()
        }
        view.prepare(emptyParser, ctx)
        isReady = true
        
        // 后台解析弹幕
        scope.launch(Dispatchers.Default) {
            val danmakuList = DanmakuParser.parse(rawData, ctx)
            Log.d(TAG, "📊 Parsed ${danmakuList.size} danmakus in ${System.currentTimeMillis() - startTime}ms")
            
            withContext(Dispatchers.Main) {
                // 等待 view 准备好
                var attempts = 0
                while (!view.isPrepared && attempts < 10) {
                    delay(20)
                    attempts++
                }
                
                if (view.isPrepared && danmakuView === view) {
                    danmakuList.forEach { view.addDanmaku(it) }
                    Log.d(TAG, "✅ Added ${danmakuList.size} danmakus")
                    isDanmakuLoaded = true  // 🔥 标记已加载
                    
                    // 同步到当前位置
                    if (player?.isPlaying == true && config.isEnabled) {
                        val position = player?.currentPosition ?: 0L
                        view.seekTo(position)
                        view.start()
                        Log.d(TAG, "🚀 Synced to position ${position}ms")
                    }
                } else {
                    Log.w(TAG, "⚠️ View changed or not prepared, skipping add")
                }
            }
        }
        
        // 如果正在播放，启动弹幕
        if (player?.isPlaying == true && config.isEnabled) {
            view.start()
        }
    }
    
    private fun startDanmaku() {
        val view = danmakuView ?: return
        
        Log.d(TAG, "🚀 startDanmaku: isReady=$isReady, isPrepared=$isPrepared")
        
        if (isReady && isPrepared) {
            if (view.visibility != android.view.View.VISIBLE) {
                view.visibility = android.view.View.VISIBLE
            }
            view.show()
            view.start()
            view.resume()
            Log.d(TAG, "✅ Danmaku started")
        }
    }
    
    private fun syncToPosition(positionMs: Long) {
        Log.d(TAG, "🔄 Syncing to ${positionMs}ms")
        danmakuView?.seekTo(positionMs)
        if (player?.isPlaying == true && config.isEnabled) {
            startDanmaku()
        } else {
            danmakuView?.pause()
        }
    }
    
    fun show() {
        val view = danmakuView ?: return
        Log.d(TAG, "👁️ show()")
        
        view.visibility = android.view.View.VISIBLE
        view.show()
        
        if (player?.isPlaying == true && isReady && isPrepared) {
            val position = player?.currentPosition ?: 0L
            view.seekTo(position)
            view.start()
            view.resume()
        }
    }
    
    fun hide() {
        danmakuView?.hide()
    }
    
    /**
     * 释放所有资源
     */
    fun release() {
        Log.d(TAG, "🗑️ release")
        loadJob?.cancel()
        playerListener?.let { player?.removeListener(it) }
        danmakuView?.release()
        danmakuView = null
        danmakuContext = null
        player = null
        playerListener = null
        isReady = false
        isPrepared = false
        // 注意：不清除缓存数据，以便下次快速恢复
    }
}

/**
 * Composable 辅助函数：获取弹幕管理器实例
 * 
 * 使用示例：
 * ```
 * val danmakuManager = rememberDanmakuManager()
 * ```
 */
@Composable
fun rememberDanmakuManager(): DanmakuManager {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val manager = remember { 
        DanmakuManager.getInstance(context, scope) 
    }
    
    // 确保 scope 是最新的
    DisposableEffect(scope) {
        DanmakuManager.updateScope(scope)
        onDispose { }
    }
    
    return manager
}
