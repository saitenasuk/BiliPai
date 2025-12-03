// 文件路径: core/theme/Theme.kt
package com.android.purebilibili.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme // 🔥 导入
import androidx.compose.material3.dynamicLightColorScheme // 🔥 导入
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// --- 扩展颜色定义 ---
private val DarkSurfaceVariant = Color(0xFF2A2A2A) // 深色模式下的搜索框背景
private val LightSurfaceVariant = Color(0xFFF1F2F3) // 浅色模式下的搜索框背景

// 深色模式配色 (自定义保底)
private val DarkColorScheme = darkColorScheme(
    primary = BiliPink,
    onPrimary = White,
    secondary = BiliPinkDim,
    background = DarkBackground,  // Scaffold 背景 (深黑)
    surface = DarkSurface,        // Card 背景 (深灰)
    onSurface = TextPrimaryDark,  // 主要文字 (浅白)
    surfaceVariant = DarkSurfaceVariant, // 搜索框/次级背景
    onSurfaceVariant = TextSecondaryDark // 次要文字
)

// 浅色模式配色 (自定义保底)
private val LightColorScheme = lightColorScheme(
    primary = BiliPink,
    onPrimary = White,
    secondary = BiliPinkDim,
    background = BiliBackground, // Scaffold 背景 (浅灰)
    surface = White,             // Card 背景 (白)
    onSurface = TextPrimary,     // 主要文字 (黑)
    surfaceVariant = LightSurfaceVariant, // 搜索框背景
    onSurfaceVariant = TextSecondary // 次要文字
)

@Composable
fun PureBiliBiliTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // 🔥🔥 [新增] 接收动态取色参数
    content: @Composable () -> Unit
) {
    // 🔥🔥 [核心修改] 颜色选择逻辑
    val colorScheme = when {
        // 如果开启了动态取色 且 系统版本 >= Android 12 (S)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // 否则使用自定义配色
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}