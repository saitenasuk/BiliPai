package com.android.purebilibili.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.iOSSystemGray4

internal data class AdaptiveBottomSheetVisualSpec(
    val cornerRadiusDp: Int,
    val useMaterialDragHandle: Boolean
)

internal fun resolveAdaptiveBottomSheetVisualSpec(
    uiPreset: UiPreset
): AdaptiveBottomSheetVisualSpec {
    return if (uiPreset == UiPreset.MD3) {
        AdaptiveBottomSheetVisualSpec(
            cornerRadiusDp = 28,
            useMaterialDragHandle = true
        )
    } else {
        AdaptiveBottomSheetVisualSpec(
            cornerRadiusDp = 14,
            useMaterialDragHandle = false
        )
    }
}

/**
 * iOS-style Modal Bottom Sheet wrapper.
 * Uses Material3 ModalBottomSheet but styled to match iOS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IOSModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { IOSDragHandle() },
    windowInsets: androidx.compose.foundation.layout.WindowInsets = androidx.compose.material3.BottomSheetDefaults.windowInsets,
    content: @Composable () -> Unit
) {
    val uiPreset = LocalUiPreset.current
    val visualSpec = remember(uiPreset) { resolveAdaptiveBottomSheetVisualSpec(uiPreset) }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = visualSpec.cornerRadiusDp.dp,
            topEnd = visualSpec.cornerRadiusDp.dp
        ),
        containerColor = if (uiPreset == UiPreset.MD3) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            containerColor
        },
        scrimColor = scrimColor,
        dragHandle = if (visualSpec.useMaterialDragHandle) {
            { BottomSheetDefaults.DragHandle() }
        } else {
            dragHandle
        },
        contentWindowInsets = { windowInsets },
        content = {
            content()
        }
    )
}

@Composable
fun IOSDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(iOSSystemGray4.copy(alpha = 0.4f))
        )
    }
}
