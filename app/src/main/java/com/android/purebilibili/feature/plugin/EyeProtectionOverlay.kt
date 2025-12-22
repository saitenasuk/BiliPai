// 文件路径: feature/plugin/EyeProtectionOverlay.kt
package com.android.purebilibili.feature.plugin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.plugin.PluginManager

/**
 * 🌙 护眼覆盖层
 * 
 * 功能：
 * 1. 在夜间护眼模式激活时，添加半透明暖色覆盖层
 * 2. 显示休息提醒对话框
 * 
 * 使用方式：在 MainActivity 的根 Composable 中添加此组件
 */
@Composable
fun EyeProtectionOverlay() {
    // 获取插件实例
    val plugin = remember { EyeProtectionPlugin.getInstance() }
    
    if (plugin == null) return
    
    // 监听插件状态
    val isNightModeActive by plugin.isNightModeActive.collectAsState()
    val brightnessLevel by plugin.brightnessLevel.collectAsState()
    val warmFilterStrength by plugin.warmFilterStrength.collectAsState()
    val showRestReminder by plugin.showRestReminder.collectAsState()
    
    // 检查插件是否启用
    val pluginEnabled by remember {
        derivedStateOf {
            PluginManager.plugins.find { it.plugin.id == "eye_protection" }?.enabled == true
        }
    }
    
    if (!pluginEnabled) return
    
    // 🔥 护眼滤镜覆盖层
    AnimatedVisibility(
        visible = isNightModeActive,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 第一层：亮度降低 + 暖色滤镜
            // 🔥🔥 关键修复：使用 Canvas 绘制，不消耗触摸事件
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                // 🔥 亮度降低效果（黑色半透明覆盖）
                drawRect(
                    color = Color.Black.copy(alpha = (1f - brightnessLevel).coerceIn(0f, 0.7f))
                )
                // 🔥 暖色滤镜效果
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFF9800).copy(alpha = warmFilterStrength * 0.3f),
                            Color(0xFFFF5722).copy(alpha = warmFilterStrength * 0.2f)
                        )
                    )
                )
            }
        }
    }
    
    // 🔥 休息提醒对话框
    if (showRestReminder) {
        RestReminderDialog(
            onDismiss = { plugin.dismissRestReminder() },
            onRest = { plugin.resetUsageTime() }
        )
    }
}

/**
 * 休息提醒对话框
 */
@Composable
private fun RestReminderDialog(
    onDismiss: () -> Unit,
    onRest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = Color(0xFF7E57C2).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.SelfImprovement,
                    contentDescription = null,
                    tint = Color(0xFF7E57C2),
                    modifier = Modifier.size(36.dp)
                )
            }
        },
        title = {
            Text(
                "休息一下吧 👀",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "你已经使用了一段时间了",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "起来活动活动，看看远方\n保护眼睛从现在开始 💪",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onRest,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7E57C2)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("我去休息一下", fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "稍后提醒",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
