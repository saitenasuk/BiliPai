package com.android.purebilibili.feature.download

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DownloadListNavigationPolicyTest {

    private val completedTask = DownloadTask(
        bvid = "BV1offline",
        cid = 1001L,
        title = "Cached video",
        cover = "cover",
        ownerName = "UP",
        ownerFace = "face",
        duration = 120,
        quality = 80,
        qualityDesc = "1080P",
        videoUrl = "https://example.com/video.m4s",
        audioUrl = "https://example.com/audio.m4s",
        status = DownloadStatus.COMPLETED,
        progress = 1f,
        filePath = "/tmp/cached.mp4"
    )

    @Test
    fun completedDownload_prefersOfflinePlaybackEvenWhenNetworkIsAvailable() {
        val target = resolveDownloadTaskClickTarget(
            task = completedTask,
            isNetworkAvailable = true
        )

        assertEquals(DownloadTaskClickTarget.OfflinePlayer, target)
    }

    @Test
    fun incompleteDownload_doesNotNavigate() {
        val target = resolveDownloadTaskClickTarget(
            task = completedTask.copy(status = DownloadStatus.DOWNLOADING, progress = 0.4f),
            isNetworkAvailable = true
        )

        assertNull(target)
    }
}
