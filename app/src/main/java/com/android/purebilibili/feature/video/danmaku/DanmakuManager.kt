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
    
    // 🔥🔥 [修复] 缓存原始数据而非解析后的弹幕列表
    // BaseDanmaku 对象与特定 DanmakuContext 绑定，无法跨 context 使用
    private var cachedRawData: ByteArray? = null
    private var cachedCid: Long = 0L
    
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
     * 获取或创建弹幕上下文（只创建一次，复用）
     */
    private fun getOrCreateContext(): DanmakuContext {
        return danmakuContext ?: DanmakuContext.create().also { ctx ->
            config.applyTo(ctx, context)
            danmakuContext = ctx
            Log.d(TAG, "✅ DanmakuContext created (singleton)")
        }
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
        
        Log.d(TAG, "📎 attachView: new view, old=${danmakuView != null}, hashCode=${view.hashCode()}, cachedRawData=${cachedRawData?.size ?: 0}")
        
        // 先暂停旧视图
        danmakuView?.let { oldView ->
            try {
                oldView.pause()
                oldView.hide()
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Error pausing old view: ${e.message}")
            }
        }
        
        danmakuView = view
        isPrepared = false  // 🔥 重置 prepared 状态
        
        // 🔥🔥 [修复] 每个新视图需要新的 DanmakuContext
        val ctx = DanmakuContext.create().also { newCtx ->
            config.applyTo(newCtx, context)
        }
        danmakuContext = ctx
        Log.d(TAG, "✅ New DanmakuContext created for view ${view.hashCode()}")
        
        // 🔥🔥🔥 [关键修复] 不在 prepared 回调中添加弹幕
        // 所有弹幕加载都通过 loadDanmaku 统一处理，避免竞态条件
        
        view.setCallback(object : DrawHandler.Callback {
            override fun prepared() {
                Log.d(TAG, "✅ DanmakuView prepared, hashCode=${view.hashCode()}, cachedCid=$cachedCid")
                isPrepared = true
                
                // 🔥🔥 只同步播放状态，不添加弹幕
                // 弹幕将由 loadDanmaku 在视图 prepared 后添加
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    player?.let { p ->
                        if (p.isPlaying && config.isEnabled) {
                            val position = p.currentPosition
                            view.seekTo(position)
                            view.start()
                            view.resume()
                            Log.d(TAG, "🚀 Synced to position ${position}ms (danmaku will be loaded by loadDanmaku)")
                        }
                    }
                }
            }
            override fun updateTimer(timer: DanmakuTimer?) {}
            override fun danmakuShown(danmaku: BaseDanmaku?) {}
            override fun drawingFinished() {}
        })
        
        view.enableDanmakuDrawingCache(true)
        
        // 🔥 使用空解析器 prepare 视图
        val emptyParser = object : BaseDanmakuParser() {
            override fun parse(): IDanmakus = Danmakus()
        }
        view.prepare(emptyParser, ctx)
        isReady = true
    }
    
    /**
     * 解绑 DanmakuView（不释放弹幕数据和 Context）
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
        // 🔥 注意：不清除 danmakuContext 和 cachedDanmakus，保持复用
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
                Log.d(TAG, "🎬 onIsPlayingChanged: isPlaying=$isPlaying, isPrepared=$isPrepared, isEnabled=${config.isEnabled}")
                if (isPlaying && isPrepared && config.isEnabled) {
                    // 🔥 恢复播放时同步位置并启动弹幕
                    val position = exoPlayer.currentPosition
                    danmakuView?.seekTo(position)
                    startDanmaku()
                } else if (!isPlaying) {
                    danmakuView?.pause()
                }
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "🎬 onPlaybackStateChanged: state=$playbackState, isPlaying=${exoPlayer.isPlaying}")
                when (playbackState) {
                    Player.STATE_READY -> {
                        if (exoPlayer.isPlaying && isPrepared && config.isEnabled) {
                            // 🔥 准备好后同步位置并启动
                            val position = exoPlayer.currentPosition
                            danmakuView?.seekTo(position)
                            startDanmaku()
                        }
                    }
                    Player.STATE_BUFFERING -> {
                        // 🔥 Buffering 时不暂停弹幕，只是等待
                        Log.d(TAG, "🎬 Buffering...")
                    }
                    Player.STATE_ENDED -> {
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
                    Log.d(TAG, "🎬 Seek detected: ${oldPosition.positionMs}ms -> ${newPosition.positionMs}ms")
                    danmakuView?.let { view ->
                        // 🔥 清除当前显示的弹幕并跳转到新位置
                        view.seekTo(newPosition.positionMs)
                        // 如果正在播放，确保弹幕继续
                        if (exoPlayer.isPlaying && config.isEnabled) {
                            view.start()
                            view.resume()
                        }
                    }
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
        
        // 🔥 如果是同一个 cid 且已有缓存数据，直接用当前 context 解析
        if (cid == cachedCid && cachedRawData != null) {
            Log.d(TAG, "📥 Using cached raw data (${cachedRawData!!.size} bytes) for cid=$cid")
            // 如果视图已准备好，重新添加弹幕（因为可能是新视图）
            if (danmakuView != null && isPrepared) {
                val ctx = danmakuContext ?: return
                val danmakuList = DanmakuParser.parse(cachedRawData!!, ctx)
                Log.d(TAG, "📎 Re-adding ${danmakuList.size} cached danmakus to current view")
                danmakuList.forEach { danmakuView!!.addDanmaku(it) }
                player?.let { syncToPosition(it.currentPosition) }
            }
            return
        }
        
        // 需要从网络加载新 cid 的弹幕
        Log.d(TAG, "📥 loadDanmaku: New cid=$cid (cached=$cachedCid), loading from network")
        isLoading = true
        cachedCid = cid
        cachedRawData = null  // 清除旧缓存
        
        // 🔥🔥🔥 [关键修复] 彻底重置 DanmakuView 和 Context
        danmakuView?.let { view ->
            Log.d(TAG, "🗑️ Releasing and re-preparing DanmakuView with NEW context for cid=$cid")
            try {
                view.release()  // 完全释放，清除所有弹幕数据
                isPrepared = false  // 重置 prepared 状态
                
                // 🔥🔥 创建全新的 DanmakuContext（不复用旧的）
                val newCtx = DanmakuContext.create().also { ctx ->
                    config.applyTo(ctx, context)
                }
                danmakuContext = newCtx
                Log.d(TAG, "✅ Created fresh DanmakuContext for cid=$cid")
                
                // 🔥🔥 重新设置回调（release 后可能需要）
                view.setCallback(object : DrawHandler.Callback {
                    override fun prepared() {
                        Log.d(TAG, "✅ DanmakuView re-prepared callback fired for cid=$cid")
                        isPrepared = true
                    }
                    override fun updateTimer(timer: DanmakuTimer?) {}
                    override fun danmakuShown(danmaku: BaseDanmaku?) {}
                    override fun drawingFinished() {}
                })
                
                // 重新准备视图
                val emptyParser = object : BaseDanmakuParser() {
                    override fun parse(): IDanmakus = Danmakus()
                }
                view.enableDanmakuDrawingCache(true)
                view.prepare(emptyParser, newCtx)
                Log.d(TAG, "✅ DanmakuView re-prepared, waiting for callback")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Error resetting DanmakuView: ${e.message}")
            }
        }
        
        loadJob?.cancel()
        loadJob = scope.launch {
            try {
                val rawData = VideoRepository.getDanmakuRawData(cid)
                if (rawData == null || rawData.isEmpty()) {
                    Log.w(TAG, "⚠️ Danmaku data is empty for cid=$cid")
                    isLoading = false
                    return@launch
                }
                
                Log.d(TAG, "📥 Raw data loaded: ${rawData.size} bytes for cid=$cid")
                
                // 🔥 缓存原始数据（而非解析后的列表）
                cachedRawData = rawData
                
                // 🔥 用当前 context 解析
                val ctx = danmakuContext ?: getOrCreateContext()
                val danmakuList = DanmakuParser.parse(rawData, ctx)
                Log.d(TAG, "📊 Parsed ${danmakuList.size} danmakus for cid=$cid")
                
                withContext(Dispatchers.Main) {
                    isLoading = false
                    
                    // 🔥🔥 等待视图准备好再添加弹幕
                    danmakuView?.let { view ->
                        // 等待 prepared 回调（最多等 500ms）
                        var waitCount = 0
                        while (!isPrepared && waitCount < 10) {
                            delay(50)
                            waitCount++
                        }
                        
                        if (isPrepared) {
                            Log.d(TAG, "📎 Adding ${danmakuList.size} danmakus to view for cid=$cid")
                            danmakuList.forEach { view.addDanmaku(it) }
                            
                            // 同步到当前位置
                            if (player?.isPlaying == true && config.isEnabled) {
                                val position = player?.currentPosition ?: 0L
                                view.seekTo(position)
                                view.start()
                                view.resume()
                                Log.d(TAG, "🚀 Synced to position ${position}ms for cid=$cid")
                            }
                        } else {
                            Log.w(TAG, "⚠️ View not prepared after waiting, cid=$cid")
                        }
                    } ?: Log.d(TAG, "📥 No view attached, raw data cached for cid=$cid")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load danmaku for cid=$cid: ${e.message}", e)
                isLoading = false
            }
        }
    }
    
    private fun startDanmaku() {
        val view = danmakuView ?: return
        
        val currentPosition = player?.currentPosition ?: 0L
        Log.d(TAG, "🚀 startDanmaku: pos=${currentPosition}ms, isReady=$isReady, isPrepared=$isPrepared, view.isPaused=${view.isPaused}")
        
        if (isReady && isPrepared) {
            // 🔥 确保视图可见
            view.visibility = android.view.View.VISIBLE
            
            // 🔥🔥 [关键修复] 正确的操作顺序：
            // 1. show() - 显示弹幕层
            // 2. start() - 启动计时器
            // 3. seekTo() - 跳转到正确位置（必须在 start 之后！）
            // 4. resume() - 恢复渲染
            view.show()
            view.start()
            view.seekTo(currentPosition)  // 🔥 seekTo 必须在 start 之后！
            view.resume()
            
            Log.d(TAG, "✅ Danmaku started, view.isPaused=${view.isPaused}")
            
            // 🔥 延迟检查可见弹幕数量
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    val visibleDanmakus = view.currentVisibleDanmakus
                    Log.d(TAG, "📊 Visible danmakus after 500ms: ${visibleDanmakus?.size() ?: 0}, currentTime=${view.currentTime}")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Failed to get visible danmakus: ${e.message}")
                }
            }, 500)
        } else {
            Log.w(TAG, "⚠️ Cannot start: isReady=$isReady, isPrepared=$isPrepared")
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
     * 🔥🔥 [内存泄漏修复] 释放视图和播放器引用
     * 应在 Activity/Fragment onDestroy 时调用
     */
    fun clearViewReference() {
        Log.d(TAG, "🗑️ clearViewReference: Clearing all references to prevent memory leak")
        
        // 🔥 移除播放器监听器并清除引用
        playerListener?.let { listener ->
            player?.removeListener(listener)
        }
        playerListener = null
        player = null  // 🔥 ExoPlayer 持有 Activity context，必须清除
        
        // 🔥 释放 DanmakuView
        danmakuView?.let { view ->
            try {
                view.pause()
                view.hide()
                view.release()  // 🔥 完全释放视图
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Error releasing view: ${e.message}")
            }
        }
        danmakuView = null
        danmakuContext = null  // 🔥 也释放 Context（包含 View 引用）
        
        // 🔥 取消加载任务
        loadJob?.cancel()
        loadJob = null
        
        isPrepared = false
        isReady = false
        isLoading = false
        
        Log.d(TAG, "✅ All references cleared")
    }
    
    /**
     * 释放所有资源
     */
    fun release() {
        Log.d(TAG, "🗑️ release")
        loadJob?.cancel()
        playerListener?.let { player?.removeListener(it) }
        
        // 🔥🔥 [内存泄漏修复] 确保彻底释放 danmakuView
        danmakuView?.let { view ->
            try {
                view.pause()
                view.hide()
                view.release()
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Error releasing danmaku view: ${e.message}")
            }
        }
        
        danmakuView = null
        danmakuContext = null
        player = null
        playerListener = null
        isReady = false
        isPrepared = false
        // 🔥 清除缓存数据，防止持有过大内存
        cachedRawData = null
        cachedCid = 0L
        Log.d(TAG, "✅ DanmakuManager fully released")
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
    
    // 🔥🔥 [内存泄漏修复] 在 Composable 离开组合时清理视图引用
    DisposableEffect(Unit) {
        onDispose {
            // 🔥 调用 clearViewReference 而非 release
            // 这样可以清理 View 引用但保持弹幕数据缓存
            manager.clearViewReference()
        }
    }
    
    return manager
}
