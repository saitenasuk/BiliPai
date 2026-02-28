package com.android.purebilibili.feature.following

import com.android.purebilibili.data.model.response.FollowingUser

internal fun isUserInDefaultFollowGroup(
    userMid: Long,
    userFollowGroupIds: Map<Long, Set<Long>>
): Boolean {
    if (!userFollowGroupIds.containsKey(userMid)) return false
    return userFollowGroupIds[userMid].isNullOrEmpty()
}

internal fun countUsersInDefaultFollowGroup(
    users: List<FollowingUser>,
    userFollowGroupIds: Map<Long, Set<Long>>
): Int {
    return users.count { user -> isUserInDefaultFollowGroup(user.mid, userFollowGroupIds) }
}

internal fun filterUsersBySelectedFollowGroup(
    users: List<FollowingUser>,
    selectedGroupFilter: Long?,
    userFollowGroupIds: Map<Long, Set<Long>>,
    defaultGroupTagId: Long,
    allGroupTagId: Long
): List<FollowingUser> {
    return when (selectedGroupFilter) {
        null, allGroupTagId -> users
        defaultGroupTagId -> users.filter { user ->
            isUserInDefaultFollowGroup(user.mid, userFollowGroupIds)
        }
        else -> users.filter { user ->
            userFollowGroupIds[user.mid]?.contains(selectedGroupFilter) == true
        }
    }
}

internal fun addFollowGroupMappingIfSuccess(
    target: MutableMap<Long, Set<Long>>,
    userMid: Long,
    result: Result<Set<Long>>
): Throwable? {
    return result.fold(
        onSuccess = { groupIds ->
            target[userMid] = groupIds
            null
        },
        onFailure = { error -> error }
    )
}
