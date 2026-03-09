package com.android.purebilibili.feature.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject
import org.json.JSONTokener
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val contentType: String
) {
    val isApk: Boolean
        get() = name.endsWith(".apk", ignoreCase = true) ||
            contentType.equals("application/vnd.android.package-archive", ignoreCase = true)
}

data class AppUpdateCheckResult(
    val isUpdateAvailable: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseUrl: String,
    val releaseNotes: String,
    val publishedAt: String?,
    val assets: List<AppUpdateAsset>,
    val message: String
)

object AppUpdateChecker {
    private const val LATEST_RELEASE_API = "https://api.github.com/repos/jay3-yy/BiliPai/releases/latest"
    private const val CONNECT_TIMEOUT_MS = 6000
    private const val READ_TIMEOUT_MS = 8000
    private val releaseJson = Json { ignoreUnknownKeys = true }

    suspend fun check(currentVersion: String): Result<AppUpdateCheckResult> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "BiliPai-UpdateChecker")
            }
            try {
                val conn = connection
                val responseCode = conn.responseCode
                if (responseCode !in 200..299) {
                    throw IllegalStateException("更新接口异常: HTTP $responseCode")
                }

                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(JSONTokener(body))

                val latestTag = json.optString("tag_name", "")
                val latestVersion = normalizeVersion(latestTag)
                if (latestVersion.isEmpty()) {
                    throw IllegalStateException("未获取到有效版本号")
                }

                val releaseUrl = json.optString("html_url", "https://github.com/jay3-yy/BiliPai/releases")
                val releaseNotes = json.optString("body", "").trim()
                val publishedAt = json.optString("published_at", "").takeIf { it.isNotBlank() }
                val assets = parseReleaseAssets(body)
                val updateAvailable = isRemoteNewer(currentVersion, latestVersion)
                val message = if (updateAvailable) {
                    "发现新版本 v$latestVersion"
                } else {
                    "已是最新版本"
                }

                AppUpdateCheckResult(
                    isUpdateAvailable = updateAvailable,
                    currentVersion = normalizeVersion(currentVersion),
                    latestVersion = latestVersion,
                    releaseUrl = releaseUrl,
                    releaseNotes = releaseNotes,
                    publishedAt = publishedAt,
                    assets = assets,
                    message = message
                )
            } finally {
                connection.disconnect()
            }
        }
    }

    internal fun normalizeVersion(version: String): String {
        return version
            .trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .trim()
    }

    internal fun isRemoteNewer(localVersion: String, remoteVersion: String): Boolean {
        val local = parseVersionParts(normalizeVersion(localVersion))
        val remote = parseVersionParts(normalizeVersion(remoteVersion))
        val maxSize = maxOf(local.size, remote.size)
        for (index in 0 until maxSize) {
            val localPart = local.getOrElse(index) { 0 }
            val remotePart = remote.getOrElse(index) { 0 }
            if (remotePart > localPart) return true
            if (remotePart < localPart) return false
        }
        return false
    }

    internal fun parseVersionParts(version: String): List<Int> {
        if (version.isBlank()) return emptyList()
        return version
            .split('.')
            .mapNotNull { part -> part.toIntOrNull() }
    }

    internal fun parseReleaseAssets(rawReleaseJson: String): List<AppUpdateAsset> {
        val assetsJson = runCatching {
            releaseJson
                .parseToJsonElement(rawReleaseJson)
                .jsonObject["assets"]
                ?.jsonArray
        }.getOrNull() ?: return emptyList()

        return buildList {
            for (assetElement in assetsJson) {
                val assetJson = assetElement.jsonObject
                val asset = AppUpdateAsset(
                    name = assetJson["name"]?.jsonPrimitive?.content.orEmpty().trim(),
                    downloadUrl = assetJson["browser_download_url"]?.jsonPrimitive?.content.orEmpty().trim(),
                    sizeBytes = assetJson["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                    contentType = assetJson["content_type"]?.jsonPrimitive?.content.orEmpty().trim()
                )
                if (asset.name.isBlank() || asset.downloadUrl.isBlank() || !asset.isApk) continue
                add(asset)
            }
        }
    }

    internal fun parseReleaseAssets(releaseJson: JSONObject): List<AppUpdateAsset> {
        return parseReleaseAssets(releaseJson.toString())
    }
}
