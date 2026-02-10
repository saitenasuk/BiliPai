package com.android.purebilibili.feature.home

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.animation.DissolvableVideoCard
import com.android.purebilibili.core.ui.animation.jiggleOnDissolve
import com.android.purebilibili.core.util.responsiveContentWidth
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.feature.home.components.cards.ElegantVideoCard
import com.android.purebilibili.feature.home.components.cards.LiveRoomCard
import com.android.purebilibili.feature.home.components.cards.StoryVideoCard

import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import androidx.compose.ui.Alignment

@Composable
fun HomeCategoryPageContent(
    category: HomeCategory,
    categoryState: CategoryContent,
    gridState: LazyGridState,
    gridColumns: Int,
    contentPadding: PaddingValues,
    dissolvingVideos: Set<String>,
    followingMids: Set<Long>,
    onVideoClick: (String, Long, String) -> Unit,
    onLiveClick: (Long, String, String) -> Unit,
    onLoadMore: () -> Unit,
    onDismissVideo: (String) -> Unit,
    onWatchLater: (String, Long) -> Unit,
    onDissolveComplete: (String) -> Unit,
    longPressCallback: (VideoItem) -> Unit, // [Feature] Long Press
    displayMode: Int,
    cardAnimationEnabled: Boolean,
    cardTransitionEnabled: Boolean,
    isDataSaverActive: Boolean,
    oldContentAnchorBvid: String? = null,
    oldContentStartIndex: Int? = null,
    modifier: Modifier = Modifier,
) {
    // Check for load more
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 4 && !categoryState.isLoading && categoryState.hasMore
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(gridColumns),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        if (category == HomeCategory.LIVE) {
            // Live Category Content
            
            // 1. Followed Live Rooms
            if (categoryState.followedLiveRooms.isNotEmpty()) {
                item(span = { GridItemSpan(gridColumns) }) {
                    Text(
                        text = "关注",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
                
                itemsIndexed(
                    items = categoryState.followedLiveRooms,
                    key = { _, room -> "followed_${room.roomid}" },
                    contentType = { _, _ -> "live_room" }
                ) { index, room ->
                    LiveRoomCard(
                        room = room,
                        index = index,
                        onClick = { onLiveClick(room.roomid, room.title, room.uname) } 
                    )
                }
            }
            
            // 2. Popular Live Rooms
            if (categoryState.liveRooms.isNotEmpty()) {
                item(span = { GridItemSpan(gridColumns) }) {
                    Text(
                        text = "推荐直播",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
                
                itemsIndexed(
                    items = categoryState.liveRooms,
                    key = { _, room -> "popular_${room.roomid}" },
                    contentType = { _, _ -> "live_room" }
                ) { index, room ->
                    LiveRoomCard(
                        room = room,
                        index = index,
                        onClick = { onLiveClick(room.roomid, room.title, room.uname) } 
                    )
                }
            }
        } else {
            // Video Category Content
            if (categoryState.videos.isNotEmpty()) {
                val shouldShowOldContentDivider = category == HomeCategory.RECOMMEND &&
                    (
                        (oldContentAnchorBvid != null && categoryState.videos.any { it.bvid == oldContentAnchorBvid }) ||
                            (oldContentStartIndex != null && oldContentStartIndex > 0 && oldContentStartIndex < categoryState.videos.size)
                        )

                categoryState.videos.forEachIndexed { index, video ->
                    val shouldInsertDividerHere = shouldShowOldContentDivider && (
                        (oldContentAnchorBvid != null && video.bvid == oldContentAnchorBvid && index > 0) ||
                            (oldContentAnchorBvid == null && index == oldContentStartIndex)
                        )
                    if (shouldInsertDividerHere) {
                        item(
                            key = "old_content_divider_$index",
                            span = { GridItemSpan(gridColumns) }
                        ) {
                            OldContentDivider()
                        }
                    }

                    item(
                        key = if (video.bvid.isNotBlank()) video.bvid else "video_${video.id}_$index"
                    ) {
                        val isDissolving = video.bvid in dissolvingVideos

                        DissolvableVideoCard(
                            isDissolving = isDissolving,
                            onDissolveComplete = { onDissolveComplete(video.bvid) },
                            cardId = video.bvid,
                            modifier = Modifier.jiggleOnDissolve(video.bvid)
                        ) {
                            when (displayMode) {
                                1 -> {
                                    StoryVideoCard(
                                        video = video,
                                        index = index,
                                        animationEnabled = cardAnimationEnabled,
                                        transitionEnabled = cardTransitionEnabled,
                                        onDismiss = { onDismissVideo(video.bvid) },
                                        onLongClick = { longPressCallback(video) },
                                        onClick = { bvid, cid -> onVideoClick(bvid, cid, video.pic) }
                                    )
                                }

                                else -> {
                                    ElegantVideoCard(
                                        video = video,
                                        index = index,
                                        isFollowing = video.owner.mid in followingMids && category != HomeCategory.FOLLOW,
                                        animationEnabled = cardAnimationEnabled,
                                        transitionEnabled = cardTransitionEnabled,
                                        isDataSaverActive = isDataSaverActive,
                                        onDismiss = { onDismissVideo(video.bvid) },
                                        onWatchLater = { onWatchLater(video.bvid, video.id) },
                                        onLongClick = { longPressCallback(video) },
                                        onClick = { bvid, cid -> onVideoClick(bvid, cid, video.pic) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Loading Indicator at bottom
        if (categoryState.isLoading || categoryState.hasMore) {
             item(span = { GridItemSpan(gridColumns) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (categoryState.isLoading) {
                        CupertinoActivityIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
        
        // Spacer
        item(span = { GridItemSpan(gridColumns) }) {
            Box(modifier = Modifier.fillMaxWidth().height(20.dp))
        }
    }
}

@Composable
private fun OldContentDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        )
        Text(
            text = "以下是上次最新的视频",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        )
    }
}
