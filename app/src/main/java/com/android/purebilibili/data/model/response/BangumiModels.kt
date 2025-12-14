// 文件路径: data/model/response/BangumiModels.kt
package com.android.purebilibili.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ========== 番剧/影视响应模型 ==========

/**
 * 番剧时间表响应
 */
@Serializable
data class BangumiTimelineResponse(
    val code: Int = 0,
    val message: String = "",
    val result: List<TimelineDay>? = null
)

@Serializable
data class TimelineDay(
    val date: String = "",           // 日期 "2024-12-14"
    @SerialName("date_ts")
    val dateTs: Long = 0,            // 时间戳
    @SerialName("day_of_week")
    val dayOfWeek: Int = 0,          // 星期几 (1-7)
    @SerialName("is_today")
    val isToday: Int = 0,            // 是否是今天
    val episodes: List<TimelineEpisode>? = null
)

@Serializable
data class TimelineEpisode(
    @SerialName("episode_id")
    val episodeId: Long = 0,
    @SerialName("season_id")
    val seasonId: Long = 0,
    val title: String = "",           // 番剧标题
    val cover: String = "",           // 封面
    @SerialName("square_cover")
    val squareCover: String = "",     // 方形封面
    @SerialName("pub_index")
    val pubIndex: String = "",        // 更新集数 "第12话"
    @SerialName("pub_time")
    val pubTime: String = "",         // 发布时间 "22:00"
    @SerialName("pub_ts")
    val pubTs: Long = 0,              // 发布时间戳
    val delay: Int = 0,               // 是否延迟
    @SerialName("delay_reason")
    val delayReason: String = "",     // 延迟原因
    val follow: Int = 0               // 是否追番
)

/**
 * 番剧索引/筛选响应
 */
@Serializable
data class BangumiIndexResponse(
    val code: Int = 0,
    val message: String = "",
    val data: BangumiIndexData? = null
)

@Serializable
data class BangumiIndexData(
    @SerialName("has_next")
    val hasNext: Int = 0,
    val list: List<BangumiItem>? = null,
    val num: Int = 0,                  // 当前页数量
    val size: Int = 0,                 // 每页数量
    val total: Int = 0                 // 总数
)

@Serializable
data class BangumiItem(
    @SerialName("season_id")
    val seasonId: Long = 0,
    @SerialName("media_id")
    val mediaId: Long = 0,
    val title: String = "",
    val cover: String = "",
    val badge: String = "",           // 角标 "会员专享" "独家"
    @SerialName("badge_type")
    val badgeType: Int = 0,
    val score: String = "",           // 评分 "9.8"
    @SerialName("new_ep")
    val newEp: NewEpInfo? = null,
    val order: String = "",           // 播放量/追番数
    @SerialName("order_type")
    val orderType: String = "",       // "追番人数" "播放数"
    @SerialName("season_type")
    val seasonType: Int = 0,          // 1=番剧 2=电影 3=纪录片 4=国创 5=电视剧
    @SerialName("season_type_name")
    val seasonTypeName: String = "",
    val subtitle: String = "",        // 副标题
    val styles: String = ""           // 风格标签
)

@Serializable
data class NewEpInfo(
    val cover: String = "",
    val id: Long = 0,
    @SerialName("index_show")
    val indexShow: String = ""        // "全13话" "更新至第12话"
)

/**
 * 番剧详情响应
 */
@Serializable
data class BangumiDetailResponse(
    val code: Int = 0,
    val message: String = "",
    val result: BangumiDetail? = null
)

@Serializable
data class BangumiDetail(
    @SerialName("season_id")
    val seasonId: Long = 0,
    @SerialName("media_id")
    val mediaId: Long = 0,
    val title: String = "",
    val cover: String = "",
    @SerialName("square_cover")
    val squareCover: String = "",
    val evaluate: String = "",        // 简介
    val rating: BangumiRating? = null,
    val stat: BangumiStat? = null,
    @SerialName("new_ep")
    val newEp: NewEpDetail? = null,
    val episodes: List<BangumiEpisode>? = null,
    val seasons: List<SeasonInfo>? = null,      // 关联季度
    val areas: List<AreaInfo>? = null,          // 地区
    val styles: List<String>? = null,           // 🔥🔥 [修复] 风格是字符串数组，不是对象数组
    val actors: String = "",                     // 演员/声优
    val staff: String = "",                      // 制作人员
    @SerialName("season_type")
    val seasonType: Int = 0,
    @SerialName("season_type_name")
    val seasonTypeName: String = "",
    val total: Int = 0,                          // 总集数
    val mode: Int = 0,                           // 2=电影 3=番剧
    val rights: BangumiRights? = null,
    @SerialName("user_status")
    val userStatus: UserStatus? = null
)

@Serializable
data class BangumiRating(
    val score: Float = 0f,
    val count: Int = 0
)

