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
import com.bytedance.danmaku.render.engine.DanmakuView
import com.bytedance.danmaku.render.engine.control.DanmakuController
import com.bytedance.danmaku.render.engine.data.DanmakuData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 弹幕管理器（单例模式）
 * 
 * 使用 ByteDance DanmakuRenderEngine 重构
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
    
    // 视图和控制器
    private var danmakuView: DanmakuView? = null
    private var controller: DanmakuController? = null
    private var player: ExoPlayer? = null
    private var playerListener: Player.Listener? = null
    private var loadJob: Job? = null
    
    // 弹幕状态
    private var isPlaying = false
    private var isLoading = false
    
    // 缓存原始数据（横竖屏切换时复用）
    private var cachedRawData: ByteArray? = null
    private var cachedDanmakuList: List<DanmakuData>? = null
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
        set(value) {
            config.opacity = value
            controller?.let { 
                config.applyTo(it.config)
                it.invalidateView()
                Log.w(TAG, "📋 Opacity changed to $value")
            }
        }
    
    var fontScale: Float
        get() = config.fontScale
        set(value) {
            config.fontScale = value
            controller?.let { 
                config.applyTo(it.config)
                it.invalidateView()
                Log.w(TAG, "📋 FontScale changed to $value")
            }
        }
    
    var speedFactor: Float
        get() = config.speedFactor
        set(value) {
            config.speedFactor = value
            controller?.let { 
                config.applyTo(it.config)
                it.invalidateView()
                Log.w(TAG, "📋 SpeedFactor changed to $value")
            }
        }
    
    var displayArea: Float
        get() = config.displayAreaRatio
        set(value) {
            config.displayAreaRatio = value
            controller?.let { 
                config.applyTo(it.config)
                it.invalidateView()
                Log.w(TAG, "📋 DisplayArea changed to $value")
            }
        }
    
    /**
     * 🔥 批量更新弹幕设置（实时生效）
     */
    fun updateSettings(
        opacity: Float = this.opacity,
        fontScale: Float = this.fontScale,
        speed: Float = this.speedFactor,
        displayArea: Float = this.displayArea
    ) {
        config.opacity = opacity
        config.fontScale = fontScale
        config.speedFactor = speed
        config.displayAreaRatio = displayArea
        
        controller?.let { ctrl ->
            config.applyTo(ctrl.config)
            ctrl.invalidateView()
            Log.w(TAG, "📋 Settings updated: opacity=$opacity, fontScale=$fontScale, speed=$speed, displayArea=$displayArea")
        }
    }
    
    /**
     * 绑定 DanmakuView
     */
    fun attachView(view: DanmakuView) {
        // 使用 Log.w (warning) 确保日志可见
        Log.w(TAG, "========== attachView CALLED ==========")
        
        // 如果是同一个视图，跳过
        if (danmakuView === view) {
            Log.w(TAG, "📎 attachView: Same view, skipping")
            return
        }
        
        Log.w(TAG, "📎 attachView: new view, old=${danmakuView != null}, hashCode=${view.hashCode()}")
        Log.w(TAG, "📎 View size: width=${view.width}, height=${view.height}, visibility=${view.visibility}")
        
        danmakuView = view
        controller = view.controller
        
        Log.w(TAG, "📎 controller obtained: ${controller != null}")
        
        // 内置渲染层（ScrollLayer, TopCenterLayer, BottomCenterLayer）由 DanmakuRenderEngine 自动注册
        // 不需要手动添加，手动添加会报错 "The custom LayerType must not be less than 2000"
        
        // 应用配置
        controller?.let { ctrl ->
            config.applyTo(ctrl.config)
            Log.w(TAG, "✅ DanmakuController configured")
        } ?: Log.e(TAG, "❌ Controller is null!")
        
        // 🔥🔥 [关键修复] 等待 View 布局完成后再设置弹幕数据
        // DanmakuRenderEngine 需要有效的 View 尺寸来计算弹幕轨道位置
        if (view.width > 0 && view.height > 0) {
            // View 已经有有效尺寸，直接设置数据
            Log.w(TAG, "📎 View has valid size, setting data immediately")
            applyDanmakuDataToController()
        } else {
            // View 尺寸为 0，等待布局完成
            Log.w(TAG, "📎 View size is 0, waiting for layout...")
            view.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    // 移除监听器，避免重复回调
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    
                    Log.w(TAG, "📎 Layout callback! width=${view.width}, height=${view.height}")
                    
                    // 确保 View 仍然是当前绑定的 View
                    if (danmakuView === view && view.width > 0 && view.height > 0) {
                        applyDanmakuDataToController()
                    } else if (danmakuView === view) {
                        // 🔥🔥 [修复] 如果布局回调时尺寸仍为 0，延迟 100ms 再试一次
                        Log.w(TAG, "⚠️ View still zero size, scheduling delayed retry...")
                        view.postDelayed({
                            if (danmakuView === view && view.width > 0 && view.height > 0) {
                                Log.w(TAG, "📎 Delayed retry: width=${view.width}, height=${view.height}")
                                applyDanmakuDataToController()
                            } else {
                                Log.w(TAG, "⚠️ View still invalid after delay, skipping")
                            }
                        }, 100)
                    } else {
                        Log.w(TAG, "⚠️ View changed, skipping setData")
                    }
                }
            })
        }
        
        Log.w(TAG, "========== attachView COMPLETED ==========")
    }
    
    /**
     * 将缓存的弹幕数据应用到 controller（内部方法）
     */
    private fun applyDanmakuDataToController() {
        Log.w(TAG, "📎 cachedDanmakuList is null? ${cachedDanmakuList == null}, size=${cachedDanmakuList?.size ?: 0}")
        cachedDanmakuList?.let { list ->
            // 🔥🔥 [修复] 始终用 playTime=0 设置数据，因为弹幕的 showAtTime 是相对于视频开头的
            Log.w(TAG, "📎 Calling setData with ${list.size} items, playTime=0 (base reference)")
            controller?.setData(list, 0)
            Log.w(TAG, "📎 setData completed")
            
            // 强制刷新视图
            controller?.invalidateView()
            Log.w(TAG, "📎 invalidateView called")
            
            // 同步到当前播放位置
            player?.let { p ->
                val position = p.currentPosition
                Log.w(TAG, "📎 Player state: isPlaying=${p.isPlaying}, isEnabled=${config.isEnabled}, position=${position}ms")
                
                // 🔥🔥 [修复] 始终先 start 到当前位置，让 controller 知道视频在哪里
                controller?.start(position)
                Log.w(TAG, "🚀 controller.start($position) called")
                
                if (p.isPlaying && config.isEnabled) {
                    isPlaying = true
                    Log.w(TAG, "▶️ Danmaku playing")
                } else {
                    // 如果视频暂停中，也暂停弹幕
                    controller?.pause()
                    isPlaying = false
                    Log.w(TAG, "⏸️ Danmaku paused (player not playing)")
                }
            } ?: Log.w(TAG, "📎 Player is null, not syncing")
        } ?: Log.w(TAG, "📎 No cached danmaku list to apply")
    }
    
    /**
     * 解绑 DanmakuView（不释放弹幕数据）
     */
    fun detachView() {
        Log.d(TAG, "📎 detachView: Pausing and clearing controller")
        controller?.pause()
        controller = null
        danmakuView = null
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
            override fun onIsPlayingChanged(isPlayerPlaying: Boolean) {
                Log.w(TAG, "🎬 onIsPlayingChanged: isPlaying=$isPlayerPlaying, isEnabled=${config.isEnabled}, hasData=${cachedDanmakuList != null}")
                
                if (isPlayerPlaying && config.isEnabled) {
                    // 🔥🔥 [修复] 只有当数据已加载时才启动弹幕
                    if (cachedDanmakuList != null) {
                        val position = exoPlayer.currentPosition
                        controller?.start(position)
                        isPlaying = true
                        Log.w(TAG, "🎬 Danmaku STARTED at ${position}ms")
                    } else {
                        Log.w(TAG, "🎬 Player playing but danmaku data not loaded yet, will start after load")
                        // 数据加载完成后会自动 start
                    }
                } else if (!isPlayerPlaying) {
                    // 暂停 - DanmakuRenderEngine 的 pause() 会让弹幕停在原地
                    controller?.pause()
                    isPlaying = false
                    Log.w(TAG, "🎬 Danmaku PAUSED (danmakus stay in place)")
                }
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "🎬 onPlaybackStateChanged: state=$playbackState")
                when (playbackState) {
                    Player.STATE_READY -> {
                        if (exoPlayer.isPlaying && config.isEnabled) {
                            val position = exoPlayer.currentPosition
                            controller?.start(position)
                            isPlaying = true
                        }
                    }
                    Player.STATE_ENDED -> {
                        // 视频结束时暂停弹幕（保持在屏幕上）
                        controller?.pause()
                        isPlaying = false
                    }
                    Player.STATE_BUFFERING -> {
                        // 缓冲时暂停弹幕
                        if (isPlaying) {
                            controller?.pause()
                            Log.d(TAG, "🎬 Buffering, danmaku paused")
                        }
                    }
                }
            }
            
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                    Log.w(TAG, "🎬 Seek detected: ${oldPosition.positionMs}ms -> ${newPosition.positionMs}ms")
                    
                    // 🔥 关键修复：Seek 时重新调用 setData(list, 0) + start(newPosition)
                    cachedDanmakuList?.let { list ->
                        Log.w(TAG, "🔄 Re-setting data with playTime=0, then start at ${newPosition.positionMs}ms")
                        controller?.setData(list, 0)  // 始终用 0 作为基准
                        controller?.start(newPosition.positionMs)  // 用实际位置启动
                        
                        if (exoPlayer.isPlaying && config.isEnabled) {
                            isPlaying = true
                            Log.w(TAG, "🚀 Danmaku restarted at ${newPosition.positionMs}ms")
                        } else {
                            controller?.pause()
                            isPlaying = false
                            Log.w(TAG, "⏸️ Danmaku paused after seek (player not playing)")
                        }
                    } ?: run {
                        controller?.clear()
                        Log.w(TAG, "⚠️ No cached danmaku, just cleared screen")
                    }
                }
            }
        }
        
        exoPlayer.addListener(playerListener!!)
    }
    
    /**
     * 加载弹幕数据
     * 
     * @param cid 视频 cid
     * @param durationMs 视频时长 (毫秒)，用于计算 Protobuf 分段数。如果为 0，则回退到 XML API
     */
    fun loadDanmaku(cid: Long, durationMs: Long = 0L) {
        Log.w(TAG, "========== loadDanmaku CALLED cid=$cid, duration=${durationMs}ms ==========")
        Log.w(TAG, "📥 loadDanmaku: cid=$cid, cached=$cachedCid, isLoading=$isLoading, controller=${controller != null}")
        
        // 如果正在加载，跳过
        if (isLoading) {
            Log.w(TAG, "📥 Already loading, skipping")
            return
        }
        
        // 如果是同一个 cid 且已有缓存数据，直接使用（横竖屏切换场景）
        if (cid == cachedCid && cachedDanmakuList != null) {
            val currentPos = player?.currentPosition ?: 0L
            Log.w(TAG, "📥 Using cached danmaku list (${cachedDanmakuList!!.size} items) for cid=$cid, position=${currentPos}ms")
            
            // 🔥🔥 [修复] 仿照 Seek 处理器的模式：先用 0 设置基准，再用 currentPos 启动
            controller?.setData(cachedDanmakuList!!, 0)  // 基准时间 0
            controller?.start(currentPos)  // 跳到当前位置
            Log.w(TAG, "🚀 Cached data: setData(0) + start(${currentPos}ms)")
            
            player?.let { p ->
                if (p.isPlaying && config.isEnabled) {
                    isPlaying = true
                    Log.w(TAG, "▶️ Player playing, danmaku active")
                } else {
                    controller?.pause()
                    isPlaying = false
                    Log.w(TAG, "⏸️ Player paused, danmaku paused")
                }
            }
            return
        }
        
        // 需要从网络加载新 cid 的弹幕
        Log.w(TAG, "📥 loadDanmaku: New cid=$cid, loading from network")
        isLoading = true
        cachedCid = cid
        cachedRawData = null
        cachedDanmakuList = null
        
        // 清除现有弹幕
        controller?.stop()
        
        loadJob?.cancel()
        loadJob = scope.launch {
            try {
                var danmakuList: List<com.bytedance.danmaku.render.engine.data.DanmakuData>? = null
                
                // 🔥🔥 [新增] 优先使用 Protobuf API (seg.so)
                if (durationMs > 0) {
                    Log.w(TAG, "📥 Trying Protobuf API (seg.so)...")
                    try {
                        val segments = com.android.purebilibili.data.repository.DanmakuRepository.getDanmakuSegments(cid, durationMs)
                        if (segments.isNotEmpty()) {
                            danmakuList = DanmakuParser.parseProtobuf(segments)
                            Log.w(TAG, "✅ Protobuf parsed ${danmakuList.size} danmakus")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Protobuf API failed: ${e.message}, falling back to XML")
                    }
                }
                
                // 🔥🔥 [后备] 如果 Protobuf 失败或未提供 duration，使用 XML API
                if (danmakuList == null || danmakuList.isEmpty()) {
                    Log.w(TAG, "📥 Trying XML API (fallback)...")
                    val rawData = com.android.purebilibili.data.repository.DanmakuRepository.getDanmakuRawData(cid)
                    if (rawData != null && rawData.isNotEmpty()) {
                        cachedRawData = rawData
                        danmakuList = DanmakuParser.parse(rawData)
                        Log.w(TAG, "✅ XML parsed ${danmakuList.size} danmakus")
                    }
                }
                
                if (danmakuList == null || danmakuList.isEmpty()) {
                    Log.w(TAG, "⚠️ No danmaku data available for cid=$cid")
                    isLoading = false
                    return@launch
                }
                
                cachedDanmakuList = danmakuList
                Log.w(TAG, "📊 Final: ${danmakuList.size} danmakus for cid=$cid")
                
                withContext(Dispatchers.Main) {
                    isLoading = false
                    
                    // 🔥🔥 [核心修复] 仿照 Seek 处理器的模式
                    val currentPlayTime = player?.currentPosition ?: 0L
                    Log.w(TAG, "📎 View size: width=${danmakuView?.width}, height=${danmakuView?.height}")
                    
                    // 🔥🔥 [核心修复] 先用 0 作为基准设置数据，再用实际位置启动
                    // 这与 Seek 处理器的模式一致，确保引擎知道完整的时间线
                    Log.w(TAG, "📎 Calling setData with ${danmakuList.size} items, playTime=0 (base)")
                    controller?.setData(danmakuList, 0)  // 基准时间 0
                    Log.w(TAG, "📎 setData completed")
                    
                    // 🔥🔥 [关键] 强制刷新视图 - 与横竖屏切换路径一致
                    controller?.invalidateView()
                    Log.w(TAG, "📎 invalidateView called")
                    
                    // start 同步到当前位置
                    controller?.start(currentPlayTime)
                    Log.w(TAG, "🚀 controller.start($currentPlayTime) called - video is at this position")
                    
                    // 如果 player 暂停中，也暂停 controller
                    if (player?.isPlaying != true) {
                        controller?.pause()
                        isPlaying = false
                        Log.w(TAG, "⏸️ Player not playing, controller paused")
                    } else {
                        isPlaying = true
                        Log.w(TAG, "▶️ Player is playing, danmaku active")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load danmaku for cid=$cid: ${e.message}", e)
                isLoading = false
            }
        }
    }
    
    fun show() {
        Log.d(TAG, "👁️ show()")
        danmakuView?.visibility = android.view.View.VISIBLE
        
        if (player?.isPlaying == true) {
            val position = player?.currentPosition ?: 0L
            controller?.start(position)
            isPlaying = true
        }
    }
    
    fun hide() {
        Log.d(TAG, "🙈 hide()")
        controller?.pause()
        danmakuView?.visibility = android.view.View.GONE
        isPlaying = false
    }
    
    /**
     * 清除视图引用（防止内存泄漏）
     */
    fun clearViewReference() {
        Log.d(TAG, "🗑️ clearViewReference: Clearing all references")
        
        // 移除播放器监听器
        playerListener?.let { listener ->
            player?.removeListener(listener)
        }
        playerListener = null
        player = null
        
        // 停止弹幕
        controller?.stop()
        controller = null
        danmakuView = null
        
        // 取消加载任务
        loadJob?.cancel()
        loadJob = null
        
        isPlaying = false
        isLoading = false
        
        Log.d(TAG, "✅ All references cleared")
    }
    
    /**
     * 释放所有资源
     */
    fun release() {
        Log.d(TAG, "🗑️ release")
        clearViewReference()
        
        // 清除缓存
        cachedRawData = null
        cachedDanmakuList = null
        cachedCid = 0L
        
        Log.d(TAG, "✅ DanmakuManager fully released")
    }
}

/**
 * Composable 辅助函数：获取弹幕管理器实例
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
