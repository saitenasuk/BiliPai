package com.android.purebilibili.feature.search

enum class SearchVerifyBadge {
    NONE,
    PERSONAL,
    ORGANIZATION
}

fun resolveSearchVerifyBadge(type: Int?, description: String?): SearchVerifyBadge {
    val verifyType = type ?: return SearchVerifyBadge.NONE
    if (verifyType < 0) return SearchVerifyBadge.NONE

    val desc = description.orEmpty().trim()
    if (desc.contains("机构")) return SearchVerifyBadge.ORGANIZATION
    if (desc.contains("个人")) return SearchVerifyBadge.PERSONAL

    return when (verifyType) {
        0, 2, 7 -> SearchVerifyBadge.PERSONAL
        1, 3 -> SearchVerifyBadge.ORGANIZATION
        else -> SearchVerifyBadge.NONE
    }
}
