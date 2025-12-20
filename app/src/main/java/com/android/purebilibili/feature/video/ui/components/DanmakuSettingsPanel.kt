// File: feature/video/ui/components/DanmakuSettingsPanel.kt
package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Bilibili Pink
private val BiliPink = Color(0xFFFB7299)
private val BiliPinkLight = Color(0xFFFF9BB8)
private val PanelBackground = Color(0xFF1E1E1E)
private val CardBackground = Color(0xFF2A2A2A)

/**
 * Danmaku Settings Panel
 * 
 * A modern, visually appealing panel for configuring danmaku settings:
 * - Opacity
 * - Font scale
 * - Speed
 * - Display area
 * 
 * Requirement Reference: AC2.4 - Reusable DanmakuSettingsPanel
 */
@Composable
fun DanmakuSettingsPanel(
    opacity: Float,
    fontScale: Float,
    speed: Float,
    displayArea: Float = 0.5f,
    onOpacityChange: (Float) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onDisplayAreaChange: (Float) -> Unit = {},
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 300.dp, max = 380.dp)
                .heightIn(max = 500.dp)  // 限制最大高度
                .clickable(enabled = false) {},
            color = PanelBackground,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 16.dp,
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())  // 添加滚动支持
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "弹幕设置",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(0.1f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color.White.copy(0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Settings Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CardBackground,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Opacity slider
                        DanmakuSliderItem(
                            label = "透明度",
                            value = opacity,
                            valueRange = 0.3f..1f,
                            displayValue = { "${(it * 100).toInt()}%" },
                            onValueChange = onOpacityChange
                        )
                        
                        // Font scale slider
                        DanmakuSliderItem(
                            label = "字体大小",
                            value = fontScale,
                            valueRange = 0.5f..2f,
                            displayValue = { "${(it * 100).toInt()}%" },
                            onValueChange = onFontScaleChange
                        )
                        
                        // Speed slider
                        DanmakuSliderItem(
                            label = "弹幕速度",
                            value = speed,
                            valueRange = 0.5f..2f,
                            displayValue = { v ->
                                when {
                                    v >= 1.5f -> "慢"
                                    v <= 0.7f -> "快"
                                    else -> "中"
                                }
                            },
                            onValueChange = onSpeedChange
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Display area selector
                DanmakuAreaSelector(
                    currentArea = displayArea,
                    onAreaChange = onDisplayAreaChange
                )
            }
        }
    }
}

/**
 * 弹幕显示区域选择器
 */
@Composable
private fun DanmakuAreaSelector(
    currentArea: Float,
    onAreaChange: (Float) -> Unit
) {
    // 🔥 本地状态确保即时 UI 响应
    var localArea by remember(currentArea) { mutableFloatStateOf(currentArea) }
    
    data class AreaOption(val value: Float, val label: String, val subLabel: String)
    
    val areaOptions = listOf(
        AreaOption(0.25f, "1/4", "顶部"),
        AreaOption(0.5f, "1/2", "半屏"),
        AreaOption(0.75f, "3/4", "大部"),
        AreaOption(1.0f, "全屏", "铺满")
    )
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBackground,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "显示区域",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                areaOptions.forEach { option ->
                    // 🔥 使用本地状态判断选中状态
                    val isSelected = kotlin.math.abs(localArea - option.value) < 0.1f
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (isSelected) {
                                    Modifier.background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(BiliPink, BiliPinkLight)
                                        )
                                    )
                                } else {
                                    Modifier
                                        .background(Color.White.copy(0.05f))
                                        .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp))
                                }
                            )
                            .clickable { 
                                localArea = option.value  // 🔥 即时更新 UI
                                onAreaChange(option.value) 
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = option.label,
                                color = if (isSelected) Color.White else Color.White.copy(0.9f),
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = option.subLabel,
                                color = if (isSelected) Color.White.copy(0.8f) else Color.White.copy(0.5f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DanmakuSliderItem(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: (Float) -> String,
    onValueChange: (Float) -> Unit
) {
    var localValue by remember(value) { mutableFloatStateOf(value) }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Surface(
                color = BiliPink.copy(0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = displayValue(localValue),
                    color = BiliPink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Slider(
            value = localValue,
            onValueChange = { newValue ->
                localValue = newValue
                onValueChange(newValue)
            },
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = BiliPink,
                inactiveTrackColor = Color.White.copy(0.15f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
