package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 用户操作相关 Repository
 * - 关注/取关 UP 主
 * - 收藏/取消收藏视频
 */
object ActionRepository {
    private val api = NetworkModule.api

    /**
     * 关注/取关 UP 主
     * @param mid UP 主的用户 ID
     * @param follow true=关注, false=取关
     */
    suspend fun followUser(mid: Long, follow: Boolean): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val csrf = TokenManager.csrfCache ?: ""
                com.android.purebilibili.core.util.Logger.d("ActionRepository", "🔥 followUser: mid=$mid, follow=$follow, csrf.length=${csrf.length}")
                if (csrf.isEmpty()) {
                    android.util.Log.e("ActionRepository", "❌ CSRF token is empty!")
                    return@withContext Result.failure(Exception("请先登录"))
                }
                
                val act = if (follow) 1 else 2
                com.android.purebilibili.core.util.Logger.d("ActionRepository", "🔥 Calling modifyRelation...")
                val response = api.modifyRelation(fid = mid, act = act, csrf = csrf)
                com.android.purebilibili.core.util.Logger.d("ActionRepository", "🔥 Response: code=${response.code}, message=${response.message}")
                
                if (response.code == 0) {
                    Result.success(follow)
                } else {
                    Result.failure(Exception(response.message.ifEmpty { "操作失败: ${response.code}" }))
                }
            } catch (e: Exception) {
                android.util.Log.e("ActionRepository", "followUser failed", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 收藏/取消收藏视频
     * @param aid 视频的 aid
     * @param favorite true=收藏, false=取消收藏
     * @param folderId 收藏夹 ID，为空时使用默认收藏夹
     */
    suspend fun favoriteVideo(aid: Long, favorite: Boolean, folderId: Long? = null): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val csrf = TokenManager.csrfCache ?: ""
                if (csrf.isEmpty()) {
                    return@withContext Result.failure(Exception("请先登录"))
                }
                
                // 如果没有指定收藏夹，需要先获取默认收藏夹
                val targetFolderId = folderId ?: getDefaultFolderId()
                if (targetFolderId == null) {
                    return@withContext Result.failure(Exception("无法获取收藏夹"))
                }
                
                val folderIdStr = targetFolderId.toString()
                val response = if (favorite) {
                    api.dealFavorite(rid = aid, addIds = folderIdStr, delIds = "", csrf = csrf)
                } else {
                    api.dealFavorite(rid = aid, addIds = "", delIds = folderIdStr, csrf = csrf)
                }
                
                if (response.code == 0) {
                    Result.success(favorite)
                } else {
                    Result.failure(Exception(response.message.ifEmpty { "操作失败: ${response.code}" }))
                }
            } catch (e: Exception) {
                android.util.Log.e("ActionRepository", "favoriteVideo failed", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 获取用户默认收藏夹 ID
     */
    private suspend fun getDefaultFolderId(): Long? {
        return try {
            val mid = TokenManager.midCache ?: return null
            val response = api.getFavFolders(mid)
            response.data?.list?.firstOrNull()?.id
        } catch (e: Exception) {
            android.util.Log.e("ActionRepository", "getDefaultFolderId failed", e)
            null
        }
    }
    
    /**
     * 🔥 检查是否已关注 UP 主
     */
    suspend fun checkFollowStatus(mid: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getRelation(mid)
                if (response.code == 0) {
                    val isFollowing = response.data?.isFollowing ?: false
                    com.android.purebilibili.core.util.Logger.d("ActionRepository", "🔥 checkFollowStatus: mid=$mid, isFollowing=$isFollowing")
                    isFollowing
                } else {
                    false
                }
            } catch (e: Exception) {
                android.util.Log.e("ActionRepository", "checkFollowStatus failed", e)
                false
            }
        }
    }
    
    /**
     * 🔥 检查视频是否已收藏
     */
    suspend fun checkFavoriteStatus(aid: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.checkFavoured(aid)
                if (response.code == 0) {
                    val isFavoured = response.data?.favoured ?: false
                    com.android.purebilibili.core.util.Logger.d("ActionRepository", "🔥 checkFavoriteStatus: aid=$aid, isFavoured=$isFavoured")
                    isFavoured
                } else {
                    false
                }
            } catch (e: Exception) {
                android.util.Log.e("ActionRepository", "checkFavoriteStatus failed", e)
                false
            }
        }
    }
    
    /**
     * 🔥 点赞/取消点赞视频
     */
    suspend fun likeVideo(aid: Long, like: Boolean): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val csrf = TokenManager.csrfCache ?: ""
                if (csrf.isEmpty()) {
                    return@withContext Result.failure(Exception("请先登录"))
                }
                
                val likeAction = if (like) 1 else 2
                val response = api.likeVideo(aid = aid, like = likeAction, csrf = csrf)
                com.android.purebilibili.core.util.Logger.d("ActionRepository", "🔥 likeVideo: aid=$aid, like=$like, code=${response.code}")
                
                if (response.code == 0) {
                    Result.success(like)
                } else {
                    Result.failure(Exception(response.message.ifEmpty { "点赞失败: ${response.code}" }))
                }
            } catch (e: Exception) {
                android.util.Log.e("ActionRepository", "likeVideo failed", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * 🔥 检查是否已点赞
     */
    suspend fun checkLikeStatus(aid: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.hasLiked(aid)
                if (response.code == 0) {
                    val isLiked = response.data == 1
                    com.android.purebilibili.core.util.Logger.d("ActionRepository", "🔥 checkLikeStatus: aid=$aid, isLiked=$isLiked")
                    isLiked
                } else {
                    false
                }
            } catch (e: Exception) {
                android.util.Log.e("ActionRepository", "checkLikeStatus failed", e)
                false
            }
        }
    }
    
    /**
     * 🔥 投币
     */
    suspend fun coinVideo(aid: Long, count: Int, alsoLike: Boolean): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val csrf = TokenManager.csrfCache ?: ""
                if (csrf.isEmpty()) {
                    return@withContext Result.failure(Exception("请先登录"))
                }
                
                val selectLike = if (alsoLike) 1 else 0
                val response = api.coinVideo(aid = aid, multiply = count, selectLike = selectLike, csrf = csrf)
                com.android.purebilibili.core.util.Logger.d("ActionRepository", "🔥 coinVideo: aid=$aid, count=$count, code=${response.code}")
                
                when (response.code) {
                    0 -> Result.success(true)
                    34004 -> Result.failure(Exception("操作太频繁，请稍后重试"))
                    34005 -> Result.failure(Exception("已投满2个硬币"))
                    -104 -> Result.failure(Exception("硬币余额不足"))
                    else -> Result.failure(Exception(response.message.ifEmpty { "投币失败: ${response.code}" }))
                }
            } catch (e: Exception) {
                android.util.Log.e("ActionRepository", "coinVideo failed", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * 🔥 检查已投币数
     */
    suspend fun checkCoinStatus(aid: Long): Int {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.hasCoined(aid)
                if (response.code == 0) {
                    val coinCount = response.data?.multiply ?: 0
                    com.android.purebilibili.core.util.Logger.d("ActionRepository", "🔥 checkCoinStatus: aid=$aid, coinCount=$coinCount")
                    coinCount
                } else {
                    0
                }
            } catch (e: Exception) {
                android.util.Log.e("ActionRepository", "checkCoinStatus failed", e)
                0
            }
        }
    }
    
    /**
     * 🔥 一键三连 (点赞 + 投币2个 + 收藏)
     */
    data class TripleResult(
        val likeSuccess: Boolean,
        val coinSuccess: Boolean,
        val coinMessage: String?,
        val favoriteSuccess: Boolean
    )
    
    suspend fun tripleAction(aid: Long): Result<TripleResult> {
        return withContext(Dispatchers.IO) {
            val csrf = TokenManager.csrfCache ?: ""
            if (csrf.isEmpty()) {
                return@withContext Result.failure(Exception("请先登录"))
            }
            
            // 1. 点赞
            val likeResult = likeVideo(aid, true)
            val likeSuccess = likeResult.isSuccess
            
            // 2. 投币 (2个，同时点赞)
            val coinResult = coinVideo(aid, 2, true)
            val coinSuccess = coinResult.isSuccess
            val coinMessage = coinResult.exceptionOrNull()?.message
            
            // 3. 收藏
            val favoriteResult = favoriteVideo(aid, true)
            val favoriteSuccess = favoriteResult.isSuccess
            
            com.android.purebilibili.core.util.Logger.d("ActionRepository", "🔥 tripleAction: like=$likeSuccess, coin=$coinSuccess, fav=$favoriteSuccess")
            
            Result.success(TripleResult(
                likeSuccess = likeSuccess,
                coinSuccess = coinSuccess,
                coinMessage = coinMessage,
                favoriteSuccess = favoriteSuccess
            ))
        }
    }
}
