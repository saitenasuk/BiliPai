// 文件路径: feature/settings/PlaybackSettingsScreen.kt
package com.android.purebilibili.feature.settings

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSTeal
import com.android.purebilibili.core.theme.iOSOrange
import com.android.purebilibili.core.theme.iOSSystemGray

/**
 * 🍎 播放设置二级页面
 * iOS 风格设计
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    
    var isStatsEnabled by remember { mutableStateOf(prefs.getBoolean("show_stats", false)) }
    var showPipPermissionDialog by remember { mutableStateOf(false) }
    
    // 🔥🔥 [修复] 设置导航栏透明，确保底部手势栏沉浸式效果
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        val originalNavBarColor = window?.navigationBarColor ?: android.graphics.Color.TRANSPARENT
        
        if (window != null) {
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        
        onDispose {
            if (window != null) {
                window.navigationBarColor = originalNavBarColor
            }
        }
    }
    
    // 检查画中画权限
    fun checkPipPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    Process.myUid(),
                    context.packageName
                )
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }
        return false
    }
    
    // 跳转到系统设置
    fun gotoPipSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(
                    "android.settings.PICTURE_IN_PICTURE_SETTINGS",
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:${context.packageName}")
            context.startActivity(intent)
        }
    }
    
    // 权限弹窗
    if (showPipPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPipPermissionDialog = false },
            title = { Text("权限申请", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("检测到未开启「画中画」权限。请在设置中开启该权限，否则无法使用小窗播放。", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        gotoPipSettings()
                        showPipPermissionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("去设置") }
            },
            dismissButton = {
                TextButton(onClick = { showPipPermissionDialog = false }) {
                    Text("暂不开启", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("播放设置", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        // 🔥🔥 [修复] 禁用 Scaffold 默认的 WindowInsets 消耗，避免底部填充
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            // 🔥🔥 [修复] 添加底部导航栏内边距，确保沉浸式效果
            contentPadding = WindowInsets.navigationBars.asPaddingValues()
        ) {
            // 🍎 解码设置
            item { SettingsSectionTitle("解码") }
            item {
                SettingsGroup {
                    SettingSwitchItem(
                        icon = Icons.Outlined.Memory,
                        title = "启用硬件解码",
                        subtitle = "减少发热和耗电 (推荐开启)",
                        checked = state.hwDecode,
                        onCheckedChange = { viewModel.toggleHwDecode(it) },
                        iconTint = iOSGreen
                    )
                }
            }
            
            // 🍎 后台播放
            item { SettingsSectionTitle("后台播放") }
            item {
                SettingsGroup {
                    SettingSwitchItem(
                        icon = Icons.Outlined.PictureInPicture,
                        title = "后台/画中画播放",
                        subtitle = "应用切到后台时继续播放",
                        checked = state.bgPlay,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                if (checkPipPermission()) {
                                    viewModel.toggleBgPlay(true)
                                } else {
                                    viewModel.toggleBgPlay(true)
                                    showPipPermissionDialog = true
                                }
                            } else {
                                viewModel.toggleBgPlay(false)
                            }
                        },
                        iconTint = iOSTeal
                    )
                }
            }
            
            // 🍎 手势设置
            item { SettingsSectionTitle("手势控制") }
            item {
                SettingsGroup {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.TouchApp,
                                contentDescription = null,
                                tint = iOSOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "手势灵敏度",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "调整快进/音量/亮度手势响应速度",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${(state.gestureSensitivity * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "较慢",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            // 🍎 iOS 风格滑块
                            io.github.alexzhirkevich.cupertino.CupertinoSlider(
                                value = state.gestureSensitivity,
                                onValueChange = { viewModel.setGestureSensitivity(it) },
                                valueRange = 0.5f..2.0f,
                                steps = 5,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )
                            Text(
                                "较快",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // 🍎 调试选项
            item { SettingsSectionTitle("调试") }
            item {
                SettingsGroup {
                    SettingSwitchItem(
                        icon = Icons.Outlined.Info,
                        title = "详细统计信息",
                        subtitle = "显示 Codec、码率等 Geek 信息",
                        checked = isStatsEnabled,
                        onCheckedChange = {
                            isStatsEnabled = it
                            prefs.edit().putBoolean("show_stats", it).apply()
                        },
                        iconTint = iOSSystemGray
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
