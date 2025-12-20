// 文件路径: feature/settings/PluginsScreen.kt
package com.android.purebilibili.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.plugin.PluginInfo
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSOrange
import com.android.purebilibili.core.theme.iOSPurple
import com.android.purebilibili.core.theme.iOSTeal
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import kotlinx.coroutines.launch

/**
 * 🔌 插件中心页面
 * 
 * 显示所有可用插件，支持启用/禁用和配置。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginsScreen(
    onBack: () -> Unit
) {
    val plugins by PluginManager.pluginsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    
    // 展开状态追踪
    var expandedPluginId by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("插件中心", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 标题说明
            item {
                Text(
                    text = "已安装插件".uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
                )
            }
            
            // 插件列表
            item {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Column {
                        plugins.forEachIndexed { index, pluginInfo ->
                            PluginItem(
                                pluginInfo = pluginInfo,
                                isExpanded = expandedPluginId == pluginInfo.plugin.id,
                                iconTint = getPluginColor(index),
                                onToggle = { enabled ->
                                    scope.launch {
                                        PluginManager.setEnabled(pluginInfo.plugin.id, enabled)
                                    }
                                },
                                onExpandToggle = {
                                    expandedPluginId = if (expandedPluginId == pluginInfo.plugin.id) {
                                        null
                                    } else {
                                        pluginInfo.plugin.id
                                    }
                                }
                            )
                            if (index < plugins.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .padding(start = 66.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                            }
                        }
                    }
                }
            }
            
            // 统计信息
            item {
                val enabledCount = plugins.count { it.enabled }
                Text(
                    text = "${plugins.size} 个插件，$enabledCount 个已启用",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 32.dp, top = 16.dp)
                )
            }
            
            // 底部说明
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "插件可以扩展应用功能，如自动跳过广告、过滤推荐内容等。\n启用插件后可点击展开查看详细设置。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun PluginItem(
    pluginInfo: PluginInfo,
    isExpanded: Boolean,
    iconTint: Color,
    onToggle: (Boolean) -> Unit,
    onExpandToggle: () -> Unit
) {
    val plugin = pluginInfo.plugin
    
    Column {
        // 主行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandToggle() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = plugin.icon ?: Icons.Outlined.Extension,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            // 标题和描述
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = plugin.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "v${plugin.version}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = plugin.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 开关
            CupertinoSwitch(
                checked = pluginInfo.enabled,
                onCheckedChange = onToggle
            )
            
            // 展开箭头
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "收起" else "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(20.dp)
            )
        }
        
        // 展开的配置区域
        AnimatedVisibility(
            visible = isExpanded && pluginInfo.enabled,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 66.dp, end = 16.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ) {
                plugin.SettingsContent()
            }
        }
    }
}

/**
 * 获取插件对应的颜色
 */
private fun getPluginColor(index: Int): Color {
    val colors = listOf(iOSTeal, iOSOrange, iOSBlue, iOSGreen, iOSPurple, BiliPink)
    return colors[index % colors.size]
}
