package com.android.purebilibili.data.model.response

import kotlinx.serialization.Serializable

// --- 1. 热搜模型 (保持不变) ---
@Serializable
data class HotSearchResponse(
    val data: HotSearchData? = null
)

@Serializable
data class HotSearchData(
    val trending: TrendingData? = null
)

@Serializable
data class TrendingData(
    val list: List<HotItem>? = null
)

@Serializable
data class HotItem(
    val keyword: String = "",
    val show_name: String = "",
    val icon: String = ""
)

// --- 2. 搜索结果模型 ---
@Serializable
data class SearchResponse(
    val data: SearchData? = null
)

@Serializable
data class SearchData(
    val result: List<SearchResultCategory>? = null
)

@Serializable
data class SearchResultCategory(
    val result_type: String = "",
    val data: List<SearchVideoItem>? = null
)

// 🔥🔥 [新增] 分类搜索响应 (search/type API)
@Serializable
data class SearchTypeResponse(
    val code: Int = 0,
    val message: String = "",
    val data: SearchTypeData? = null
)

@Serializable
data class SearchTypeData(
    val page: Int = 1,
    val pagesize: Int = 20,
    val numResults: Int = 0,
    val numPages: Int = 0,
    val result: List<SearchVideoItem>? = null  // 直接返回视频列表
)

@Serializable
data class SearchVideoItem(
    val id: Long = 0,
    val bvid: String = "",
    val title: String = "",
    val pic: String = "",
    val author: String = "",
    val play: Int = 0,
    val video_review: Int = 0,
    val duration: String = "",
    // 🔥 新增：发布时间戳（秒）
    val pubdate: Long = 0
) {
    fun toVideoItem(): VideoItem {
        return VideoItem(
            id = id,
            bvid = bvid,
            // 🔥🔥🔥 核心修复：使用正则表达式清洗 HTML 标签和转义字符 🔥🔥🔥
            title = title.replace(Regex("<.*?>"), "") // 去除 <em class="..."> 和 </em>
                .replace("&quot;", "\"")      // 修复双引号转义
                .replace("&amp;", "&")        // 修复 & 符号转义
                .replace("&lt;", "<")         // 修复 < 符号
                .replace("&gt;", ">"),        // 修复 > 符号

            pic = if (pic.startsWith("//")) "https:$pic" else pic,
            owner = Owner(name = author),
            stat = Stat(view = play, danmaku = video_review),
            duration = parseDuration(duration),
            // 🔥 传递发布时间
            pubdate = pubdate
        )
    }

    private fun parseDuration(raw: String): Int {
        if (raw.isBlank()) return 0
        if (raw.all { it.isDigit() }) return raw.toIntOrNull() ?: 0
        val parts = raw.split(":")
        return when (parts.size) {
            2 -> (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
            3 -> (parts[0].toIntOrNull() ?: 0) * 3600 + (parts[1].toIntOrNull() ?: 0) * 60 + (parts[2].toIntOrNull() ?: 0)
            else -> 0
        }
    }
}

// --- 3. 🔥 UP主 搜索结果模型 ---
@Serializable
data class SearchUpItem(
    val mid: Long = 0,
    val uname: String = "",
    val usign: String = "", // 个性签名
    val upic: String = "", // 头像
    val fans: Int = 0, // 粉丝数
    val videos: Int = 0, // 视频数
    val level: Int = 0, // 等级
    val official_verify: SearchOfficialVerify? = null,
    val is_senior_member: Int = 0 // 是否硬核会员
) {
    fun cleanupFields(): SearchUpItem {
        return this.copy(
            uname = uname.replace(Regex("<.*?>"), ""),
            usign = usign.replace(Regex("<.*?>"), ""),
            upic = if (upic.startsWith("//")) "https:$upic" else upic
        )
    }
}

@Serializable
data class SearchOfficialVerify(
    val type: Int = -1, // 0: 个人, 1: 机构, -1: 无
    val desc: String = ""
)

// --- 4. 🔥 搜索类型枚举 ---
enum class SearchType(val value: String, val displayName: String) {
    VIDEO("video", "视频"),
    UP("bili_user", "UP主"),
    BANGUMI("media_bangumi", "番剧"),
    LIVE("live_room", "直播");
    
    companion object {
        fun fromValue(value: String): SearchType {
            return entries.find { it.value == value } ?: VIDEO
        }
    }
}

// --- 5. 🔥 搜索建议模型 ---
@Serializable
data class SearchSuggestResponse(
    val code: Int = 0,
    val result: SearchSuggestResult? = null
)

@Serializable
data class SearchSuggestResult(
    val tag: List<SearchSuggestTag>? = null
)

@Serializable
data class SearchSuggestTag(
    val value: String = "",    // 搜索建议词
    val name: String = "",     // 显示名称 (可能包含高亮)
    val ref: Int = 0,
    val spid: Int = 0
)