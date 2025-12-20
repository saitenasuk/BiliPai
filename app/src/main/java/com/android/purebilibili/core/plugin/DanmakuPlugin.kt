// 文件路径: core/plugin/DanmakuPlugin.kt
package com.android.purebilibili.core.plugin

import androidx.compose.ui.graphics.Color

/**
 * 💬 弹幕增强插件接口
 * 
 * 用于实现弹幕相关的增强功能，如：
 * - 关键词屏蔽
 * - 同传弹幕高亮
 * - 弹幕翻译
 */
interface DanmakuPlugin : Plugin {
    
    /**
     * 过滤弹幕
     * 
     * @param danmaku 原始弹幕
     * @return 处理后的弹幕，返回 null 表示屏蔽该弹幕
     */
    fun filterDanmaku(danmaku: DanmakuItem): DanmakuItem?
    
    /**
     * 获取弹幕样式
     * 
     * @param danmaku 弹幕内容
     * @return 自定义样式，返回 null 表示使用默认样式
     */
    fun styleDanmaku(danmaku: DanmakuItem): DanmakuStyle? = null
}

/**
 * 弹幕数据项（简化版，用于插件处理）
 */
data class DanmakuItem(
    val id: Long,
    val content: String,
    val timeMs: Long,
    val type: Int = 1,      // 1=滚动, 4=底部, 5=顶部
    val color: Int = 0xFFFFFF,
    val userId: String = ""
)

/**
 * 弹幕自定义样式
 */
data class DanmakuStyle(
    val textColor: Color? = null,
    val borderColor: Color? = null,
    val backgroundColor: Color? = null,
    val bold: Boolean = false,
    val scale: Float = 1.0f
)
