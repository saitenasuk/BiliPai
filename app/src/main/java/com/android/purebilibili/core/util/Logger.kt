// 文件路径: core/util/Logger.kt
package com.android.purebilibili.core.util

import android.util.Log
import com.android.purebilibili.BuildConfig

/**
 * 🔥 统一日志工具类
 * 
 * 在 Release 版本中自动禁用日志输出，减少性能开销
 */
object Logger {
    
    private val isDebug = BuildConfig.DEBUG
    
    /**
     * Debug 日志 - 仅在 Debug 版本输出
     */
    fun d(tag: String, message: String) {
        if (isDebug) Log.d(tag, message)
    }
    
    /**
     * Info 日志 - 仅在 Debug 版本输出
     */
    fun i(tag: String, message: String) {
        if (isDebug) Log.i(tag, message)
    }
    
    /**
     * Warning 日志 - 始终输出
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }
    
    /**
     * Error 日志 - 始终输出
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
}
