package com.android.purebilibili.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- 0. 通用简单响应（用于操作类接口如关注/收藏）---
@Serializable
data class SimpleApiResponse(
    val code: Int = 0,
    val message: String = "",
    val ttl: Int = 0
)

// --- 0.1 关注关系响应 ---
@Serializable
data class RelationResponse(
    val code: Int = 0,
    val message: String = "",
    val data: RelationData? = null
)

@Serializable
data class RelationData(
    val mid: Long = 0,
    val attribute: Int = 0,  // 0=未关注, 2=已关注, 6=互相关注, 128=已拉黑
    val mtime: Long = 0,
    val tag: List<Int>? = null,
    val special: Int = 0
) {
    // 是否已关注 (attribute == 2 或 6 表示已关注)
    val isFollowing: Boolean get() = attribute == 2 || attribute == 6
}

// --- 0.2 收藏状态响应 ---
@Serializable
data class FavouredResponse(
    val code: Int = 0,
    val message: String = "",
    val data: FavouredData? = null
)

@Serializable
data class FavouredData(
    val count: Int = 0,
    val favoured: Boolean = false
)

// --- 0.3 点赞状态响应 ---
@Serializable
data class HasLikedResponse(
    val code: Int = 0,
    val message: String = "",
    val data: Int = 0       // 0=未点赞, 1=已点赞
)

// --- 0.4 投币状态响应 ---
@Serializable
data class HasCoinedResponse(
    val code: Int = 0,
    val message: String = "",
    val data: CoinedData? = null
)

@Serializable
data class CoinedData(
    val multiply: Int = 0   // 已投币数量 (0/1/2)
)

// --- 0.5 关注列表响应 ---
@Serializable
data class FollowingsResponse(
    val code: Int = 0,
    val message: String = "",
    val data: FollowingsData? = null
)

@Serializable
data class FollowingsData(
    val list: List<FollowingUser>? = null,
    val total: Int = 0
)

@Serializable
data class FollowingUser(
    val mid: Long = 0,
    val uname: String = "",
    val face: String = "",
    val sign: String = ""
)

// --- 1. 核心通用视频模型 (UI层使用) ---
@Serializable
data class VideoItem(
    val id: Long = 0,
    val bvid: String = "",
    val title: String = "",
    val pic: String = "", // 封面图 URL
    val owner: Owner = Owner(),
    val stat: Stat = Stat(),
    // 🔥 关键修复：补全时长字段，解决 HomeScreen 报错
    val duration: Int = 0,
    // 🔥 新增：历史记录进度字段
    val progress: Int = -1,
    val view_at: Long = 0,
    // 🔥 新增：发布时间戳（秒），用于搜索结果显示
    val pubdate: Long = 0
)

@Serializable
data class Owner(
    val mid: Long = 0,
    val name: String = "",
    val face: String = ""
)

@Serializable
data class Stat(
    val view: Int = 0,
    val danmaku: Int = 0,
    val reply: Int = 0,
    val like: Int = 0,
    // 🔥 UI 美化增强：添加更多统计字段
    val coin: Int = 0,
    val favorite: Int = 0,
    val share: Int = 0
)

// --- 2. 历史记录相关模型 ---
@Serializable
data class HistoryData(
    val title: String = "",
    val pic: String = "", // 历史记录接口返回的封面字段是 pic
    val cover: String = "", // 🔥 有时接口返回 cover
    val author_name: String = "",
    val author_face: String = "",
    val duration: Int = 0,
    // 历史记录的 BVID 藏在 history 对象里
    val history: HistoryPage? = null,
    val stat: Stat? = null, // 🔥 stat 可能为空
    val progress: Int = -1, // 观看进度
    val view_at: Long = 0 // 观看时间戳
) {
    // 转换函数：转为通用 VideoItem
    fun toVideoItem(): VideoItem {
        return VideoItem(
            id = history?.oid ?: 0,
            bvid = history?.bvid ?: "",
            title = title,
            pic = if (cover.isNotEmpty()) cover else pic, // 🔥 优先使用 cover
            owner = Owner(name = author_name, face = author_face),
            // 🔥 如果 stat 为空或 view 用 0，尝试隐式处理，但这里我们无法伪造数据。
            // 至少确保不会因为 null 崩溃。
            stat = stat ?: Stat(), 
            duration = duration,
            progress = progress,
            view_at = view_at
        )
    }
}

@Serializable
data class HistoryPage(
    val oid: Long = 0,
    val bvid: String = ""
)

// --- 3. 收藏夹相关模型 ---
// 收藏夹列表响应
@Serializable
data class FavFolderResponse(
    val code: Int = 0,
    val data: FavFolderList? = null
)

@Serializable
data class FavFolderList(
    val list: List<FavFolder>? = null
)

@Serializable
data class FavFolder(
    val id: Long = 0,
    val fid: Long = 0,
    val mid: Long = 0,
    val title: String = "",
    val media_count: Int = 0
)

