// 文件路径: data/repository/BangumiRepository.kt
package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 番剧/影视 Repository
 * 处理番剧、电影、电视剧、纪录片等 PGC 内容
 */
object BangumiRepository {
    private val api = NetworkModule.bangumiApi
    
    /**
     * 获取番剧时间表
     * @param type 1=番剧 4=国创
     */
    suspend fun getTimeline(type: Int = 1): Result<List<TimelineDay>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getTimeline(types = type)
            if (response.code == 0 && response.result != null) {
                Result.success(response.result)
            } else {
                Result.failure(Exception("获取时间表失败: ${response.message}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("BangumiRepo", "getTimeline error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 获取番剧索引/列表
     * @param seasonType 1=番剧 2=电影 3=纪录片 4=国创 5=电视剧 7=综艺
     */
    suspend fun getBangumiIndex(
        seasonType: Int = 1,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<BangumiIndexData> = withContext(Dispatchers.IO) {
        try {
            val response = api.getBangumiIndex(
                seasonType = seasonType,
                st = seasonType,  // 🔥🔥 [修复] st 必须与 seasonType 相同
                page = page,
                pageSize = pageSize
            )
            if (response.code == 0 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception("获取番剧列表失败: ${response.message}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("BangumiRepo", "getBangumiIndex error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 获取番剧详情
     */
    suspend fun getSeasonDetail(seasonId: Long): Result<BangumiDetail> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSeasonDetail(seasonId)
            if (response.code == 0 && response.result != null) {
                Result.success(response.result)
            } else {
                Result.failure(Exception("获取番剧详情失败: ${response.message}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("BangumiRepo", "getSeasonDetail error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 获取番剧播放地址
     */
    suspend fun getBangumiPlayUrl(
        epId: Long,
        qn: Int = 80
    ): Result<BangumiVideoInfo> = withContext(Dispatchers.IO) {
        try {
            val response = api.getBangumiPlayUrl(epId = epId, qn = qn)
            android.util.Log.d("BangumiRepo", "getBangumiPlayUrl response code: ${response.code}, has result: ${response.result != null}, has videoInfo: ${response.result?.videoInfo != null}")
            
            if (response.code == 0 && response.result?.videoInfo != null) {
                Result.success(response.result.videoInfo)
            } else {
                val errorMsg = when (response.code) {
                    -10403 -> "需要大会员才能观看"
                    -404 -> "视频不存在"
                    -101 -> "请先登录后观看"  // 🔥 新增：检测需要登录
                    -400 -> "请求参数错误"
                    -403 -> "访问权限不足"
                    else -> "获取播放地址失败: ${response.message} (code=${response.code})"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("BangumiRepo", "getBangumiPlayUrl error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 追番/追剧
     */
    suspend fun followBangumi(seasonId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val csrf = TokenManager.csrfCache ?: return@withContext Result.failure(Exception("未登录"))
            val response = api.followBangumi(seasonId = seasonId, csrf = csrf)
            if (response.code == 0) {
                Result.success(true)
            } else {
                Result.failure(Exception("追番失败: ${response.message}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("BangumiRepo", "followBangumi error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 取消追番/追剧
     */
    suspend fun unfollowBangumi(seasonId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val csrf = TokenManager.csrfCache ?: return@withContext Result.failure(Exception("未登录"))
            val response = api.unfollowBangumi(seasonId = seasonId, csrf = csrf)
            if (response.code == 0) {
                Result.success(true)
            } else {
                Result.failure(Exception("取消追番失败: ${response.message}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("BangumiRepo", "unfollowBangumi error: ${e.message}")
            Result.failure(e)
        }
    }
}
