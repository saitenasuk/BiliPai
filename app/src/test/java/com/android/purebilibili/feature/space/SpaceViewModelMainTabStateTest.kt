package com.android.purebilibili.feature.space

import androidx.lifecycle.SavedStateHandle
import kotlin.test.Test
import kotlin.test.assertEquals

class SpaceViewModelMainTabStateTest {

    @Test
    fun `selectMainTab updates selectedMainTab state`() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = SpaceViewModel(savedStateHandle)
        assertEquals(2, viewModel.selectedMainTab.value)

        viewModel.selectMainTab(3)
        assertEquals(3, viewModel.selectedMainTab.value)
        assertEquals(3, savedStateHandle.get<Int>("space_selected_main_tab"))
    }

    @Test
    fun `selectedMainTab restores from saved state`() {
        val savedStateHandle = SavedStateHandle(mapOf("space_selected_main_tab" to 3))
        val viewModel = SpaceViewModel(savedStateHandle)

        assertEquals(3, viewModel.selectedMainTab.value)
    }
}
