package com.android.purebilibili.core.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * 🔥 卡片位置管理器
 * 
 * 用于记录点击卡片的位置，以便在返回动画时
 * 将缩放动画指向正确的卡片位置
 */
object CardPositionManager {
    
    /**
     * 最后点击的卡片边界（在 Root 坐标系中）
     */
    var lastClickedCardBounds: Rect? = null
        private set
    
    /**
     * 最后点击的卡片中心点（归一化坐标 0-1）
     */
    var lastClickedCardCenter: Offset? = null
        private set
    
    /**
     * 🔥 是否正在从视频详情页返回
     * 用于跳过首页卡片的入场动画
     */
    var isReturningFromDetail: Boolean = false
        private set
    
    /**
     * 🔥 是否是单列卡片（故事卡片）
     * 用于决定导航动画方向：单列用垂直滑动，双列用水平滑动
     */
    var isSingleColumnCard: Boolean = false
        private set
    
    /**
     * 记录卡片位置
     * @param bounds 卡片在 Root 坐标系中的边界
     * @param screenWidth 屏幕宽度
     * @param screenHeight 屏幕高度
     * @param isSingleColumn 是否是单列卡片（故事卡片）
     */
    fun recordCardPosition(
        bounds: Rect, 
        screenWidth: Float, 
        screenHeight: Float,
        isSingleColumn: Boolean = false
    ) {
        lastClickedCardBounds = bounds
        // 计算归一化的中心点坐标 (0-1 范围)
        lastClickedCardCenter = Offset(
            x = bounds.center.x / screenWidth,
            y = bounds.center.y / screenHeight
        )
        isSingleColumnCard = isSingleColumn
    }
    
    /**
     * 🔥 标记正在返回
     */
    fun markReturning() {
        isReturningFromDetail = true
    }
    
    /**
     * 🔥 清除返回标记
     */
    fun clearReturning() {
        isReturningFromDetail = false
    }
    
    /**
     * 清除记录的位置
     */
    fun clear() {
        lastClickedCardBounds = null
        lastClickedCardCenter = null
        isReturningFromDetail = false
    }
}
