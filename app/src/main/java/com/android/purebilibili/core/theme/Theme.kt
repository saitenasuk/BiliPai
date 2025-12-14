// 文件路径: core/theme/Theme.kt
package com.android.purebilibili.core.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// --- 扩展颜色定义 ---
private val LightSurfaceVariant = Color(0xFFF1F2F3)

// 🔥🔥 [优化] 根据主题色索引生成配色方案
private fun createDarkColorScheme(primaryColor: Color) = darkColorScheme(
    primary = primaryColor,
    onPrimary = White,
    secondary = primaryColor.copy(alpha = 0.85f),
    background = DarkBackground,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = DarkSurfaceElevated,
    outline = Color(0xFF3D3D3D),
    outlineVariant = Color(0xFF2A2A2A)
)

private fun createLightColorScheme(primaryColor: Color) = lightColorScheme(
    primary = primaryColor,
    onPrimary = White,
    secondary = primaryColor.copy(alpha = 0.8f),
    background = Color(0xFFF5F5F5),  // 🔥 柔和浅灰背景
    surface = Color(0xFFFAFAFA),     // 🔥 微灰表面，不再纯白
    onSurface = TextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondary
)

// 保留默认配色作为后备 (使用 iOS 系统蓝)
private val DarkColorScheme = createDarkColorScheme(iOSSystemBlue)
private val LightColorScheme = createLightColorScheme(iOSSystemBlue)

@Composable
fun PureBiliBiliTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themeColorIndex: Int = 0, // 🔥🔥 默认 0 = iOS 蓝色
    content: @Composable () -> Unit
) {
    // 🔥 获取自定义主题色 (默认 iOS 蓝)
    val customPrimaryColor = ThemeColors.getOrElse(themeColorIndex) { iOSSystemBlue }
    
    val colorScheme = when {
        // 如果开启了动态取色 且 系统版本 >= Android 12 (S)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // 🔥🔥 [新增] 使用自定义主题色
        darkTheme -> createDarkColorScheme(customPrimaryColor)
        else -> createLightColorScheme(customPrimaryColor)
    }

    // 🔥🔥 [新增] 动态设置状态栏图标颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 设置状态栏图标颜色：
            // - 深色模式：使用浅色图标 (isAppearanceLightStatusBars = false)
            // - 浅色模式：使用深色图标 (isAppearanceLightStatusBars = true)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BiliTypography,
        content = content
    )
}