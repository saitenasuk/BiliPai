package com.android.purebilibili.feature.settings.share

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.android.purebilibili.BuildConfig
import com.android.purebilibili.core.store.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

internal const val DEFAULT_SETTINGS_SHARE_PROFILE_NAME = "BiliPai 设置分享"

interface SettingsShareServiceContract {
    suspend fun exportToUri(
        uri: Uri,
        profileName: String = DEFAULT_SETTINGS_SHARE_PROFILE_NAME
    ): Result<SettingsShareExportArtifact>

    suspend fun createShareUri(
        profileName: String = DEFAULT_SETTINGS_SHARE_PROFILE_NAME
    ): Result<Uri>

    suspend fun readImportSession(uri: Uri): Result<SettingsShareImportSession>

    suspend fun applyImport(session: SettingsShareImportSession): Result<SettingsShareApplyResult>
}

class SettingsShareService(private val context: Context) : SettingsShareServiceContract {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun createExportArtifact(
        profileName: String = DEFAULT_SETTINGS_SHARE_PROFILE_NAME
    ): SettingsShareExportArtifact = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val profile = buildSettingsShareProfile(
            profileName = profileName,
            appVersion = BuildConfig.VERSION_NAME,
            exportedAtIso = Instant.ofEpochMilli(now).toString(),
            rawSettings = SettingsManager.exportShareableSettingsSnapshot(context),
            definitions = SettingsManager.getShareableSettingsEntryDefinitions()
        )
        SettingsShareExportArtifact(
            fileName = buildSettingsShareFileName(
                appVersion = BuildConfig.VERSION_NAME,
                epochMs = now
            ),
            json = json.encodeToString(SettingsShareProfile.serializer(), profile),
            profile = profile
        )
    }

    override suspend fun exportToUri(
        uri: Uri,
        profileName: String
    ): Result<SettingsShareExportArtifact> = withContext(Dispatchers.IO) {
        runCatching {
            val artifact = createExportArtifact(profileName)
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(artifact.json.toByteArray(Charsets.UTF_8))
            } ?: error("无法写入导出文件")
            artifact
        }
    }

    override suspend fun createShareUri(
        profileName: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val artifact = createExportArtifact(profileName)
            val shareDir = File(context.cacheDir, "logs/settings-share").apply { mkdirs() }
            val shareFile = File(shareDir, artifact.fileName)
            shareFile.writeText(artifact.json, Charsets.UTF_8)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                shareFile
            )
        }
    }

    override suspend fun readImportSession(uri: Uri): Result<SettingsShareImportSession> =
        withContext(Dispatchers.IO) {
            runCatching {
                val rawJson = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                } ?: error("无法读取导入文件")
                val profile = decodeProfile(rawJson)
                val preview = resolveSettingsShareImportPreview(
                    profile = profile,
                    definitions = SettingsManager.getShareableSettingsEntryDefinitions()
                )
                SettingsShareImportSession(
                    profile = profile,
                    preview = preview,
                    rawJson = rawJson
                )
            }
        }

    override suspend fun applyImport(session: SettingsShareImportSession): Result<SettingsShareApplyResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val settings = flattenSettingsShareSections(session.profile.sections)
                SettingsManager.applyShareableSettingsSnapshot(
                    context = context,
                    settings = settings
                )
            }
        }

    private fun decodeProfile(rawJson: String): SettingsShareProfile {
        return json.decodeFromString(SettingsShareProfile.serializer(), rawJson)
    }
}
