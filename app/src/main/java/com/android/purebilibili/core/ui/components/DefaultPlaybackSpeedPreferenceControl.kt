package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset
import io.github.alexzhirkevich.cupertino.CupertinoSlider
import kotlin.math.abs
import kotlin.math.roundToInt

internal const val DEFAULT_PLAYBACK_SPEED_MIN = 0.5f
internal const val DEFAULT_PLAYBACK_SPEED_MAX = 2.0f
internal const val DEFAULT_PLAYBACK_SPEED_STEP = 0.05f
internal val DEFAULT_PLAYBACK_SPEED_PRESETS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
private const val DEFAULT_PLAYBACK_SPEED_EPSILON = 0.001f
private val DEFAULT_PLAYBACK_SPEED_STEPS =
    (((DEFAULT_PLAYBACK_SPEED_MAX - DEFAULT_PLAYBACK_SPEED_MIN) / DEFAULT_PLAYBACK_SPEED_STEP).roundToInt() - 1)
        .coerceAtLeast(0)

internal fun normalizeDefaultPlaybackPreferenceSpeed(speed: Float): Float {
    val clamped = speed.coerceIn(DEFAULT_PLAYBACK_SPEED_MIN, DEFAULT_PLAYBACK_SPEED_MAX)
    val stepsFromMin = ((clamped - DEFAULT_PLAYBACK_SPEED_MIN) / DEFAULT_PLAYBACK_SPEED_STEP).roundToInt()
    val normalized = DEFAULT_PLAYBACK_SPEED_MIN + stepsFromMin * DEFAULT_PLAYBACK_SPEED_STEP
    return ((normalized.coerceIn(DEFAULT_PLAYBACK_SPEED_MIN, DEFAULT_PLAYBACK_SPEED_MAX) * 100f).roundToInt()) / 100f
}

internal fun resolveDefaultPlaybackPreset(speed: Float): Float? {
    val normalized = normalizeDefaultPlaybackPreferenceSpeed(speed)
    return DEFAULT_PLAYBACK_SPEED_PRESETS.firstOrNull { preset ->
        abs(preset - normalized) < DEFAULT_PLAYBACK_SPEED_EPSILON
    }
}

internal fun formatDefaultPlaybackSpeed(speed: Float): String {
    val normalized = normalizeDefaultPlaybackPreferenceSpeed(speed)
    return if (normalized % 1f == 0f) {
        "${normalized.toInt()}x"
    } else {
        "${String.format("%.2f", normalized).trimEnd('0').trimEnd('.')}x"
    }
}

@Composable
fun DefaultPlaybackSpeedPreferenceControl(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = "默认播放速度",
    subtitle: String? = null,
    showCurrentValue: Boolean = true
) {
    val uiPreset = LocalUiPreset.current
    var sliderValue by remember(currentSpeed) {
        mutableFloatStateOf(normalizeDefaultPlaybackPreferenceSpeed(currentSpeed))
    }
    val selectedPreset = resolveDefaultPlaybackPreset(sliderValue)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (title != null || subtitle != null || showCurrentValue) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (title != null || subtitle != null) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (title != null) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (showCurrentValue) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = formatDefaultPlaybackSpeed(sliderValue),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDefaultPlaybackSpeed(DEFAULT_PLAYBACK_SPEED_MIN),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (uiPreset == UiPreset.MD3) {
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = normalizeDefaultPlaybackPreferenceSpeed(it) },
                    onValueChangeFinished = { onSpeedChange(sliderValue) },
                    valueRange = DEFAULT_PLAYBACK_SPEED_MIN..DEFAULT_PLAYBACK_SPEED_MAX,
                    steps = DEFAULT_PLAYBACK_SPEED_STEPS,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
            } else {
                CupertinoSlider(
                    value = sliderValue,
                    onValueChange = { sliderValue = normalizeDefaultPlaybackPreferenceSpeed(it) },
                    onValueChangeFinished = { onSpeedChange(sliderValue) },
                    valueRange = DEFAULT_PLAYBACK_SPEED_MIN..DEFAULT_PLAYBACK_SPEED_MAX,
                    steps = DEFAULT_PLAYBACK_SPEED_STEPS,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
            }
            Text(
                text = formatDefaultPlaybackSpeed(DEFAULT_PLAYBACK_SPEED_MAX),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DEFAULT_PLAYBACK_SPEED_PRESETS.forEach { preset ->
                val isSelected = selectedPreset == preset
                Surface(
                    onClick = {
                        val normalizedPreset = normalizeDefaultPlaybackPreferenceSpeed(preset)
                        sliderValue = normalizedPreset
                        onSpeedChange(normalizedPreset)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.height(32.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = formatDefaultPlaybackSpeed(preset),
                            fontSize = 13.sp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}
