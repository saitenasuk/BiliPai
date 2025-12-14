// 文件路径: feature/home/components/BottomBar.kt
package com.android.purebilibili.feature.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.core.theme.iOSSystemGray

/**
 * 底部导航项枚举
 */
enum class BottomNavItem(
    val label: String,
    val selectedIcon: @Composable () -> Unit,
    val unselectedIcon: @Composable () -> Unit
) {
    HOME(
        "首页",
        { Icon(Icons.Filled.Home, null) },
        { Icon(Icons.Outlined.Home, null) }
    ),
    DYNAMIC(
        "动态",
        { Icon(Icons.Outlined.Subscriptions, null) },
        { Icon(Icons.Outlined.Subscriptions, null) }
    ),
    HISTORY(
        "历史",
        { Icon(Icons.Outlined.History, null) },
        { Icon(Icons.Outlined.History, null) }
    ),
    PROFILE(
        "我的",
        { Icon(Icons.Outlined.AccountCircle, null) },
        { Icon(Icons.Outlined.AccountCircle, null) }
    )
}

/**
 * 🔥 iOS 风格磨砂玻璃底部导航栏
 * 
 * 特性：
 * - 实时磨砂玻璃效果 (使用 Haze 库)
 * - 悬浮圆角设计
 * - 自动适配深色/浅色模式
 * - 🍎 点击触觉反馈
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun FrostedBottomBar(
    currentItem: BottomNavItem = BottomNavItem.HOME,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    isFloating: Boolean = true,
    onHomeDoubleTap: () -> Unit = {}  // 🍎 双击首页回到顶部
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f
    val haptic = rememberHapticFeedback()  // 🍎 触觉反馈

    // 🔥 样式参数计算
    // Floating 使用固定高度，Docked 使用自适应高度 (Content 64dp + SystemBars)
    val barHorizontalPadding = if (isFloating) 24.dp else 0.dp
    val barBottomPadding = if (isFloating) 16.dp else 0.dp
    val barShape = if (isFloating) RoundedCornerShape(36.dp) else androidx.compose.ui.graphics.RectangleShape  // 🍎 iOS 风格：紧贴底部无圆角
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = barHorizontalPadding)
            .padding(bottom = barBottomPadding)
            .then(if (isFloating) Modifier.navigationBarsPadding() else Modifier)
    ) {
        // 🔥 主内容层
        Surface(
            modifier = Modifier
                .then(
                    if (isFloating) {
                         Modifier
                            .shadow(
                                elevation = 8.dp,
                                shape = barShape,
                                ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            .height(72.dp) // Floating 固定高度
                    } else {
                        Modifier // Docked 高度由内容撑开
                    }
                )
                .fillMaxWidth()
                .clip(barShape)
                .then(
                    if (hazeState != null) {
                        Modifier.hazeChild(
                            state = hazeState,
                            style = HazeMaterials.thin(),
                            shape = barShape
                        )
                    } else {
                        Modifier
                    }
                ),
            // 🔥 背景色：模糊开启时透明让 Haze 负责，关闭时使用实心背景
            color = if (hazeState != null) {
                Color.Transparent  // Haze 全权负责模糊和着色
            } else {
                // 无模糊时使用实心背景
                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            }, 
            shape = barShape,
            shadowElevation = 0.dp,
            border = if (hazeState != null) {
                // 🍎 iOS 风格：非悬浮模式只显示顶部边框
                if (!isFloating) {
                    androidx.compose.foundation.BorderStroke(
                        width = 0.5.dp,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
                } else {
                    // 有模糊时显示边框增加质感
                    androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            )
                        )
                    )
                }
            } else {
                // 无模糊时使用更淡的边框
                androidx.compose.foundation.BorderStroke(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        ) {
            // 📱 Telegram 风格滑动指示器
            val itemCount = BottomNavItem.entries.size
            val selectedIndex = BottomNavItem.entries.indexOf(currentItem)
            
            // 🍎 iOS 风格：内容区固定高度，导航栏区域作为 padding 包含在 Surface 内
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isFloating) Modifier.fillMaxHeight()
                            else Modifier.height(64.dp)  // 非悬浮固定内容高度
                        )
                ) {
                // 🔥 考虑 Row 的 padding 后的实际可用宽度
                val rowPadding = 12.dp
                val actualContentWidth = maxWidth - (rowPadding * 2)
                val itemWidth = actualContentWidth / itemCount
                
                // 🔥 Telegram 风格滑动胶囊指示器
                val indicatorOffset by animateDpAsState(
                    targetValue = rowPadding + (itemWidth * selectedIndex) + (itemWidth - 60.dp) / 2,
                    animationSpec = spring(
                        dampingRatio = 0.7f,  // 柔和阻尼
                        stiffness = 400f       // 较快响应
                    ),
                    label = "indicator_offset"
                )
                
                // 指示器胶囊
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .padding(vertical = if (isFloating) 14.dp else 10.dp)
                        .width(60.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                )
                
                // 导航项 Row
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = rowPadding),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem.entries.forEach { item ->
                    val isSelected = item == currentItem
                    
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else iOSSystemGray,  // 🍎 iOS 系统灰
                        animationSpec = spring(),
                        label = "iconColor"
                    )
                    
                    // 🍎 弹性缩放动画 (选中时放大并弹跳)
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = 0.4f,  // 🍎 更低阻尼创造明显弹跳
                            stiffness = 350f
                        ),
                        label = "scale"
                    )
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(
                                if (item == BottomNavItem.HOME) {
                                    // 🍎 HOME 项支持双击回到顶部
                                    Modifier.pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = {
                                                haptic(HapticType.LIGHT)
                                                onItemClick(item)
                                            },
                                            onDoubleTap = {
                                                haptic(HapticType.MEDIUM)  // 双击用更强反馈
                                                onHomeDoubleTap()
                                            }
                                        )
                                    }
                                } else {
                                    // 其他项保持普通点击
                                    Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { 
                                        haptic(HapticType.LIGHT)
                                        onItemClick(item) 
                                    }
                                }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)  // 🍎 增大图标尺寸
                                .scale(scale),
                            contentAlignment = Alignment.Center
                        ) {
                            CompositionLocalProvider(
                                LocalContentColor provides iconColor
                            ) {
                                if (isSelected) item.selectedIcon() else item.unselectedIcon()
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = item.label,
                            fontSize = 12.sp,  // 🍎 增大字号
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,  // 🍎 未选中也使用 Medium
                            color = iconColor
                        )
                    }
                }
            }
            }  // 🔥 BoxWithConstraints 闭合
                
                // 🍎 iOS 风格：非悬浮模式时，导航栏区域作为 Spacer 包含在 Surface 内
                if (!isFloating) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    )
                }
            }  // 🔥 Column 闭合
        }
    }
}
