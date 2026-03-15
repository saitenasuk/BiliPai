package com.android.purebilibili.feature.dynamic.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import com.android.purebilibili.feature.video.ui.components.SubReplySheet
import com.android.purebilibili.feature.video.viewmodel.SubReplyUiState

@Composable
fun DynamicSubReplyPreviewHost(
    state: SubReplyUiState,
    onDismiss: () -> Unit,
    onLoadMore: () -> Unit
) {
    var showImagePreview by remember { mutableStateOf(false) }
    var previewImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewInitialIndex by remember { mutableIntStateOf(0) }
    var previewSourceRect by remember { mutableStateOf<Rect?>(null) }
    var previewTextContent by remember { mutableStateOf<ImagePreviewTextContent?>(null) }

    if (showImagePreview && previewImages.isNotEmpty()) {
        ImagePreviewDialog(
            images = previewImages,
            initialIndex = previewInitialIndex,
            sourceRect = previewSourceRect,
            textContent = previewTextContent,
            onDismiss = {
                showImagePreview = false
                previewTextContent = null
            }
        )
    }

    SubReplySheet(
        state = state,
        emoteMap = emptyMap(),
        onDismiss = onDismiss,
        onLoadMore = onLoadMore,
        onImagePreview = { images, index, rect, textContent ->
            previewImages = images
            previewInitialIndex = index
            previewSourceRect = rect
            previewTextContent = textContent
            showImagePreview = true
        }
    )
}
