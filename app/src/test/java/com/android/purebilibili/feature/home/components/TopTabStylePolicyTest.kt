package com.android.purebilibili.feature.home.components

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.UiPreset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopTabStylePolicyTest {

    @Test
    fun `floating plus liquid uses liquid glass`() {
        val state = resolveTopTabStyle(
            isBottomBarFloating = true,
            isBottomBarBlurEnabled = true,
            isLiquidGlassEnabled = true
        )

        assertEquals(true, state.floating)
        assertEquals(TopTabMaterialMode.LIQUID_GLASS, state.materialMode)
    }

    @Test
    fun `floating without liquid but blur enabled uses blur`() {
        val state = resolveTopTabStyle(
            isBottomBarFloating = true,
            isBottomBarBlurEnabled = true,
            isLiquidGlassEnabled = false
        )

        assertEquals(true, state.floating)
        assertEquals(TopTabMaterialMode.BLUR, state.materialMode)
    }

    @Test
    fun `floating without blur and liquid uses plain`() {
        val state = resolveTopTabStyle(
            isBottomBarFloating = true,
            isBottomBarBlurEnabled = false,
            isLiquidGlassEnabled = false
        )

        assertEquals(true, state.floating)
        assertEquals(TopTabMaterialMode.PLAIN, state.materialMode)
    }

    @Test
    fun `docked with blur uses blur`() {
        val state = resolveTopTabStyle(
            isBottomBarFloating = false,
            isBottomBarBlurEnabled = true,
            isLiquidGlassEnabled = false
        )

        assertEquals(false, state.floating)
        assertEquals(TopTabMaterialMode.BLUR, state.materialMode)
    }

    @Test
    fun `docked with liquid downgrades to blur when blur enabled`() {
        val state = resolveTopTabStyle(
            isBottomBarFloating = false,
            isBottomBarBlurEnabled = true,
            isLiquidGlassEnabled = true
        )

        assertEquals(false, state.floating)
        assertEquals(TopTabMaterialMode.BLUR, state.materialMode)
    }

    @Test
    fun `docked without blur uses plain`() {
        val state = resolveTopTabStyle(
            isBottomBarFloating = false,
            isBottomBarBlurEnabled = false,
            isLiquidGlassEnabled = true
        )

        assertEquals(false, state.floating)
        assertEquals(TopTabMaterialMode.PLAIN, state.materialMode)
    }

    @Test
    fun `reduced interaction budget keeps home header tab material mode`() {
        assertEquals(
            TopTabMaterialMode.LIQUID_GLASS,
            resolveEffectiveHomeHeaderTabMaterialMode(
                materialMode = TopTabMaterialMode.LIQUID_GLASS,
                interactionBudget = HomeInteractionMotionBudget.REDUCED
            )
        )
        assertEquals(
            TopTabMaterialMode.BLUR,
            resolveEffectiveHomeHeaderTabMaterialMode(
                materialMode = TopTabMaterialMode.BLUR,
                interactionBudget = HomeInteractionMotionBudget.REDUCED
            )
        )
    }

    @Test
    fun `reduced interaction budget keeps top tab liquid glass enabled`() {
        assertTrue(
            resolveEffectiveTopTabLiquidGlassEnabled(
                isLiquidGlassEnabled = true,
                interactionBudget = HomeInteractionMotionBudget.REDUCED
            )
        )
    }

    @Test
    fun `balanced visual tuning shrinks top indicator footprint`() {
        val tuning = resolveTopTabVisualTuning()

        assertTrue(tuning.nonFloatingIndicatorHeightDp < 34f)
        assertTrue(tuning.nonFloatingIndicatorWidthRatio < 0.78f)
        assertTrue(tuning.floatingIndicatorHeightDp < 52f)
    }

    @Test
    fun `md3 top tabs keep material typography spacing`() {
        val textSize = resolveTopTabLabelTextSizeSp(labelMode = 0)
        val lineHeight = resolveTopTabLabelLineHeightSp(labelMode = 0)

        assertEquals(14f, textSize, 0.001f)
        assertEquals(20f, lineHeight, 0.001f)
        assertTrue(lineHeight >= textSize)
    }

    @Test
    fun `md3 top tabs should use compact text first underline sizing`() {
        val spec = resolveMd3TopTabVisualSpec(isFloatingStyle = false)

        assertEquals(48.dp, spec.rowHeight)
        assertEquals(3.dp, spec.selectedCapsuleHeight)
        assertEquals(2.dp, spec.selectedCapsuleCornerRadius)
        assertEquals(18.dp, spec.iconSize)
        assertEquals(14.sp, spec.labelTextSize)
        assertEquals(20.sp, spec.labelLineHeight)
        assertEquals(0.dp, spec.iconLabelSpacing)
        assertEquals(16.dp, spec.itemHorizontalPadding)
        assertEquals(0.dp, spec.selectedCapsuleShadowElevation)
        assertEquals(0.dp, spec.selectedCapsuleTonalElevation)
    }

    @Test
    fun `md3 selected top tab should reuse material primary emphasis`() {
        val colorScheme = lightColorScheme(
            surface = Color.White,
            primary = Color(0xFF2D6A4F),
            secondaryContainer = Color(0xFFDCEFD8),
            onSecondaryContainer = Color(0xFF1A1C18),
            onSurface = Color(0xFF1B1C1F),
            onSurfaceVariant = Color(0xFF6A5E61)
        )

        assertEquals(colorScheme.primary, resolveMd3TopTabSelectedContainerColor(colorScheme))
        assertEquals(colorScheme.primary, resolveMd3TopTabSelectedIconColor(colorScheme))
        assertEquals(colorScheme.primary, resolveMd3TopTabSelectedLabelColor(colorScheme))
        assertEquals(colorScheme.onSurfaceVariant, resolveMd3TopTabUnselectedIconColor(colorScheme))
        assertEquals(colorScheme.onSurfaceVariant, resolveMd3TopTabUnselectedLabelColor(colorScheme))
    }

    @Test
    fun `md3 preset uses material tab indicator style`() {
        assertEquals(
            TopTabIndicatorStyle.MATERIAL,
            resolveTopTabIndicatorStyle(UiPreset.MD3)
        )
        assertEquals(
            TopTabIndicatorStyle.CAPSULE,
            resolveTopTabIndicatorStyle(UiPreset.IOS)
        )
    }

    @Test
    fun `md3 top tabs use underline row semantics and tighter action shape`() {
        assertEquals(
            "UNDERLINE_FIXED",
            resolveMd3TopTabRowVariant().name
        )
        assertEquals(18.dp, resolveMd3TopTabActionButtonCorner(isFloatingStyle = true))
        assertEquals(16.dp, resolveMd3TopTabActionButtonCorner(isFloatingStyle = false))
    }
}