@Serializable
data class BangumiStat(
    val views: Long = 0,              // 播放量
    val danmakus: Long = 0,           // 弹幕数
    val favorites: Long = 0,          // 追番/追剧数
    val coins: Long = 0,
    val likes: Long = 0,
    val reply: Long = 0,              // 评论数
    val share: Long = 0
)

@Serializable
data class NewEpDetail(
    val id: Long = 0,
    val title: String = "",
    val desc: String = "",            // "全13话"
    @SerialName("is_new")
    val isNew: Int = 0
)

@Serializable
data class BangumiEpisode(
    val id: Long = 0,                 // ep_id
    val aid: Long = 0,                // 对应的视频 aid
    val bvid: String = "",
    val cid: Long = 0,
    val title: String = "",           // 集标题 "第1话 开始"
    @SerialName("long_title")
    val longTitle: String = "",       // 长标题
    val cover: String = "",
    val duration: Long = 0,           // 时长（毫秒）
    val badge: String = "",           // "会员" "预告"
    @SerialName("badge_type")
    val badgeType: Int = 0,
    val status: Int = 0,              // 状态
    @SerialName("pub_time")
    val pubTime: Long = 0,
    val skip: EpisodeSkip? = null     // 跳过片头片尾信息
)

@Serializable
data class EpisodeSkip(
    val op: SkipRange? = null,        // 片头
    val ed: SkipRange? = null         // 片尾
)

@Serializable
data class SkipRange(
    val start: Int = 0,
    val end: Int = 0
)

@Serializable
data class SeasonInfo(
    @SerialName("season_id")
    val seasonId: Long = 0,
    @SerialName("season_title")
    val seasonTitle: String = "",     // "第一季" "第二季"
    val title: String = "",
    val cover: String = "",
    val badge: String = "",
    @SerialName("is_new")
    val isNew: Int = 0
)

@Serializable
data class AreaInfo(
    val id: Int = 0,
    val name: String = ""             // "日本" "中国大陆"
)

@Serializable
data class StyleInfo(
    val id: Int = 0,
    val name: String = ""             // "热血" "恋爱"
)

@Serializable
data class BangumiRights(
    @SerialName("allow_download")
    val allowDownload: Int = 0,
    @SerialName("allow_review")
    val allowReview: Int = 0,
    @SerialName("is_preview")
    val isPreview: Int = 0,           // 是否预告/预览
    @SerialName("watch_platform")
    val watchPlatform: Int = 0
)

@Serializable
data class UserStatus(
    val follow: Int = 0,              // 是否追番
    @SerialName("follow_status")
    val followStatus: Int = 0,
    val vip: Int = 0,                 // 是否大会员
    @SerialName("vip_frozen")
    val vipFrozen: Int = 0,
    val progress: WatchProgress? = null
)

@Serializable
data class WatchProgress(
    @SerialName("last_ep_id")
    val lastEpId: Long = 0,
    @SerialName("last_ep_index")
    val lastEpIndex: String = "",
    @SerialName("last_time")
    val lastTime: Long = 0            // 上次观看时间点
)

/**
 * 番剧播放地址响应
 * 注意：实际 API 响应结构是 result -> video_info -> dash
 */
@Serializable
data class BangumiPlayUrlResponse(
    val code: Int = 0,
    val message: String = "",
    val result: BangumiPlayUrlResult? = null
)

/**
 * 番剧播放响应 result 层
 */
@Serializable
data class BangumiPlayUrlResult(
    @SerialName("video_info")
    val videoInfo: BangumiVideoInfo? = null
)

/**
 * 番剧播放视频信息（包含 DASH 等）
 * 注意：移除了类型不稳定的字段（has_paid, is_preview 等），它们有时返回 Int 有时返回 Boolean
 */
@Serializable
data class BangumiVideoInfo(
    val quality: Int = 0,
    val format: String = "",
    val timelength: Long = 0,
    @SerialName("accept_format")
    val acceptFormat: String = "",
    @SerialName("accept_quality")
    val acceptQuality: List<Int>? = null,
    @SerialName("accept_description")
    val acceptDescription: List<String>? = null,
    @SerialName("video_codecid")
    val videoCodecid: Int = 0,
    // 🔥🔥 关键：durl 和 dash 字段
    val durl: List<Durl>? = null,
    val durls: List<Durl>? = null,  // 某些情况下叫 durls
    val dash: Dash? = null,
    @SerialName("support_formats")
    val supportFormats: List<FormatItem>? = null
    // 🔥🔥 [修复] 移除类型不稳定的字段：has_paid, is_preview, status 等
    // 这些字段有时返回 Int (0/1)，有时返回 Boolean (true/false)，导致解析失败
)

/**
 * 番剧类型枚举
 */
enum class BangumiType(val value: Int, val label: String) {
    ANIME(1, "番剧"),
    MOVIE(2, "电影"),
    DOCUMENTARY(3, "纪录片"),
    GUOCHUANG(4, "国创"),
    TV_SHOW(5, "电视剧"),
    VARIETY(7, "综艺")
}

