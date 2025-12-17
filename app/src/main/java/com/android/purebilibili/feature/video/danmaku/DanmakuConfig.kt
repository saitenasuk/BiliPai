// 文件路径: feature/video/danmaku/DanmakuConfig.kt
package com.android.purebilibili.feature.video.danmaku

import android.content.Context
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.IDisplayer
import master.flame.danmaku.danmaku.model.android.DanmakuContext

/**
 * 弹幕配置管理
 * 
 * 管理弹幕的样式、速度、透明度等设置
 */
class DanmakuConfig {
    
    // 弹幕开关
    var isEnabled = true
    
    // 透明度 (0.0 - 1.0)
    var opacity = 0.85f
    
    // 字体缩放 (0.5 - 2.0)
    var fontScale = 1.0f
    
    // 滚动速度因子 (数值越大弹幕越慢)
    var speedFactor = 1.2f
    
    // 显示区域比例 (0.25, 0.5, 0.75, 1.0)
    var displayAreaRatio = 0.5f
    
    // 顶部边距（像素）
    var topMarginPx = 0
    
    /**
     * 应用配置到 DanmakuContext
     */
    fun applyTo(ctx: DanmakuContext, context: Context) {
        ctx.apply {
            // 描边样式
            setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3.5f)
            // 合并重复弹幕
            setDuplicateMergingEnabled(true)
            // 滚动速度
            setScrollSpeedFactor(speedFactor)
            // 字体大小
            setScaleTextSize(fontScale)
            // 透明度
            setDanmakuTransparency(opacity)
            // 禁用粗体
            setDanmakuBold(false)
            // 顶部边距
            val margin = if (topMarginPx > 0) topMarginPx else getStatusBarHeight(context) + 20
            setDanmakuMargin(margin)
            // 最大行数
            setMaximumLines(getMaxLines())
        }
    }
    
    /**
     * 更新单项配置
     */
    fun updateOpacity(ctx: DanmakuContext?, value: Float) {
        opacity = value
        ctx?.setDanmakuTransparency(value)
    }
    
    fun updateFontScale(ctx: DanmakuContext?, value: Float) {
        fontScale = value
        ctx?.setScaleTextSize(value)
    }
    
    fun updateSpeedFactor(ctx: DanmakuContext?, value: Float) {
        speedFactor = value
        ctx?.setScrollSpeedFactor(value)
    }
    
    fun updateTopMargin(ctx: DanmakuContext?, value: Int) {
        topMarginPx = value
        ctx?.setDanmakuMargin(value)
    }
    
    private fun getMaxLines(): Map<Int, Int> {
        val maxLines = when {
            displayAreaRatio <= 0.25f -> 3
            displayAreaRatio <= 0.5f -> 5
            displayAreaRatio <= 0.75f -> 8
            else -> Int.MAX_VALUE
        }
        return mapOf(
            BaseDanmaku.TYPE_SCROLL_RL to maxLines,
            BaseDanmaku.TYPE_SCROLL_LR to maxLines
        )
    }
    
    companion object {
        /**
         * 获取状态栏高度（像素）
         */
        fun getStatusBarHeight(context: Context): Int {
            val resourceId = context.resources.getIdentifier(
                "status_bar_height", "dimen", "android"
            )
            return if (resourceId > 0) {
                context.resources.getDimensionPixelSize(resourceId)
            } else {
                (24 * context.resources.displayMetrics.density).toInt()
            }
        }
    }
}
