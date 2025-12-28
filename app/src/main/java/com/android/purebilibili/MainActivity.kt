// 文件路径: app/src/main/java/com/android/purebilibili/MainActivity.kt
package com.android.purebilibili

import android.app.PictureInPictureParams
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import com.android.purebilibili.core.util.Logger
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.theme.PureBiliBiliTheme
import com.android.purebilibili.feature.settings.AppThemeMode
import com.android.purebilibili.navigation.AppNavigation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

import com.android.purebilibili.feature.video.player.MiniPlayerManager
import com.android.purebilibili.feature.video.ui.overlay.MiniPlayerOverlay
import com.android.purebilibili.feature.video.ui.overlay.FullscreenPlayerOverlay
import com.android.purebilibili.core.ui.SharedTransitionProvider
import com.android.purebilibili.feature.plugin.EyeProtectionOverlay
import coil.compose.AsyncImage
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.animation.doOnEnd
import android.widget.ImageView
import com.android.purebilibili.feature.onboarding.OnboardingBottomSheet
import dev.chrisbanes.haze.haze

private const val TAG = "MainActivity"
private const val PREFS_NAME = "app_welcome"
private const val KEY_FIRST_LAUNCH = "first_launch_shown"

class MainActivity : ComponentActivity() {
    
    // 🔥 PiP 状态
    var isInPipMode by mutableStateOf(false)
        private set
    
    // 🔥 是否在视频页面 (用于决定是否进入 PiP)
    var isInVideoDetail by mutableStateOf(false)
    
    // 🔥 小窗管理器
    private lateinit var miniPlayerManager: MiniPlayerManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // 🚀 安装 SplashScreen
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        // 🔥 初始调用，后续会根据主题动态更新
        enableEdgeToEdge()
        
        // 初始化小窗管理器
        miniPlayerManager = MiniPlayerManager.getInstance(this)

