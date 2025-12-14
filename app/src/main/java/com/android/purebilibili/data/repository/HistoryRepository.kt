package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.HistoryData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object HistoryRepository {
    private val api = NetworkModule.api

    suspend fun getHistoryList(ps: Int = 20): Result<List<HistoryData>> {
        return withContext(Dispatchers.IO) {
            try {
                com.android.purebilibili.core.util.Logger.d("HistoryRepo", "🔴 Fetching history list...")
                val response = api.getHistoryList(ps)
                com.android.purebilibili.core.util.Logger.d("HistoryRepo", "🔴 Response code=${response.code}, items=${response.data?.list?.size ?: 0}")
                // 打印前两条记录的标题以便调试
                response.data?.list?.take(2)?.forEach {
                    com.android.purebilibili.core.util.Logger.d("HistoryRepo", "🔴 Item: ${it.title}")
                }
                
                if (response.code == 0) {
                    // ListData 中 list 字段存储历史记录
                    Result.success(response.data?.list ?: emptyList())
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                android.util.Log.e("HistoryRepo", "❌ Error: ${e.message}")
                Result.failure(e)
            }
        }
    }
}
