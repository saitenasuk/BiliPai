package com.android.purebilibili.feature.download

import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.Page
import com.android.purebilibili.data.model.response.Stat
import com.android.purebilibili.data.model.response.UgcEpisode
import com.android.purebilibili.data.model.response.UgcEpisodeArc
import com.android.purebilibili.data.model.response.UgcSeason
import com.android.purebilibili.data.model.response.UgcSection
import com.android.purebilibili.data.model.response.ViewInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BatchDownloadCandidatePolicyTest {

    @Test
    fun resolveBatchDownloadCandidates_buildsPageCandidates_andPreselectsCurrentPage() {
        val info = ViewInfo(
            bvid = "BV1xx",
            cid = 1002L,
            title = "视频标题",
            pic = "cover",
            owner = Owner(name = "UP"),
            stat = Stat(),
            pages = listOf(
                Page(cid = 1001L, page = 1, part = "开头"),
                Page(cid = 1002L, page = 2, part = "中段")
            )
        )

        val candidates = resolveBatchDownloadCandidates(info)

        assertEquals(2, candidates.size)
        assertEquals("BV1xx#1001", candidates[0].id)
        assertEquals("P1 开头", candidates[0].label)
        assertTrue(candidates[1].selected)
    }

    @Test
    fun resolveBatchDownloadCandidates_buildsCollectionCandidates_andDropsDuplicates() {
        val info = ViewInfo(
            bvid = "BV1root",
            cid = 2002L,
            title = "合集主标题",
            pic = "cover",
            owner = Owner(name = "UP"),
            stat = Stat(),
            ugc_season = UgcSeason(
                id = 1L,
                title = "合集",
                sections = listOf(
                    UgcSection(
                        id = 11L,
                        title = "正片",
                        episodes = listOf(
                            UgcEpisode(
                                bvid = "BVep1",
                                cid = 2001L,
                                title = "EP1",
                                arc = UgcEpisodeArc(title = "第一集", pic = "ep1")
                            ),
                            UgcEpisode(
                                bvid = "BVep2",
                                cid = 2002L,
                                title = "EP2",
                                arc = UgcEpisodeArc(title = "第二集", pic = "ep2")
                            ),
                            UgcEpisode(
                                bvid = "BVep2",
                                cid = 2002L,
                                title = "重复",
                                arc = UgcEpisodeArc(title = "重复", pic = "dup")
                            )
                        )
                    )
                )
            )
        )

        val candidates = resolveBatchDownloadCandidates(info)

        assertEquals(2, candidates.size)
        assertEquals("BVep1#2001", candidates[0].id)
        assertEquals("第一集", candidates[0].label)
        assertTrue(candidates[1].selected)
    }
}
