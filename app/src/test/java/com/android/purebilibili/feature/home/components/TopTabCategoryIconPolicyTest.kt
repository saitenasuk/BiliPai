package com.android.purebilibili.feature.home.components

import androidx.compose.ui.graphics.vector.ImageVector
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Cpu
import io.github.alexzhirkevich.cupertino.icons.outlined.Lightbulb
import io.github.alexzhirkevich.cupertino.icons.outlined.PlayCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.Tv
import kotlin.test.Test
import kotlin.test.assertEquals

class TopTabCategoryIconPolicyTest {

    @Test
    fun topTabCategoryIconPolicy_usesSemanticIosIcons() {
        assertSameVectorAsset(CupertinoIcons.Outlined.PlayCircle, resolveTopTabCategoryIcon("游戏"))
        assertSameVectorAsset(CupertinoIcons.Outlined.Tv, resolveTopTabCategoryIcon("追番"))
        assertSameVectorAsset(CupertinoIcons.Outlined.Lightbulb, resolveTopTabCategoryIcon("知识"))
        assertSameVectorAsset(CupertinoIcons.Outlined.Cpu, resolveTopTabCategoryIcon("科技"))
    }

    private fun assertSameVectorAsset(expected: ImageVector, actual: ImageVector) {
        assertEquals(expected.name, actual.name)
        assertEquals(expected.defaultWidth, actual.defaultWidth)
        assertEquals(expected.defaultHeight, actual.defaultHeight)
        assertEquals(expected.viewportWidth, actual.viewportWidth)
        assertEquals(expected.viewportHeight, actual.viewportHeight)
    }
}
