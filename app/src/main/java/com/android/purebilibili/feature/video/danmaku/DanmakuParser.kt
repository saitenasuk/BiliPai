// 文件路径: feature/video/danmaku/DanmakuParser.kt
package com.android.purebilibili.feature.video.danmaku

import android.graphics.Color
import android.util.Log
import android.util.Xml
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.GlobalFlagValues
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream

/**
 * 弹幕解析器
 * 
 * 解析 Bilibili 弹幕 XML 格式
 * 格式: <d p="time,type,fontSize,color,timestamp,pool,userId,dmid">content</d>
 */
object DanmakuParser {
    
    private const val TAG = "DanmakuParser"
    
    /**
     * 解析 XML 弹幕数据
     * 
     * @param rawData 原始 XML 数据
     * @param ctx DanmakuContext 用于创建弹幕对象
     * @return 弹幕列表
     */
    fun parse(rawData: ByteArray, ctx: DanmakuContext): List<BaseDanmaku> {
        val danmakuList = mutableListOf<BaseDanmaku>()
        
        try {
            val parser = Xml.newPullParser()
            parser.setInput(ByteArrayInputStream(rawData), "UTF-8")
            
            var eventType = parser.eventType
            var count = 0
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "d") {
                    val pAttr = parser.getAttributeValue(null, "p")
                    parser.next()
                    val content = if (parser.eventType == XmlPullParser.TEXT) parser.text else ""
                    
                    if (pAttr != null && content.isNotEmpty()) {
                        val danmaku = createDanmaku(pAttr, content, ctx)
                        if (danmaku != null) {
                            danmakuList.add(danmaku)
                            count++
                            if (count <= 3) {
                                Log.d(TAG, "📝 Danmaku #$count: time=${danmaku.time}ms, text='${danmaku.text}'")
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            
            Log.d(TAG, "✅ Parsed $count danmakus")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Parse error: ${e.message}", e)
        }
        
        return danmakuList
    }
    
    /**
     * 从属性字符串创建单条弹幕
     * 
     * @param pAttr p 属性值 "time,type,fontSize,color,..."
     * @param content 弹幕文本内容
     * @param ctx DanmakuContext
     * @return 弹幕对象，解析失败返回 null
     */
    private fun createDanmaku(pAttr: String, content: String, ctx: DanmakuContext): BaseDanmaku? {
        try {
            val parts = pAttr.split(",")
            if (parts.size < 4) return null
            
            val time = (parts[0].toFloatOrNull() ?: 0f) * 1000  // 转换为毫秒
            val type = parts[1].toIntOrNull() ?: 1
            val fontSize = parts[2].toFloatOrNull() ?: 25f
            val colorInt = parts[3].toLongOrNull() ?: 0xFFFFFF
            
            // 映射弹幕类型
            val danmakuType = mapDanmakuType(type)
            
            val danmaku = ctx.mDanmakuFactory?.createDanmaku(danmakuType, ctx) ?: return null
            
            danmaku.apply {
                this.time = time.toLong()
                this.text = content
                this.textSize = fontSize * 2.0f
                this.textColor = colorInt.toInt() or 0xFF000000.toInt()
                this.textShadowColor = if (colorInt == 0xFFFFFF.toLong()) Color.BLACK else Color.WHITE
                this.flags = GlobalFlagValues()
                this.priority = 0
                this.isLive = false
                // 初始化 duration 避免 NPE
                this.duration = master.flame.danmaku.danmaku.model.Duration(4000)
            }
            
            return danmaku
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * 映射 Bilibili 弹幕类型到 DanmakuFlameMaster 类型
     */
    private fun mapDanmakuType(type: Int): Int = when (type) {
        1, 2, 3 -> BaseDanmaku.TYPE_SCROLL_RL  // 滚动弹幕（右→左）
        4 -> BaseDanmaku.TYPE_FIX_BOTTOM       // 底部弹幕
        5 -> BaseDanmaku.TYPE_FIX_TOP          // 顶部弹幕
        6 -> BaseDanmaku.TYPE_SCROLL_LR        // 逆向滚动（左→右）
        7 -> BaseDanmaku.TYPE_SPECIAL          // 高级弹幕
        else -> BaseDanmaku.TYPE_SCROLL_RL
    }
}
