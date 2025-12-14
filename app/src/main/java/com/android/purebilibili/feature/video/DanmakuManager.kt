// 文件路径: feature/video/DanmakuManager.kt
package com.android.purebilibili.feature.video

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.util.Xml
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.android.purebilibili.data.repository.VideoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import master.flame.danmaku.controller.DrawHandler
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.DanmakuTimer
import master.flame.danmaku.danmaku.model.GlobalFlagValues
import master.flame.danmaku.danmaku.model.IDanmakus
import master.flame.danmaku.danmaku.model.IDisplayer
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.danmaku.model.android.DanmakuFactory
import master.flame.danmaku.danmaku.model.android.Danmakus
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser
import master.flame.danmaku.ui.widget.DanmakuView
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream

/**
 * 弹幕管理器
 * 基于 Bilibili 官方 DanmakuFlameMaster 库
 * 
 * 职责：
 * 1. 加载和解析弹幕数据
 * 2. 与 ExoPlayer 同步弹幕播放
 * 3. 管理弹幕生命周期
 */
class DanmakuManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "DanmakuManager"
    }

    private var danmakuView: DanmakuView? = null
    private var danmakuContext: DanmakuContext? = null
    private var player: ExoPlayer? = null
    private var playerListener: Player.Listener? = null
    private var loadJob: Job? = null
    
    // 弹幕状态
    private var isReady = false
    private var isPrepared = false
    
    // 弹幕设置
    var isEnabled = true
        set(value) {
            field = value
            if (value) show() else hide()
        }
    var opacity = 1.0f
        set(value) {
            field = value
            danmakuContext?.setDanmakuTransparency(value)
        }
    var fontScale = 1.0f
        set(value) {
            field = value
            danmakuContext?.setScaleTextSize(value)
        }
    var speedFactor = 1.2f
        set(value) {
            field = value
            danmakuContext?.setScrollSpeedFactor(value)
        }
    var displayAreaRatio = 0.5f  // 显示区域比例 (0.25, 0.5, 0.75, 1.0)
        set(value) {
            field = value
            updateMaxLines()
        }

    /**
     * 初始化弹幕 Context（在 attachView 之前或之后调用均可）
     */
    private fun initDanmakuContext() {
        if (danmakuContext != null) return
        
        danmakuContext = DanmakuContext.create().apply {
            // 🎨 描边样式 - 增粗描边使弹幕更清晰可见
            setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3.5f)
            // 合并重复弹幕
            setDuplicateMergingEnabled(true)
            // 🚀 滚动速度 - 1.5f 适中速度（数值越大弹幕越慢），便于阅读
            setScrollSpeedFactor(1.5f)
            // 📏 字体大小缩放 - 1.0f 原始大小
            setScaleTextSize(1.0f)
            // 🌟 透明度 - 0.85f 既清晰又不遮挡画面
            setDanmakuTransparency(0.85f)
            // 禁用粗体（使用正常字重）
            setDanmakuBold(false)
            // 设置最大行数
            updateMaxLines()
        }
        Log.d(TAG, "✅ DanmakuContext initialized with optimized settings")
    }
    
    private fun updateMaxLines() {
        val maxLines = when {
            displayAreaRatio <= 0.25f -> 3
            displayAreaRatio <= 0.5f -> 5
            displayAreaRatio <= 0.75f -> 8
            else -> Int.MAX_VALUE
        }
        danmakuContext?.setMaximumLines(
            mapOf(
                BaseDanmaku.TYPE_SCROLL_RL to maxLines,
                BaseDanmaku.TYPE_SCROLL_LR to maxLines
            )
        )
    }

    /**
     * 绑定 DanmakuView
     */
    fun attachView(view: DanmakuView) {
        Log.d(TAG, "📎 attachView")
        danmakuView = view
        initDanmakuContext()
        
        view.setCallback(object : DrawHandler.Callback {
            override fun prepared() {
                Log.d(TAG, "✅ DanmakuView prepared")
                isPrepared = true
                // 🔥🔥 [修复] prepared 回调在后台线程，需要切换到主线程访问 ExoPlayer
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    player?.let { syncToPlayerPosition(it) }
                }
            }
            
            override fun updateTimer(timer: DanmakuTimer?) {}
            override fun danmakuShown(danmaku: BaseDanmaku?) {
                // 🔥🔥 [调试] 验证弹幕是否被渲染
                if (danmaku != null) {
                    Log.d(TAG, "👁️ danmakuShown: time=${danmaku.time}, text='${danmaku.text}'")
                }
            }
            override fun drawingFinished() {}
        })
        
        view.enableDanmakuDrawingCache(true)
    }

    /**
     * 绑定 ExoPlayer 并同步弹幕
     */
    fun attachPlayer(exoPlayer: ExoPlayer) {
        Log.d(TAG, "🎬 attachPlayer")
        
        // 移除旧的监听器
        playerListener?.let { player?.removeListener(it) }
        
        player = exoPlayer
        
        // 创建新的监听器
        playerListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d(TAG, "🎬 onIsPlayingChanged: isPlaying=$isPlaying, isPrepared=$isPrepared, isEnabled=$isEnabled")
                if (isPlaying && isPrepared && isEnabled) {
                    startDanmaku()
                } else {
                    danmakuView?.pause()
                }
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "🎬 onPlaybackStateChanged: state=$playbackState")
                when (playbackState) {
                    Player.STATE_READY -> {
                        if (exoPlayer.isPlaying && isPrepared && isEnabled) {
                            startDanmaku()
                        }
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
                // 用户拖动进度条时同步弹幕位置
                if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                    Log.d(TAG, "🎬 Seek detected, sync danmaku to ${newPosition.positionMs}ms")
                    danmakuView?.seekTo(newPosition.positionMs)
                }
            }
        }
        
        exoPlayer.addListener(playerListener!!)
    }
    
    /**
     * 🔥🔥 [修复] 启动弹幕（确保先 start 再 resume）
     */
    private fun startDanmaku() {
        val view = danmakuView ?: run {
            Log.e(TAG, "🚀 startDanmaku: danmakuView is null!")
            return
        }
        
        Log.d(TAG, "🚀 startDanmaku: isReady=$isReady, isPrepared=$isPrepared")
        Log.d(TAG, "🚀 DanmakuView state: width=${view.width}, height=${view.height}, isShown=${view.isShown}, visibility=${view.visibility}")
        Log.d(TAG, "🚀 DanmakuView isPrepared=${view.isPrepared}")
        
        if (isReady && isPrepared) {
            // DanmakuView 必须先 start() 才能正常显示
            if (view.visibility != android.view.View.VISIBLE) {
                view.visibility = android.view.View.VISIBLE
            }
            view.show()
            view.start()
            view.resume()
            Log.d(TAG, "✅ startDanmaku: started and resumed!")
        } else {
            Log.w(TAG, "⚠️ startDanmaku: not ready or not prepared, skipping")
        }
    }
    
    private fun syncToPlayerPosition(player: ExoPlayer) {
        val position = player.currentPosition
        Log.d(TAG, "🔄 Syncing danmaku to position: ${position}ms, isPlaying=${player.isPlaying}")
        danmakuView?.seekTo(position)
        if (player.isPlaying && isEnabled) {
            startDanmaku()
        } else {
            // 🔥🔥 [修复] 如果视频暂停，弹幕也需要暂停
            danmakuView?.pause()
            Log.d(TAG, "⏸️ Video paused, danmaku paused during sync")
        }
    }

    /**
     * 加载弹幕数据
     */
    fun loadDanmaku(cid: Long) {
        Log.d(TAG, "📥 loadDanmaku: cid=$cid")
        
        loadJob?.cancel()
        loadJob = scope.launch {
            try {
                val rawData = VideoRepository.getDanmakuRawData(cid)
                if (rawData == null || rawData.isEmpty()) {
                    Log.w(TAG, "⚠️ Danmaku data is null or empty")
                    return@launch
                }
                
                Log.d(TAG, "📥 Danmaku raw data loaded: ${rawData.size} bytes")
                
                withContext(Dispatchers.Main) {
                    parseDanmaku(rawData)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load danmaku: ${e.message}", e)
            }
        }
    }
    
    private fun parseDanmaku(rawData: ByteArray) {
        try {
            val view = danmakuView ?: run {
                Log.e(TAG, "❌ DanmakuView is null")
                return
            }
            
            val ctx = danmakuContext ?: run {
                Log.e(TAG, "❌ DanmakuContext is null")
                return
            }
            
            Log.d(TAG, "🎯 Preparing DanmakuView...")
            
            // 🔥🔥 [关键修复] 使用空解析器初始化 DanmakuView
            val emptyParser = object : BaseDanmakuParser() {
                override fun parse(): IDanmakus = Danmakus()
            }
            view.prepare(emptyParser, ctx)
            isReady = true
            
            Log.d(TAG, "✅ DanmakuView prepared, now parsing and adding danmakus manually...")
            
            // 🔥🔥 在后台线程解析弹幕，然后在主线程添加
            scope.launch(Dispatchers.Default) {
                val danmakuList = parseXmlDanmaku(rawData, ctx)
                Log.d(TAG, "📊 Parsed ${danmakuList.size} danmakus, now adding to view...")
                
                withContext(Dispatchers.Main) {
                    // 等待 DanmakuView 完全准备好
                    var attempts = 0
                    while (!view.isPrepared && attempts < 50) {
                        kotlinx.coroutines.delay(50)
                        attempts++
                    }
                    
                    if (view.isPrepared) {
                        // 逐条添加弹幕
                        danmakuList.forEach { danmaku ->
                            view.addDanmaku(danmaku)
                        }
                        Log.d(TAG, "✅ Added ${danmakuList.size} danmakus to DanmakuView")
                    } else {
                        Log.e(TAG, "❌ DanmakuView not prepared after waiting")
                    }
                }
            }
            
            // 如果已经在播放，启动弹幕
            if (player?.isPlaying == true && isEnabled) {
                view.start()
            }
            
            Log.d(TAG, "✅ Danmaku loading initiated")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Danmaku parse error: ${e.message}", e)
        }
    }
    
    /**
     * 🔥🔥 [新增] 在后台线程解析 XML 弹幕数据
     */
    private fun parseXmlDanmaku(rawData: ByteArray, ctx: DanmakuContext): List<BaseDanmaku> {
        val danmakuList = mutableListOf<BaseDanmaku>()
        
        try {
            val parser = Xml.newPullParser()
            parser.setInput(ByteArrayInputStream(rawData), "UTF-8")
            
            var eventType = parser.eventType
            var count = 0
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "d") {
                    val pAttr = parser.getAttributeValue(null, "p")
                    parser.next()
                    val content = if (parser.eventType == XmlPullParser.TEXT) parser.text else ""
                    
                    if (pAttr != null && content.isNotEmpty()) {
                        val danmaku = createDanmakuFromAttr(pAttr, content, ctx)
                        if (danmaku != null) {
                            danmakuList.add(danmaku)
                            count++
                            if (count <= 5) {
                                Log.d(TAG, "📝 Danmaku #$count: time=${danmaku.time}ms, text='${danmaku.text}'")
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            
            Log.d(TAG, "✅ Parsed $count danmakus total")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Parse error: ${e.message}", e)
        }
        
        return danmakuList
    }
    
    /**
     * 🔥🔥 [新增] 从属性字符串创建单条弹幕
     */
    private fun createDanmakuFromAttr(pAttr: String, content: String, ctx: DanmakuContext): BaseDanmaku? {
        try {
            val parts = pAttr.split(",")
            if (parts.size < 4) return null
            
            val time = (parts[0].toFloatOrNull() ?: 0f) * 1000  // 转换为毫秒
            val type = parts[1].toIntOrNull() ?: 1
            val fontSize = parts[2].toFloatOrNull() ?: 25f
            val colorInt = parts[3].toLongOrNull() ?: 0xFFFFFF
            
            // 映射弹幕类型
            val danmakuType = when (type) {
                1, 2, 3 -> BaseDanmaku.TYPE_SCROLL_RL
                4 -> BaseDanmaku.TYPE_FIX_BOTTOM
                5 -> BaseDanmaku.TYPE_FIX_TOP
                6 -> BaseDanmaku.TYPE_SCROLL_LR
                7 -> BaseDanmaku.TYPE_SPECIAL
                else -> BaseDanmaku.TYPE_SCROLL_RL
            }
            
            val danmaku = ctx.mDanmakuFactory?.createDanmaku(danmakuType, ctx) ?: return null
            danmaku.time = time.toLong()
            danmaku.text = content
            danmaku.textSize = fontSize * 2.0f  // 适中字体大小
            danmaku.textColor = colorInt.toInt() or 0xFF000000.toInt()
            danmaku.textShadowColor = if (colorInt == 0xFFFFFF.toLong()) Color.BLACK else Color.WHITE
            danmaku.flags = GlobalFlagValues()
            danmaku.priority = 0
            danmaku.isLive = false
            // 🔥🔥 [关键修复] 初始化 duration 以避免 NullPointerException
            danmaku.duration = master.flame.danmaku.danmaku.model.Duration(4000)  // 默认显示 4 秒
            
            return danmaku
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 显示弹幕 - 🔥🔥 [修复] 完整的重新启动逻辑
     */
    fun show() {
        val view = danmakuView ?: return
        Log.d(TAG, "👁️ show() called: isReady=$isReady, isPrepared=$isPrepared, playerPlaying=${player?.isPlaying}")
        
        // 1. 确保视图可见
        view.visibility = android.view.View.VISIBLE
        view.show()
        
        // 2. 如果播放器正在播放，需要完整重新启动弹幕
        if (player?.isPlaying == true && isReady && isPrepared) {
            // 同步到当前播放位置
            val position = player?.currentPosition ?: 0L
            view.seekTo(position)
            Log.d(TAG, "👁️ show(): seekTo($position), starting danmaku...")
            
            // 启动弹幕
            view.start()
            view.resume()
            Log.d(TAG, "✅ show(): danmaku restarted successfully")
        }
    }

    /**
     * 隐藏弹幕
     */
    fun hide() {
        danmakuView?.hide()
    }

    /**
     * 释放资源
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
    }
}

/**
 * 自定义 Bilibili 弹幕 XML 解析器
 * 解析格式: <d p="time,type,fontSize,color,timestamp,pool,userId,dmid">content</d>
 */
class BiliDanmakuXmlParser(private val rawData: ByteArray) : BaseDanmakuParser() {
    
    override fun parse(): IDanmakus {
        val danmakus = Danmakus()
        
        try {
            val parser = Xml.newPullParser()
            parser.setInput(ByteArrayInputStream(rawData), "UTF-8")
            
            var eventType = parser.eventType
            var danmakuCount = 0
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "d") {
                    val pAttr = parser.getAttributeValue(null, "p")
                    parser.next()
                    val content = if (parser.eventType == XmlPullParser.TEXT) parser.text else ""
                    
                    if (pAttr != null && content.isNotEmpty()) {
                        val danmaku = parseDanmakuItem(pAttr, content)
                        if (danmaku != null) {
                            danmakus.addItem(danmaku)
                            danmakuCount++
                            // 🔥🔥 [调试] 打印前 5 条弹幕
                            if (danmakuCount <= 5) {
                                Log.d("BiliDanmakuParser", "📝 Danmaku #$danmakuCount: time=${danmaku.time}ms, text='${danmaku.text}'")
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            
            Log.d("BiliDanmakuParser", "✅ Parsed $danmakuCount danmakus")
        } catch (e: Exception) {
            Log.e("BiliDanmakuParser", "❌ Parse error: ${e.message}", e)
        }
        
        return danmakus
    }
    
    /**
     * 解析单条弹幕
     * p 格式: time,type,fontSize,color,timestamp,pool,userId,dmid
     */
    private fun parseDanmakuItem(pAttr: String, content: String): BaseDanmaku? {
        try {
            val parts = pAttr.split(",")
            if (parts.size < 4) return null
            
            val time = (parts[0].toFloatOrNull() ?: 0f) * 1000  // 转换为毫秒
            val type = parts[1].toIntOrNull() ?: 1
            val fontSize = parts[2].toFloatOrNull() ?: 25f
            val colorInt = parts[3].toLongOrNull() ?: 0xFFFFFF
            
            // 映射弹幕类型
            val danmakuType = when (type) {
                1, 2, 3 -> BaseDanmaku.TYPE_SCROLL_RL  // 滚动弹幕
                4 -> BaseDanmaku.TYPE_FIX_BOTTOM      // 底部弹幕
                5 -> BaseDanmaku.TYPE_FIX_TOP         // 顶部弹幕
                6 -> BaseDanmaku.TYPE_SCROLL_LR       // 逆向滚动
                7 -> BaseDanmaku.TYPE_SPECIAL         // 高级弹幕
                else -> BaseDanmaku.TYPE_SCROLL_RL
            }
            
            // 🔥 检查 mContext 是否已初始化
            if (mContext == null || mContext.mDanmakuFactory == null) {
                Log.w("BiliDanmakuParser", "mContext or mDanmakuFactory is null, skipping danmaku")
                return null
            }
            
            val danmaku = mContext.mDanmakuFactory.createDanmaku(danmakuType, mContext)
            if (danmaku != null) {
                danmaku.time = time.toLong()
                danmaku.text = content
                // 🔥🔥 [修复] 使用正确的文本大小计算：fontSize * 密度因子
                // mDispDensity 通常是屏幕密度值，直接乘以 fontSize
                val calculatedSize = fontSize * mDispDensity
                danmaku.textSize = calculatedSize.coerceAtLeast(20f)  // 确保最小 20px
                danmaku.textColor = colorInt.toInt() or 0xFF000000.toInt()  // 确保不透明
                danmaku.textShadowColor = if (colorInt == 0xFFFFFF.toLong()) Color.BLACK else Color.WHITE
                
                // 🔥🔥 [关键修复] 初始化 flags 以避免 NullPointerException
                danmaku.flags = GlobalFlagValues()
                danmaku.priority = 0
                danmaku.isLive = false
            }
            
            return danmaku
        } catch (e: Exception) {
            Log.w("BiliDanmakuParser", "Failed to parse danmaku: $pAttr", e)
            return null
        }
    }
}

