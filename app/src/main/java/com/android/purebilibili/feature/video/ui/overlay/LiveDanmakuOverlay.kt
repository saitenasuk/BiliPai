package com.android.purebilibili.feature.video.ui.overlay

import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.android.purebilibili.feature.live.LiveDanmakuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bytedance.danmaku.render.engine.control.DanmakuController
import com.bytedance.danmaku.render.engine.data.DanmakuData
import com.bytedance.danmaku.render.engine.render.draw.text.TextData
import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_BOTTOM_CENTER
import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_SCROLL
import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_TOP_CENTER
import com.bytedance.danmaku.render.engine.DanmakuView
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.delay

/**
 * 直播弹幕图层
 * 使用 ByteDance DanmakuRenderEngine 渲染
 */
@Composable
fun LiveDanmakuOverlay(
    danmakuFlow: SharedFlow<LiveDanmakuItem>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 使用 remember 保持 DanmakuView 和相关状态的稳定性
    val danmakuViewState = remember {
        object {
            var view: DanmakuView? = null
            var controller: DanmakuController? = null
            var startTime: Long = 0L
            val danmakuList = mutableListOf<DanmakuData>()
            var isStarted = false
        }
    }

    AndroidView(
        factory = { ctx ->
            DanmakuView(ctx).apply {
                // 设置透明背景
                setBackgroundColor(AndroidColor.TRANSPARENT)
                
                // 设置布局参数
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                // 保存引用
                danmakuViewState.view = this
                danmakuViewState.controller = this.controller
                danmakuViewState.startTime = System.currentTimeMillis()
                
                android.util.Log.d("LiveDanmakuOverlay", "🟢 DanmakuView created, starting controller")
                
                // 启动渲染引擎
                this.controller.start(0)
                danmakuViewState.isStarted = true
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { view ->
            // 确保控制器正在运行
            if (!danmakuViewState.isStarted) {
                android.util.Log.d("LiveDanmakuOverlay", "🟡 Controller not started, starting...")
                val currentTime = System.currentTimeMillis() - danmakuViewState.startTime
                view.controller.start(currentTime)
                danmakuViewState.isStarted = true
            }
        }
    )

    // 持续驱动播放时间更新 - 每帧调用 start() 来推进渲染
    LaunchedEffect(Unit) {
        while (true) {
            val ctrl = danmakuViewState.controller
            if (ctrl != null && danmakuViewState.isStarted) {
                val currentTime = System.currentTimeMillis() - danmakuViewState.startTime
                // 定期调用 start() 更新播放进度
                ctrl.start(currentTime)
            }
            delay(50) // ~20fps 足够流畅
        }
    }
    
    // 监听弹幕流
    LaunchedEffect(danmakuFlow) {
        danmakuFlow.collect { item ->
            val ctrl = danmakuViewState.controller
            if (ctrl != null) {
                android.util.Log.d("LiveDanmakuOverlay", "🔴 Received: ${item.text}")
                
                // 计算当前相对时间
                val currentTime = System.currentTimeMillis() - danmakuViewState.startTime
                val danmakuData = createDanmakuData(item, currentTime, context, ctrl)
                
                // 添加到列表
                val list = danmakuViewState.danmakuList
                // 移除过期弹幕 (20秒前)
                list.removeAll { it.showAtTime < currentTime - 20_000 }
                list.add(danmakuData)
                // 排序
                list.sortBy { it.showAtTime }
                
                android.util.Log.d("LiveDanmakuOverlay", "🔴 setData: size=${list.size}, time=$currentTime")
                
                // 更新数据
                ctrl.setData(list.toList(), currentTime)
                ctrl.invalidateView()
            }
        }
    }
    
    // 清理
    DisposableEffect(Unit) {
        onDispose {
            android.util.Log.d("LiveDanmakuOverlay", "🔴 Disposing DanmakuView")
            danmakuViewState.controller?.stop()
            danmakuViewState.danmakuList.clear()
            danmakuViewState.isStarted = false
        }
    }
}


private fun createDanmakuData(
    item: LiveDanmakuItem, 
    currentTime: Long, 
    context: android.content.Context,
    controller: DanmakuController?
): DanmakuData {
    val textSize = 42f
    val layerType = when (item.mode) {
        4 -> LAYER_TYPE_BOTTOM_CENTER
        5 -> LAYER_TYPE_TOP_CENTER
        else -> LAYER_TYPE_SCROLL
    }
    
    val textColor = if (item.color == 0) {
        AndroidColor.WHITE
    } else {
        (0xFF000000 or item.color.toLong()).toInt()
    }

    return com.android.purebilibili.feature.video.danmaku.createBitmapDanmaku(
        context = context,
        text = item.text,
        textColor = textColor,
        textSize = textSize,
        layerType = layerType,
        showAtTime = currentTime + 50L,
        onUpdate = {
            // 当图片加载完成后刷新视图
            controller?.invalidateView()
        }
    )
}
