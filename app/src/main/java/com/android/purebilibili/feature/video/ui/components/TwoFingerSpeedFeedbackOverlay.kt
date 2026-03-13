package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.blur.unifiedBlur
import com.android.purebilibili.feature.video.ui.gesture.TwoFingerSpeedGestureMode
import dev.chrisbanes.haze.HazeState
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowLeftArrowRight
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowUpArrowDown

@Composable
fun BoxScope.TwoFingerSpeedFeedbackOverlay(
    visible: Boolean,
    speed: Float,
    mode: TwoFingerSpeedGestureMode,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    if (mode == TwoFingerSpeedGestureMode.Off) return

    val shape = RoundedCornerShape(26.dp)
    val cueText = when (mode) {
        TwoFingerSpeedGestureMode.Vertical -> "双指上下调速"
        TwoFingerSpeedGestureMode.Horizontal -> "双指左右调速"
        TwoFingerSpeedGestureMode.Off -> ""
    }
    val cueIcon = when (mode) {
        TwoFingerSpeedGestureMode.Vertical -> CupertinoIcons.Outlined.ArrowUpArrowDown
        TwoFingerSpeedGestureMode.Horizontal -> CupertinoIcons.Outlined.ArrowLeftArrowRight
        TwoFingerSpeedGestureMode.Off -> CupertinoIcons.Outlined.ArrowUpArrowDown
    }
    val gradientColors = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
        MaterialTheme.colorScheme.surface.copy(alpha = 0.10f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.94f),
        exit = fadeOut() + scaleOut(targetScale = 0.96f),
        modifier = modifier
            .align(Alignment.TopCenter)
            .padding(top = 18.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.06f),
            contentColor = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            shape = shape,
            modifier = Modifier
                .clip(shape)
                .unifiedBlur(hazeState = hazeState)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.18f),
                    shape = shape
                )
                .widthIn(min = 176.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Brush.horizontalGradient(gradientColors))
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = cueIcon,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.86f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = cueText,
                            color = Color.White.copy(alpha = 0.76f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = PlaybackSpeed.formatSpeedFull(speed),
                        color = Color.White.copy(alpha = 0.98f),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
