package com.android.purebilibili.feature.settings

import androidx.compose.ui.graphics.vector.ImageVector
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Bookmark
import io.github.alexzhirkevich.cupertino.icons.outlined.ChartBar
import io.github.alexzhirkevich.cupertino.icons.outlined.Cpu
import io.github.alexzhirkevich.cupertino.icons.outlined.Lightbulb
import io.github.alexzhirkevich.cupertino.icons.outlined.PersonCropCircleBadgePlus
import io.github.alexzhirkevich.cupertino.icons.outlined.RectangleStack
import io.github.alexzhirkevich.cupertino.icons.outlined.Star
import kotlin.test.Test
import kotlin.test.assertEquals

class BottomBarSettingsScreenIconPolicyTest {

    @Test
    fun bottomBarIconPolicy_usesSemanticIconsForSecondaryTabs() {
        assertSameVectorAsset(CupertinoIcons.Outlined.RectangleStack, resolveBottomBarTabIcon("DYNAMIC"))
        assertSameVectorAsset(CupertinoIcons.Outlined.Star, resolveBottomBarTabIcon("FAVORITE"))
        assertSameVectorAsset(CupertinoIcons.Outlined.Bookmark, resolveBottomBarTabIcon("WATCHLATER"))
    }

    @Test
    fun topTabIconPolicy_usesSemanticIconsForContentCategories() {
        assertSameVectorAsset(CupertinoIcons.Outlined.PersonCropCircleBadgePlus, resolveTopTabIcon("FOLLOW"))
        assertSameVectorAsset(CupertinoIcons.Outlined.ChartBar, resolveTopTabIcon("POPULAR"))
        assertSameVectorAsset(CupertinoIcons.Outlined.Lightbulb, resolveTopTabIcon("KNOWLEDGE"))
        assertSameVectorAsset(CupertinoIcons.Outlined.Cpu, resolveTopTabIcon("TECH"))
    }

    private fun assertSameVectorAsset(expected: ImageVector, actual: ImageVector) {
        assertEquals(expected.name, actual.name)
        assertEquals(expected.defaultWidth, actual.defaultWidth)
        assertEquals(expected.defaultHeight, actual.defaultHeight)
        assertEquals(expected.viewportWidth, actual.viewportWidth)
        assertEquals(expected.viewportHeight, actual.viewportHeight)
    }
}
