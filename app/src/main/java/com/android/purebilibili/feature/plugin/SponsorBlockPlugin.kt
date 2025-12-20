// 文件路径: feature/plugin/SponsorBlockPlugin.kt
package com.android.purebilibili.feature.plugin

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.LocalUriHandler
import com.android.purebilibili.core.plugin.PlayerPlugin
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.core.plugin.SkipAction
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.model.response.SponsorSegment
import com.android.purebilibili.data.repository.SponsorBlockRepository
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

private const val TAG = "SponsorBlockPlugin"

/**
 * 🚀 空降助手插件
 * 
 * 基于 SponsorBlock 数据库自动跳过视频中的广告、赞助、片头片尾等片段。
 */
class SponsorBlockPlugin : PlayerPlugin {
    
    override val id = "sponsor_block"
    override val name = "空降助手"
    override val description = "自动跳过视频中的广告、赞助、片头片尾等片段"
    override val version = "1.0.0"
    override val icon: ImageVector = Icons.Outlined.RocketLaunch
    
    // 当前视频的跳过片段
    private var segments: List<SponsorSegment> = emptyList()
    
    // 已跳过的片段 UUID（防止重复跳过）
    private val skippedIds = mutableSetOf<String>()
    
    // 配置
    private var config: SponsorBlockConfig = SponsorBlockConfig()
    
    override suspend fun onEnable() {
        Logger.d(TAG, "✅ 空降助手已启用")
    }
    
    override suspend fun onDisable() {
        segments = emptyList()
        skippedIds.clear()
        Logger.d(TAG, "🔴 空降助手已禁用")
    }
    
    override suspend fun onVideoLoad(bvid: String, cid: Long) {
        // 重置状态
        segments = emptyList()
        skippedIds.clear()
        
        // 加载片段数据
        try {
            segments = SponsorBlockRepository.getSegments(bvid)
            Logger.d(TAG, "📦 加载了 ${segments.size} 个片段 for $bvid")
        } catch (e: Exception) {
            Logger.w(TAG, "⚠️ 加载片段失败: ${e.message}")
        }
    }
    
    override suspend fun onPositionUpdate(positionMs: Long): SkipAction? {
        if (segments.isEmpty()) return SkipAction.None
        
        // 查找当前位置是否在某个片段内
        val segment = segments.find { seg ->
            positionMs in seg.startTimeMs..seg.endTimeMs && seg.UUID !in skippedIds
        } ?: return SkipAction.None
        
        // 如果配置为自动跳过
        if (config.autoSkip) {
            skippedIds.add(segment.UUID)
            Logger.d(TAG, "⏭️ 自动跳过: ${segment.categoryName}")
            return SkipAction.SkipTo(
                positionMs = segment.endTimeMs,
                reason = "已跳过: ${segment.categoryName}"
            )
        }
        
        // 非自动跳过模式：返回 None，让 UI 层显示按钮
        return SkipAction.None
    }
    
    override fun onVideoEnd() {
        segments = emptyList()
        skippedIds.clear()
    }

    private fun loadConfig(context: Context) {
        runBlocking {
            val jsonStr = PluginStore.getConfigJson(context, id)
            if (jsonStr != null) {
                try {
                    config = Json.decodeFromString<SponsorBlockConfig>(jsonStr)
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to decode config", e)
                }
            }
        }
    }
    
    @Composable
    override fun SettingsContent() {
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        var autoSkip by remember { mutableStateOf(config.autoSkip) }
        
        // 加载配置
        LaunchedEffect(Unit) {
            loadConfig(context)
            autoSkip = config.autoSkip
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 使用原设置组件 - 自动跳过
            com.android.purebilibili.feature.settings.SettingSwitchItem(
                icon = Icons.Outlined.FlashOn,
                title = "自动跳过",
                subtitle = "关闭后将显示手动跳过按钮而非自动跳过",
                checked = autoSkip,
                onCheckedChange = { newValue ->
                    autoSkip = newValue
                    config = config.copy(autoSkip = newValue)
                    runBlocking {
                        PluginStore.setConfigJson(context, id, Json.encodeToString(config))
                    }
                },
                iconTint = androidx.compose.ui.graphics.Color(0xFFFF9800) // iOS Orange
            )
            
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            
            // 使用原设置组件 - 关于空降助手
            com.android.purebilibili.feature.settings.SettingClickableItem(
                icon = Icons.Outlined.Info,
                title = "关于空降助手",
                value = "BilibiliSponsorBlock",
                onClick = { uriHandler.openUri("https://github.com/hanydd/BilibiliSponsorBlock") },
                iconTint = androidx.compose.ui.graphics.Color(0xFF2196F3) // iOS Blue
            )
        }
    }
}

/**
 * 空降助手配置
 */
@Serializable
data class SponsorBlockConfig(
    val autoSkip: Boolean = true,
    val skipSponsor: Boolean = true,
    val skipIntro: Boolean = true,
    val skipOutro: Boolean = true,
    val skipInteraction: Boolean = true
)
