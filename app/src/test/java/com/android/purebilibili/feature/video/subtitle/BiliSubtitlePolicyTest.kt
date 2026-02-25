package com.android.purebilibili.feature.video.subtitle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BiliSubtitlePolicyTest {

    @Test
    fun normalizeBilibiliSubtitleUrl_supportsProtocolRelativeUrl() {
        val normalized = normalizeBilibiliSubtitleUrl("//aisubtitle.hdslb.com/bfs/subtitle/abc.json")

        assertEquals(
            "https://aisubtitle.hdslb.com/bfs/subtitle/abc.json",
            normalized
        )
    }

    @Test
    fun parseBiliSubtitleBody_parsesAndSortsSubtitleBody() {
        val rawJson = """
            {
              "body": [
                { "from": 2.0, "to": 3.0, "content": "world" },
                { "from": 0.5, "to": 1.5, "content": "hello" }
              ]
            }
        """.trimIndent()

        val cues = parseBiliSubtitleBody(rawJson)

        assertEquals(2, cues.size)
        assertEquals("hello", cues[0].content)
        assertEquals(500L, cues[0].startMs)
        assertEquals(1500L, cues[0].endMs)
        assertEquals("world", cues[1].content)
        assertEquals(2000L, cues[1].startMs)
    }

    @Test
    fun resolveDefaultSubtitleLanguages_prefersChineseAndEnglish() {
        val tracks = listOf(
            SubtitleTrackMeta(lan = "ja-JP", lanDoc = "日语", subtitleUrl = "https://a"),
            SubtitleTrackMeta(lan = "en-US", lanDoc = "英语", subtitleUrl = "https://b"),
            SubtitleTrackMeta(lan = "zh-Hans", lanDoc = "中文（简体）", subtitleUrl = "https://c")
        )

        val selection = resolveDefaultSubtitleLanguages(tracks)

        assertEquals("zh-Hans", selection.primaryLanguage)
        assertEquals("en-US", selection.secondaryLanguage)
    }

    @Test
    fun resolveSubtitleTextAt_returnsMatchingCue() {
        val cues = listOf(
            SubtitleCue(startMs = 0L, endMs = 900L, content = "A"),
            SubtitleCue(startMs = 1000L, endMs = 1900L, content = "B")
        )

        assertEquals("B", resolveSubtitleTextAt(cues, 1200L))
        assertNull(resolveSubtitleTextAt(cues, 950L))
    }

    @Test
    fun resolveDefaultSubtitleDisplayMode_prefersBilingualWhenBothTracksAvailable() {
        assertEquals(
            SubtitleDisplayMode.BILINGUAL,
            resolveDefaultSubtitleDisplayMode(
                hasPrimaryTrack = true,
                hasSecondaryTrack = true
            )
        )
        assertEquals(
            SubtitleDisplayMode.PRIMARY_ONLY,
            resolveDefaultSubtitleDisplayMode(
                hasPrimaryTrack = true,
                hasSecondaryTrack = false
            )
        )
        assertEquals(
            SubtitleDisplayMode.SECONDARY_ONLY,
            resolveDefaultSubtitleDisplayMode(
                hasPrimaryTrack = false,
                hasSecondaryTrack = true
            )
        )
        assertEquals(
            SubtitleDisplayMode.OFF,
            resolveDefaultSubtitleDisplayMode(
                hasPrimaryTrack = false,
                hasSecondaryTrack = false
            )
        )
    }

    @Test
    fun normalizeSubtitleDisplayMode_fallsBackWhenTargetTrackUnavailable() {
        assertEquals(
            SubtitleDisplayMode.PRIMARY_ONLY,
            normalizeSubtitleDisplayMode(
                preferredMode = SubtitleDisplayMode.BILINGUAL,
                hasPrimaryTrack = true,
                hasSecondaryTrack = false
            )
        )
        assertEquals(
            SubtitleDisplayMode.SECONDARY_ONLY,
            normalizeSubtitleDisplayMode(
                preferredMode = SubtitleDisplayMode.BILINGUAL,
                hasPrimaryTrack = false,
                hasSecondaryTrack = true
            )
        )
        assertEquals(
            SubtitleDisplayMode.OFF,
            normalizeSubtitleDisplayMode(
                preferredMode = SubtitleDisplayMode.PRIMARY_ONLY,
                hasPrimaryTrack = false,
                hasSecondaryTrack = false
            )
        )
    }

    @Test
    fun resolveSubtitleDisplayOptions_returnsOffChineseEnglishAndBilingual() {
        val options = resolveSubtitleDisplayOptions(
            primaryLabel = "中文",
            secondaryLabel = "英文",
            hasPrimaryTrack = true,
            hasSecondaryTrack = true
        )

        assertEquals(4, options.size)
        assertEquals(SubtitleDisplayMode.OFF, options[0].mode)
        assertEquals("关闭", options[0].label)
        assertEquals(SubtitleDisplayMode.PRIMARY_ONLY, options[1].mode)
        assertEquals("中文", options[1].label)
        assertEquals(SubtitleDisplayMode.SECONDARY_ONLY, options[2].mode)
        assertEquals("英文", options[2].label)
        assertEquals(SubtitleDisplayMode.BILINGUAL, options[3].mode)
        assertEquals("双语", options[3].label)
        assertTrue(options.all { it.enabled })
    }
}
