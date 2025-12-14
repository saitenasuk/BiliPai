// 文件路径: core/store/SettingsManager.kt
package com.android.purebilibili.core.store

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.android.purebilibili.feature.settings.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 声明 DataStore 扩展属性
private val Context.settingsDataStore by preferencesDataStore(name = "settings_prefs")

object SettingsManager {
    // 键定义
    private val KEY_AUTO_PLAY = booleanPreferencesKey("auto_play")
    private val KEY_HW_DECODE = booleanPreferencesKey("hw_decode")
    private val KEY_THEME_MODE = intPreferencesKey("theme_mode_v2")
    private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    private val KEY_BG_PLAY = booleanPreferencesKey("bg_play")
    // 🔥🔥 [新增] 手势灵敏度和主题色
    private val KEY_GESTURE_SENSITIVITY = floatPreferencesKey("gesture_sensitivity")
    private val KEY_THEME_COLOR_INDEX = intPreferencesKey("theme_color_index")
    // 🔥🔥 [新增] 应用图标 Key (Blue, Red, Green...)
    private val KEY_APP_ICON = androidx.datastore.preferences.core.stringPreferencesKey("app_icon_key")
    // 🔥🔥 [新增] 底部栏样式 (true=悬浮, false=贴底)
    private val KEY_BOTTOM_BAR_FLOATING = booleanPreferencesKey("bottom_bar_floating")
    // 🔥🔥 [新增] 模糊效果开关
    private val KEY_HEADER_BLUR_ENABLED = booleanPreferencesKey("header_blur_enabled")
    private val KEY_BOTTOM_BAR_BLUR_ENABLED = booleanPreferencesKey("bottom_bar_blur_enabled")

