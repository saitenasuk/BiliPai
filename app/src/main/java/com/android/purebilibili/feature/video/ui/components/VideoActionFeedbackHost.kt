package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.blur.unifiedBlur
import com.android.purebilibili.feature.video.ui.feedback.VideoFeedbackAnchor
import com.android.purebilibili.feature.video.ui.feedback.VideoFeedbackPlacement
import dev.chrisbanes.haze.HazeState

@Composable
fun BoxScope.VideoActionFeedbackHost(
    message: String?,
    visible: Boolean,
    placement: VideoFeedbackPlacement,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val alignment = when (placement.anchor) {
        VideoFeedbackAnchor.BottomCenter -> Alignment.BottomCenter
        VideoFeedbackAnchor.BottomTrailing -> Alignment.BottomEnd
    }

    AnimatedVisibility(
        visible = visible && !message.isNullOrBlank(),
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.96f),
        modifier = modifier
            .align(alignment)
            .padding(
                start = 20.dp,
                end = placement.sideInsetDp.dp.coerceAtLeast(20.dp),
                top = 16.dp,
                bottom = placement.bottomInsetDp.dp
            )
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.46f),
            contentColor = Color.White,
            shape = RoundedCornerShape(22.dp),
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .unifiedBlur(hazeState = hazeState)
                .widthIn(min = 120.dp, max = 280.dp)
        ) {
            Text(
                text = message.orEmpty(),
                color = Color.White.copy(alpha = 0.98f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}