// 收藏夹内容单项
@Serializable
data class FavoriteData(
    val id: Long = 0,
    val title: String = "",
    val cover: String = "", // 收藏夹接口返回的封面字段是 cover
    val bvid: String = "",
    val duration: Int = 0,
    val upper: Upper? = null,
    val cnt_info: CntInfo? = null
) {
    // 转换函数：转为通用 VideoItem
    fun toVideoItem(): VideoItem {
        return VideoItem(
            id = id,
            bvid = bvid,
            title = title,
            pic = cover, // 注意这里映射 cover -> pic
            owner = Owner(mid = upper?.mid ?: 0, name = upper?.name ?: "", face = upper?.face ?: ""),
            stat = Stat(view = cnt_info?.play ?: 0, danmaku = cnt_info?.danmaku ?: 0),
            duration = duration
        )
    }
}

@Serializable
data class Upper(
    val mid: Long = 0,
    val name: String = "",
    val face: String = ""
)

@Serializable
data class CntInfo(
    val play: Int = 0,
    val danmaku: Int = 0,
    val collect: Int = 0
)

// --- 4. 通用列表响应包装类 ---
@Serializable
data class ListResponse<T>(
    val code: Int = 0,
    val message: String = "",
    val data: ListData<T>? = null
)

@Serializable
data class ListData<T>(
    // 历史记录接口用 "list"，收藏夹接口用 "medias"
    // 我们在这里定义两个字段，Json 解析时只会填充其中一个
    val list: List<T>? = null,
    val medias: List<T>? = null
)
// --- 5. 推荐视频 Response (追加内容) ---
@Serializable
data class RecommendResponse(
    val code: Int = 0,
    val message: String = "",
    val ttl: Int = 0,
    val data: RecommendData? = null
)

@Serializable
data class RecommendData(
    val item: List<RecommendItem>? = null
)

@Serializable
data class RecommendItem(
    val id: Long = 0,
    val bvid: String? = null,
    val cid: Long? = null,
    val goto: String? = null,
    val uri: String? = null,
    val pic: String? = null, // 推荐接口的封面通常是 pic
    val title: String? = null,
    val duration: Int? = null,
    val pubdate: Long? = null,
    val owner: RecommendOwner? = null,
    val stat: RecommendStat? = null
) {
    // 转换函数：转为通用 VideoItem，方便 UI 显示
    fun toVideoItem(): VideoItem {
        return VideoItem(
            id = id,
            bvid = bvid ?: "",
            title = title ?: "",
            pic = pic ?: "",
            owner = Owner(mid = owner?.mid ?: 0, name = owner?.name ?: "", face = owner?.face ?: ""),
            stat = Stat(view = requestStatConvert(stat?.view), like = requestStatConvert(stat?.like), danmaku = requestStatConvert(stat?.danmaku)),
            duration = duration ?: 0
        )
    }

    // 辅助函数：处理可能为 Long 也可能为 Int 的数据
    private fun requestStatConvert(num: Long?): Int {
        return num?.toInt() ?: 0
    }
}

@Serializable
data class RecommendOwner(
    val mid: Long = 0,
    val name: String = "",
    val face: String = ""
)

@Serializable
data class RecommendStat(
    val view: Long = 0,
    val like: Long = 0,
    val danmaku: Long = 0
)

// --- 6. 热门视频 Response (字段结构不同于推荐) ---
@Serializable
data class PopularResponse(
    val code: Int = 0,
    val message: String = "",
    val data: PopularData? = null
)

@Serializable
data class PopularData(
    val list: List<PopularItem>? = null,
    val no_more: Boolean = false
)

@Serializable
data class PopularItem(
    val bvid: String = "",
    val cid: Long = 0,
    val pic: String = "",
    val title: String = "",
    val duration: Int = 0,
    val pubdate: Long = 0,
    val owner: Owner = Owner(),
    val stat: PopularStat = PopularStat()
) {
    fun toVideoItem(): VideoItem {
        return VideoItem(
            id = cid,
            bvid = bvid,
            title = title,
            pic = pic,
            owner = owner,
            stat = Stat(view = stat.view, like = stat.like, danmaku = stat.danmaku),
            duration = duration
        )
    }
}

@Serializable
data class PopularStat(
    val view: Int = 0,
    val like: Int = 0,
    val danmaku: Int = 0,
    val reply: Int = 0,
    val coin: Int = 0,
    val favorite: Int = 0,
    val share: Int = 0
)

// --- 7. 直播列表 Response ---
@Serializable
data class LiveResponse(
    val code: Int = 0,
    val message: String = "",
    val data: LiveData? = null
)

@Serializable
data class LiveData(
    val list: List<LiveRoom>? = null,
    val count: Int = 0,
    val has_more: Int = 0
)

@Serializable
data class LiveRoom(
    val roomid: Long = 0,
    val uid: Long = 0,
    val title: String = "",
    val uname: String = "",
    val face: String = "",
    val cover: String = "",
    @SerialName("user_cover") val userCover: String = "",
    val online: Int = 0,
    @SerialName("area_name") val areaName: String = "",
    @SerialName("parent_name") val parentName: String = "",
    val keyframe: String = ""  // 关键帧图片
)

