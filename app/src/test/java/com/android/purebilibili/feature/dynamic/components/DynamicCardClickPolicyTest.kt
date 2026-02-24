package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.data.model.response.ArchiveMajor
import com.android.purebilibili.data.model.response.DynamicContentModule
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicMajor
import com.android.purebilibili.data.model.response.DynamicModules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DynamicCardClickPolicyTest {

    @Test
    fun resolveDynamicCardClickAction_prefersVideoWhenArchiveBvidExists() {
        val item = DynamicItem(
            id_str = "123",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        archive = ArchiveMajor(bvid = "BV1xx411c7mD")
                    )
                )
            )
        )

        val action = resolveDynamicCardClickAction(item)

        assertTrue(action is DynamicCardClickAction.OpenVideo)
        assertEquals("BV1xx411c7mD", (action as DynamicCardClickAction.OpenVideo).bvid)
    }

    @Test
    fun resolveDynamicCardClickAction_opensDynamicDetailWhenNoVideo() {
        val item = DynamicItem(id_str = "  987654321  ")

        val action = resolveDynamicCardClickAction(item)

        assertTrue(action is DynamicCardClickAction.OpenDynamicDetail)
        assertEquals("987654321", (action as DynamicCardClickAction.OpenDynamicDetail).dynamicId)
    }

    @Test
    fun resolveDynamicCardClickAction_returnsNoneWhenNoVideoAndNoId() {
        val item = DynamicItem(id_str = "  ")

        val action = resolveDynamicCardClickAction(item)

        assertTrue(action is DynamicCardClickAction.None)
    }
}
