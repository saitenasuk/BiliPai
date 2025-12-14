// 文件路径: feature/video/DanmakuManagerV2.kt
package com.android.purebilibili.feature.video

import android.content.Context
import android.graphics.Color
import android.util.Xml
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.repository.VideoRepository
import com.bytedance.danmaku.render.engine.DanmakuView
import com.bytedance.danmaku.render.engine.control.DanmakuController
import com.bytedance.danmaku.render.engine.data.DanmakuData
import com.bytedance.danmaku.render.engine.render.draw.text.TextData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream

/**
 * 弹幕管理器 V2 - 使用 ByteDance DanmakuRenderEngine
 * 
 * 相比旧版 DanmakuFlameMaster 的优势：
 * 1. 更高性能渲染和更低内存使用
 * 2. 更现代的 API 设计
 * 3. 活跃维护
 * 
 * 职责：
 * 1. 加载和解析 B站弹幕数据
 * 2. 与 ExoPlayer 同步弹幕播放
 * 3. 管理弹幕生命周期
 */
class DanmakuManagerV2(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "DanmakuManagerV2"
        
        // LayerType 常量 (来自 DanmakuRenderEngine utils/Constants.kt)
        const val LAYER_TYPE_SCROLL = 1001        // 滚动弹幕
        const val LAYER_TYPE_TOP_CENTER = 1002    // 顶部居中
        const val LAYER_TYPE_BOTTOM_CENTER = 1003 // 底部居中
        
        // DrawType 常量
        const val DRAW_TYPE_TEXT = 1001           // 文本类型
    }

    private var danmakuView: DanmakuView? = null
    private var controller: DanmakuController? = null
    private var player: ExoPlayer? = null
    private var playerListener: Player.Listener? = null
    private var loadJob: Job? = null
    
    // 弹幕状态
    private var isDataLoaded = false
    private var currentDanmakuList: List<DanmakuData> = emptyList()
    
    // 弹幕设置
    var isEnabled = true
        set(value) {
            field = value
            Logger.d(TAG, "🎚️ isEnabled set to $value, controller=${controller != null}")
            // 只有在 controller 绑定后才执行 show/hide
            if (controller != null) {
                if (value) show() else hide()
            }
        }
    
    var opacity = 1.0f
        set(value) {
            field = value
            updateConfig()
        }
    
    var fontScale = 1.0f
        set(value) {
            field = value
            // 需要重新加载弹幕以应用新的字体大小
        }
    
    var speedFactor = 1.2f
        set(value) {
            field = value
            updateConfig()
        }
    
    var displayAreaRatio = 0.5f
        set(value) {
            field = value
            updateConfig()
        }

    /**
     * 绑定 DanmakuView
     */
    fun attachView(view: DanmakuView) {
        Logger.d(TAG, "📎 attachView: view.width=${view.width}, view.height=${view.height}")
        danmakuView = view
        controller = view.controller
        
        Logger.d(TAG, "✅ DanmakuView attached, controller=${controller != null}, config=${controller?.config != null}")
        
        // 🔥 如果弹幕数据已经加载，立即设置到 controller
        if (isDataLoaded && currentDanmakuList.isNotEmpty()) {
            Logger.d(TAG, "📊 Setting pending danmaku data: ${currentDanmakuList.size} items")
            controller?.setData(currentDanmakuList, 0L)
            
            // 如果正在播放，启动弹幕
            if (player?.isPlaying == true && isEnabled) {
                val position = player?.currentPosition ?: 0L
                Logger.d(TAG, "▶️ Starting danmaku from position: $position")
                controller?.start(position)
            }
        }
    }

    /**
     * 绑定 ExoPlayer 并同步弹幕
     */
    fun attachPlayer(exoPlayer: ExoPlayer) {
        Logger.d(TAG, "🎬 attachPlayer")
        
        // 移除旧的监听器
        playerListener?.let { player?.removeListener(it) }
        
        player = exoPlayer
        
        // 创建新的监听器
        playerListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Logger.d(TAG, "🎬 onIsPlayingChanged: isPlaying=$isPlaying, isDataLoaded=$isDataLoaded, isEnabled=$isEnabled")
                if (isPlaying && isDataLoaded && isEnabled) {
                    val position = exoPlayer.currentPosition
                    controller?.start(position)
                } else {
                    controller?.pause()
                }
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                Logger.d(TAG, "🎬 onPlaybackStateChanged: state=$playbackState")
                when (playbackState) {
                    Player.STATE_READY -> {
                        if (exoPlayer.isPlaying && isDataLoaded && isEnabled) {
                            controller?.start(exoPlayer.currentPosition)
                        }
                    }
                    Player.STATE_ENDED -> {
                        controller?.stop()
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
                    Logger.d(TAG, "🎬 Seek detected, sync danmaku to ${newPosition.positionMs}ms")
                    if (isDataLoaded && isEnabled) {
                        controller?.start(newPosition.positionMs)
                    }
                }
            }
        }
        
        exoPlayer.addListener(playerListener!!)
    }
    
    /**
     * 更新弹幕配置
     */
    private fun updateConfig() {
        controller?.let { ctrl ->
            val config = ctrl.config
            // DanmakuConfig 可以在这里修改
            // 例如：config.common.alpha = opacity
        }
    }

    /**
     * 加载弹幕数据
     */
    fun loadDanmaku(cid: Long) {
        Logger.d(TAG, "📥 loadDanmaku: cid=$cid")
        
        loadJob?.cancel()
        loadJob = scope.launch {
            try {
                val rawData = VideoRepository.getDanmakuRawData(cid)
                if (rawData == null || rawData.isEmpty()) {
                    Logger.w(TAG, "⚠️ Danmaku data is null or empty")
                    return@launch
                }
                
                Logger.d(TAG, "📥 Danmaku raw data loaded: ${rawData.size} bytes")
                
                // 在后台线程解析弹幕
                val danmakuList = withContext(Dispatchers.Default) {
                    parseBiliDanmaku(rawData)
                }
                
                Logger.d(TAG, "📊 Parsed ${danmakuList.size} danmakus")
                
                withContext(Dispatchers.Main) {
                    currentDanmakuList = danmakuList
                    isDataLoaded = true
                    
                    Logger.d(TAG, "🔧 controller=${controller != null}, danmakuView=${danmakuView != null}, player=${player != null}")
                    
                    // 设置弹幕数据
                    if (controller != null) {
                        val position = player?.currentPosition ?: 0L
                        // 🔥 使用带位置的 setData，会自动调用 onPlay
                        controller?.setData(danmakuList, position)
                        Logger.d(TAG, "📤 setData called with ${danmakuList.size} items at position $position")
                        
                        // 🔥🔥 强制启动弹幕并刷新视图
                        if (isEnabled) {
                            controller?.start(position)
                            danmakuView?.invalidate()
                            Logger.d(TAG, "▶️ Started danmaku and invalidated view")
                        }
                    } else {
                        Logger.w(TAG, "⚠️ controller is NULL, danmaku data will be set when view is attached")
                    }
                    
                    Logger.d(TAG, "✅ Danmaku data set successfully")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "❌ Failed to load danmaku: ${e.message}", e)
            }
        }
    }
    
    /**
     * 解析 B站弹幕 XML 格式 -> DanmakuRenderEngine 的 TextData 列表
     * 
     * B站弹幕 p 属性格式: time,type,fontSize,color,timestamp,pool,userId,dmid
     * - type 1,2,3: 滚动弹幕 (从右向左)
     * - type 4: 底部弹幕
     * - type 5: 顶部弹幕
     * - type 6: 逆向滚动 (从左向右，较少见)
     * - type 7: 高级弹幕 (复杂，暂不支持)
     */
    private fun parseBiliDanmaku(rawData: ByteArray): List<DanmakuData> {
        val danmakuList = mutableListOf<DanmakuData>()
        
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
                        val danmaku = createTextData(pAttr, content)
                        if (danmaku != null) {
                            danmakuList.add(danmaku)
                            count++
                            if (count <= 5) {
                                Logger.d(TAG, "📝 Danmaku #$count: time=${danmaku.showAtTime}ms, text='${danmaku.text}'")
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            
            Logger.d(TAG, "✅ Parsed $count danmakus total")
        } catch (e: Exception) {
            Logger.e(TAG, "❌ Parse error: ${e.message}", e)
        }
        
        return danmakuList
    }
    
    /**
     * 从 B站弹幕属性创建 TextData
     */
    private fun createTextData(pAttr: String, content: String): TextData? {
        try {
            val parts = pAttr.split(",")
            if (parts.size < 4) return null
            
            val timeSeconds = parts[0].toFloatOrNull() ?: 0f
            val timeMs = (timeSeconds * 1000).toLong()
            val type = parts[1].toIntOrNull() ?: 1
            val fontSize = parts[2].toFloatOrNull() ?: 25f
            val colorInt = parts[3].toLongOrNull() ?: 0xFFFFFF
            
            // 映射弹幕类型到 LayerType
            val layerType = when (type) {
                1, 2, 3 -> LAYER_TYPE_SCROLL        // 滚动弹幕
                4 -> LAYER_TYPE_BOTTOM_CENTER       // 底部弹幕
                5 -> LAYER_TYPE_TOP_CENTER          // 顶部弹幕
                6 -> LAYER_TYPE_SCROLL              // 逆向滚动 (暂用普通滚动)
                else -> LAYER_TYPE_SCROLL           // 默认滚动
            }
            
            // 计算文字颜色 (确保不透明)
            val textColor = colorInt.toInt() or 0xFF000000.toInt()
            
            // 描边颜色：白色文字用黑色描边，其他用白色描边
            val strokeColor = if (colorInt == 0xFFFFFF.toLong()) Color.BLACK else Color.WHITE
            
            // 创建 TextData
            return TextData().apply {
                this.layerType = layerType
                this.drawType = DRAW_TYPE_TEXT
                this.showAtTime = timeMs
                this.text = content
                this.textColor = textColor
                this.textSize = fontSize * fontScale * 2.5f  // 适配屏幕密度
                this.textStrokeWidth = 2.5f
                this.textStrokeColor = strokeColor
            }
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to parse danmaku: $pAttr", e)
            return null
        }
    }

    /**
     * 显示弹幕
     */
    fun show() {
        Logger.d(TAG, "👁️ show()")
        danmakuView?.visibility = android.view.View.VISIBLE
        if (player?.isPlaying == true && isDataLoaded) {
            controller?.start(player?.currentPosition ?: 0L)
        }
    }

    /**
     * 隐藏弹幕
     */
    fun hide() {
        Logger.d(TAG, "🔒 hide()")
        controller?.pause()
        danmakuView?.visibility = android.view.View.GONE
    }
    
    /**
     * Seek 到指定位置
     */
    fun seekTo(position: Long) {
        Logger.d(TAG, "⏩ seekTo: $position")
        if (isDataLoaded && isEnabled) {
            controller?.start(position)
        }
    }
    
    /**
     * 暂停弹幕
     */
    fun pause() {
        controller?.pause()
    }
    
    /**
     * 恢复弹幕
     */
    fun resume() {
        if (isEnabled && isDataLoaded) {
            controller?.start(player?.currentPosition ?: 0L)
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        Logger.d(TAG, "🗑️ release")
        loadJob?.cancel()
        playerListener?.let { player?.removeListener(it) }
        controller?.stop()
        controller?.clear()
        danmakuView = null
        controller = null
        player = null
        playerListener = null
        isDataLoaded = false
        currentDanmakuList = emptyList()
    }
}
