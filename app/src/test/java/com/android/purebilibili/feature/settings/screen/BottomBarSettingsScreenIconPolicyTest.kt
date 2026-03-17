package com.android.purebilibili.feature.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.purebilibili.core.theme.UiPreset
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
        assertSameVectorAsset(CupertinoIcons.Outlined.PersonCropCircleBadgePlus, resolveTopTabIcon("FOLLOW", UiPreset.IOS))
        assertSameVectorAsset(CupertinoIcons.Outlined.ChartBar, resolveTopTabIcon("POPULAR", UiPreset.IOS))
        assertSameVectorAsset(CupertinoIcons.Outlined.Lightbulb, resolveTopTabIcon("KNOWLEDGE", UiPreset.IOS))
        assertSameVectorAsset(CupertinoIcons.Outlined.Cpu, resolveTopTabIcon("TECH", UiPreset.IOS))
    }

    @Test
    fun topTabIconPolicy_usesMaterialIconsForMd3Preset() {
        assertSameVectorAsset(Icons.Outlined.Person, resolveTopTabIcon("FOLLOW", UiPreset.MD3))
        assertSameVectorAsset(Icons.AutoMirrored.Outlined.TrendingUp, resolveTopTabIcon("POPULAR", UiPreset.MD3))
        assertSameVectorAsset(Icons.Outlined.Lightbulb, resolveTopTabIcon("KNOWLEDGE", UiPreset.MD3))
        assertSameVectorAsset(Icons.Outlined.SmartToy, resolveTopTabIcon("TECH", UiPreset.MD3))
    }

    private fun assertSameVectorAsset(expected: ImageVector, actual: ImageVector) {
        assertEquals(expected.name, actual.name)
        assertEquals(expected.defaultWidth, actual.defaultWidth)
        assertEquals(expected.defaultHeight, actual.defaultHeight)
        assertEquals(expected.viewportWidth, actual.viewportWidth)
        assertEquals(expected.viewportHeight, actual.viewportHeight)
    }
}
