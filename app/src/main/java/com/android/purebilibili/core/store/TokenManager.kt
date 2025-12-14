// 文件路径: core/store/TokenManager.kt
package com.android.purebilibili.core.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

object TokenManager {
    private val SESSDATA_KEY = stringPreferencesKey("sessdata")
    private val BUVID3_KEY = stringPreferencesKey("buvid3")

    // 🔥 [新增] SharedPreferences 备份，解决冷启动时 DataStore 异步加载慢导致 ApiClient 无 Cookie 的问题
    private const val SP_NAME = "token_backup_sp"
    private const val SP_KEY_SESS = "sessdata_backup"
    private const val SP_KEY_BUVID = "buvid3_backup"
    private const val SP_KEY_CSRF = "bili_jct_backup"  // 🔥 新增 CSRF 持久化
    private const val SP_KEY_MID = "mid_backup"        // 🔥 新增 MID 持久化

    @Volatile
    var sessDataCache: String? = null
        private set

    // 🔥 [修复]：移除了 private set，允许 ApiClient 生成临时 ID 后写入
    @Volatile
    var buvid3Cache: String? = null
    
    // 🔥 [新增] VIP 状态缓存 (1=有效大会员, 0=非VIP)
    @Volatile
    var isVipCache: Boolean = false
    
    // 🔥 [新增] CSRF Token 缓存 (bili_jct)
    @Volatile
    var csrfCache: String? = null
    
    // 🔥 [新增] 用户 MID 缓存
    @Volatile
    var midCache: Long? = null

    fun init(context: Context) {
        // 1. 🔥 同步读取 SP 备份，确保主线程立即有数据
        val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        sessDataCache = sp.getString(SP_KEY_SESS, null)
        buvid3Cache = sp.getString(SP_KEY_BUVID, null)
        csrfCache = sp.getString(SP_KEY_CSRF, null)  // 🔥 读取 CSRF
        midCache = sp.getLong(SP_KEY_MID, 0L).takeIf { it > 0 }  // 🔥 读取 MID
        
        com.android.purebilibili.core.util.Logger.d("TokenManager", "🔥 init: sessData=${sessDataCache?.take(10)}..., csrf=${csrfCache?.take(10)}..., mid=$midCache")

        // 2. 启动 DataStore 监听 (主要数据源)
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.data.collect { prefs ->
                val dsSess = prefs[SESSDATA_KEY]
                val dsBuvid = prefs[BUVID3_KEY]

                // 更新内存
                sessDataCache = dsSess
                
                if (dsBuvid == null) {
                    val newBuvid = generateBuvid3()
                    saveBuvid3(context, newBuvid)
                } else {
                    buvid3Cache = dsBuvid
                }

                // 🔥 数据同步：如果 DataStore 有值但 SP 没值 (或值不同)，同步写入 SP (从 V1 迁移到 V2)
                if (sessDataCache != null && sessDataCache != sp.getString(SP_KEY_SESS, null)) {
                    sp.edit().putString(SP_KEY_SESS, sessDataCache).apply()
                }
                if (buvid3Cache != null && buvid3Cache != sp.getString(SP_KEY_BUVID, null)) {
                    sp.edit().putString(SP_KEY_BUVID, buvid3Cache).apply()
                }
            }
        }
    }
    
    // 🔥 [新增] 保存 CSRF Token
    fun saveCsrf(context: Context, csrf: String) {
        csrfCache = csrf
        context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            .edit().putString(SP_KEY_CSRF, csrf).apply()
        com.android.purebilibili.core.util.Logger.d("TokenManager", "🔥 saveCsrf: ${csrf.take(10)}...")
    }
    
    // 🔥 [新增] 保存用户 MID
    fun saveMid(context: Context, mid: Long) {
        midCache = mid
        context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            .edit().putLong(SP_KEY_MID, mid).apply()
        com.android.purebilibili.core.util.Logger.d("TokenManager", "🔥 saveMid: $mid")
    }

    suspend fun saveCookies(context: Context, sessData: String) {
        sessDataCache = sessData
        
        // 1. 存入 SP (同步/快速)
        context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            .edit().putString(SP_KEY_SESS, sessData).apply()

        // 2. 存入 DataStore (异步/持久)
        context.dataStore.edit { prefs ->
            prefs[SESSDATA_KEY] = sessData
        }
    }

    suspend fun saveBuvid3(context: Context, buvid3: String) {
        buvid3Cache = buvid3
        
        // 1. 存入 SP
        context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            .edit().putString(SP_KEY_BUVID, buvid3).apply()

        // 2. 存入 DataStore
        context.dataStore.edit { prefs ->
            prefs[BUVID3_KEY] = buvid3
        }
    }

    fun getSessData(context: Context): Flow<String?> {
        return context.dataStore.data.map { prefs -> prefs[SESSDATA_KEY] }
    }

    suspend fun clear(context: Context) {
        sessDataCache = null
        
        // 清除 SP
        context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            .edit().remove(SP_KEY_SESS).apply()

        // 清除 DataStore
        context.dataStore.edit {
            it.remove(SESSDATA_KEY)
        }
    }

    private fun generateBuvid3(): String {
        return UUID.randomUUID().toString().replace("-", "") + "infoc"
    }
}