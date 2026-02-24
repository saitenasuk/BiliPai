package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.data.model.response.DynamicBasic
import com.android.purebilibili.data.model.response.DynamicItem

internal data class DynamicCommentTarget(
    val oid: Long,
    val type: Int
)

private fun String.toPositiveLongOrNull(): Long? {
    return trim().toLongOrNull()?.takeIf { it > 0L }
}

private fun resolveCommentTargetFromBasic(basic: DynamicBasic?): DynamicCommentTarget? {
    if (basic == null || basic.comment_type <= 0) return null
    val oid = basic.comment_id_str.toPositiveLongOrNull()
        ?: basic.rid_str.toPositiveLongOrNull()
        ?: return null
    return DynamicCommentTarget(oid = oid, type = basic.comment_type)
}

internal fun shouldIncludeDynamicItemInVideoTab(item: DynamicItem): Boolean {
    return when (item.type.trim()) {
        "DYNAMIC_TYPE_AV",
        "DYNAMIC_TYPE_PGC",
        "DYNAMIC_TYPE_UGC_SEASON" -> true
        else -> {
            val major = item.modules.module_dynamic?.major
            major?.archive != null || major?.ugc_season != null
        }
    }
}

internal fun resolveDynamicCommentTarget(item: DynamicItem): DynamicCommentTarget? {
    resolveCommentTargetFromBasic(item.basic)?.let { return it }

    val major = item.modules.module_dynamic?.major
    when (major?.type.orEmpty()) {
        "MAJOR_TYPE_OPUS" -> {
            val oid = item.id_str.toPositiveLongOrNull() ?: return null
            return DynamicCommentTarget(oid = oid, type = 17)
        }
        "MAJOR_TYPE_ARCHIVE" -> {
            val aid = major?.archive?.aid?.toPositiveLongOrNull() ?: return null
            return DynamicCommentTarget(oid = aid, type = 1)
        }
        "MAJOR_TYPE_UGC_SEASON" -> {
            val aid = major?.ugc_season?.aid?.takeIf { it > 0L } ?: return null
            return DynamicCommentTarget(oid = aid, type = 1)
        }
        "MAJOR_TYPE_DRAW" -> {
            val drawId = major?.draw?.id?.takeIf { it > 0L } ?: return null
            return DynamicCommentTarget(oid = drawId, type = 11)
        }
    }

    val basic = item.basic
    return when (item.type.trim()) {
        "DYNAMIC_TYPE_AV",
        "DYNAMIC_TYPE_PGC",
        "DYNAMIC_TYPE_UGC_SEASON" -> {
            val oid = basic?.comment_id_str?.toPositiveLongOrNull()
                ?: basic?.rid_str?.toPositiveLongOrNull()
                ?: major?.archive?.aid?.toPositiveLongOrNull()
                ?: major?.ugc_season?.aid?.takeIf { it > 0L }
                ?: return null
            DynamicCommentTarget(oid = oid, type = 1)
        }
        "DYNAMIC_TYPE_DRAW" -> {
            val drawId = major?.draw?.id?.takeIf { it > 0L }
            if (drawId != null) {
                DynamicCommentTarget(oid = drawId, type = 11)
            } else {
                val oid = item.id_str.toPositiveLongOrNull() ?: return null
                DynamicCommentTarget(oid = oid, type = 17)
            }
        }
        "DYNAMIC_TYPE_WORD",
        "DYNAMIC_TYPE_FORWARD",
        "DYNAMIC_TYPE_LIVE_RCMD",
        "DYNAMIC_TYPE_COMMON_SQUARE",
        "DYNAMIC_TYPE_COMMON_VERTICAL" -> {
            val oid = item.id_str.toPositiveLongOrNull()
                ?: basic?.comment_id_str?.toPositiveLongOrNull()
                ?: return null
            DynamicCommentTarget(oid = oid, type = 17)
        }
        "DYNAMIC_TYPE_ARTICLE" -> {
            val oid = basic?.comment_id_str?.toPositiveLongOrNull()
                ?: basic?.rid_str?.toPositiveLongOrNull()
                ?: return null
            DynamicCommentTarget(oid = oid, type = 12)
        }
        "DYNAMIC_TYPE_MUSIC" -> {
            val oid = basic?.comment_id_str?.toPositiveLongOrNull()
                ?: basic?.rid_str?.toPositiveLongOrNull()
                ?: return null
            DynamicCommentTarget(oid = oid, type = 14)
        }
        "DYNAMIC_TYPE_MEDIALIST" -> {
            val oid = basic?.comment_id_str?.toPositiveLongOrNull()
                ?: basic?.rid_str?.toPositiveLongOrNull()
                ?: return null
            DynamicCommentTarget(oid = oid, type = 19)
        }
        else -> {
            val oid = item.id_str.toPositiveLongOrNull()
                ?: basic?.comment_id_str?.toPositiveLongOrNull()
                ?: return null
            DynamicCommentTarget(oid = oid, type = 17)
        }
    }
}
