package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.ReplyItem
import java.text.SimpleDateFormat
import java.util.*

// 🔥 优化后的颜色常量 (使用 MaterialTheme 替代硬编码)
// private val SubReplyBgColor = Color(0xFFF7F8FA)  // OLD
// private val TextSecondaryColor = Color(0xFF9499A0)  // OLD
// private val TextTertiaryColor = Color(0xFFB2B7BF)   // OLD

@Composable
fun ReplyHeader(count: Int) {
    Row(
        modifier = Modifier
        .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "评论",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = FormatUtils.formatStat(count.toLong()),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ReplyItemView(
    item: ReplyItem,
    emoteMap: Map<String, String> = emptyMap(),
    onClick: () -> Unit,
    onSubClick: (ReplyItem) -> Unit
) {
    val localEmoteMap = remember(item.content.emote, emoteMap) {
        val mergedMap = emoteMap.toMutableMap()
        item.content.emote?.forEach { (key, value) -> mergedMap[key] = value.url }
        mergedMap
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // 头像
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(FormatUtils.fixImageUrl(item.member.avatar))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 🔥 用户名 + 等级 - 统一颜色风格
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.member.uname,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        // 🔥 VIP 用户使用粉色，普通用户使用次要色适配深色模式
                        color = if (item.member.vip?.vipStatus == 1) BiliPink
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // 🔥 优化后的等级标签
                    LevelTag(level = item.member.levelInfo.currentLevel)
                }
                
                Spacer(modifier = Modifier.height(6.dp))

                // 正文
                EmojiText(
                    text = item.content.message,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,  // 🔥 适配深色模式
                    emoteMap = localEmoteMap
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 🔥 时间 + 点赞 + 回复 - 统一使用浅灰色
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTime(item.ctime),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(20.dp))

                    Icon(
                        imageVector = Icons.Outlined.ThumbUp,
                        contentDescription = "点赞",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    if (item.like > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = FormatUtils.formatStat(item.like.toLong()),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "回复",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onSubClick(item) }
                    )
                }

                // 🔥 楼中楼预览 - 使用更浅的背景色
                if (!item.replies.isNullOrEmpty() || item.rcount > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), // 🔥 适配深色
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSubClick(item) }
                            .padding(12.dp)
                    ) {
                        item.replies?.take(3)?.forEach { subReply ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                // 🔥 子评论用户名 - 使用统一的次要色
                                Text(
                                    text = subReply.member.uname,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = ": ",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                // 子评论内容
                                Text(
                                    text = subReply.content.message,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    maxLines = 2,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                        if (item.rcount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "共${item.rcount}条回复 >",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 🔥 分割线 - 更细更浅
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 68.dp)  // 对齐头像右边
    )
}

@Composable
fun EmojiText(
    text: String,
    fontSize: TextUnit,
    color: Color = MaterialTheme.colorScheme.onSurface,
    emoteMap: Map<String, String>
) {
    val annotatedString = buildAnnotatedString {
        // 高亮 "回复 @某人 :"
        val replyPattern = "^回复 @(.*?) :".toRegex()
        val replyMatch = replyPattern.find(text)
        var startIndex = 0
        if (replyMatch != null) {
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)) {
                append(replyMatch.value)
            }
            startIndex = replyMatch.range.last + 1
        }

        val remainingText = text.substring(startIndex)
        val emotePattern = """\[(.*?)\]""".toRegex()
        var lastIndex = 0
        emotePattern.findAll(remainingText).forEach { matchResult ->
            append(remainingText.substring(lastIndex, matchResult.range.first))
            val emojiKey = matchResult.value
            if (emoteMap.containsKey(emojiKey)) {
                appendInlineContent(id = emojiKey, alternateText = emojiKey)
            } else {
                append(emojiKey)
            }
            lastIndex = matchResult.range.last + 1
        }
        if (lastIndex < remainingText.length) {
            append(remainingText.substring(lastIndex))
        }
    }

    val inlineContent = emoteMap.mapValues { (_, url) ->
        InlineTextContent(
            Placeholder(width = 1.4.em, height = 1.4.em, placeholderVerticalAlign = PlaceholderVerticalAlign.Center)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    Text(
        text = annotatedString,
        inlineContent = inlineContent,
        fontSize = fontSize,
        color = color,
        lineHeight = (fontSize.value * 1.5).sp
    )
}

// 🔥 优化后的等级标签 - 无边框，使用柔和的背景色
@Composable
fun LevelTag(level: Int) {
    // 根据等级设置不同颜色 (适配 DarkMode: 使用容器色)
    val bgColor = when {
        level >= 6 -> BiliPink.copy(alpha = 0.15f)
        level >= 4 -> Color(0xFFFF9500).copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = when {
        level >= 6 -> BiliPink
        level >= 4 -> Color(0xFFFF9500)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(3.dp)
    ) {
        Text(
            text = "LV$level",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

fun formatTime(timestamp: Long): String {
    val date = Date(timestamp * 1000)
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(date)
}