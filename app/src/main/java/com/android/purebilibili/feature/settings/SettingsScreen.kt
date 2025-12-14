// 文件路径: feature/settings/SettingsScreen.kt
package com.android.purebilibili.feature.settings

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSOrange
import com.android.purebilibili.core.theme.iOSPurple
import com.android.purebilibili.core.theme.iOSPink
import com.android.purebilibili.core.theme.iOSTeal
import com.android.purebilibili.core.ui.AppIcons
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import io.github.alexzhirkevich.cupertino.CupertinoSlider
import io.github.alexzhirkevich.cupertino.CupertinoSliderDefaults
import io.github.alexzhirkevich.cupertino.theme.CupertinoColors

const val GITHUB_URL = "https://github.com/jay3-yy/BiliPai/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit,
    onOpenSourceLicensesClick: () -> Unit,
    onAppearanceClick: () -> Unit = {},    // 🔥 外观设置
    onPlaybackClick: () -> Unit = {}       // 🔥 播放设置
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val state by viewModel.state.collectAsState()
    
    var showCacheDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshCacheSize()
    }

    // 缓存清理弹窗
    if (showCacheDialog) {
        AlertDialog(
            onDismissRequest = { showCacheDialog = false },
            title = { Text("清除缓存", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("确定要清除所有图片和视频缓存吗？", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCache()
                        Toast.makeText(context, "缓存已清除", Toast.LENGTH_SHORT).show()
                        showCacheDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("确认清除") }
            },
            dismissButton = { TextButton(onClick = { showCacheDialog = false }) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
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
            // 🔥 作者联系方式 (置顶)
            item { SettingsSectionTitle("关注作者") }
            item {
                SettingsGroup {
                    SettingClickableItem(
                        iconPainter = androidx.compose.ui.res.painterResource(com.android.purebilibili.R.drawable.ic_telegram_logo),
                        title = "Telegram 频道",
                        value = "@BiliPai",
                        onClick = { uriHandler.openUri("https://t.me/BiliPai") },
                        iconTint = Color.Unspecified
                    )
                    Divider()
                    SettingClickableItem(
                        icon = AppIcons.Twitter,
                        title = "Twitter / X",
                        value = "@YangY_0x00",
                        onClick = { uriHandler.openUri("https://x.com/YangY_0x00") },
                        iconTint = Color(0xFF1DA1F2)
                    )
                }
            }
            
            // 🍎 iOS 风格快捷入口
            item { SettingsSectionTitle("设置") }
            item {
                SettingsGroup {
                    SettingClickableItem(
                        icon = Icons.Outlined.Palette,
                        title = "外观设置",
                        value = "主题、图标、模糊效果",
                        onClick = onAppearanceClick,
                        iconTint = iOSPink
                    )
                    Divider()
                    SettingClickableItem(
                        icon = Icons.Outlined.PlayCircleOutline,
                        title = "播放设置",
                        value = "解码、手势、后台播放",
                        onClick = onPlaybackClick,
                        iconTint = iOSGreen
                    )
                }
            }
            
            item { SettingsSectionTitle("高级选项") }
            item {
                SettingsGroup {
                    SettingClickableItem(
                        icon = Icons.Outlined.DeleteOutline,
                        title = "清除缓存",
                        value = state.cacheSize,
                        onClick = { showCacheDialog = true },
                        iconTint = iOSPink
                    )
                    Divider()
                    SettingClickableItem(
                        icon = Icons.Outlined.Description,
                        title = "开源许可证",
                        value = "License",
                        onClick = onOpenSourceLicensesClick,
                        iconTint = iOSOrange
                    )
                    Divider()
                    SettingClickableItem(
                        icon = Icons.Outlined.Code,
                        title = "开源主页",
                        value = "GitHub",
                        onClick = { uriHandler.openUri(GITHUB_URL) },
                        iconTint = iOSPurple
                    )
                    Divider()
                    SettingClickableItem(
                        icon = Icons.Outlined.Info,
                        title = "版本",
                        value = "v${com.android.purebilibili.BuildConfig.VERSION_NAME}",
                        onClick = null,
                        iconTint = iOSTeal
                    )
                }
            }
            
            // 🧪 实验性功能
            item { SettingsSectionTitle("实验性功能") }
            item {
                SettingsGroup {
                    SettingSwitchItem(
                        icon = Icons.Outlined.HighQuality,
                        title = "登录用户默认 1080P",
                        subtitle = "已登录时自动选择最高画质",
                        checked = state.auto1080p,
                        onCheckedChange = { viewModel.toggleAuto1080p(it) },
                        iconTint = iOSBlue
                    )
                    Divider()
                    SettingSwitchItem(
                        icon = Icons.Outlined.SkipNext,
                        title = "自动跳过片头片尾",
                        subtitle = "视频开头/结尾时自动跳过 (部分视频)",
                        checked = state.autoSkipOpEd,
                        onCheckedChange = { viewModel.toggleAutoSkipOpEd(it) },
                        iconTint = iOSOrange
                    )
                    Divider()
                    SettingSwitchItem(
                        icon = Icons.Outlined.Speed,
                        title = "预加载下一个视频",
                        subtitle = "提前缓存推荐视频，消耗更多流量",
                        checked = state.prefetchVideo,
                        onCheckedChange = { viewModel.togglePrefetchVideo(it) },
                        iconTint = iOSGreen
                    )
                    Divider()
                    SettingSwitchItem(
                        icon = Icons.Outlined.ThumbUp,
                        title = "双击点赞",
                        subtitle = "双击视频画面快捷点赞",
                        checked = state.doubleTapLike,
                        onCheckedChange = { viewModel.toggleDoubleTapLike(it) },
                        iconTint = iOSPink
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

// ... 底部组件封装保持不变 ...
@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),  // 🍎 iOS 风格大写
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,  // 🍎 更淡的颜色
        letterSpacing = 0.5.sp,  // 🍎 字符间距
        modifier = Modifier.padding(start = 32.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp)),  // 🍎 iOS 圆角
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,  // 🍎 iOS 不太使用阴影
        tonalElevation = 1.dp
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingSwitchItem(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    // 🔥 新增：图标颜色
    iconTint: Color = BiliPink
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            // 🔥 彩色圆形背景图标
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // 🍎 iOS 风格开关
        CupertinoSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingClickableItem(
    icon: ImageVector? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    // 🔥 新增：图标颜色
    iconTint: Color = BiliPink
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null || iconPainter != null) {
            if (iconTint != Color.Unspecified) {
                // 🔥 彩色圆形背景图标
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                    } else if (iconPainter != null) {
                        Icon(painter = iconPainter, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                // 🔥 使用图标原始颜色（无背景容器）
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(36.dp))
                    } else if (iconPainter != null) {
                        Icon(painter = iconPainter, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(36.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
        }
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onClick != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun Divider() {
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(MaterialTheme.colorScheme.surfaceVariant))
}

fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)