    // --- Auto Play ---
    fun getAutoPlay(context: Context): Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[KEY_AUTO_PLAY] ?: true }

    suspend fun setAutoPlay(context: Context, value: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[KEY_AUTO_PLAY] = value }
    }

    // --- HW Decode ---
    fun getHwDecode(context: Context): Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[KEY_HW_DECODE] ?: true }

    suspend fun setHwDecode(context: Context, value: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[KEY_HW_DECODE] = value }
        // 🔥 同步到 SharedPreferences，供同步读取使用
        context.getSharedPreferences("hw_decode_cache", Context.MODE_PRIVATE)
            .edit().putBoolean("hw_decode_enabled", value).apply()
    }

    // --- Theme Mode ---
    fun getThemeMode(context: Context): Flow<AppThemeMode> = context.settingsDataStore.data
        .map { preferences ->
            val modeInt = preferences[KEY_THEME_MODE] ?: AppThemeMode.FOLLOW_SYSTEM.value
            AppThemeMode.fromValue(modeInt)
        }

    suspend fun setThemeMode(context: Context, mode: AppThemeMode) {
        context.settingsDataStore.edit { preferences -> preferences[KEY_THEME_MODE] = mode.value }
        // 🚀 同步到 SharedPreferences，供 PureApplication 同步读取使用
        // 使用 commit() 确保立即写入
        val success = context.getSharedPreferences("theme_cache", Context.MODE_PRIVATE)
            .edit().putInt("theme_mode", mode.value).commit()
        com.android.purebilibili.core.util.Logger.d("SettingsManager", "🎨 Theme mode saved: ${mode.value} (${mode.label}), success=$success")
        
        // 🚀 同时应用到 AppCompatDelegate，使当前运行时生效
        val nightMode = when (mode) {
            AppThemeMode.FOLLOW_SYSTEM -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            AppThemeMode.LIGHT -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            AppThemeMode.DARK -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
        }
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    // --- Dynamic Color ---
    fun getDynamicColor(context: Context): Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[KEY_DYNAMIC_COLOR] ?: true }

    suspend fun setDynamicColor(context: Context, value: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[KEY_DYNAMIC_COLOR] = value }
    }

    // --- 后台/画中画播放 ---
    fun getBgPlay(context: Context): Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[KEY_BG_PLAY] ?: false }

    suspend fun setBgPlay(context: Context, value: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[KEY_BG_PLAY] = value }
    }

    // 🔥🔥 [新增] --- 手势灵敏度 (0.5 ~ 2.0, 默认 1.0) ---
    fun getGestureSensitivity(context: Context): Flow<Float> = context.settingsDataStore.data
        .map { preferences -> preferences[KEY_GESTURE_SENSITIVITY] ?: 1.0f }

    suspend fun setGestureSensitivity(context: Context, value: Float) {
        context.settingsDataStore.edit { preferences -> 
            preferences[KEY_GESTURE_SENSITIVITY] = value.coerceIn(0.5f, 2.0f) 
        }
    }

    // 🔥🔥 [新增] --- 主题色索引 (0-5, 默认 0 = BiliPink) ---
    fun getThemeColorIndex(context: Context): Flow<Int> = context.settingsDataStore.data
        .map { preferences -> preferences[KEY_THEME_COLOR_INDEX] ?: 0 }

    suspend fun setThemeColorIndex(context: Context, index: Int) {
        context.settingsDataStore.edit { preferences -> 
            preferences[KEY_THEME_COLOR_INDEX] = index.coerceIn(0, 5)
        }
    }
    
    // 🔥🔥 [新增] --- 首页展示模式 (0=Grid, 1=Card) ---
    private val KEY_DISPLAY_MODE = intPreferencesKey("display_mode")
    
    fun getDisplayMode(context: Context): Flow<Int> = context.settingsDataStore.data
        .map { preferences -> preferences[KEY_DISPLAY_MODE] ?: 0 }

    suspend fun setDisplayMode(context: Context, mode: Int) {
        context.settingsDataStore.edit { preferences -> 
            preferences[KEY_DISPLAY_MODE] = mode
        }
    }

    // 🔥🔥 [新增] --- 应用图标 ---
    fun getAppIcon(context: Context): Flow<String> = context.settingsDataStore.data
        .map { preferences -> preferences[KEY_APP_ICON] ?: "3D" }

    suspend fun setAppIcon(context: Context, iconKey: String) {
        context.settingsDataStore.edit { preferences -> 
            preferences[KEY_APP_ICON] = iconKey
        }
    }

    // 🔥🔥 [新增] --- 底部栏样式 ---
    fun getBottomBarFloating(context: Context): Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[KEY_BOTTOM_BAR_FLOATING] ?: true }

    suspend fun setBottomBarFloating(context: Context, value: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[KEY_BOTTOM_BAR_FLOATING] = value }
    }
    
    // 🔥🔥 [新增] --- 搜索框模糊效果 ---
    fun getHeaderBlurEnabled(context: Context): Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[KEY_HEADER_BLUR_ENABLED] ?: true }

    suspend fun setHeaderBlurEnabled(context: Context, value: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[KEY_HEADER_BLUR_ENABLED] = value }
    }
    
    // 🔥🔥 [新增] --- 底栏模糊效果 ---
    fun getBottomBarBlurEnabled(context: Context): Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[KEY_BOTTOM_BAR_BLUR_ENABLED] ?: true }

    suspend fun setBottomBarBlurEnabled(context: Context, value: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[KEY_BOTTOM_BAR_BLUR_ENABLED] = value }
    }
    
    // ========== 🔥🔥 弹幕设置 ==========
    
    private val KEY_DANMAKU_ENABLED = booleanPreferencesKey("danmaku_enabled")
    private val KEY_DANMAKU_OPACITY = floatPreferencesKey("danmaku_opacity")
    private val KEY_DANMAKU_FONT_SCALE = floatPreferencesKey("danmaku_font_scale")
    private val KEY_DANMAKU_SPEED = floatPreferencesKey("danmaku_speed")
    private val KEY_DANMAKU_AREA = floatPreferencesKey("danmaku_area")
    
    // --- 弹幕开关 ---
    fun getDanmakuEnabled(context: Context): Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[KEY_DANMAKU_ENABLED] ?: true }

    suspend fun setDanmakuEnabled(context: Context, value: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[KEY_DANMAKU_ENABLED] = value }
    }
    
    // --- 弹幕透明度 (0.0 ~ 1.0, 默认 1.0) ---
    fun getDanmakuOpacity(context: Context): Flow<Float> = context.settingsDataStore.data
        .map { preferences -> preferences[KEY_DANMAKU_OPACITY] ?: 1.0f }

    suspend fun setDanmakuOpacity(context: Context, value: Float) {
        context.settingsDataStore.edit { preferences -> 
            preferences[KEY_DANMAKU_OPACITY] = value.coerceIn(0.0f, 1.0f)
        }
    }
    
    // --- 弹幕字体大小 (0.5 ~ 2.0, 默认 1.0) ---
    fun getDanmakuFontScale(context: Context): Flow<Float> = context.settingsDataStore.data
        .map { preferences -> preferences[KEY_DANMAKU_FONT_SCALE] ?: 1.0f }

    suspend fun setDanmakuFontScale(context: Context, value: Float) {
        context.settingsDataStore.edit { preferences -> 
            preferences[KEY_DANMAKU_FONT_SCALE] = value.coerceIn(0.5f, 2.0f)
        }
    }
    
    // --- 弹幕速度 (0.5 ~ 2.0, 默认 1.2) ---
    fun getDanmakuSpeed(context: Context): Flow<Float> = context.settingsDataStore.data
        .map { preferences -> preferences[KEY_DANMAKU_SPEED] ?: 1.2f }

    suspend fun setDanmakuSpeed(context: Context, value: Float) {
        context.settingsDataStore.edit { preferences -> 
            preferences[KEY_DANMAKU_SPEED] = value.coerceIn(0.5f, 2.0f)
        }
    }
    
    // --- 弹幕显示区域 (0.25, 0.5, 0.75, 1.0, 默认 0.5) ---
    fun getDanmakuArea(context: Context): Flow<Float> = context.settingsDataStore.data
        .map { preferences -> preferences[KEY_DANMAKU_AREA] ?: 0.5f }

    suspend fun setDanmakuArea(context: Context, value: Float) {
        context.settingsDataStore.edit { preferences -> 
            preferences[KEY_DANMAKU_AREA] = value.coerceIn(0.25f, 1.0f)
        }
    }
    
    // ========== 🧪 实验性功能 ==========
    
    private val KEY_AUTO_1080P = booleanPreferencesKey("exp_auto_1080p")
    private val KEY_AUTO_SKIP_OP_ED = booleanPreferencesKey("exp_auto_skip_op_ed")
    private val KEY_PREFETCH_VIDEO = booleanPreferencesKey("exp_prefetch_video")
    private val KEY_DOUBLE_TAP_LIKE = booleanPreferencesKey("exp_double_tap_like")
    
    // --- 已登录用户默认 1080P ---
    fun getAuto1080p(context: Context): Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[KEY_AUTO_1080P] ?: true }  // 默认开启

    suspend fun setAuto1080p(context: Context, value: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[KEY_AUTO_1080P] = value }
    }
    
    // --- 自动跳过片头片尾 ---
    fun getAutoSkipOpEd(context: Context): Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[KEY_AUTO_SKIP_OP_ED] ?: false }

    suspend fun setAutoSkipOpEd(context: Context, value: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[KEY_AUTO_SKIP_OP_ED] = value }
    }
    
    // --- 预加载下一个视频 ---
    fun getPrefetchVideo(context: Context): Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[KEY_PREFETCH_VIDEO] ?: false }

    suspend fun setPrefetchVideo(context: Context, value: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[KEY_PREFETCH_VIDEO] = value }
    }
    
    // --- 双击点赞 ---
    fun getDoubleTapLike(context: Context): Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[KEY_DOUBLE_TAP_LIKE] ?: true }  // 默认开启

    suspend fun setDoubleTapLike(context: Context, value: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[KEY_DOUBLE_TAP_LIKE] = value }
    }
}