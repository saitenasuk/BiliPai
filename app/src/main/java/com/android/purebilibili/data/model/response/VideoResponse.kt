package com.android.purebilibili.data.model.response

import com.android.purebilibili.data.model.VideoDecodeFormat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- 播放地址 Response (参考 PiliPala url.dart) ---
@Serializable
data class PlayUrlResponse(
    val code: Int = 0,
    val message: String = "",
    val data: PlayUrlData? = null
)

@Serializable
data class PlayUrlData(
    val quality: Int = 0,
    val format: String = "",
    val timelength: Long = 0,
    @SerialName("accept_format")
    val acceptFormat: String = "",
    @SerialName("accept_description")
    val acceptDescription: List<String> = emptyList(),
    @SerialName("accept_quality") 
    val acceptQuality: List<Int> = emptyList(),
    @SerialName("video_codecid")
    val videoCodecid: Int = 0,
    val durl: List<Durl>? = null,
    val dash: Dash? = null,
    @SerialName("support_formats")
    val supportFormats: List<FormatItem>? = null,
    @SerialName("last_play_time")
    val lastPlayTime: Int? = null,
    @SerialName("last_play_cid")
    val lastPlayCid: Long? = null
) {
    // 🔥 PiliPala 风格：提供便捷的访问方法
    val accept_quality: List<Int> get() = acceptQuality
    val accept_description: List<String> get() = acceptDescription
}

@Serializable
data class Durl(
    val order: Int = 0,
    val length: Long = 0,
    val size: Long = 0,
    val url: String = "",
    @SerialName("backup_url")
    val backupUrl: List<String>? = null
) {
    val backup_url: List<String>? get() = backupUrl
}

@Serializable
data class Dash(
    val duration: Int = 0,
    val minBufferTime: Float = 0f,
    val video: List<DashVideo> = emptyList(),
    val audio: List<DashAudio>? = emptyList(),
    val dolby: Dolby? = null,
    val flac: Flac? = null
)

// 🔥 DASH 视频流 (重命名避免与 ListModels.VideoItem 冲突)
@Serializable
data class DashVideo(
    val id: Int = 0,
    val baseUrl: String = "",
    val backupUrl: List<String>? = null,
    @SerialName("bandwidth")
    val bandwidth: Int = 0,
    @SerialName("mime_type")
    val mimeType: String = "",
    val codecs: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: String = "",
    val sar: String = "",
    val startWithSap: Int? = null,
    val segmentBase: SegmentBase? = null,
    val codecid: Int? = null
) {
    fun getValidUrl(): String = baseUrl.takeIf { it.isNotEmpty() }
        ?: backupUrl?.firstOrNull { it.isNotEmpty() } ?: ""
    
    val decodeFormat: VideoDecodeFormat?
        get() = VideoDecodeFormat.fromCodecs(codecs)
}

// 🔥 DASH 音频流
@Serializable
data class DashAudio(
    val id: Int = 0,
    val baseUrl: String = "",
    val backupUrl: List<String>? = null,
    @SerialName("bandwidth")
    val bandwidth: Int = 0,
    @SerialName("mime_type")
    val mimeType: String = "",
    val codecs: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: String = "",
    val sar: String = "",
    val startWithSap: Int? = null,
    val segmentBase: SegmentBase? = null,
    val codecid: Int? = null
) {
    fun getValidUrl(): String = baseUrl.takeIf { it.isNotEmpty() }
        ?: backupUrl?.firstOrNull { it.isNotEmpty() } ?: ""
}

@Serializable
data class SegmentBase(
    val initialization: String? = null,
    val indexRange: String? = null
)

@Serializable
data class FormatItem(
    val quality: Int = 0,
    val format: String = "",
    @SerialName("new_description")
    val newDescription: String = "",
    @SerialName("display_desc")
    val displayDesc: String = "",
    val codecs: List<String>? = null
)

@Serializable
data class Dolby(
    val type: Int = 0,
    val audio: List<DashAudio>? = null
)

@Serializable
data class Flac(
    val display: Boolean = false,
    val audio: DashAudio? = null
)

// 兼容旧代码的类型别名
typealias DashMedia = DashVideo

// 🔥🔥 扩展函数：获取最佳视频流
fun Dash.getBestVideo(targetQn: Int, preferCodec: String = "avc"): DashVideo? {
    if (video.isEmpty()) {
        android.util.Log.w("VideoResponse", "⚠️ getBestVideo: video list is empty!")
        return null
    }
    
    com.android.purebilibili.core.util.Logger.d("VideoResponse", "🔍 getBestVideo: targetQn=$targetQn, availableIds=${video.map { it.id }}")
    
    val validVideos = video.filter { it.getValidUrl().isNotEmpty() }
    if (validVideos.isEmpty()) {
        android.util.Log.w("VideoResponse", "⚠️ getBestVideo: no video has valid URL")
        return video.firstOrNull()
    }
    
    val grouped = validVideos.groupBy { it.id }
    
    val targetVideos = grouped[targetQn] 
        ?: grouped.entries.filter { it.key <= targetQn }.maxByOrNull { it.key }?.value
        ?: grouped.entries.minByOrNull { kotlin.math.abs(it.key - targetQn) }?.value
        ?: validVideos
    
    val selected = targetVideos
        .sortedByDescending { 
            when {
                it.codecs.startsWith("avc", ignoreCase = true) -> 2
                it.codecs.contains(preferCodec, ignoreCase = true) -> 1
                else -> 0
            }
        }
        .firstOrNull()
    
    com.android.purebilibili.core.util.Logger.d("VideoResponse", "✅ getBestVideo: selected id=${selected?.id}, codec=${selected?.codecs}")
    return selected
}

// 🔥🔥 扩展函数：获取最佳音频流
fun Dash.getBestAudio(): DashAudio? {
    if (audio.isNullOrEmpty()) {
        com.android.purebilibili.core.util.Logger.d("VideoResponse", "ℹ️ getBestAudio: no audio streams")
        return null
    }
    
    val validAudios = audio.filter { it.getValidUrl().isNotEmpty() }
    if (validAudios.isEmpty()) {
        return audio.firstOrNull()
    }
    
    val selected = validAudios.maxByOrNull { it.bandwidth }
    com.android.purebilibili.core.util.Logger.d("VideoResponse", "✅ getBestAudio: selected id=${selected?.id}, bandwidth=${selected?.bandwidth}")
    return selected
}