package com.android.purebilibili.core.network

import java.net.URLEncoder
import java.security.MessageDigest

object WbiUtils {
    private val mixinKeyEncTab = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
        33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40,
        61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11,
        36, 20, 34, 44, 52
    )

    private fun getMixinKey(orig: String): String {
        val sb = StringBuilder()
        for (i in mixinKeyEncTab) {
            if (i < orig.length) sb.append(orig[i])
        }
        return sb.toString().substring(0, 32)
    }

    // 🔥 过滤非法字符 (Bilibili 要求)
    private fun filterIllegalChars(value: String): String {
        return value.replace(Regex("[!'()*]"), "")
    }

    // 标准化 URL 编码 (仅用于计算签名，不改变原始参数)
    private fun encodeURIComponent(value: String): String {
        return URLEncoder.encode(value, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")
    }

    private fun md5(str: String): String {
        return MessageDigest.getInstance("MD5").digest(str.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    /**
     * 核心修改：返回的 Map 中，Value 保持原始状态（未编码），让 Retrofit 去编码。
     * 签名计算时使用编码后的值。
     * 
     * 🔥 2024 更新：添加 dm_img 系列参数以通过 Bilibili 风控
     */
    fun sign(params: Map<String, String>, imgKey: String, subKey: String): Map<String, String> {
        val mixinKey = getMixinKey(imgKey + subKey)
        val currTime = System.currentTimeMillis() / 1000

        // 1. 准备原始参数 (加入 wts)，并过滤非法字符
        val rawParams = mutableMapOf<String, String>()
        for ((key, value) in params) {
            rawParams[key] = filterIllegalChars(value)
        }
        rawParams["wts"] = currTime.toString()
        
        // 🔥🔥 [关键] 添加 dm_img 系列参数以通过风控
        // 这些是 Bilibili 2024 年新增的风控参数，代表设备指纹信息
        rawParams["dm_img_list"] = "[]"
        rawParams["dm_img_str"] = "V2ViR0wgMS4wIChPcGVuR0wgRVMgMi4wIENocm9taXVtKQ"  // Base64 of "WebGL 1.0 (OpenGL ES 2.0 Chromium)"
        rawParams["dm_cover_img_str"] = "QU5HTEUgKE5WSURJQSwgTlZJRElBIEdlRm9yY2UgR1RYIDEwNjAgNkdCIERpcmVjdDNEMTEgdnNfNV8wIHBzXzVfMCwgRDNEMTEp"  // Base64 of GPU info
        rawParams["dm_img_inter"] = """{"ds":[],"wh":[0,0,0],"of":[0,0,0]}"""

        // 2. 排序 Key
        val sortedKeys = rawParams.keys.sorted()

        // 3. 拼接字符串用于计算 Hash (Key=EncodedValue)
        val queryBuilder = StringBuilder()
        for (key in sortedKeys) {
            val value = rawParams[key]
            if (value != null) {
                // 重点：这里编码只是为了算 Hash
                val encodedValue = encodeURIComponent(value)

                if (queryBuilder.isNotEmpty()) {
                    queryBuilder.append("&")
                }
                queryBuilder.append(key).append("=").append(encodedValue)
            }
        }

        // 4. 计算签名
        val strToHash = queryBuilder.toString() + mixinKey
        val wRid = md5(strToHash)
        
        com.android.purebilibili.core.util.Logger.d("WbiUtils", "🔐 w_rid: $wRid, params count: ${rawParams.size}")

        // 5. 将签名加入原始参数表
        rawParams["w_rid"] = wRid

        // 返回未编码的 Map，交给 Retrofit 处理
        return rawParams
    }
}