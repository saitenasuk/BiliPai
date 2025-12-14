// 文件路径: feature/settings/SettingsViewModel.kt
package com.android.purebilibili.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.util.CacheUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val hwDecode: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.FOLLOW_SYSTEM,
    val dynamicColor: Boolean = true,
    val bgPlay: Boolean = false,
    val gestureSensitivity: Float = 1.0f,
    val themeColorIndex: Int = 0,
    val appIcon: String = "3D",
    val isBottomBarFloating: Boolean = true,
    val headerBlurEnabled: Boolean = true,
    val bottomBarBlurEnabled: Boolean = true,
    val displayMode: Int = 0,
    val cacheSize: String = "计算中...",
    // 🧪 实验性功能
    val auto1080p: Boolean = true,
    val autoSkipOpEd: Boolean = false,
    val prefetchVideo: Boolean = false,
    val doubleTapLike: Boolean = true
)

// 内部数据类，用于分批合并流
private data class CoreSettings(
    val hwDecode: Boolean,
    val themeMode: AppThemeMode,
    val dynamicColor: Boolean,
    val bgPlay: Boolean
)

data class ExtraSettings(
    val gestureSensitivity: Float,
    val themeColorIndex: Int,
    val appIcon: String,
    val isBottomBarFloating: Boolean,
    val headerBlurEnabled: Boolean,
    val bottomBarBlurEnabled: Boolean,
    val displayMode: Int
)

// 🧪 实验性功能设置
data class ExperimentalSettings(
    val auto1080p: Boolean,
    val autoSkipOpEd: Boolean,
    val prefetchVideo: Boolean,
    val doubleTapLike: Boolean
)

