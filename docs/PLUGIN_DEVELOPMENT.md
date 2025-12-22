# 🔌 BiliPai 插件开发指南

本文档详细介绍如何为 BiliPai 开发插件，扩展应用功能。

## 📋 目录

1. [插件系统概述](#插件系统概述)
2. [插件类型](#插件类型)
3. [快速开始](#快速开始)
4. [核心接口](#核心接口)
5. [配置持久化](#配置持久化)
6. [设置界面](#设置界面)
7. [注册插件](#注册插件)
8. [完整示例](#完整示例)
9. [最佳实践](#最佳实践)

---

## 插件系统概述

BiliPai 的插件系统采用**接口驱动设计**，允许开发者通过实现特定接口来扩展应用功能。

### 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    PureApplication                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                  PluginManager                       │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐   │   │
│  │  │ PlayerPlugin│ │DanmakuPlugin│ │  FeedPlugin │   │   │
│  │  └─────────────┘ └─────────────┘ └─────────────┘   │   │
│  │           ▲              ▲              ▲           │   │
│  │           └──────────────┼──────────────┘           │   │
│  │                          │                          │   │
│  │                   ┌──────┴──────┐                  │   │
│  │                   │   Plugin    │                  │   │
│  │                   └─────────────┘                  │   │
│  └─────────────────────────────────────────────────────┘   │
│                            │                                │
│                   ┌────────┴────────┐                      │
│                   │   PluginStore   │                      │
│                   │   (DataStore)   │                      │
│                   └─────────────────┘                      │
└─────────────────────────────────────────────────────────────┘
```

### 核心组件

| 组件 | 说明 |
|------|------|
| `Plugin` | 所有插件的基础接口 |
| `PluginManager` | 单例，管理插件注册、启用/禁用、生命周期 |
| `PluginStore` | 使用 DataStore 持久化插件状态和配置 |

---

## 插件类型

BiliPai 支持三种专用插件类型：

### 1️⃣ PlayerPlugin - 播放器增强

用于视频播放相关的功能增强。

**典型应用场景**：

- 自动跳过广告片段（如 SponsorBlock）
- 自动跳过片头/片尾
- 播放统计

**关键方法**：

```kotlin
interface PlayerPlugin : Plugin {
    suspend fun onVideoLoad(bvid: String, cid: Long)
    suspend fun onPositionUpdate(positionMs: Long): SkipAction?
    fun onVideoEnd()
}
```

### 2️⃣ DanmakuPlugin - 弹幕增强

用于弹幕的过滤和样式定制。

**典型应用场景**：

- 关键词屏蔽
- 同传弹幕高亮
- 弹幕翻译

**关键方法**：

```kotlin
interface DanmakuPlugin : Plugin {
    fun filterDanmaku(danmaku: DanmakuItem): DanmakuItem?
    fun styleDanmaku(danmaku: DanmakuItem): DanmakuStyle?
}
```

### 3️⃣ FeedPlugin - 信息流处理

用于首页推荐流的过滤和增强。

**典型应用场景**：

- 广告过滤
- 推广内容过滤
- 自定义过滤规则

**关键方法**：

```kotlin
interface FeedPlugin : Plugin {
    fun shouldShowItem(item: VideoItem): Boolean
}
```

---

## 快速开始

### 步骤 1：创建插件类

在 `feature/plugin/` 目录下创建新文件：

```kotlin
// feature/plugin/MyAwesomePlugin.kt
package com.android.purebilibili.feature.plugin

class MyAwesomePlugin : Plugin {
    override val id = "my_awesome_plugin"
    override val name = "我的插件"
    override val description = "这是一个示例插件"
    override val version = "1.0.0"
    
    override suspend fun onEnable() {
        Logger.d("MyPlugin", "✅ 插件已启用")
    }
    
    override suspend fun onDisable() {
        Logger.d("MyPlugin", "🔴 插件已禁用")
    }
}
```

### 步骤 2：注册插件

在 `PureApplication.kt` 中注册：

```kotlin
// app/PureApplication.kt
override fun onCreate() {
    super.onCreate()
    
    // 初始化插件系统
    PluginManager.initialize(this)
    
    // 注册你的插件
    PluginManager.register(MyAwesomePlugin())
}
```

---

## 核心接口

### Plugin 基础接口

```kotlin
interface Plugin {
    /** 唯一标识符 */
    val id: String
    
    /** 显示名称 */
    val name: String
    
    /** 插件描述 */
    val description: String
    
    /** 版本号 */
    val version: String
    
    /** 图标 (可选) */
    val icon: ImageVector?
        get() = null
    
    /** 启用时回调 */
    suspend fun onEnable() {}
    
    /** 禁用时回调 */
    suspend fun onDisable() {}
    
    /** 设置界面 (可选) */
    @Composable
    fun SettingsContent(): Unit = Unit
}
```

### SkipAction 跳过动作

PlayerPlugin 的 `onPositionUpdate` 方法返回值：

```kotlin
sealed class SkipAction {
    /** 不执行跳过 */
    object None : SkipAction()
    
    /** 自动跳转到指定位置 */
    data class SkipTo(
        val positionMs: Long,
        val reason: String
    ) : SkipAction()
    
    /** 显示跳过按钮（手动模式） */
    data class ShowButton(
        val skipToMs: Long,
        val label: String,
        val segmentId: String
    ) : SkipAction()
}
```

---

## 配置持久化

使用 `PluginStore` 保存和读取配置：

### 定义配置数据类

```kotlin
@Serializable
data class MyPluginConfig(
    val enabled: Boolean = true,
    val threshold: Int = 10,
    val keywords: List<String> = emptyList()
)
```

### 保存配置

```kotlin
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val config = MyPluginConfig(enabled = true, threshold = 20)
PluginStore.setConfigJson(context, pluginId, Json.encodeToString(config))
```

### 读取配置

```kotlin
import kotlinx.serialization.decodeFromString

val jsonStr = PluginStore.getConfigJson(context, pluginId)
if (jsonStr != null) {
    config = Json.decodeFromString<MyPluginConfig>(jsonStr)
}
```

---

## 设置界面

重写 `SettingsContent()` 方法提供 Compose UI：

```kotlin
@Composable
override fun SettingsContent() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(config.enabled) }
    
    // 加载配置
    LaunchedEffect(Unit) {
        loadConfig(context)
        enabled = config.enabled
    }
    
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 使用统一的设置组件
        com.android.purebilibili.feature.settings.SettingSwitchItem(
            icon = Icons.Default.Settings,
            title = "启用功能",
            subtitle = "功能描述",
            checked = enabled,
            onCheckedChange = { newValue ->
                enabled = newValue
                config = config.copy(enabled = newValue)
                runBlocking {
                    PluginStore.setConfigJson(context, id, Json.encodeToString(config))
                }
            }
        )
    }
}
```

### 推荐使用的设置组件

| 组件 | 用途 |
|------|------|
| `SettingSwitchItem` | 开关设置项 |
| `SettingClickableItem` | 可点击设置项 |
| `CupertinoSwitch` | iOS 风格开关 |

---

## 注册插件

### 在 Application 中注册

```kotlin
// app/PureApplication.kt
class PureApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 1. 初始化插件管理器
        PluginManager.initialize(this)
        
        // 2. 注册内置插件
        PluginManager.register(SponsorBlockPlugin())
        PluginManager.register(AdFilterPlugin())
        PluginManager.register(DanmakuEnhancePlugin())
        PluginManager.register(MyAwesomePlugin())  // 你的插件
    }
}
```

### 获取 Context

插件可通过 `PluginManager.getContext()` 获取 Application Context。

---

## 完整示例

### 示例：播放器插件（跳过片头）

```kotlin
// feature/plugin/IntroSkipperPlugin.kt
package com.android.purebilibili.feature.plugin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.runtime.*
import com.android.purebilibili.core.plugin.PlayerPlugin
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.core.plugin.SkipAction
import com.android.purebilibili.core.util.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

private const val TAG = "IntroSkipper"

class IntroSkipperPlugin : PlayerPlugin {
    
    override val id = "intro_skipper"
    override val name = "跳过片头"
    override val description = "自动跳过视频开头的固定片头"
    override val version = "1.0.0"
    override val icon = Icons.Outlined.SkipNext
    
    private var config = IntroSkipperConfig()
    private var hasSkipped = false
    
    override suspend fun onEnable() {
        Logger.d(TAG, "✅ 跳过片头已启用")
    }
    
    override suspend fun onDisable() {
        Logger.d(TAG, "🔴 跳过片头已禁用")
    }
    
    override suspend fun onVideoLoad(bvid: String, cid: Long) {
        hasSkipped = false
        loadConfig()
        Logger.d(TAG, "📦 视频加载: $bvid, 跳过时长: ${config.skipDurationSec}秒")
    }
    
    override suspend fun onPositionUpdate(positionMs: Long): SkipAction? {
        // 如果已跳过或位置超过设定时长，返回 None
        if (hasSkipped || positionMs > config.skipDurationSec * 1000) {
            return SkipAction.None
        }
        
        // 检测到在片头范围内
        if (positionMs < 1000) {  // 只在前1秒触发
            hasSkipped = true
            val targetMs = config.skipDurationSec * 1000L
            
            return if (config.autoSkip) {
                Logger.d(TAG, "⏭️ 自动跳过片头 -> ${targetMs}ms")
                SkipAction.SkipTo(targetMs, "已跳过片头")
            } else {
                SkipAction.ShowButton(targetMs, "跳过片头", "intro")
            }
        }
        
        return SkipAction.None
    }
    
    override fun onVideoEnd() {
        hasSkipped = false
    }
    
    private suspend fun loadConfig() {
        try {
            val context = PluginManager.getContext()
            val json = PluginStore.getConfigJson(context, id)
            if (json != null) {
                config = Json.decodeFromString(json)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "加载配置失败", e)
        }
    }
}

@Serializable
data class IntroSkipperConfig(
    val skipDurationSec: Int = 5,
    val autoSkip: Boolean = true
)
```

### 示例：信息流插件（关键词屏蔽）

```kotlin
// feature/plugin/KeywordBlockerPlugin.kt
class KeywordBlockerPlugin : FeedPlugin {
    
    override val id = "keyword_blocker"
    override val name = "关键词屏蔽"
    override val description = "根据关键词过滤推荐内容"
    override val version = "1.0.0"
    override val icon = Icons.Outlined.Block
    
    private var blockedKeywords = listOf("关键词1", "关键词2")
    
    override fun shouldShowItem(item: VideoItem): Boolean {
        val title = item.title.lowercase()
        
        for (keyword in blockedKeywords) {
            if (title.contains(keyword.lowercase())) {
                Logger.d("KeywordBlocker", "🚫 屏蔽: ${item.title}")
                return false
            }
        }
        
        return true
    }
}
```

---

## 最佳实践

### ✅ 推荐做法

1. **唯一 ID**：使用小写字母和下划线，如 `sponsor_block`
2. **异常处理**：在 `onEnable`/`onDisable` 中捕获异常
3. **日志记录**：使用 `Logger` 而非 `Log`
4. **配置持久化**：使用 `@Serializable` 和 `PluginStore`
5. **轻量初始化**：耗时操作放在 `onVideoLoad` 而非 `onEnable`

### ❌ 避免做法

1. **阻塞主线程**：耗时操作使用 `suspend` 或协程
2. **内存泄漏**：在 `onVideoEnd`/`onDisable` 中清理资源
3. **硬编码配置**：使用 DataStore 持久化用户设置
4. **忽略错误**：始终捕获并记录异常

### 调试技巧

```kotlin
// 使用 TAG 前缀方便过滤
private const val TAG = "MyPlugin"

Logger.d(TAG, "📦 调试信息")
Logger.w(TAG, "⚠️ 警告信息")
Logger.e(TAG, "❌ 错误信息", exception)
```

Logcat 过滤：

```
tag:MyPlugin
```

---

## 文件结构参考

```
app/src/main/java/com/android/purebilibili/
├── core/plugin/
│   ├── Plugin.kt              # 基础接口
│   ├── PlayerPlugin.kt        # 播放器插件接口
│   ├── DanmakuPlugin.kt       # 弹幕插件接口
│   ├── FeedPlugin.kt          # 信息流插件接口
│   ├── PluginManager.kt       # 插件管理器（单例）
│   └── PluginStore.kt         # 配置持久化
│
├── feature/plugin/
│   ├── SponsorBlockPlugin.kt  # 空降助手（示例）
│   ├── AdFilterPlugin.kt      # 去广告增强（示例）
│   ├── DanmakuEnhancePlugin.kt # 弹幕增强（示例）
│   └── MyAwesomePlugin.kt     # 你的插件
│
└── feature/settings/
    └── PluginsScreen.kt       # 插件中心 UI
```

---

## 需要帮助？

如有问题，请查阅：

- 现有插件实现：`feature/plugin/SponsorBlockPlugin.kt`
- PluginManager：`core/plugin/PluginManager.kt`
- 设置 UI 组件：`feature/settings/AppearanceSettingsScreen.kt`

Happy Coding! 🚀
