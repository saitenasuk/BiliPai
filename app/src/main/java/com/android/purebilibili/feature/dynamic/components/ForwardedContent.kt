// 文件路径: feature/dynamic/components/ForwardedContent.kt
package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import com.android.purebilibili.data.model.response.DynamicDesc
import com.android.purebilibili.data.model.response.DynamicItem

/**
 *  转发的原始内容
 */
@Composable
fun ForwardedContent(
    orig: DynamicItem,
    onVideoClick: (String) -> Unit,
    onUserClick: (Long) -> Unit,
    gifImageLoader: ImageLoader
) {
    val author = orig.modules.module_author
    val content = orig.modules.module_dynamic
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) // 主题自适应背景
            .padding(12.dp)
    ) {
        // 原作者
        if (author != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "@${author.name}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary, // 主题自适应颜色
                    modifier = Modifier.clickable { onUserClick(author.mid) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    author.pub_time,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // 原文字内容 - 使用 RichTextContent 支持表情
        content?.desc?.let { desc ->
            if (desc.text.isNotEmpty()) {
                RichTextContent(
                    desc = desc,
                    onUserClick = onUserClick
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        // 原视频
        content?.major?.archive?.let { archive ->
            VideoCardSmall(
                archive = archive,
                onClick = { onVideoClick(archive.bvid) }
            )
        }
        
        // 原图片
        content?.major?.draw?.let { draw ->
            DrawGridV2(items = draw.items.take(4), gifImageLoader = gifImageLoader)
        }
    }
}
