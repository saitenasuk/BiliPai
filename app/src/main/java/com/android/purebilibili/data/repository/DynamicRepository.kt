// 文件路径: data/repository/DynamicRepository.kt
package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.DynamicFeedResponse
import com.android.purebilibili.data.model.response.DynamicItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 *  动态数据仓库
 * 
 * 负责从 B站 API 获取动态 Feed 数据
 */
object DynamicRepository {
    private val feedPagination = DynamicFeedPaginationRegistry()
    
    //  [新增] 用户动态的独立分页状态
    private var userLastOffset: String = ""
    private var userHasMore: Boolean = true
    private var currentUserMid: Long? = null
    
    /**
     * 获取动态列表
     * @param refresh 是否刷新 (重置分页)
     */
    suspend fun getDynamicFeed(
        refresh: Boolean = false,
        scope: DynamicFeedScope = DynamicFeedScope.DYNAMIC_SCREEN
    ): Result<List<DynamicItem>> = withContext(Dispatchers.IO) {
        try {
            if (refresh) {
                feedPagination.reset(scope)
            }
            
            if (!feedPagination.hasMore(scope) && !refresh) {
                return@withContext Result.success(emptyList())
            }
            
            val response = NetworkModule.dynamicApi.getDynamicFeed(
                type = "all",
                offset = feedPagination.offset(scope)
            )
            
            if (response.code != 0) {
                return@withContext Result.failure(Exception("API error: ${response.message}"))
            }
            
            val data = response.data ?: return@withContext Result.success(emptyList())
            
            // 更新分页状态
            feedPagination.update(scope = scope, offset = data.offset, hasMore = data.has_more)
            
            // 过滤不可见的动态
            val visibleItems = data.items.filter { it.visible }
            
            Result.success(visibleItems)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     *  [新增] 获取指定用户的动态列表
     * @param hostMid UP主 mid
     * @param refresh 是否刷新 (重置分页)
     */
    suspend fun getUserDynamicFeed(hostMid: Long, refresh: Boolean = false): Result<List<DynamicItem>> = withContext(Dispatchers.IO) {
        try {
            // 如果切换了用户或刷新，重置分页
            if (refresh || currentUserMid != hostMid) {
                userLastOffset = ""
                userHasMore = true
                currentUserMid = hostMid
            }
            
            if (!userHasMore && !refresh) {
                return@withContext Result.success(emptyList())
            }
            
            val response = NetworkModule.dynamicApi.getUserDynamicFeed(
                hostMid = hostMid,
                offset = userLastOffset
            )
            
            if (response.code != 0) {
                return@withContext Result.failure(Exception("API error: ${response.message}"))
            }
            
            val data = response.data ?: return@withContext Result.success(emptyList())
            
            // 更新分页状态
            userLastOffset = data.offset
            userHasMore = data.has_more
            
            // 过滤不可见的动态
            val visibleItems = data.items.filter { it.visible }
            
            Result.success(visibleItems)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     *  [新增] 获取单条动态详情（桌面端详情接口）
     */
    suspend fun getDynamicDetail(dynamicId: String): Result<DynamicItem> = withContext(Dispatchers.IO) {
        try {
            val cleanedId = dynamicId.trim()
            if (cleanedId.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("dynamicId 不能为空"))
            }

            val desktopResponse = NetworkModule.dynamicApi.getDynamicDetail(id = cleanedId)
            if (desktopResponse.code == 0) {
                val item = desktopResponse.data?.item
                    ?: return@withContext Result.failure(Exception("动态详情为空"))
                if (!shouldFallbackForDynamicDetail(item)) {
                    return@withContext Result.success(item)
                }

                val fallbackResponse = NetworkModule.dynamicApi.getDynamicDetailFallback(id = cleanedId)
                if (fallbackResponse.code == 0) {
                    val fallbackItem = fallbackResponse.data?.item
                    if (fallbackItem != null) {
                        return@withContext Result.success(fallbackItem)
                    }
                }
                // fallback 失败时保底返回 desktop 结果，避免直接报错
                return@withContext Result.success(item)
            }

            // desktop 接口失败时降级到 web 详情接口（兼容更多动态类型）
            val fallbackResponse = NetworkModule.dynamicApi.getDynamicDetailFallback(id = cleanedId)
            if (fallbackResponse.code == 0) {
                val item = fallbackResponse.data?.item
                    ?: return@withContext Result.failure(Exception("动态详情为空"))
                return@withContext Result.success(item)
            }

            Result.failure(
                Exception(
                    "API error: ${desktopResponse.message.ifBlank { "desktop=${desktopResponse.code}" }}; " +
                        "fallback=${fallbackResponse.message.ifBlank { fallbackResponse.code.toString() }}"
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * 是否还有更多数据
     */
    fun hasMoreData(scope: DynamicFeedScope = DynamicFeedScope.DYNAMIC_SCREEN): Boolean {
        return feedPagination.hasMore(scope)
    }
    
    /**
     *  [新增] 用户动态是否还有更多
     */
    fun userHasMoreData(): Boolean = userHasMore
    
    /**
     * 重置分页状态
     */
    fun resetPagination(scope: DynamicFeedScope = DynamicFeedScope.DYNAMIC_SCREEN) {
        feedPagination.reset(scope)
    }
    
    /**
     *  [新增] 重置用户动态分页状态
     */
    fun resetUserPagination() {
        userLastOffset = ""
        userHasMore = true
        currentUserMid = null
    }
}

enum class DynamicFeedScope {
    DYNAMIC_SCREEN,
    HOME_FOLLOW
}

internal data class DynamicPaginationState(
    var offset: String = "",
    var hasMore: Boolean = true
)

internal class DynamicFeedPaginationRegistry {
    private val stateByScope = mutableMapOf<DynamicFeedScope, DynamicPaginationState>()

    fun reset(scope: DynamicFeedScope) {
        stateByScope[scope] = DynamicPaginationState()
    }

    fun update(scope: DynamicFeedScope, offset: String, hasMore: Boolean) {
        stateByScope[scope] = DynamicPaginationState(offset = offset, hasMore = hasMore)
    }

    fun offset(scope: DynamicFeedScope): String {
        return stateByScope[scope]?.offset.orEmpty()
    }

    fun hasMore(scope: DynamicFeedScope): Boolean {
        return stateByScope[scope]?.hasMore ?: true
    }
}
