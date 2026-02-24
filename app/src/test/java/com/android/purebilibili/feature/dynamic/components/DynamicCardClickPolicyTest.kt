package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.data.model.response.ArchiveMajor
import com.android.purebilibili.data.model.response.DynamicContentModule
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicMajor
import com.android.purebilibili.data.model.response.DynamicModules
import com.android.purebilibili.data.model.response.UgcSeasonMajor
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

    @Test
    fun resolveDynamicCardClickAction_usesArchiveJumpUrlWhenBvidMissing() {
        val item = DynamicItem(
            id_str = "123",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        archive = ArchiveMajor(
                            bvid = "",
                            jump_url = "//www.bilibili.com/video/BV1d4421Z7nW/"
                        )
                    )
                )
            )
        )

        val action = resolveDynamicCardClickAction(item)

        assertTrue(action is DynamicCardClickAction.OpenVideo)
        assertEquals("BV1d4421Z7nW", (action as DynamicCardClickAction.OpenVideo).bvid)
    }

    @Test
    fun resolveDynamicCardClickAction_usesUgcSeasonJumpUrlWhenArchiveMissing() {
        val item = DynamicItem(
            id_str = "123",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        ugc_season = UgcSeasonMajor(
                            title = "合集标题",
                            jump_url = "//www.bilibili.com/video/BV1oeWNebEv2/"
                        )
                    )
                )
            )
        )

        val action = resolveDynamicCardClickAction(item)

        assertTrue(action is DynamicCardClickAction.OpenVideo)
        assertEquals("BV1oeWNebEv2", (action as DynamicCardClickAction.OpenVideo).bvid)
    }

    @Test
    fun resolveDynamicCardClickAction_usesUgcSeasonAidWhenJumpUrlMissing() {
        val item = DynamicItem(
            id_str = "123",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        ugc_season = UgcSeasonMajor(
                            title = "合集标题",
                            aid = 1129813966L
                        )
                    )
                )
            )
        )

        val action = resolveDynamicCardClickAction(item)

        assertTrue(action is DynamicCardClickAction.OpenVideo)
        assertEquals("av1129813966", (action as DynamicCardClickAction.OpenVideo).bvid)
    }
}
