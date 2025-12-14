package com.android.purebilibili.feature.video

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 🔥 iOS 风格毛玻璃卡片
 * 
 * 使用半透明背景和模糊效果创建类似 iOS 的磨砂玻璃效果
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    glassAlpha: Float = 0.15f,
    borderAlpha: Float = 0.2f,
    content: @Composable BoxScope.() -> Unit
) {
    // 🔥 使用 MaterialTheme 颜色代替硬编码
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline
    
    // 毛玻璃颜色 - 使用 surface 色
    val glassColor = surfaceColor.copy(alpha = glassAlpha + 0.5f)
    
    // 边框颜色 - 使用 outline 色
    val borderColor = outlineColor.copy(alpha = borderAlpha)
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(glassColor)
            .border(
                width = 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius)
            ),
        content = content
    )
}

/**
 * 🔥 iOS 风格时长标签 (毛玻璃效果)
 * 注意：此组件用于视频封面上，保持固定的黑色半透明背景以确保可读性
 */
@Composable
fun GlassDurationTag(
    duration: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        androidx.compose.material3.Text(
            text = duration,
            color = Color.White,
            fontSize = 11.sp
        )
    }
}
