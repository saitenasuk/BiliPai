package com.android.purebilibili.feature.video.ui

internal enum class FollowButtonTone {
    PRIMARY,
    PRIMARY_CONTAINER
}

internal enum class FollowTextTone {
    ON_PRIMARY,
    ON_PRIMARY_CONTAINER
}

internal enum class FollowBadgeTone {
    PRIMARY
}

internal data class VideoFollowVisualPolicy(
    val detailButtonTone: FollowButtonTone,
    val detailTextTone: FollowTextTone,
    val relatedBadgeTone: FollowBadgeTone?
)

internal fun resolveVideoFollowVisualPolicy(isFollowing: Boolean): VideoFollowVisualPolicy {
    return if (isFollowing) {
        VideoFollowVisualPolicy(
            detailButtonTone = FollowButtonTone.PRIMARY_CONTAINER,
            detailTextTone = FollowTextTone.ON_PRIMARY_CONTAINER,
            relatedBadgeTone = FollowBadgeTone.PRIMARY
        )
    } else {
        VideoFollowVisualPolicy(
            detailButtonTone = FollowButtonTone.PRIMARY,
            detailTextTone = FollowTextTone.ON_PRIMARY,
            relatedBadgeTone = null
        )
    }
}
