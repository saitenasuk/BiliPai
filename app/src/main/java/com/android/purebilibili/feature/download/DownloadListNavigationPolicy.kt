package com.android.purebilibili.feature.download

internal enum class DownloadTaskClickTarget {
    OfflinePlayer
}

internal fun resolveDownloadTaskClickTarget(
    task: DownloadTask,
    isNetworkAvailable: Boolean
): DownloadTaskClickTarget? {
    return if (task.isComplete) {
        DownloadTaskClickTarget.OfflinePlayer
    } else {
        null
    }
}