private data class BaseSettings(
    val hwDecode: Boolean,
    val themeMode: AppThemeMode,
    val dynamicColor: Boolean,
    val bgPlay: Boolean,
    val gestureSensitivity: Float,
    val themeColorIndex: Int,
    val appIcon: String,
    val isBottomBarFloating: Boolean,
    val headerBlurEnabled: Boolean,
    val bottomBarBlurEnabled: Boolean,
    val displayMode: Int // 🔥 新增
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext

    // 本地状态流：缓存大小
    private val _cacheSize = MutableStateFlow("计算中...")

    // 🔥🔥 [核心修复] 分步合并，解决 combine 参数限制报错
    // 第 1 步：合并前 4 个设置
    private val coreSettingsFlow = combine(
        SettingsManager.getHwDecode(context),
        SettingsManager.getThemeMode(context),
        SettingsManager.getDynamicColor(context),
        SettingsManager.getBgPlay(context)
    ) { hwDecode, themeMode, dynamicColor, bgPlay ->
        CoreSettings(hwDecode, themeMode, dynamicColor, bgPlay)
    }
    
    // 第 2 步：合并界面设置 (5个) - 增加 DisplayMode
    private val uiSettingsFlow = combine(
        SettingsManager.getGestureSensitivity(context),
        SettingsManager.getThemeColorIndex(context),
        SettingsManager.getAppIcon(context),
        SettingsManager.getBottomBarFloating(context),
        SettingsManager.getDisplayMode(context) // 🔥 新增
    ) { gestureSensitivity, themeColorIndex, appIcon, isBottomBarFloating, displayMode ->
        listOf(gestureSensitivity, themeColorIndex, appIcon, isBottomBarFloating, displayMode)
    }
    
    // 第 3 步：合并模糊设置 (2个)
    private val blurSettingsFlow = combine(
        SettingsManager.getHeaderBlurEnabled(context),
        SettingsManager.getBottomBarBlurEnabled(context)
    ) { headerBlur, bottomBarBlur ->
        Pair(headerBlur, bottomBarBlur)
    }
    
    // 第 4 步：合并 UI 和 模糊设置
    private val extraSettingsFlow = combine(uiSettingsFlow, blurSettingsFlow) { ui, blur ->
        ExtraSettings(
            gestureSensitivity = ui[0] as Float,
            themeColorIndex = ui[1] as Int,
            appIcon = ui[2] as String,
            isBottomBarFloating = ui[3] as Boolean,
            displayMode = ui[4] as Int,
            headerBlurEnabled = blur.first,
            bottomBarBlurEnabled = blur.second
        )
    }
    
    // 🧪 第 4.5 步：合并实验性功能设置
    private val experimentalSettingsFlow = combine(
        SettingsManager.getAuto1080p(context),
        SettingsManager.getAutoSkipOpEd(context),
        SettingsManager.getPrefetchVideo(context),
        SettingsManager.getDoubleTapLike(context)
    ) { auto1080p, autoSkipOpEd, prefetchVideo, doubleTapLike ->
        ExperimentalSettings(auto1080p, autoSkipOpEd, prefetchVideo, doubleTapLike)
    }
    
    // 第 5 步：合并两组设置
    private val baseSettingsFlow = combine(coreSettingsFlow, extraSettingsFlow) { core, extra ->
        BaseSettings(
            hwDecode = core.hwDecode,
            themeMode = core.themeMode,
            dynamicColor = core.dynamicColor,
            bgPlay = core.bgPlay,
            gestureSensitivity = extra.gestureSensitivity,
            themeColorIndex = extra.themeColorIndex,
            appIcon = extra.appIcon,
            isBottomBarFloating = extra.isBottomBarFloating,
            headerBlurEnabled = extra.headerBlurEnabled,
            bottomBarBlurEnabled = extra.bottomBarBlurEnabled,
            displayMode = extra.displayMode // 🔥 新增
        )
    }

    // 第 6 步：与缓存大小和实验性功能合并
    val state: StateFlow<SettingsUiState> = combine(
        baseSettingsFlow,
        _cacheSize,
        experimentalSettingsFlow
    ) { settings, cacheSize, experimental ->
        SettingsUiState(
            hwDecode = settings.hwDecode,
            themeMode = settings.themeMode,
            dynamicColor = settings.dynamicColor,
            bgPlay = settings.bgPlay,
            gestureSensitivity = settings.gestureSensitivity,
            themeColorIndex = settings.themeColorIndex,
            appIcon = settings.appIcon,
            isBottomBarFloating = settings.isBottomBarFloating,
            headerBlurEnabled = settings.headerBlurEnabled,
            bottomBarBlurEnabled = settings.bottomBarBlurEnabled,
            displayMode = settings.displayMode,
            cacheSize = cacheSize,
            // 🧪 实验性功能
            auto1080p = experimental.auto1080p,
            autoSkipOpEd = experimental.autoSkipOpEd,
            prefetchVideo = experimental.prefetchVideo,
            doubleTapLike = experimental.doubleTapLike
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        refreshCacheSize()
    }

    // --- 功能方法 ---

    fun refreshCacheSize() {
        viewModelScope.launch { _cacheSize.value = CacheUtils.getTotalCacheSize(context) }
    }

    fun clearCache() {
        viewModelScope.launch {
            CacheUtils.clearAllCache(context)
            _cacheSize.value = CacheUtils.getTotalCacheSize(context)
        }
    }

    fun toggleHwDecode(value: Boolean) { viewModelScope.launch { SettingsManager.setHwDecode(context, value) } }
    fun setThemeMode(mode: AppThemeMode) { viewModelScope.launch { SettingsManager.setThemeMode(context, mode) } }
    fun toggleDynamicColor(value: Boolean) { viewModelScope.launch { SettingsManager.setDynamicColor(context, value) } }
    fun toggleBgPlay(value: Boolean) { viewModelScope.launch { SettingsManager.setBgPlay(context, value) } }
    // 🔥🔥 [新增] 手势灵敏度和主题色
    fun setGestureSensitivity(value: Float) { viewModelScope.launch { SettingsManager.setGestureSensitivity(context, value) } }
    fun setThemeColorIndex(index: Int) { 
        viewModelScope.launch { 
            SettingsManager.setThemeColorIndex(context, index)
            // 🔥 选择自定义主题色时，自动关闭动态取色
            if (index != 0) {
                SettingsManager.setDynamicColor(context, false)
            }
        }
    }

    // 🔥🔥 [新增] 切换应用图标
    fun setAppIcon(iconKey: String) {
        viewModelScope.launch {
            // 1. 保存偏好
            SettingsManager.setAppIcon(context, iconKey)
            
            // 2. 应用 Alias
            val pm = context.packageManager
            val packageName = context.packageName
            
            // alias 映射
            val allAliases = listOf(
                "3D" to "${packageName}.MainActivityAlias3D",
                "Blue" to "${packageName}.MainActivityAliasBlue",
                "Retro" to "${packageName}.MainActivityAliasRetro",
                "Flat" to "${packageName}.MainActivityAliasFlat",
                "Neon" to "${packageName}.MainActivityAliasNeon"
            )
            
            // 找到需要启用的 alias
            val targetAlias = allAliases.find { it.first == iconKey }?.second
                ?: "${packageName}.MainActivityAlias3D" // 默认3D
            
            // 禁用所有其他 alias，启用目标 alias
            allAliases.forEach { (_, aliasFullName) ->
                pm.setComponentEnabledSetting(
                    android.content.ComponentName(packageName, aliasFullName),
                    if (aliasFullName == targetAlias) 
                        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED 
                    else 
                        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
            }
        }
    }

    // 🔥🔥 [新增] 切换底栏样式
    fun toggleBottomBarFloating(value: Boolean) { viewModelScope.launch { SettingsManager.setBottomBarFloating(context, value) } }
    
    // 🔥🔥 [新增] 模糊效果开关
    fun toggleHeaderBlur(value: Boolean) { viewModelScope.launch { SettingsManager.setHeaderBlurEnabled(context, value) } }
    fun toggleBottomBarBlur(value: Boolean) { viewModelScope.launch { SettingsManager.setBottomBarBlurEnabled(context, value) } }
    
    // 🔥🔥 [新增] 首页展示模式
    fun setDisplayMode(mode: Int) { 
        viewModelScope.launch { 
            // 兼容旧的 shared preferences
            context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                .edit().putInt("display_mode", mode).apply()
            // 触发 flow 更新 (如果需要，或者仅仅依赖 prefs 监听? 这里简化处理，假设 ViewModel 只负责写，读在 flow 中)
            // 实际上这里的 flow 是基于 SettingsManager (DataStore) 的。
            // 如果 display_mode 还是 SharedPreferences，我们需要一个 flow 来通过 DataStore 或者手动构建。
            //为了简单统一，建议迁移到 SettingsManager。但为了不破坏 HomeScreen 读取，我们先保持 Prefs，
            // 并在 SettingsManager 中增加对 display_mode 的支持 (或者直接在这里用 MutableStateFlow 桥接?)
            // 鉴于 HomeScreen 可能直接读 Prefs，我们这里只需写 Prefs。
            // 但为了 UI 响应，我们需要通知 UIState。
            // 由于 SettingsManager 目前不管理 display_mode，我们需要添加它。
            // 既然要 refactor，就彻底点。
            SettingsManager.setDisplayMode(context, mode)
        } 
    }
    
    // 🧪🧪 [新增] 实验性功能
    fun toggleAuto1080p(value: Boolean) { viewModelScope.launch { SettingsManager.setAuto1080p(context, value) } }
    fun toggleAutoSkipOpEd(value: Boolean) { viewModelScope.launch { SettingsManager.setAutoSkipOpEd(context, value) } }
    fun togglePrefetchVideo(value: Boolean) { viewModelScope.launch { SettingsManager.setPrefetchVideo(context, value) } }
    fun toggleDoubleTapLike(value: Boolean) { viewModelScope.launch { SettingsManager.setDoubleTapLike(context, value) } }
}

// Move DisplayMode enum here to be accessible
enum class DisplayMode(val title: String, val value: Int) {
    Grid("双列网格 (默认)", 0),
    StoryCards("故事卡片", 1),    // 🔥 电影宽屏风格
    GlassCards("玻璃拟态", 2)     // 🔥 毛玻璃效果
}