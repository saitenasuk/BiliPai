package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.feature.home.components.BottomBarLiquidSegmentedControl
import com.android.purebilibili.feature.video.viewmodel.CommentSortMode
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Person

/**
 *  评论排序筛选栏 (iOS Style)
 *  Header: "评论 (123)"
 *  Controls: Segmented Control [按热度 | 按时间]
 */
@Composable
fun CommentSortFilterBar(
    count: Int,
    sortMode: CommentSortMode,
    onSortModeChange: (CommentSortMode) -> Unit,
    upOnly: Boolean = false,
    onUpOnlyToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sortModes = remember { CommentSortMode.entries.toList() }
    val appearance = rememberVideoCommentAppearance()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        //  Left: Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "评论",
                fontSize = 20.sp, // iOS Large Title style scale
                fontWeight = FontWeight.Bold,
                color = appearance.primaryTextColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = FormatUtils.formatStat(count.toLong()),
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = appearance.secondaryTextColor
            )
        }

        // Right: Sort Control + Only UP Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Only UP Toggle
            iOSToggleButton(
                isChecked = upOnly,
                onToggle = onUpOnlyToggle,
                icon = CupertinoIcons.Filled.Person
            )

            // Segmented Control
            iOSSegmentedControl(
                items = sortModes.map { it.label },
                selectedIndex = sortModes.indexOf(sortMode).coerceAtLeast(0),
                onScaleChange = { index ->
                    sortModes.getOrNull(index)?.let(onSortModeChange)
                }
            )
        }
    }
}

/**
 * Bottom-bar matched segmented control.
 */
@Composable
fun iOSSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onScaleChange: (Int) -> Unit
) {
    BottomBarLiquidSegmentedControl(
        items = items,
        selectedIndex = selectedIndex,
        onSelected = onScaleChange,
        itemWidth = if (items.size >= 4) 56.dp else 66.dp,
        height = 32.dp,
        indicatorHeight = 26.dp,
        labelFontSize = 13.sp
    )
}

/**
 * iOS Style Toggle Button (Optional usage)
 */
@Composable
fun iOSToggleButton(
    isChecked: Boolean,
    onToggle: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val appearance = rememberVideoCommentAppearance()
    val backgroundColor = if (isChecked) {
        appearance.toggleCheckedBackgroundColor
    } else {
        appearance.toggleUncheckedBackgroundColor
    }
    val contentColor = if (isChecked) {
        appearance.toggleCheckedContentColor
    } else {
        appearance.toggleUncheckedContentColor
    }
    
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
    }
}
