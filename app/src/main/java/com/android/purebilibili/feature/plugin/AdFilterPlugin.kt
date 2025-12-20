// 文件路径: feature/plugin/AdFilterPlugin.kt
package com.android.purebilibili.feature.plugin

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.plugin.FeedPlugin
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.model.response.VideoItem
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

private const val TAG = "AdFilterPlugin"

/**
 * 🚫 去广告增强插件
 * 
 * 过滤首页推荐中的广告、推广、商业合作内容。
 */
class AdFilterPlugin : FeedPlugin {
    
    override val id = "adfilter"
    override val name = "去广告增强"
    override val description = "过滤首页推广内容、商业合作视频"
    override val version = "1.0.0"
    override val icon: ImageVector = Icons.Outlined.Block
    
    private var config: AdFilterConfig = AdFilterConfig()
    private var filteredCount = 0
    
    override suspend fun onEnable() {
        filteredCount = 0
        Logger.d(TAG, "✅ 去广告增强已启用")
    }
    
    override suspend fun onDisable() {
        Logger.d(TAG, "🔴 去广告增强已禁用，本次过滤了 $filteredCount 条内容")
        filteredCount = 0
    }
    
    override fun shouldShowItem(item: VideoItem): Boolean {
        // 检测推广关键词
        val title = item.title
        
        // 检测商业合作/恰饭
        if (config.filterSponsored) {
            val sponsorKeywords = listOf("商业合作", "恰饭", "推广", "广告")
            if (sponsorKeywords.any { title.contains(it, ignoreCase = true) }) {
                filteredCount++
                Logger.d(TAG, "🚫 过滤商业合作: $title")
                return false
            }
        }
        
        // 检测低质量标题
        if (config.filterClickbait) {
            val clickbaitPatterns = listOf(
                "震惊", "惊呆了", "太厉害了", "绝了", "离谱",
                "价值几万", "价值百万", "一定要看"
            )
            if (clickbaitPatterns.any { title.contains(it, ignoreCase = true) }) {
                filteredCount++
                Logger.d(TAG, "🚫 过滤标题党: $title")
                return false
            }
        }
        
        return true
    }
    
    private fun loadConfig(context: Context) {
        runBlocking {
            val jsonStr = PluginStore.getConfigJson(context, id)
            if (jsonStr != null) {
                try {
                    config = Json.decodeFromString<AdFilterConfig>(jsonStr)
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to decode config", e)
                }
            }
        }
    }
    
    @Composable
    override fun SettingsContent() {
        val context = LocalContext.current
        var filterSponsored by remember { mutableStateOf(config.filterSponsored) }
        var filterClickbait by remember { mutableStateOf(config.filterClickbait) }
        
        // 加载配置
        LaunchedEffect(Unit) {
            loadConfig(context)
            filterSponsored = config.filterSponsored
            filterClickbait = config.filterClickbait
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 过滤商业合作
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "过滤商业合作",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "隐藏带有\"恰饭\"\"推广\"等标签的视频",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CupertinoSwitch(
                    checked = filterSponsored,
                    onCheckedChange = { newValue ->
                        filterSponsored = newValue
                        config = config.copy(filterSponsored = newValue)
                        runBlocking { 
                            PluginStore.setConfigJson(context, id, Json.encodeToString(config)) 
                        }
                    }
                )
            }
            
            // 过滤标题党
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "过滤标题党",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "隐藏包含\"震惊\"\"惊呆了\"等词汇的视频",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CupertinoSwitch(
                    checked = filterClickbait,
                    onCheckedChange = { newValue ->
                        filterClickbait = newValue
                        config = config.copy(filterClickbait = newValue)
                        runBlocking { 
                            PluginStore.setConfigJson(context, id, Json.encodeToString(config)) 
                        }
                    }
                )
            }
        }
    }
}

/**
 * 去广告配置
 */
@Serializable
data class AdFilterConfig(
    val filterSponsored: Boolean = true,
    val filterClickbait: Boolean = false
)
