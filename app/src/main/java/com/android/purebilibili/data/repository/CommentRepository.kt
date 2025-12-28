// 文件路径: data/repository/CommentRepository.kt
package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiUtils
import com.android.purebilibili.data.model.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TreeMap

/**
 * 评论相关数据仓库
 * 从 VideoRepository 拆分出来，专注于评论功能
 */
object CommentRepository {
    private val api = NetworkModule.api

    // WBI Key 缓存
    private var wbiKeysCache: Pair<String, String>? = null
    private var wbiKeysTimestamp: Long = 0
    private const val WBI_CACHE_DURATION = 1000 * 60 * 30 // 30分钟缓存

    /**
     * 获取 WBI Keys（用于 WBI 签名）
     */
    private suspend fun getWbiKeys(): Pair<String, String> {
        val currentCheck = System.currentTimeMillis()
        val cached = wbiKeysCache
        if (cached != null && (currentCheck - wbiKeysTimestamp < WBI_CACHE_DURATION)) {
            return cached
        }

        val maxRetries = 3
        var lastError: Exception? = null
        
        for (attempt in 1..maxRetries) {
            try {
                val navResp = api.getNavInfo()
                val wbiImg = navResp.data?.wbi_img
                
                if (wbiImg != null) {
                    val imgKey = wbiImg.img_url.substringAfterLast("/").substringBefore(".")
                    val subKey = wbiImg.sub_url.substringAfterLast("/").substringBefore(".")
                    
                    wbiKeysCache = Pair(imgKey, subKey)
                    wbiKeysTimestamp = System.currentTimeMillis()
                    com.android.purebilibili.core.util.Logger.d("CommentRepo", "✅ WBI Keys obtained successfully (attempt $attempt)")
                    return wbiKeysCache!!
                }
            } catch (e: Exception) {
                lastError = e
                android.util.Log.w("CommentRepo", "getWbiKeys attempt $attempt failed: ${e.message}")
                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay(200L * attempt) // 递增延迟
                }
            }
        }
        
        throw Exception("Wbi Keys Error after $maxRetries attempts: ${lastError?.message}")
    }

    /**
     * 获取评论列表
     * @param mode 排序模式: 3=热度(默认), 2=时间
     */
    suspend fun getComments(aid: Long, page: Int, ps: Int = 20, mode: Int = 3): Result<ReplyData> = withContext(Dispatchers.IO) {
        try {
            // 确保 buvid3 已初始化
            VideoRepository.ensureBuvid3()
            
            val response = if (mode == 2) {
                // 时间排序使用旧版 API
                com.android.purebilibili.core.util.Logger.d("CommentRepo", "🔥 getComments (Legacy): aid=$aid, page=$page, sort=0 (时间)")
                api.getReplyListLegacy(
                    oid = aid,
                    type = 1,
                    pn = page,
                    ps = ps,
                    sort = 0  // 旧版 API: 0=按时间, 1=按点赞
                )
            } else {
                // 热度排序使用 WBI API
                val (imgKey, subKey) = getWbiKeys()
                com.android.purebilibili.core.util.Logger.d("CommentRepo", "🔥 getComments (WBI): aid=$aid, page=$page, mode=3 (热度)")
                
                val params = TreeMap<String, String>()
                params["oid"] = aid.toString()
                params["type"] = "1"
                params["mode"] = "3"  // WBI API: 3=热度
                params["next"] = page.toString()
                params["ps"] = ps.toString()

                val signedParams = WbiUtils.sign(params, imgKey, subKey)
                api.getReplyList(signedParams)
            }
            
            val sortLabel = if (mode == 2) "时间" else "热度"
            com.android.purebilibili.core.util.Logger.d("CommentRepo", "🔥 getComments result: mode=$mode($sortLabel), replies=${response.data?.replies?.size ?: 0}, code=${response.code}")

            if (response.code == 0) {
                Result.success(response.data ?: ReplyData())
            } else {
                val errorMsg = when (response.code) {
                    -352 -> "请求频率过高，请稍后再试"
                    -111 -> "签名验证失败"
                    -101 -> "需要登录后才能查看评论"
                    -400 -> "请求参数错误"
                    -412 -> "请求被拦截，请稍后再试"
                    12002 -> "评论区已关闭"
                    12009 -> "评论内容不存在"
                    else -> "加载评论失败 (${response.code})"
                }
                android.util.Log.e("CommentRepo", "❌ getComments failed: ${response.code} - ${response.message}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("CommentRepo", "❌ getComments exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 获取二级评论（楼中楼）
     */
    suspend fun getSubComments(aid: Long, rootId: Long, page: Int, ps: Int = 20): Result<ReplyData> = withContext(Dispatchers.IO) {
        try {
            // 确保 buvid3 已初始化
            VideoRepository.ensureBuvid3()
            
            com.android.purebilibili.core.util.Logger.d("CommentRepo", "🔥 getSubComments: aid=$aid, rootId=$rootId, page=$page")
            
            val response = api.getReplyReply(
                oid = aid,
                root = rootId,
                pn = page,
                ps = ps
            )
            
            com.android.purebilibili.core.util.Logger.d("CommentRepo", "🔥 getSubComments response: code=${response.code}, replies=${response.data?.replies?.size ?: 0}")
            
            if (response.code == 0) {
                Result.success(response.data ?: ReplyData())
            } else {
                android.util.Log.e("CommentRepo", "❌ getSubComments failed: ${response.code} - ${response.message}")
                Result.failure(Exception("加载回复失败 (${response.code})"))
            }
        } catch (e: Exception) {
            android.util.Log.e("CommentRepo", "❌ getSubComments exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 获取表情包映射
     */
    suspend fun getEmoteMap(): Map<String, String> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<String, String>()
        // 默认表情
        map["[doge]"] = "http://i0.hdslb.com/bfs/emote/6f8743c3c13009f4705307b2750e32f5068225e3.png"
        map["[笑哭]"] = "http://i0.hdslb.com/bfs/emote/500b63b2f293309a909403a746566fdd6104d498.png"
        map["[妙啊]"] = "http://i0.hdslb.com/bfs/emote/03c39c8eb009f63568971032b49c716259c72441.png"
        try {
            val response = api.getEmotes()
            response.data?.packages?.forEach { pkg ->
                pkg.emote?.forEach { emote -> map[emote.text] = emote.url }
            }
        } catch (e: Exception) { e.printStackTrace() }
        map
    }
}
