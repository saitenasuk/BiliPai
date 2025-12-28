// 文件路径: data/repository/DanmakuRepository.kt
package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 弹幕相关数据仓库
 * 从 VideoRepository 拆分出来，专注于弹幕功能
 */
object DanmakuRepository {
    private val api = NetworkModule.api

    // 弹幕数据缓存 - 避免横竖屏切换时重复下载
    private val danmakuCache = LinkedHashMap<Long, ByteArray>(5, 0.75f, true)
    private const val MAX_DANMAKU_CACHE_SIZE = 5  // 最多缓存5个视频的弹幕
    
    // Protobuf 弹幕分段缓存
    private val danmakuSegmentCache = LinkedHashMap<Long, List<ByteArray>>(5, 0.75f, true)

    /**
     * 清除弹幕缓存
     */
    fun clearDanmakuCache() {
        synchronized(danmakuCache) {
            danmakuCache.clear()
        }
        synchronized(danmakuSegmentCache) {
            danmakuSegmentCache.clear()
        }
        com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "🧹 Danmaku cache cleared")
    }

    /**
     * 获取 XML 格式弹幕原始数据
     */
    suspend fun getDanmakuRawData(cid: Long): ByteArray? = withContext(Dispatchers.IO) {
        com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "🎯 getDanmakuRawData: cid=$cid")
        
        // 先检查缓存
        synchronized(danmakuCache) {
            danmakuCache[cid]?.let {
                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "✅ Danmaku cache hit for cid=$cid, size=${it.size}")
                return@withContext it
            }
        }
        
        try {
            val responseBody = api.getDanmakuXml(cid)
            val bytes = responseBody.bytes()
            com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "🎯 Danmaku raw bytes: ${bytes.size}, first byte: ${if (bytes.isNotEmpty()) String.format("0x%02X", bytes[0]) else "empty"}")

            if (bytes.isEmpty()) {
                android.util.Log.w("DanmakuRepo", "⚠️ Danmaku response is empty!")
                return@withContext null
            }

            val result: ByteArray?
            
            // 检查首字节判断是否压缩
            // XML 以 '<' 开头 (0x3C)
            if (bytes[0] == 0x3C.toByte()) {
                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "✅ Danmaku is plain XML, size=${bytes.size}")
                result = bytes
            } else {
                // 尝试 Deflate 解压
                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "🔄 Danmaku appears compressed, attempting deflate...")
                result = try {
                    val inflater = java.util.zip.Inflater(true) // nowrap=true
                    inflater.setInput(bytes)
                    val outputStream = java.io.ByteArrayOutputStream(bytes.size * 3)
                    val tempBuffer = ByteArray(1024)
                    while (!inflater.finished()) {
                        val count = inflater.inflate(tempBuffer)
                        if (count == 0) {
                             if (inflater.needsInput()) break
                             if (inflater.needsDictionary()) break
                        }
                        outputStream.write(tempBuffer, 0, count)
                    }
                    inflater.end()
                    val decompressed = outputStream.toByteArray()
                    com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "✅ Danmaku decompressed: ${bytes.size} → ${decompressed.size} bytes")
                    decompressed
                } catch (e: Exception) {
                    android.util.Log.e("DanmakuRepo", "❌ Deflate failed: ${e.message}")
                    e.printStackTrace()
                    // 解压失败，返回原始数据
                    bytes
                }
            }
            
            // 存入缓存
            if (result != null) {
                synchronized(danmakuCache) {
                    // 缓存已满时，移除最老的条目
                    while (danmakuCache.size >= MAX_DANMAKU_CACHE_SIZE) {
                        val oldestKey = danmakuCache.keys.firstOrNull()
                        if (oldestKey != null) {
                            danmakuCache.remove(oldestKey)
                            com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "🗑️ Danmaku cache evicted: cid=$oldestKey")
                        }
                    }
                    danmakuCache[cid] = result
                    com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "💾 Danmaku cached: cid=$cid, size=${result.size}, cacheSize=${danmakuCache.size}")
                }
            }
            
            result
        } catch (e: Exception) {
            android.util.Log.e("DanmakuRepo", "❌ getDanmakuRawData failed: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 获取 Protobuf 格式弹幕 (分段加载)
     * 
     * @param cid 视频 cid
     * @param durationMs 视频时长 (毫秒)，用于计算所需分段数
     * @return 所有分段的 Protobuf 数据列表
     */
    suspend fun getDanmakuSegments(cid: Long, durationMs: Long): List<ByteArray> = withContext(Dispatchers.IO) {
        com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "🎯 getDanmakuSegments: cid=$cid, duration=${durationMs}ms")
        
        // 检查缓存
        synchronized(danmakuSegmentCache) {
            danmakuSegmentCache[cid]?.let {
                com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "✅ Protobuf danmaku cache hit: cid=$cid, segments=${it.size}")
                return@withContext it
            }
        }
        
        // 计算所需分段数 (每段 6 分钟 = 360000ms)
        val segmentDurationMs = 360000L
        val segmentCount = ((durationMs + segmentDurationMs - 1) / segmentDurationMs).toInt().coerceAtLeast(1)
        
        com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "📊 Fetching $segmentCount segments for ${durationMs}ms video")
        
        // 顺序获取所有分段
        val results = mutableListOf<ByteArray>()
        for (index in 1..segmentCount) {
            try {
                val response = api.getDanmakuSeg(oid = cid, segmentIndex = index)
                val bytes = response.bytes()
                if (bytes.isNotEmpty()) {
                    com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "✅ Segment $index: ${bytes.size} bytes")
                    results.add(bytes)
                } else {
                    com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "⚠️ Segment $index is empty")
                }
            } catch (e: Exception) {
                android.util.Log.w("DanmakuRepo", "❌ Segment $index failed: ${e.message}")
            }
        }
        
        com.android.purebilibili.core.util.Logger.d("DanmakuRepo", "📊 Got ${results.size}/$segmentCount segments for cid=$cid")
        
        // 缓存结果
        if (results.isNotEmpty()) {
            synchronized(danmakuSegmentCache) {
                while (danmakuSegmentCache.size >= MAX_DANMAKU_CACHE_SIZE) {
                    danmakuSegmentCache.keys.firstOrNull()?.let { danmakuSegmentCache.remove(it) }
                }
                danmakuSegmentCache[cid] = results.toList()
            }
        }
        
        results.toList()
    }
}
