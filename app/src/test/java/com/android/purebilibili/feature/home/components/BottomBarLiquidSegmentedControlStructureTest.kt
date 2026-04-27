package com.android.purebilibili.feature.home.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomBarLiquidSegmentedControlStructureTest {

    @Test
    fun `segmented control follows bottom bar liquid indicator structure`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarLiquidSegmentedControl.kt"
        )

        assertTrue(source.contains("BottomBarMotionProfile.ANDROID_NATIVE_FLOATING"))
        assertTrue(source.contains("resolveBottomBarRefractionMotionProfile("))
        assertTrue(source.contains("rememberCombinedBackdrop(shellBackdrop, contentBackdrop)"))
        assertTrue(source.contains(".layerBackdrop(contentBackdrop)"))
        assertTrue(source.contains(".offset(x = segmentWidth * dragState.value)"))
        assertTrue(source.contains("val indicatorScale = androidx.compose.ui.util.lerp(1f, 78f / 56f, motionProgress)"))
        assertTrue(source.contains("val velocity = dragState.velocity / 10f"))
        assertTrue(source.contains("scaleX = indicatorScale /"))
        assertTrue(source.contains("scaleY = indicatorScale *"))
        assertTrue(source.contains("chromaticAberration = true"))
        assertTrue(source.contains("Shadow(alpha = motionProgress)"))
        assertTrue(source.contains("InnerShadow("))
        assertTrue(source.contains("resolveBottomBarItemMotionVisual("))
        assertFalse(source.contains("LiquidIndicator("))
        assertFalse(source.contains("resolveBottomBarIndicatorPolicy(itemCount = itemCount)"))
        assertFalse(source.contains("indicatorWidthMultiplier = 0.92f"))
        assertFalse(source.contains("maxScale = 1.06f"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
