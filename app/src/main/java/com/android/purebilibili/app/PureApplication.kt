// 文件路径: app/PureApplication.kt
package com.android.purebilibili.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiKeyManager
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.feature.plugin.AdFilterPlugin
import com.android.purebilibili.feature.plugin.DanmakuEnhancePlugin
import com.android.purebilibili.feature.plugin.SponsorBlockPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "PureApplication"

// 🚀 实现 ImageLoaderFactory 以提供自定义 Coil 配置
// 🚀 实现 ComponentCallbacks2 响应系统内存警告
class PureApplication : Application(), ImageLoaderFactory, ComponentCallbacks2 {
    
    // 🔥 保存 ImageLoader 引用以便在 onTrimMemory 中使用
    private var _imageLoader: ImageLoader? = null
    
    // 🚀 Coil 图片加载器 - 优化内存和磁盘缓存
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            // 🚀 内存缓存：使用 30% 可用内存（提升缓存命中率）
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.30)  // 30% of available memory
                    .build()
            }
            // 🚀 磁盘缓存：150MB（减少重复下载）
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024)  // 150 MB
                    .build()
            }
            // 🚀 优先使用缓存
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            // 🚀 启用 Bitmap 复用减少内存分配
            .allowRgb565(true)
            // 🚀 跨淡入效果
            .crossfade(true)
            .build()
            .also { _imageLoader = it }  // 保存引用
    }
    
    override fun onCreate() {
        // 🚀🚀🚀 [关键] 必须在 super.onCreate() 之前设置！
        // 这样系统在初始化时就能读取到正确的夜间模式配置
        applyThemePreference()
        
        super.onCreate()
        
        // 🔥 关键初始化（同步，必须在启动时完成）
        NetworkModule.init(this)
        TokenManager.init(this)
        com.android.purebilibili.feature.download.DownloadManager.init(this)  // 🔥 下载管理器
        
        // 🔌 插件系统初始化
        PluginManager.initialize(this)
        PluginManager.register(SponsorBlockPlugin())
        PluginManager.register(AdFilterPlugin())
        PluginManager.register(DanmakuEnhancePlugin())
        Logger.d(TAG, "🔌 Plugin system initialized with 3 built-in plugins")
        
        createNotificationChannel()
        
        // 🔥 初始化 Firebase Crashlytics
        initCrashlytics()
        
        // 📊 初始化 Firebase Analytics
        initAnalytics()
        
        // 🚀🚀🚀 [冷启动优化] 延迟非关键初始化到主线程空闲时
        Handler(Looper.getMainLooper()).post {
            // 🔥 恢复 WBI 密钥缓存
            WbiKeyManager.restoreFromStorage(this)
            
            // 🔥 同步应用图标状态（确保只有一个图标在桌面显示）
            syncAppIconState()
            
            // 🔥 异步预热 WBI Keys，减少首次视频加载延迟
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    WbiKeyManager.getWbiKeys()
                    Logger.d(TAG, "✅ WBI Keys preloaded successfully")
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "⚠️ WBI Keys preload failed: ${e.message}")
                }
            }
        }
    }
    
    // 🔥 初始化 Firebase Crashlytics
    private fun initCrashlytics() {
        try {
            // 🔥 读取用户设置（默认开启）
            val prefs = getSharedPreferences("crash_tracking", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("enabled", true)  // 默认开启
            
            // 🔥 根据用户设置启用/禁用 Crashlytics
            com.android.purebilibili.core.util.CrashReporter.setEnabled(enabled)
            
            if (enabled) {
                // 设置应用版本信息
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                com.android.purebilibili.core.util.CrashReporter.setCustomKey("app_version", packageInfo.versionName ?: "unknown")
                com.android.purebilibili.core.util.CrashReporter.setCustomKey("version_code", packageInfo.versionCode)
                
                // 设置设备信息
                com.android.purebilibili.core.util.CrashReporter.setCustomKey("device_model", android.os.Build.MODEL)
                com.android.purebilibili.core.util.CrashReporter.setCustomKey("android_version", android.os.Build.VERSION.SDK_INT)
            }
            
            Logger.d(TAG, "🔥 Firebase Crashlytics initialized (enabled=$enabled)")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to init Crashlytics", e)
        }
    }
    
    // � 初始化 Firebase Analytics
    private fun initAnalytics() {
        try {
            // 初始化 AnalyticsHelper
            com.android.purebilibili.core.util.AnalyticsHelper.init(this)
            
            // 🔥 读取用户设置（默认开启）
            val prefs = getSharedPreferences("analytics_tracking", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("enabled", true)  // 默认开启
            
            // 🔥 根据用户设置启用/禁用 Analytics
            com.android.purebilibili.core.util.AnalyticsHelper.setEnabled(enabled)
            
            if (enabled) {
                // 记录应用打开事件
                com.android.purebilibili.core.util.AnalyticsHelper.logAppOpen()
            }
            
            Logger.d(TAG, "📊 Firebase Analytics initialized (enabled=$enabled)")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to init Analytics", e)
        }
    }
    
    // �🚀🚀 [后台内存优化] 响应系统内存警告
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                // 🔥 UI 隐藏时(进入后台)，清理图片内存缓存
                _imageLoader?.memoryCache?.clear()
                Logger.d(TAG, "🧹 UI hidden, cleared image memory cache")
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                // 🔥 低内存时，更激进地清理
                _imageLoader?.memoryCache?.clear()
                System.gc()
                Logger.d(TAG, "⚠️ Low memory, aggressive cleanup")
            }
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                // 🔥 进程即将被杀死，释放所有可能的内存
                _imageLoader?.memoryCache?.clear()
                Logger.d(TAG, "🚨 TRIM_MEMORY_COMPLETE, released all caches")
            }
        }
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        _imageLoader?.memoryCache?.clear()
        Logger.d(TAG, "🚨 onLowMemory, cleared all caches")
    }

    private fun createNotificationChannel() {
        // 仅在 Android 8.0 (API 26) 及以上需要通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "media_playback_channel" // 这个 ID 需要保持固定
            val channelName = "媒体播放"
            val channelDescription = "显示正在播放的视频控制条"

            // 重要：媒体通知的优先级通常设为 LOW
            // 这样可以显示在状态栏和下拉栏，但不会发出提示音打断视频声音
            val importance = NotificationManager.IMPORTANCE_LOW

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                setShowBadge(false) // 媒体通知通常不需要在图标上显示角标
                setSound(null, null) // 关键：设为静音，防止切歌时发出系统提示音
            }

            // 向系统注册渠道
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 🚀 应用主题偏好 - 在 Splash Screen 显示前调用
     * 
     * 这解决了：用户在应用内强制深色模式，但系统是浅色时，启动屏仍然是白色的问题。
     * 通过 AppCompatDelegate.setDefaultNightMode() 强制系统使用正确的深色/浅色模式。
     */
    private fun applyThemePreference() {
        // 同步读取保存的主题设置（必须同步，因为 Splash Screen 马上就会显示）
        val prefs = getSharedPreferences("theme_cache", Context.MODE_PRIVATE)
        val themeModeValue = prefs.getInt("theme_mode", 0)  // 0 = FOLLOW_SYSTEM
        
        val nightMode = when (themeModeValue) {
            0 -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM  // 跟随系统
            1 -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO             // 浅色
            2 -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES            // 深色
            else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(nightMode)
        Logger.d(TAG, "🎨 Applied theme mode: $themeModeValue -> nightMode=$nightMode")
    }
    
    /**
     * 🔥 同步应用图标状态 - 确保桌面只显示一个图标
     * 
     * 这解决了：切换图标后重新安装或更新应用导致多个图标出现的问题。
     * 在应用启动时读取用户保存的图标偏好，然后同步所有 alias 的启用状态。
     */
    private fun syncAppIconState() {
        try {
            val pm = packageManager
            val currentIcon = com.android.purebilibili.core.store.SettingsManager.getAppIconSync(this)
            
            // alias 映射
            val allAliases = listOf(
                "3D" to "${packageName}.MainActivityAlias3D",
                "Blue" to "${packageName}.MainActivityAliasBlue",
                "Retro" to "${packageName}.MainActivityAliasRetro",
                "Flat" to "${packageName}.MainActivityAliasFlat",
                "Neon" to "${packageName}.MainActivityAliasNeon",
                "Telegram Blue" to "${packageName}.MainActivityAliasTelegramBlue",
                "Pink" to "${packageName}.MainActivityAliasPink",
                "Purple" to "${packageName}.MainActivityAliasPurple",
                "Green" to "${packageName}.MainActivityAliasGreen",
                "Dark" to "${packageName}.MainActivityAliasDark"
            )
            
            // 找到需要启用的 alias
            val targetAlias = allAliases.find { it.first == currentIcon }?.second
                ?: "${packageName}.MainActivityAlias3D" // 默认3D
            
            // 同步所有 alias 状态：只有目标启用，其他禁用
            allAliases.forEach { (_, aliasFullName) ->
                val currentState = pm.getComponentEnabledSetting(
                    android.content.ComponentName(packageName, aliasFullName)
                )
                val shouldBeEnabled = aliasFullName == targetAlias
                val targetState = if (shouldBeEnabled) {
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
                
                // 只在状态不一致时修改，减少不必要的操作
                if (currentState != targetState) {
                    pm.setComponentEnabledSetting(
                        android.content.ComponentName(packageName, aliasFullName),
                        targetState,
                        android.content.pm.PackageManager.DONT_KILL_APP
                    )
                }
            }
            
            Logger.d(TAG, "🎨 Synced app icon state: $currentIcon")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to sync app icon state", e)
        }
    }
}