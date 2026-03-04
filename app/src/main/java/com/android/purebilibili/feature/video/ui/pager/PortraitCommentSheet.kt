package com.android.purebilibili.feature.video.ui.pager

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.data.model.CommentFraudStatus
import com.android.purebilibili.feature.video.ui.components.CommentFraudDetectingBanner
import com.android.purebilibili.feature.video.ui.components.CommentFraudResultDialog
import com.android.purebilibili.feature.video.ui.components.CommentSortFilterBar
import com.android.purebilibili.feature.video.ui.components.ReplyItemView
import com.android.purebilibili.feature.video.viewmodel.CommentSortMode
import com.android.purebilibili.feature.video.viewmodel.VideoCommentViewModel
import kotlinx.coroutines.launch

/**
 * Custom PortraitCommentSheet to avoid WindowInsets quirks of standard ModalBottomSheet.
 * This implementation renders directly in the hierarchy (Box) to ensure full edge-to-edge layout.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun PortraitCommentSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    commentViewModel: VideoCommentViewModel,
    aid: Long,
    upMid: Long = 0,
    onUserClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val defaultSortMode by com.android.purebilibili.core.store.SettingsManager
        .getCommentDefaultSortMode(context)
        .collectAsState(
            initial = com.android.purebilibili.core.store.SettingsManager.getCommentDefaultSortModeSync(context)
        )
    val preferredSortMode = remember(defaultSortMode) {
        CommentSortMode.fromApiMode(defaultSortMode)
    }

    // 监听返回键
    BackHandler(enabled = visible) {
        val subState = commentViewModel.subReplyState.value
        if (subState.visible) {
             commentViewModel.closeSubReply()
        } else {
             onDismiss()
        }
    }

    LaunchedEffect(aid, visible, preferredSortMode, upMid) {
        if (visible) {
             commentViewModel.init(aid, upMid = upMid, preferredSortMode = preferredSortMode)
        }
    }

    // [新增] 评论反诈检测结果弹窗状态
    var fraudDialogStatus by remember { mutableStateOf<CommentFraudStatus?>(null) }
    LaunchedEffect(Unit) {
        commentViewModel.fraudEvent.collect { status ->
            // 只在非正常状态时弹窗
            if (status != CommentFraudStatus.NORMAL) {
                fraudDialogStatus = status
            }
        }
    }

    // 检测结果弹窗
    fraudDialogStatus?.let { status ->
        CommentFraudResultDialog(
            status = status,
            onDismiss = {
                fraudDialogStatus = null
                commentViewModel.dismissFraudResult()
            },
            onDeleteComment = if (status == CommentFraudStatus.SHADOW_BANNED) {
                {
                    val rpid = commentViewModel.commentState.value.fraudDetectRpid
                    if (rpid > 0) {
                        commentViewModel.startDissolve(rpid)
                    }
                }
            } else null
        )
    }

    // 整个覆盖层动画 (Fade In/Out)
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
             // 弹窗内容动画 (Slide Up/Down)
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { it }, // 从底部滑入
                    animationSpec = tween(300)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it }, // 滑出到底部
                    animationSpec = tween(300)
                ),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                // Surface 用于承载内容，点击它不应该透过而触发 Dismiss
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.60f) // 📱 [修复] 减少覆盖高度，让视频可见
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {} // 拦截点击事件
                        ),
                    color = MaterialTheme.colorScheme.surface,
                    // 关键：不设置任何默认 contentColor 或 padding，完全由内部控制
                ) {
                    val subReplyState by commentViewModel.subReplyState.collectAsState()

                    // 内容区域
                    if (subReplyState.visible) {
                         SubReplyContent(
                             viewModel = commentViewModel,
                             onBack = { commentViewModel.closeSubReply() },
                             onUserClick = onUserClick
                         )
                    } else {
                         MainCommentList(
                             viewModel = commentViewModel,
                             onUserClick = onUserClick
                         )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainCommentList(
    viewModel: VideoCommentViewModel,
    onUserClick: (Long) -> Unit
) {
    val state by viewModel.commentState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    Column(modifier = Modifier.fillMaxSize()) {
        CommentSortFilterBar(
            count = state.replyCount,
            sortMode = state.sortMode,
            onSortModeChange = { mode ->
                viewModel.setSortMode(mode)
                scope.launch {
                    com.android.purebilibili.core.store.SettingsManager
                        .setCommentDefaultSortMode(context, mode.apiMode)
                }
            },
            upOnly = state.upOnlyFilter,
            onUpOnlyToggle = { viewModel.toggleUpOnly() }
        )

        // [新增] 评论反诈检测中提示条
        CommentFraudDetectingBanner(isDetecting = state.isDetectingFraud)
        
        if (state.isRepliesLoading && state.replies.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // [关键修复] 设置 contentPadding 为 navigationBars，
                // 这样列表内容最后会被抬高，避免被小白条遮挡，但背景是通透到底的。
                contentPadding = WindowInsets.navigationBars.asPaddingValues() 
            ) {
                items(state.replies, key = { it.rpid }) { reply ->
                    ReplyItemView(
                        item = reply,
                        upMid = state.upMid,
                        isPinned = false,
                        onClick = { 
                            // TODO: 回复点击
                        },
                        onSubClick = { parentReply ->
                            viewModel.openSubReply(parentReply)
                        },
                        onLikeClick = { viewModel.likeComment(reply.rpid) },
                        onAvatarClick = { mid -> mid.toLongOrNull()?.let { onUserClick(it) } }
                    )
                }
                
                // 加载更多
                item {
                    if (!state.isRepliesEnd) {
                        LaunchedEffect(Unit) {
                            viewModel.loadComments()
                        }
                        LoadingFooter()
                    } else {
                         NoMoreFooter()
                    }
                }
            }
        }
    }
}

@Composable
private fun SubReplyContent(
    viewModel: VideoCommentViewModel,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit
) {
    val state by viewModel.subReplyState.collectAsState()
    val rootReply = state.rootReply ?: return
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 二级评论头部 (带返回按钮)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "评论详情",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // [关键修复] 同样应用导航栏内边距
            contentPadding = WindowInsets.navigationBars.asPaddingValues()
        ) {
            // 根评论 (楼主)
            item {
                ReplyItemView(
                    item = rootReply,
                    upMid = state.upMid,
                    isPinned = false,
                    onClick = { /* TODO */ },
                    onSubClick = { /* 已经是详情页，忽略 */ },
                    hideSubPreview = true, // 详情页不显示楼中楼预览
                    onLikeClick = { viewModel.likeComment(rootReply.rpid) },
                    onAvatarClick = { mid -> mid.toLongOrNull()?.let { onUserClick(it) } }
                )
                
                HorizontalDivider(
                    thickness = 8.dp, 
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
                
                Text(
                    text = "相关回复",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
                    fontWeight = FontWeight.Medium
                )
            }
            
            // 相关子评论
            items(state.items, key = { it.rpid }) { reply ->
               ReplyItemView(
                    item = reply,
                    upMid = state.upMid,
                    isPinned = false,
                    onClick = { /* TODO: 回复子评论 */ },
                    onSubClick = { /* 子评论没有子子评论预览 */ },
                    onLikeClick = { viewModel.likeComment(reply.rpid) },
                    onAvatarClick = { mid -> mid.toLongOrNull()?.let { onUserClick(it) } }
                )
            }
            
             // 加载更多
            item {
                if (state.isLoading) {
                    LoadingFooter()
                } else if (!state.isEnd) {
                    LaunchedEffect(Unit) {
                        viewModel.loadMoreSubReplies()
                    }
                    LoadingFooter()
                } else {
                     NoMoreFooter()
                }
            }
        }
    }
}

@Composable
private fun LoadingFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.height(24.dp),
            strokeWidth = 2.dp
        )
    }
}

@Composable
private fun NoMoreFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "没有更多了",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
