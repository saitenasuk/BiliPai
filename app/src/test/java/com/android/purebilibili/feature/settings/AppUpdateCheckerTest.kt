package com.android.purebilibili.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {

    @Test
    fun `normalizeVersion should trim v prefix and prerelease suffix`() {
        assertEquals("5.3.1", AppUpdateChecker.normalizeVersion("v5.3.1-beta.1"))
        assertEquals("5.3.1", AppUpdateChecker.normalizeVersion(" V5.3.1 "))
    }

    @Test
    fun `isRemoteNewer should compare semantic version parts`() {
        assertTrue(AppUpdateChecker.isRemoteNewer("5.3.1", "5.3.2"))
        assertTrue(AppUpdateChecker.isRemoteNewer("5.3.1", "5.4.0"))
        assertFalse(AppUpdateChecker.isRemoteNewer("5.3.1", "5.3.1"))
        assertFalse(AppUpdateChecker.isRemoteNewer("5.3.2", "5.3.1"))
    }

    @Test
    fun `isRemoteNewer should handle different part lengths`() {
        assertTrue(AppUpdateChecker.isRemoteNewer("5.3", "5.3.1"))
        assertFalse(AppUpdateChecker.isRemoteNewer("5.3.1", "5.3"))
    }

    @Test
    fun `parseReleaseAssets should keep apk metadata and ignore non apk assets`() {
        val assets = AppUpdateChecker.parseReleaseAssets(
            """
            {
              "assets": [
                {
                  "name": "BiliPai-v6.9.3.apk",
                  "browser_download_url": "https://example.com/BiliPai-v6.9.3.apk",
                  "size": 104857600,
                  "content_type": "application/vnd.android.package-archive"
                },
                {
                  "name": "BiliPai-v6.9.3-arm64-v8a.apk",
                  "browser_download_url": "https://example.com/BiliPai-v6.9.3-arm64-v8a.apk",
                  "size": 73400320,
                  "content_type": "application/vnd.android.package-archive"
                },
                {
                  "name": "checksums.txt",
                  "browser_download_url": "https://example.com/checksums.txt",
                  "size": 512,
                  "content_type": "text/plain"
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(2, assets.size)
        assertEquals("BiliPai-v6.9.3.apk", assets[0].name)
        assertEquals("https://example.com/BiliPai-v6.9.3.apk", assets[0].downloadUrl)
        assertEquals(104857600L, assets[0].sizeBytes)
        assertEquals("application/vnd.android.package-archive", assets[0].contentType)
        assertTrue(assets.all { it.isApk })
    }

    @Test
    fun `parseReleaseAssets should return empty list when assets are missing`() {
        assertTrue(AppUpdateChecker.parseReleaseAssets("""{"tag_name":"v6.9.3"}""").isEmpty())
    }
}
