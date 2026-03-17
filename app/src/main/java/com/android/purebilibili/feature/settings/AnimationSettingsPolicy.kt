package com.android.purebilibili.feature.settings

internal const val PREDICTIVE_BACK_TOGGLE_TITLE = "启用预测性返回预览"
internal const val PREDICTIVE_BACK_TOGGLE_ACTIVE_SUBTITLE =
    "当前使用系统原生返回预览，关闭后改用经典回退动画"
internal const val PREDICTIVE_BACK_TOGGLE_INACTIVE_SUBTITLE =
    "当前使用经典回退动画，开启后跟随系统返回预览"
internal const val PREDICTIVE_BACK_TOGGLE_DEPENDENCY_SUBTITLE =
    "需先开启“过渡动画”后，才能启用返回预览效果"

internal data class PredictiveBackToggleUiState(
    val title: String,
    val enabled: Boolean,
    val checked: Boolean,
    val subtitle: String
)

internal fun resolvePredictiveBackToggleUiState(
    cardTransitionEnabled: Boolean,
    predictiveBackAnimationEnabled: Boolean
): PredictiveBackToggleUiState {
    if (!cardTransitionEnabled) {
        return PredictiveBackToggleUiState(
            title = PREDICTIVE_BACK_TOGGLE_TITLE,
            enabled = false,
            checked = false,
            subtitle = PREDICTIVE_BACK_TOGGLE_DEPENDENCY_SUBTITLE
        )
    }
    return PredictiveBackToggleUiState(
        title = PREDICTIVE_BACK_TOGGLE_TITLE,
        enabled = true,
        checked = predictiveBackAnimationEnabled,
        subtitle = if (predictiveBackAnimationEnabled) {
            PREDICTIVE_BACK_TOGGLE_ACTIVE_SUBTITLE
        } else {
            PREDICTIVE_BACK_TOGGLE_INACTIVE_SUBTITLE
        }
    )
}
