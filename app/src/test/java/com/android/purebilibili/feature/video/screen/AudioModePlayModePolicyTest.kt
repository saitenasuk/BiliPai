package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.feature.video.player.PlayMode
import kotlin.test.Test
import kotlin.test.assertEquals

class AudioModePlayModePolicyTest {

    @Test
    fun resolveAudioPlayModeLabel_mapsAllModes() {
        assertEquals("顺序播放", resolveAudioPlayModeLabel(PlayMode.SEQUENTIAL))
        assertEquals("随机播放", resolveAudioPlayModeLabel(PlayMode.SHUFFLE))
        assertEquals("单曲循环", resolveAudioPlayModeLabel(PlayMode.REPEAT_ONE))
    }
}
