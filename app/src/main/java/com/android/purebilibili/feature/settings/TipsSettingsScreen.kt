package com.android.purebilibili.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.components.IOSSectionTitle
import com.android.purebilibili.core.ui.animation.staggeredEntrance
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.InfoCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronBackward

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipsSettingsScreen(
    onBack: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("小贴士", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(CupertinoIcons.Outlined.ChevronBackward, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Box(modifier = Modifier.staggeredEntrance(0, isVisible)) {
                    IOSSectionTitle("隐藏操作")
                }
            }
            item {
                Box(modifier = Modifier.staggeredEntrance(1, isVisible)) {
                    TipsSection()
                }
            }
        }
    }
}

@Composable
private fun TipsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        TipItem(
            titile = "1. 摸鱼神器：下滑隐身",
            content = "在视频详情页，手指轻轻往下一滑，播放器瞬间消失！\n此时视频声音继续播放，老板来了也不怕~"
        )
        TipDivider()
        TipItem(
            titile = "2. 咻的一下回顶部",
            content = "首页刷太深回不去？双击底部的那个小房子图标，或者顶部的“推荐”二字，嗖的一下就回家啦！"
        )
        TipDivider()
        TipItem(
            titile = "3. 长按有惊喜",
            content = "看到感兴趣的封面不要犹豫，长按它！\n超大图预览马上奉上，还能顺手点个“稍后再看”，简直是囤片党福音。"
        )
        TipDivider()
        TipItem(
            titile = "4. 左右横跳",
            content = "首页不仅能上下刷，还能左右滑哦~\n在不同的频道分区之间反复横跳，总有一个适合你！"
        )
    }
}

@Composable
private fun TipItem(titile: String, content: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = CupertinoIcons.Filled.InfoCircle,
            contentDescription = null,
            tint = com.android.purebilibili.core.theme.iOSBlue,
            modifier = Modifier.size(20.dp).padding(top = 2.dp) // Align with text top
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = titile,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TipDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 48.dp), // Indent to align with text
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
        thickness = 0.5.dp
    )
}
