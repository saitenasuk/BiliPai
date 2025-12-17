// File: feature/video/usecase/VideoInteractionUseCase.kt
package com.android.purebilibili.feature.video.usecase

import com.android.purebilibili.core.util.AnalyticsHelper
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.repository.ActionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Video Interaction UseCase
 * 
 * Handles all user interaction operations:
 * - Like/unlike
 * - Coin
 * - Favorite/unfavorite
 * - Follow/unfollow
 * - Triple action (like + coin + favorite)
 * 
 * Requirement Reference: AC1.2 - User interactions handled by UseCase
 */
class VideoInteractionUseCase {
    
    companion object {
        private const val TAG = "VideoInteractionUseCase"
    }
    
    /**
     * Toggle like status
     */
    suspend fun toggleLike(
        aid: Long, 
        currentlyLiked: Boolean,
        bvid: String = ""
    ): Result<Boolean> {
        Logger.d(TAG, "toggleLike: aid=$aid, currentlyLiked=$currentlyLiked")
        val newLiked = !currentlyLiked
        
        return ActionRepository.likeVideo(aid, newLiked).also { result ->
            result.onSuccess { liked ->
                AnalyticsHelper.logLike(bvid, liked)
            }
        }
    }
    
    /**
     * Toggle favorite status
     */
    suspend fun toggleFavorite(
        aid: Long, 
        currentlyFavorited: Boolean,
        bvid: String = ""
    ): Result<Boolean> {
        Logger.d(TAG, "toggleFavorite: aid=$aid, currentlyFavorited=$currentlyFavorited")
        val newFavorited = !currentlyFavorited
        
        return ActionRepository.favoriteVideo(aid, newFavorited).also { result ->
            result.onSuccess { favorited ->
                AnalyticsHelper.logFavorite(bvid, favorited)
            }
        }
    }
    
    /**
     * Toggle follow status
     */
    suspend fun toggleFollow(
        mid: Long, 
        currentlyFollowing: Boolean
    ): Result<Boolean> {
        Logger.d(TAG, "toggleFollow: mid=$mid, currentlyFollowing=$currentlyFollowing")
        val newFollowing = !currentlyFollowing
        
        return ActionRepository.followUser(mid, newFollowing).also { result ->
            result.onSuccess { following ->
                AnalyticsHelper.logFollow(mid.toString(), following)
            }
        }
    }
    
    /**
     * Coin a video
     */
    suspend fun doCoin(
        aid: Long, 
        count: Int, 
        alsoLike: Boolean,
        bvid: String = ""
    ): Result<Boolean> {
        Logger.d(TAG, "doCoin: aid=$aid, count=$count, alsoLike=$alsoLike")
        
        return ActionRepository.coinVideo(aid, count, alsoLike).also { result ->
            result.onSuccess {
                AnalyticsHelper.logCoin(bvid, count)
            }
        }
    }
    
    /**
     * Triple action (like + coin + favorite)
     */
    suspend fun doTripleAction(aid: Long): Result<TripleActionResult> {
        Logger.d(TAG, "doTripleAction: aid=$aid")
        
        return ActionRepository.tripleAction(aid).map { repoResult ->
            TripleActionResult(
                likeSuccess = repoResult.likeSuccess,
                coinSuccess = repoResult.coinSuccess,
                coinMessage = repoResult.coinMessage,
                favoriteSuccess = repoResult.favoriteSuccess
            )
        }
    }
    
    /**
     * Check video interaction status
     */
    suspend fun checkInteractionStatus(aid: Long, mid: Long): InteractionStatus = coroutineScope {
        Logger.d(TAG, "checkInteractionStatus: aid=$aid, mid=$mid")
        
        val isLikedDeferred = async { ActionRepository.checkLikeStatus(aid) }
        val isFavoritedDeferred = async { ActionRepository.checkFavoriteStatus(aid) }
        val isFollowingDeferred = async { ActionRepository.checkFollowStatus(mid) }
        val coinCountDeferred = async { ActionRepository.checkCoinStatus(aid) }
        
        InteractionStatus(
            isLiked = isLikedDeferred.await(),
            isFavorited = isFavoritedDeferred.await(),
            isFollowing = isFollowingDeferred.await(),
            coinCount = coinCountDeferred.await()
        )
    }
}

/**
 * Interaction status data class
 */
data class InteractionStatus(
    val isLiked: Boolean = false,
    val isFavorited: Boolean = false,
    val isFollowing: Boolean = false,
    val coinCount: Int = 0
)

/**
 * Triple action result
 */
data class TripleActionResult(
    val likeSuccess: Boolean,
    val coinSuccess: Boolean,
    val coinMessage: String?,
    val favoriteSuccess: Boolean
) {
    val allSuccess: Boolean
        get() = likeSuccess && coinSuccess && favoriteSuccess
    
    fun toSummaryMessage(): String {
        if (allSuccess) return "Triple action success!"
        
        val parts = mutableListOf<String>()
        if (likeSuccess) parts.add("Like OK")
        if (coinSuccess) parts.add("Coin OK")
        else if (coinMessage != null) parts.add("Coin: $coinMessage")
        if (favoriteSuccess) parts.add("Favorite OK")
        
        return parts.joinToString(" ")
    }
}
