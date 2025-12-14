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
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.core.util.Logger
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
        createNotificationChannel()
        
        // 🚀🚀 [冷启动优化] 延迟非关键初始化到主线程空闲时
        Handler(Looper.getMainLooper()).post {
            // 🔥 恢复 WBI 密钥缓存
            WbiKeyManager.restoreFromStorage(this)
            
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
    
    // 🚀🚀 [后台内存优化] 响应系统内存警告
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
}