// --- 8. 直播播放 URL Response (兼容新旧 API) ---
@Serializable
data class LivePlayUrlResponse(
    val code: Int = 0,
    val message: String = "",
    val data: LivePlayUrlData? = null
)

@Serializable
data class LivePlayUrlData(
    // 旧 API 字段
    val durl: List<LiveDurl>? = null,
    val quality_description: List<LiveQuality>? = null,
    val current_quality: Int = 0,
    // 🔥 新 xlive API 字段
    val playurl_info: PlayurlInfo? = null
)

@Serializable
data class PlayurlInfo(
    val playurl: Playurl? = null
)

@Serializable
data class Playurl(
    val stream: List<StreamInfo>? = null
)

@Serializable
data class StreamInfo(
    @SerialName("protocol_name") val protocolName: String = "",
    val format: List<FormatInfo>? = null
)

@Serializable
data class FormatInfo(
    @SerialName("format_name") val formatName: String = "",
    val codec: List<CodecInfo>? = null
)

@Serializable
data class CodecInfo(
    @SerialName("codec_name") val codecName: String = "",
    @SerialName("base_url") val baseUrl: String = "",
    val url_info: List<UrlInfo>? = null
)

@Serializable
data class UrlInfo(
    val host: String = "",
    val extra: String = ""
)

@Serializable
data class LiveDurl(
    val url: String = "",
    val order: Int = 0
)

@Serializable
data class LiveQuality(
    val qn: Int = 0,
    val desc: String = ""
)
@Serializable
data class FollowedLiveResponse(
    val code: Int = 0,
    val message: String = "",
    val data: FollowedLiveData? = null
)

@Serializable
data class FollowedLiveData(
    val list: List<FollowedLiveRoom>? = null,
    @SerialName("living_num") val livingNum: Int = 0,
    @SerialName("not_living_num") val notLivingNum: Int = 0,
    val pageinfo: PageInfo? = null
)

@Serializable
data class PageInfo(
    val page: Int = 0,
    val page_size: Int = 0,
    val total_page: Int = 0
)

// 🔥🔥 [新增] 直播间详情响应（用于获取在线人数）
@Serializable
data class RoomInfoResponse(
    val code: Int = 0,
    val message: String = "",
    val data: RoomInfoData? = null
)

@Serializable
data class RoomInfoData(
    val room_id: Long = 0,
    val uid: Long = 0,
    val title: String = "",
    val online: Int = 0,  // 🔥 在线人数
    val attention: Int = 0,  // 关注数
    @SerialName("live_status") val liveStatus: Int = 0,
    @SerialName("area_name") val areaName: String = ""
)

@Serializable
data class WatchedShow(
    val switch: Boolean = false,
    val num: Int = 0,
    @SerialName("text_small") val textSmall: String = "",
    @SerialName("text_large") val textLarge: String = ""
)

@Serializable
data class FollowedLiveRoom(
    val roomid: Long = 0,
    val uid: Long = 0,
    val title: String = "",
    val uname: String = "",
    val face: String = "",
    val cover: String = "",  // 🔥 新增：有些 API 直接返回 cover
    @SerialName("room_cover") val roomCover: String = "",
    @SerialName("user_cover") val userCover: String = "",
    @SerialName("system_cover") val systemCover: String = "",
    val online: Int = 0,
    val popularity: Int = 0,
    val attention: Long = 0,
    @SerialName("watched_show") val watchedShow: WatchedShow? = null, // 🔥 新增：可能是 watched_show
    @SerialName("area_name") val areaName: String = "",
    @SerialName("live_status") val liveStatus: Int = 0,  // 1=直播中
    @SerialName("live_time") val liveTime: Long = 0
) {
    // 🔥 转换为 LiveRoom（统一格式）
    fun toLiveRoom(): LiveRoom {
        // 🔥 尝试多个封面来源
        val validCover = listOf(cover, roomCover, userCover, systemCover, face)
            .firstOrNull { it.isNotEmpty() } ?: ""
            
        // 🔥 优先使用 popularity，其次 watched_show，最后 online
        val validOnline = when {
            popularity > 0 -> popularity
            attention > 0 -> attention.toInt()
            watchedShow?.num != null && watchedShow.num > 0 -> watchedShow.num
            else -> online
        }
        
        return LiveRoom(
            roomid = roomid,
            uid = uid,
            title = title,
            uname = uname,
            face = face,
            cover = validCover,
            userCover = userCover.ifEmpty { validCover },
            online = validOnline,
            areaName = areaName,
            keyframe = validCover  // 🔥 使用相同封面作为 keyframe 后备
        )
    }
}

// --- 🔥 分区视频 Response ---
@Serializable
data class RegionVideosResponse(
    val code: Int = 0,
    val message: String = "",
    val data: RegionVideosData? = null
)

@Serializable
data class RegionVideosData(
    val archives: List<PopularItem>? = null,  // 复用 PopularItem 结构
    val page: RegionPage? = null
)

@Serializable
data class RegionPage(
    val count: Int = 0,
    val num: Int = 1,
    val size: Int = 30
)