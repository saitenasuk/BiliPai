// 文件路径: feature/settings/AppearanceSettingsScreen.kt
package com.android.purebilibili.feature.settings

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSPink
import com.android.purebilibili.core.theme.iOSPurple
import com.android.purebilibili.core.theme.iOSTeal

/**
 * 🍎 外观设置二级页面
 * iOS 风格设计
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    
    var showThemeDialog by remember { mutableStateOf(false) }
    
    // 主题模式弹窗
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("外观模式", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    AppThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (state.themeMode == mode),
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = mode.label, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = { 
                TextButton(onClick = { showThemeDialog = false }) { 
                    Text("取消", color = MaterialTheme.colorScheme.primary) 
                } 
            },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("外观设置", fontWeight = FontWeight.SemiBold) },
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // 🍎 首页展示
            item { SettingsSectionTitle("首页展示") }
            item {
                SettingsGroup {
                    val displayMode = state.displayMode
                    
                    DisplayMode.entries.forEachIndexed { index, mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setDisplayMode(mode.value) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (displayMode == mode.value) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        // 分割线
                        if (index < DisplayMode.entries.size - 1) {
                            Divider(modifier = Modifier.padding(start = 16.dp)) 
                        }
                    }
                }
            }

            // 🍎 深色模式
            item { SettingsSectionTitle("主题") }
            item {
                SettingsGroup {
                    SettingClickableItem(
                        icon = Icons.Outlined.DarkMode,
                        title = "深色模式",
                        value = state.themeMode.label,
                        onClick = { showThemeDialog = true },
                        iconTint = iOSBlue
                    )
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Divider()
                        SettingSwitchItem(
                            icon = Icons.Outlined.Palette,
                            title = "动态取色 (Material You)",
                            subtitle = "跟随系统壁纸变换应用主题色",
                            checked = state.dynamicColor,
                            onCheckedChange = { viewModel.toggleDynamicColor(it) },
                            iconTint = iOSPink
                        )
                    }
                    
                    Divider()
                    
                    // 主题色选择器
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.ColorLens,
                                contentDescription = null,
                                tint = iOSPink,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "主题色",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (state.dynamicColor) "已启用动态取色，此设置无效" 
                                           else "选择应用主色调",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            com.android.purebilibili.core.theme.ThemeColors.forEachIndexed { index, color ->
                                val isSelected = state.themeColorIndex == index && !state.dynamicColor
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .then(
                                            if (isSelected) Modifier.border(
                                                3.dp, 
                                                MaterialTheme.colorScheme.onSurface,
                                                CircleShape
                                            ) else Modifier
                                        )
                                        .clickable(enabled = !state.dynamicColor) { 
                                            viewModel.setThemeColorIndex(index) 
                                        }
                                        .graphicsLayer { 
                                            alpha = if (state.dynamicColor) 0.4f else 1f 
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // 🍎 应用图标
            item { SettingsSectionTitle("图标") }
            item {
                SettingsGroup {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Apps,
                                contentDescription = null,
                                tint = iOSPurple,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "应用图标",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "切换个性化启动图标",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        data class IconOption(val key: String, val name: String, val desc: String)
                        val iconOptions = listOf(
                            IconOption("3D", "3D立体", "默认"),
                            IconOption("Blue", "经典蓝", "原版"),
                            IconOption("Retro", "复古怀旧", "80年代"),
                            IconOption("Flat", "扁平现代", "Material"),
                            IconOption("Neon", "霜虹发光", "赛博朋克")
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(iconOptions.size) { index ->
                                val option = iconOptions[index]
                                val isSelected = state.appIcon == option.key
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { 
                                                if (!isSelected) {
                                                    Toast.makeText(context, "正在切换图标...", Toast.LENGTH_SHORT).show()
                                                    viewModel.setAppIcon(option.key)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val iconRes = when(option.key) {
                                            "3D" -> com.android.purebilibili.R.mipmap.ic_launcher_3d
                                            "Blue" -> com.android.purebilibili.R.mipmap.ic_launcher_blue
                                            "Retro" -> com.android.purebilibili.R.mipmap.ic_launcher_retro
                                            "Flat" -> com.android.purebilibili.R.mipmap.ic_launcher_flat
                                            "Neon" -> com.android.purebilibili.R.mipmap.ic_launcher_neon
                                            else -> com.android.purebilibili.R.mipmap.ic_launcher
                                        }
                                        AsyncImage(
                                            model = iconRes,
                                            contentDescription = option.name,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .background(Color.Black.copy(alpha = 0.3f))
                                            )
                                            Icon(
                                                Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = option.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    Text(
                                        text = option.desc,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // 🍎 界面效果
            item { SettingsSectionTitle("界面效果") }
            item {
                SettingsGroup {
                    SettingSwitchItem(
                        icon = Icons.Outlined.ViewStream,
                        title = "悬浮底栏",
                        subtitle = "关闭后底栏将沉浸式贴底显示",
                        checked = state.isBottomBarFloating,
                        onCheckedChange = { viewModel.toggleBottomBarFloating(it) },
                        iconTint = iOSTeal
                    )
                    Divider()
                    
                    SettingSwitchItem(
                        icon = Icons.Outlined.BlurCircular,
                        title = "底栏磨砂效果",
                        subtitle = "底部导航栏的毛玻璃模糊",
                        checked = state.bottomBarBlurEnabled,
                        onCheckedChange = { viewModel.toggleBottomBarBlur(it) },
                        iconTint = iOSBlue
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
