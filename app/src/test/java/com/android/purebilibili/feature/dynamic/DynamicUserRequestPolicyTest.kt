package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicModules
import com.android.purebilibili.data.model.response.DynamicAuthorModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicUserRequestPolicyTest {

    @Test
    fun `user dynamic result applies only when uid and token still match`() {
        assertTrue(
            shouldApplyUserDynamicsResult(
                selectedUid = 123L,
                requestUid = 123L,
                activeRequestToken = 10L,
                requestToken = 10L
            )
        )
        assertFalse(
            shouldApplyUserDynamicsResult(
                selectedUid = 456L,
                requestUid = 123L,
                activeRequestToken = 10L,
                requestToken = 10L
            )
        )
        assertFalse(
            shouldApplyUserDynamicsResult(
                selectedUid = 123L,
                requestUid = 123L,
                activeRequestToken = 11L,
                requestToken = 10L
            )
        )
    }

    @Test
    fun `selected user reload policy retries same user when scoped state is empty or failed`() {
        assertFalse(
            shouldReloadSelectedUserDynamics(
                previousUid = 123L,
                nextUid = 123L,
                currentItems = listOf(DynamicItem(id_str = "cached")),
                userError = null,
                localMatchCount = 1
            )
        )
        assertTrue(
            shouldReloadSelectedUserDynamics(
                previousUid = 123L,
                nextUid = 123L,
                currentItems = emptyList(),
                userError = null,
                localMatchCount = 0
            )
        )
        assertTrue(
            shouldReloadSelectedUserDynamics(
                previousUid = 123L,
                nextUid = 123L,
                currentItems = listOf(DynamicItem(id_str = "cached")),
                userError = "加载失败",
                localMatchCount = 1
            )
        )
    }

    @Test
    fun `selected user should show local timeline items before remote feed arrives`() {
        val localOnly = listOf(
            buildDynamicItem(id = "local_1", mid = 10001L),
            buildDynamicItem(id = "other", mid = 10002L),
            buildDynamicItem(id = "local_2", mid = 10001L)
        )

        val result = resolveSelectedUserVisibleItems(
            timelineItems = localOnly,
            remoteUserItems = emptyList(),
            selectedUid = 10001L
        )

        assertEquals(listOf("local_1", "local_2"), result.map { it.id_str })
    }

    @Test
    fun `selected user auto load only starts when local timeline has no match`() {
        assertFalse(
            shouldAutoLoadSelectedUserDynamics(
                previousUid = null,
                nextUid = 10001L,
                currentItems = emptyList(),
                userError = null,
                localMatchCount = 2
            )
        )
        assertTrue(
            shouldAutoLoadSelectedUserDynamics(
                previousUid = null,
                nextUid = 10001L,
                currentItems = emptyList(),
                userError = null,
                localMatchCount = 0
            )
        )
    }
}

private fun buildDynamicItem(id: String, mid: Long) = DynamicItem(
    id_str = id,
    modules = DynamicModules(
        module_author = DynamicAuthorModule(
            mid = mid,
            name = "user_$mid"
        )
    )
)
