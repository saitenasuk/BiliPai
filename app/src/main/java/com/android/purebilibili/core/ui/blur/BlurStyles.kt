// 文件路径: core/ui/blur/BlurStyles.kt
package com.android.purebilibili.core.ui.blur

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * 🔥🔥 模糊强度枚举
 * 用户可选的三种模糊强度等级
 */
enum class BlurIntensity {
    ULTRA_THIN,  // 轻盈 - 通透感强，性能最佳
    THIN,        // 标准 - 平衡美观与性能（默认）
    THICK        // 浓郁 - 强烈磨砂质感
}

/**
 * 🎨 模糊样式管理
 * 
 * 提供三种用户可选的模糊强度：
 * - ULTRA_THIN: 轻盈模糊，通透感强，背景清晰可见
 * - THIN: 标准模糊，平衡美观与性能（推荐）
 * - THICK: 浓郁模糊，强烈磨砂质感
 * 
 * 同时根据Android版本提供最优的模糊配置：
 * - Android 16+: expressiveBlur (配合Material 3 Expressive)
 * - Android 12-15: standardBlur (标准Material模糊)
 * - Android 11: experimentalBlur (轻量，减少性能开销)
 */
object BlurStyles {
    
    /**
     * 🔥🔥 根据用户选择的强度获取模糊样式
     * 使用自定义参数实现更明显的层级差异
     */
    @Composable
    fun getBlurStyle(intensity: BlurIntensity): HazeStyle {
        // 🔥 使用 HazeMaterials 官方预设，效果更明显且稳定
        return when (intensity) {
            BlurIntensity.ULTRA_THIN -> HazeMaterials.ultraThin()  // 轻盈
            BlurIntensity.THIN -> HazeMaterials.thin()             // 标准
            BlurIntensity.THICK -> HazeMaterials.thick()           // 浓郁
        }
    }
    
    /**
     * Android 16 Material 3 Expressive风格
     * 更轻盈的模糊，配合系统设计语言
     */
    @RequiresApi(35)
    fun expressiveBlur(): HazeStyle {
        return HazeStyle(
            blurRadius = 15.dp,
            tint = HazeTint(Color.Transparent),
            noiseFactor = 0.05f
        )
    }
    
    /**
     * Android 12-15 标准风格
     * 等效于 HazeMaterials.thin() 但不需要 @Composable
     */
    fun standardBlur(): HazeStyle {
        return HazeStyle(
            blurRadius = 20.dp,
            tint = HazeTint(Color.White.copy(alpha = 0.7f)),
            noiseFactor = 0.04f
        )
    }
    
    /**
     * Android 11 实验性（轻量）
     * 等效于 HazeMaterials.ultraThin() 但不需要 @Composable
     */
    fun experimentalBlur(): HazeStyle {
        return HazeStyle(
            blurRadius = 12.dp,
            tint = HazeTint(Color.White.copy(alpha = 0.5f)),
            noiseFactor = 0.02f
        )
    }
    
    /**
     * 自动选择最优风格（兼容用户偏好）
     * 如果未提供偏好，使用默认标准强度
     */
    @Composable
    fun rememberOptimalBlurStyle(
        userPreference: BlurIntensity = BlurIntensity.THIN
    ): HazeStyle {
        return getBlurStyle(userPreference)
    }
    
    /**
     * 获取最优模糊风格（非Compose版本，基于Android版本）
     */
    fun getOptimalBlurStyle(): HazeStyle {
        return when {
            Build.VERSION.SDK_INT >= 35 -> expressiveBlur()
            Build.VERSION.SDK_INT >= 31 -> standardBlur()
            Build.VERSION.SDK_INT >= 30 -> experimentalBlur()
            else -> standardBlur()
        }
    }
}
