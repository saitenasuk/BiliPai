// 文件路径: navigation/AppNavigation.kt
package com.android.purebilibili.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState // 🔥 新增
import androidx.compose.runtime.getValue // 🔥 新增
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.android.purebilibili.feature.home.HomeScreen
import com.android.purebilibili.feature.home.HomeViewModel
import com.android.purebilibili.feature.login.LoginScreen
import com.android.purebilibili.feature.profile.ProfileScreen
import com.android.purebilibili.feature.search.SearchScreen
import com.android.purebilibili.feature.settings.SettingsScreen
import com.android.purebilibili.feature.settings.AppearanceSettingsScreen
import com.android.purebilibili.feature.settings.PlaybackSettingsScreen
import com.android.purebilibili.feature.list.CommonListScreen
import com.android.purebilibili.feature.list.HistoryViewModel
import com.android.purebilibili.feature.list.FavoriteViewModel
import com.android.purebilibili.feature.video.VideoDetailScreen
import com.android.purebilibili.feature.video.MiniPlayerManager
import com.android.purebilibili.feature.dynamic.DynamicScreen

// 定义路由参数结构
object VideoRoute {
    const val base = "video"
    const val route = "$base/{bvid}?cid={cid}&cover={cover}"

    // 构建 helper
    fun createRoute(bvid: String, cid: Long, coverUrl: String): String {
        val encodedCover = Uri.encode(coverUrl)
        return "$base/$bvid?cid=$cid&cover=$encodedCover"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    // 🔥 小窗管理器
    miniPlayerManager: MiniPlayerManager? = null,
    // 🔥 PiP 支持参数
    isInPipMode: Boolean = false,
    onVideoDetailEnter: () -> Unit = {},
    onVideoDetailExit: () -> Unit = {}
) {
    val homeViewModel: HomeViewModel = viewModel()

    // 统一跳转逻辑
    fun navigateToVideo(bvid: String, cid: Long = 0L, coverUrl: String = "") {
        // 🔥 如果有小窗在播放，先退出小窗模式
        miniPlayerManager?.exitMiniMode()
        navController.navigate(VideoRoute.createRoute(bvid, cid, coverUrl))
    }

    // 动画时长
    val animDuration = 350

    NavHost(
        navController = navController,
        startDestination = ScreenRoutes.Home.route
    ) {
        // --- 1. 首页 ---
        composable(
            route = ScreenRoutes.Home.route,
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            HomeScreen(
                viewModel = homeViewModel,
                onVideoClick = { bvid, cid, cover -> navigateToVideo(bvid, cid, cover) },
                onSearchClick = { navController.navigate(ScreenRoutes.Search.route) },
                onAvatarClick = { navController.navigate(ScreenRoutes.Login.route) },
                onProfileClick = { navController.navigate(ScreenRoutes.Profile.route) },
                onSettingsClick = { navController.navigate(ScreenRoutes.Settings.route) },
                onDynamicClick = { navController.navigate(ScreenRoutes.Dynamic.route) },
                onHistoryClick = { navController.navigate(ScreenRoutes.History.route) },
                onPartitionClick = { navController.navigate(ScreenRoutes.Partition.route) },  // 🔥 分区点击
                onLiveClick = { roomId, title, uname ->
                    navController.navigate(ScreenRoutes.Live.createRoute(roomId, title, uname))
                },
                // 🔥🔥 [修复] 番剧点击导航，接受类型参数
                onBangumiClick = { initialType ->
                    navController.navigate(ScreenRoutes.Bangumi.createRoute(initialType))
                },
                // 🔥 分类点击：跳转到分类详情页面
                onCategoryClick = { tid, name ->
                    navController.navigate(ScreenRoutes.Category.createRoute(tid, name))
                }
            )
        }

        // --- 2. 视频详情页 ---
        composable(
            route = VideoRoute.route,
            arguments = listOf(
                navArgument("bvid") { type = NavType.StringType },
                navArgument("cid") { type = NavType.LongType; defaultValue = 0L },
                navArgument("cover") { type = NavType.StringType; defaultValue = "" },
                navArgument("fullscreen") { type = NavType.BoolType; defaultValue = false }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) { backStackEntry ->
            val bvid = backStackEntry.arguments?.getString("bvid") ?: ""
            val coverUrl = backStackEntry.arguments?.getString("cover") ?: ""
            val startFullscreen = backStackEntry.arguments?.getBoolean("fullscreen") ?: false

            // 🔥 进入视频详情页时通知 MainActivity
            DisposableEffect(Unit) {
                onVideoDetailEnter()
                onDispose {
                    onVideoDetailExit()
                }
            }

            VideoDetailScreen(
                bvid = bvid,
                coverUrl = coverUrl,
                onUpClick = { mid -> navController.navigate(ScreenRoutes.Space.createRoute(mid)) },  // 🔥 点击UP跳转空间
                miniPlayerManager = miniPlayerManager,
                isInPipMode = isInPipMode,
                isVisible = true,
                startInFullscreen = startFullscreen,  // 🔥 传递全屏参数
                onBack = { 
                    // 🔥 返回时进入小窗模式（而非直接停止播放）
                    miniPlayerManager?.enterMiniMode()
                    navController.popBackStack() 
                }
            )
        }

        // --- 3. 个人中心 ---
        composable(
            route = ScreenRoutes.Profile.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onGoToLogin = { navController.navigate(ScreenRoutes.Login.route) },
                onLogoutSuccess = { homeViewModel.refresh() },
                onSettingsClick = { navController.navigate(ScreenRoutes.Settings.route) },
                onHistoryClick = { navController.navigate(ScreenRoutes.History.route) },
                onFavoriteClick = { navController.navigate(ScreenRoutes.Favorite.route) }
            )
        }

        // --- 4. 历史记录 ---
        composable(
            route = ScreenRoutes.History.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            val historyViewModel: HistoryViewModel = viewModel()
            
            // 🔥🔥 [修复] 每次进入历史记录页面时刷新数据
            androidx.compose.runtime.LaunchedEffect(Unit) {
                historyViewModel.loadData()
            }
            
            CommonListScreen(
                viewModel = historyViewModel,
                onBack = { navController.popBackStack() },
                onVideoClick = { bvid, cid -> navigateToVideo(bvid, cid, "") }
            )
        }

        // --- 5. 收藏 ---
        composable(
            route = ScreenRoutes.Favorite.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            val favoriteViewModel: FavoriteViewModel = viewModel()
            CommonListScreen(
                viewModel = favoriteViewModel,
                onBack = { navController.popBackStack() },
                onVideoClick = { bvid, cid -> navigateToVideo(bvid, cid, "") }
            )
        }

        // --- 6. 动态页面 ---
        composable(
            route = ScreenRoutes.Dynamic.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            DynamicScreen(
                onVideoClick = { bvid -> navigateToVideo(bvid, 0L, "") },
                onUserClick = { mid -> navController.navigate(ScreenRoutes.Space.createRoute(mid)) },
                onBack = { navController.popBackStack() }
            )
        }

        // --- 7. 搜索 (核心修复) ---
        composable(
            route = ScreenRoutes.Search.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            // 🔥 从 homeViewModel 获取最新的用户状态 (包括头像)
            val homeState by homeViewModel.uiState.collectAsState()

            SearchScreen(
                userFace = homeState.user.face, // 传入头像 URL
                onBack = { navController.popBackStack() },
                onVideoClick = { bvid, cid -> navigateToVideo(bvid, cid, "") },
                onAvatarClick = {
                    // 如果已登录 -> 去个人中心，未登录 -> 去登录页
                    if (homeState.user.isLogin) {
                        navController.navigate(ScreenRoutes.Profile.route)
                    } else {
                        navController.navigate(ScreenRoutes.Login.route)
                    }
                }
            )
        }

        // --- Settings & Login ---
        composable(
            route = ScreenRoutes.Settings.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenSourceLicensesClick = { navController.navigate(ScreenRoutes.OpenSourceLicenses.route) },
                onAppearanceClick = { navController.navigate(ScreenRoutes.AppearanceSettings.route) },
                onPlaybackClick = { navController.navigate(ScreenRoutes.PlaybackSettings.route) }
            )
        }

        composable(
            route = ScreenRoutes.Login.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(animDuration)) }
        ) {
            LoginScreen(
                onClose = { navController.popBackStack() },
                onLoginSuccess = {
                    navController.popBackStack()
                    homeViewModel.refresh()
                }
            )
        }

        // --- 8. 开源许可证 ---
        composable(
            route = ScreenRoutes.OpenSourceLicenses.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            com.android.purebilibili.feature.settings.OpenSourceLicensesScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // --- 🔥 外观设置二级页面 ---
        composable(
            route = ScreenRoutes.AppearanceSettings.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            AppearanceSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // --- 🔥 播放设置二级页面 ---
        composable(
            route = ScreenRoutes.PlaybackSettings.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            PlaybackSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // --- 9. 🔥🔥 [新增] UP主空间页面 ---
        composable(
            route = ScreenRoutes.Space.route,
            arguments = listOf(
                navArgument("mid") { type = NavType.LongType }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) { backStackEntry ->
            val mid = backStackEntry.arguments?.getLong("mid") ?: 0L
            com.android.purebilibili.feature.space.SpaceScreen(
                mid = mid,
                onBack = { navController.popBackStack() },
                onVideoClick = { bvid -> navigateToVideo(bvid, 0L, "") }
            )
        }
        
        // --- 10. 🔥🔥 [新增] 直播播放页面 ---
        composable(
            route = ScreenRoutes.Live.route,
            arguments = listOf(
                navArgument("roomId") { type = NavType.LongType },
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("uname") { type = NavType.StringType; defaultValue = "" }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(animDuration)) }
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getLong("roomId") ?: 0L
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val uname = backStackEntry.arguments?.getString("uname") ?: ""
            com.android.purebilibili.feature.live.LivePlayerScreen(
                roomId = roomId,
                title = Uri.decode(title),
                uname = Uri.decode(uname),
                onBack = { navController.popBackStack() }
            )
        }
        
        // --- 11. 🔥🔥 [新增] 番剧/影视主页面 ---
        composable(
            route = ScreenRoutes.Bangumi.route,
            arguments = listOf(
                navArgument("type") { type = NavType.IntType; defaultValue = 1 }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) { backStackEntry ->
            val initialType = backStackEntry.arguments?.getInt("type") ?: 1
            com.android.purebilibili.feature.bangumi.BangumiScreen(
                onBack = { navController.popBackStack() },
                onBangumiClick = { seasonId ->
                    navController.navigate(ScreenRoutes.BangumiDetail.createRoute(seasonId))
                },
                initialType = initialType  // 🔥🔥 [修复] 传入初始类型
            )
        }
        
        // --- 12. 🔥🔥 [新增] 番剧/影视详情页面 ---
        composable(
            route = ScreenRoutes.BangumiDetail.route,
            arguments = listOf(
                navArgument("seasonId") { type = NavType.LongType }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) { backStackEntry ->
            val seasonId = backStackEntry.arguments?.getLong("seasonId") ?: 0L
            com.android.purebilibili.feature.bangumi.BangumiDetailScreen(
                seasonId = seasonId,
                onBack = { navController.popBackStack() },
                onEpisodeClick = { episode ->
                    // 🔥🔥 [修改] 跳转到番剧播放页
                    navController.navigate(ScreenRoutes.BangumiPlayer.createRoute(seasonId, episode.id))
                },
                onSeasonClick = { newSeasonId ->
                    // 🔥 切换到其他季度（替换当前页面）
                    navController.navigate(ScreenRoutes.BangumiDetail.createRoute(newSeasonId)) {
                        popUpTo(ScreenRoutes.BangumiDetail.createRoute(seasonId)) { inclusive = true }
                    }
                }
            )
        }
        
        // --- 13. 🔥🔥 [新增] 番剧播放页面 ---
        composable(
            route = ScreenRoutes.BangumiPlayer.route,
            arguments = listOf(
                navArgument("seasonId") { type = NavType.LongType },
                navArgument("epId") { type = NavType.LongType }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) { backStackEntry ->
            val seasonId = backStackEntry.arguments?.getLong("seasonId") ?: 0L
            val epId = backStackEntry.arguments?.getLong("epId") ?: 0L
            com.android.purebilibili.feature.bangumi.BangumiPlayerScreen(
                seasonId = seasonId,
                epId = epId,
                onBack = { navController.popBackStack() }
            )
        }
        
        // --- 14. 🔥 分区页面 ---
        composable(
            route = ScreenRoutes.Partition.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(animDuration)) }
        ) {
            com.android.purebilibili.feature.partition.PartitionScreen(
                onBack = { navController.popBackStack() },
                onPartitionClick = { id, name ->
                    // 🔥 点击分区后，跳转到分类详情页面
                    navController.navigate(ScreenRoutes.Category.createRoute(id, name))
                }
            )
        }
        
        // --- 15. 🔥 分类详情页面 ---
        composable(
            route = ScreenRoutes.Category.route,
            arguments = listOf(
                navArgument("tid") { type = NavType.IntType },
                navArgument("name") { type = NavType.StringType; defaultValue = "" }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) { backStackEntry ->
            val tid = backStackEntry.arguments?.getInt("tid") ?: 0
            val name = Uri.decode(backStackEntry.arguments?.getString("name") ?: "")
            com.android.purebilibili.feature.category.CategoryScreen(
                tid = tid,
                name = name,
                onBack = { navController.popBackStack() },
                onVideoClick = { bvid, cid, cover -> navigateToVideo(bvid, cid, cover) }
            )
        }
    }
}