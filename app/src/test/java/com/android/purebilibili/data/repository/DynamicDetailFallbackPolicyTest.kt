package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.DynamicAuthorModule
import com.android.purebilibili.data.model.response.DynamicContentModule
import com.android.purebilibili.data.model.response.DynamicDesc
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicModules
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class DynamicDetailFallbackPolicyTest {

    @Test
    fun shouldFallback_returnsTrue_whenAuthorAndContentMissing() {
        val item = DynamicItem(modules = DynamicModules())
        assertTrue(shouldFallbackForDynamicDetail(item))
    }

    @Test
    fun shouldFallback_returnsFalse_whenDescTextExists() {
        val item = DynamicItem(
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    desc = DynamicDesc(text = "text")
                )
            )
        )
        assertFalse(shouldFallbackForDynamicDetail(item))
    }

    @Test
    fun shouldFallback_returnsTrue_whenOnlyAuthorExists() {
        val item = DynamicItem(
            modules = DynamicModules(
                module_author = DynamicAuthorModule(mid = 1, name = "author")
            )
        )
        assertTrue(shouldFallbackForDynamicDetail(item))
    }
}
