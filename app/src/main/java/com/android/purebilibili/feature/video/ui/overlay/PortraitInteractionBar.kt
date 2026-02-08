package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material.icons.rounded.Comment
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.util.FormatUtils



/**
 * 竖屏播放器右侧互动栏 (Refined Style)
 *
 * 移除了头像，仅保留操作按钮：点赞、评论、收藏、分享
 */
@Composable
fun PortraitInteractionBar(
    isLiked: Boolean,
    likeCount: Int,
    isFavorited: Boolean,
    favoriteCount: Int,
    commentCount: Int,
    shareCount: Int,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(end = 8.dp, bottom = 180.dp), // [Fix] Increased padding to move buttons up
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        // 点赞
        InteractionButton(
            icon = if (isLiked) Icons.Rounded.ThumbUp else Icons.Outlined.ThumbUp,
            countText = if (likeCount > 0) FormatUtils.formatStat(likeCount.toLong()) else "点赞",
            isActive = isLiked,
            activeColor = BiliPink,
            onClick = onLikeClick
        )
        
        // 评论
        InteractionButton(
            icon = Icons.Rounded.Comment,
            countText = if (commentCount > 0) FormatUtils.formatStat(commentCount.toLong()) else "评论",
            isActive = false,
            onClick = onCommentClick
        )
        
        // 收藏
        InteractionButton(
            icon = if (isFavorited) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            countText = if (favoriteCount > 0) FormatUtils.formatStat(favoriteCount.toLong()) else "收藏",
            isActive = isFavorited,
            activeColor = BiliPink,
            onClick = onFavoriteClick
        )
        
        // 分享
        InteractionButton(
            icon = Icons.Rounded.Share,
            countText = if (shareCount > 0) FormatUtils.formatStat(shareCount.toLong()) else "分享",
            isActive = false,
            onClick = onShareClick
        )
    }
}

/**
 * 互动按钮组件 (带数字)
 */
@Composable
private fun InteractionButton(
    icon: ImageVector,
    countText: String,
    isActive: Boolean,
    activeColor: Color = BiliPink,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) activeColor else Color.White,
            modifier = Modifier.size(34.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = countText,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelSmall.copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    blurRadius = 8f
                )
            )
        )
    }
}
