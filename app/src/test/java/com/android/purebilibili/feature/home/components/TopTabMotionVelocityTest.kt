package com.android.purebilibili.feature.home.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TopTabMotionVelocityTest {

    @Test
    fun `horizontal only when liquid glass disabled`() {
        val velocity = resolveTopTabIndicatorVelocity(
            horizontalVelocityPxPerSecond = 1200f,
            verticalVelocityPxPerSecond = 900f,
            enableVerticalLiquidMotion = false
        )

        assertEquals(1200f, velocity, 0.001f)
    }

    @Test
    fun `vertical contributes when liquid glass enabled`() {
        val velocity = resolveTopTabIndicatorVelocity(
            horizontalVelocityPxPerSecond = 1000f,
            verticalVelocityPxPerSecond = 800f,
            enableVerticalLiquidMotion = true
        )

        assertEquals(1800f, velocity, 0.001f)
    }

    @Test
    fun `result is clamped to avoid excessive distortion`() {
        val velocity = resolveTopTabIndicatorVelocity(
            horizontalVelocityPxPerSecond = 5000f,
            verticalVelocityPxPerSecond = 5000f,
            enableVerticalLiquidMotion = true
        )

        assertEquals(4200f, velocity, 0.001f)
    }

    @Test
    fun `vertical motion marks interacting when liquid glass enabled`() {
        val interacting = shouldTopTabIndicatorBeInteracting(
            pagerIsScrolling = false,
            combinedVelocityPxPerSecond = 10f,
            verticalVelocityPxPerSecond = 30f,
            liquidGlassEnabled = true
        )

        assertEquals(true, interacting)
    }

    @Test
    fun `vertical motion ignored when liquid glass disabled`() {
        val interacting = shouldTopTabIndicatorBeInteracting(
            pagerIsScrolling = false,
            combinedVelocityPxPerSecond = 10f,
            verticalVelocityPxPerSecond = 30f,
            liquidGlassEnabled = false
        )

        assertEquals(false, interacting)
    }
}