        setContent {
            val context = LocalContext.current
            val navController = androidx.navigation.compose.rememberNavController()
            
            // 🔥 首次启动检测
            val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
            var showWelcome by remember { mutableStateOf(!prefs.getBoolean(KEY_FIRST_LAUNCH, false)) }

            // 1. 获取存储的模式 (默认为跟随系统)
            val themeMode by SettingsManager.getThemeMode(context).collectAsState(initial = AppThemeMode.FOLLOW_SYSTEM)

            // 🔥🔥 2. [新增] 获取动态取色设置 (默认为 true)
            val dynamicColor by SettingsManager.getDynamicColor(context).collectAsState(initial = true)
            
            // 🔥🔥 3. [新增] 获取主题色索引
            val themeColorIndex by SettingsManager.getThemeColorIndex(context).collectAsState(initial = 0)

            // 4. 获取系统当前的深色状态
            val systemInDark = isSystemInDarkTheme()

            // 5. 根据枚举值决定是否开启 DarkTheme
            val useDarkTheme = when (themeMode) {
                AppThemeMode.FOLLOW_SYSTEM -> systemInDark // 跟随系统：系统黑则黑，系统白则白
                AppThemeMode.LIGHT -> false                // 强制浅色
                AppThemeMode.DARK -> true                  // 强制深色
            }

            // 🔥🔥 [新增] 根据主题动态更新状态栏样式
            LaunchedEffect(useDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (useDarkTheme) {
                        androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        androidx.activity.SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
            }
            
            // 🔥 全局 Haze 状态，用于实现毛玻璃效果
            val mainHazeState = remember { dev.chrisbanes.haze.HazeState() }

            // 6. 传入参数
            PureBiliBiliTheme(
                darkTheme = useDarkTheme,
                dynamicColor = dynamicColor,
                themeColorIndex = themeColorIndex // 🔥🔥 传入主题色索引
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        // 🔥🔥 [修复] 将 .haze() 移到 Surface 内部
                        // 这样 haze 源是 AppNavigation 内容，不会被 Surface 的不透明背景遮挡
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .haze(state = mainHazeState)
                        ) {
                            // 🔥 SharedTransitionProvider 包裹导航，启用共享元素过渡
                            SharedTransitionProvider {
                                AppNavigation(
                                    navController = navController,
                                    miniPlayerManager = miniPlayerManager,
                                    isInPipMode = isInPipMode,
                                    onVideoDetailEnter = { 
                                        isInVideoDetail = true
                                        Logger.d(TAG, "🎬 进入视频详情页")
                                    },
                                    onVideoDetailExit = { 
                                        isInVideoDetail = false
                                        Logger.d(TAG, "🔙 退出视频详情页")
                                    },
                                    mainHazeState = mainHazeState // 🔥🔥 传递全局 Haze 状态
                                )
                            }
                            
                            // 🔥🔥 [关键修复] OnboardingBottomSheet 必须在 haze 源 Box 内部
                            // 这样 hazeChild 可以模糊同一个 Box 内的兄弟内容 (AppNavigation)
                            // 与 HomeScreen 中 FrostedBottomBar 的工作原理一致
                            OnboardingBottomSheet(
                                visible = showWelcome,
                                onDismiss = {
                                    prefs.edit().putBoolean(KEY_FIRST_LAUNCH, true).apply()
                                    showWelcome = false
                                },
                                mainHazeState = mainHazeState
                            )
                        }
                    }
                    // 🔥 小窗全屏状态
                    var showFullscreen by remember { mutableStateOf(false) }
                    
                    // 🔥 小窗播放器覆盖层
                    MiniPlayerOverlay(
                        miniPlayerManager = miniPlayerManager,
                        onExpandClick = {
                            // 🔥 直接显示全屏播放器（无需导航）
                            showFullscreen = true
                            miniPlayerManager.exitMiniMode()
                        }
                    )
                    
                    // 🔥 全屏播放器覆盖层（包含亮度、音量、进度调节）
                    if (showFullscreen) {
                        FullscreenPlayerOverlay(
                            miniPlayerManager = miniPlayerManager,
                            onDismiss = { 
                                showFullscreen = false
                                miniPlayerManager.enterMiniMode()
                            },
                            onNavigateToDetail = {
                                // 🔥 关闭全屏覆盖层并导航到视频详情页
                                showFullscreen = false
                                miniPlayerManager.currentBvid?.let { bvid ->
                                    // 🔥🔥 [修复] 使用正确的 cid，而不是 0
                                    val cid = miniPlayerManager.currentCid
                                    navController.navigate("video/$bvid?cid=$cid&cover=") {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                    
                    // 🌙 护眼模式覆盖层（最顶层，应用于所有内容）
                    EyeProtectionOverlay()
                }
            }
        }
    }
    
    // 🔥 用户按 Home 键或切换应用时触发
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        
        Logger.d(TAG, "👋 onUserLeaveHint 触发, isInVideoDetail=$isInVideoDetail, isMiniMode=${miniPlayerManager.isMiniMode}")
        
        // 🔥🔥 [重构] 使用新的模式判断方法
        val shouldEnterPip = miniPlayerManager.shouldEnterPip()
        val currentMode = miniPlayerManager.getCurrentMode()
        
        // 🔥🔥 [新增] 支持小窗模式下按 Home 键进入系统 PiP
        // 条件：在视频详情页 或 小窗播放中
        val shouldTriggerPip = (isInVideoDetail || miniPlayerManager.isMiniMode) && shouldEnterPip
        
        Logger.d(TAG, "📺 miniPlayerMode=$currentMode, shouldEnterPip=$shouldEnterPip, shouldTriggerPip=$shouldTriggerPip, API=${Build.VERSION.SDK_INT}")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && shouldTriggerPip) {
            try {
                Logger.d(TAG, "🎬 尝试进入 PiP 模式...")
                
                val pipParams = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                
                // Android 12+: 启用自动进入和无缝调整
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    pipParams.setAutoEnterEnabled(true)
                    pipParams.setSeamlessResizeEnabled(true)
                }
                
                enterPictureInPictureMode(pipParams.build())
                Logger.d(TAG, "✅ 成功进入 PiP 模式")
            } catch (e: Exception) {
                com.android.purebilibili.core.util.Logger.e(TAG, "❌ 进入 PiP 失败", e)
            }
        } else {
            Logger.d(TAG, "⏳ 未满足 PiP 条件: API>=${Build.VERSION_CODES.O}=${Build.VERSION.SDK_INT >= Build.VERSION_CODES.O}, shouldTriggerPip=$shouldTriggerPip")
        }
    }
    
    // 🔥 PiP 模式变化回调
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        Logger.d(TAG, "📱 PiP 模式变化: $isInPictureInPictureMode")
    }
}

/**
 * 🔥 首次启动欢迎弹窗 - 精美设计版
 */
@Composable
fun WelcomeDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 🔥 应用 Logo - 使用实际应用图标
                AsyncImage(
                    model = R.mipmap.ic_launcher,
                    contentDescription = "BiliPai Logo",
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "欢迎使用 BiliPai",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    "简洁 · 流畅 · 开源",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // 🔥 特性介绍
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FeatureChip("🎬", "高清播放")
                    FeatureChip("💬", "弹幕评论")
                    FeatureChip("🔒", "隐私保护")
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 🔥 开源信息卡片
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("https://github.com/jay3-yy/BiliPai") }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⭐", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "开源项目",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "github.com/jay3-yy/BiliPai",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text("→", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 🔥 免责声明 - 适配深色模式
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("📋", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "使用须知",
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "本应用仅供学习交流，所有内容版权归 Bilibili 及原作者。",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    "开始探索",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    )
}

/**
 * 🔥 特性小标签
 */
@Composable
private fun FeatureChip(emoji: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}