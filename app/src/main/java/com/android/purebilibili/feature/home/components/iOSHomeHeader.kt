// 文件路径: feature/home/components/iOSHomeHeader.kt
package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.iOSTapEffect
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.feature.home.UserState
import com.android.purebilibili.core.theme.iOSSystemGray
import dev.chrisbanes.haze.HazeState

/**
 * 🍎 简洁版首页头部 (带滚动隐藏/显示动画)
 * 
 * 注意：Header 不使用 hazeChild 模糊效果（会导致渲染问题）
 * 磨砂效果仅保留给 BottomBar（在屏幕底部可以正常工作）
 * hazeState 参数保留以保持 API 兼容性
 */
@Composable
fun iOSHomeHeader(
    scrollOffset: Float,
    user: UserState,
    onAvatarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    categoryIndex: Int,
    onCategorySelected: (Int) -> Unit,
    onPartitionClick: () -> Unit = {},  // 🔥 新增：分区按钮回调
    isScrollingUp: Boolean = true,
    collapseThreshold: androidx.compose.ui.unit.Dp = 60.dp,
    hazeState: HazeState? = null,  // 保留参数兼容性，但不用于模糊
    onStatusBarDoubleTap: () -> Unit = {}
) {
    val haptic = rememberHapticFeedback()
    val density = LocalDensity.current

    // 计算滚动进度
    val maxOffsetPx = with(density) { 50.dp.toPx() }
    val progress = (scrollOffset / maxOffsetPx).coerceIn(0f, 1f)
    
    // 状态栏高度
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val searchBarHeight = 52.dp
    val totalHeaderTopPadding = statusBarHeight + searchBarHeight
    
    // 背景颜色 - 始终使用实心背景
    val bgColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // ===== 分类标签栏 =====
        if (progress < 0.99f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = totalHeaderTopPadding)
                    .graphicsLayer {
                        alpha = 1f - progress
                        translationY = -progress * size.height * 0.8f
                        val scale = 1f - (progress * 0.15f)
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(bgColor)
            ) {
                CategoryTabRow(
                    selectedIndex = categoryIndex,
                    onCategorySelected = onCategorySelected,
                    onPartitionClick = onPartitionClick  // 🔥 传递分区回调
                )
            }
        }

        // ===== 搜索栏区域 - 使用简单的实心背景 =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .background(bgColor)
        ) {
            // 状态栏占位
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            haptic(HapticType.MEDIUM)
                            onStatusBarDoubleTap()
                        })
                    }
            )
            
            // 搜索栏 + 头像 + 设置
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .iOSTapEffect { onAvatarClick() }
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    if (user.isLogin && user.face.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(FormatUtils.fixImageUrl(user.face))
                                .crossfade(true).build(),
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("未", fontSize = 11.sp, fontWeight = FontWeight.Bold, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 🍎 搜索框 - iOS 风格
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { 
                            haptic(HapticType.LIGHT)
                            onSearchClick() 
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Search,
                            null,
                            tint = iOSSystemGray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "搜索视频、UP主...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = iOSSystemGray,
                            maxLines = 1
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // 设置按钮
                IconButton(
                    onClick = { 
                        haptic(HapticType.LIGHT)
                        onSettingsClick() 
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
