// File: feature/video/ui/components/DanmakuSettingsPanel.kt
package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Danmaku Settings Panel
 * 
 * Displays a panel for configuring danmaku settings:
 * - Opacity
 * - Font scale
 * - Speed
 * 
 * Requirement Reference: AC2.4 - Reusable DanmakuSettingsPanel
 */
@Composable
fun DanmakuSettingsPanel(
    opacity: Float,
    fontScale: Float,
    speed: Float,
    onOpacityChange: (Float) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 360.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = false) {},
            color = Color(0xFF2B2B2B),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "\u5f39\u5e55\u8bbe\u7f6e",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Opacity slider
                DanmakuSliderItem(
                    label = "\u900f\u660e\u5ea6",
                    value = opacity,
                    valueRange = 0.3f..1f,
                    displayText = "${(opacity * 100).toInt()}%",
                    onValueChange = onOpacityChange
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Font scale slider
                DanmakuSliderItem(
                    label = "\u5b57\u4f53\u5927\u5c0f",
                    value = fontScale,
                    valueRange = 0.5f..2f,
                    displayText = "${(fontScale * 100).toInt()}%",
                    onValueChange = onFontScaleChange
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Speed slider
                DanmakuSliderItem(
                    label = "\u5f39\u5e55\u901f\u5ea6",
                    value = speed,
                    valueRange = 0.5f..2f,
                    displayText = when {
                        speed <= 0.7f -> "\u6162"
                        speed >= 1.5f -> "\u5feb"
                        else -> "\u4e2d"
                    },
                    onValueChange = onSpeedChange
                )
            }
        }
    }
}

@Composable
fun DanmakuSliderItem(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayText: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White.copy(0.9f),
                fontSize = 14.sp
            )
            Text(
                text = displayText,
                color = Color(0xFFFB7299),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFB7299),
                activeTrackColor = Color(0xFFFB7299),
                inactiveTrackColor = Color.White.copy(0.2f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
