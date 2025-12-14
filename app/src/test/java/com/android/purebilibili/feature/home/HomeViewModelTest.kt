package com.android.purebilibili.feature.home

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * HomeViewModel 单元测试
 * 
 * 测试覆盖:
 * - 数据类验证
 * - 分类枚举验证
 */
class HomeViewModelTest {
    
    @Test
    @DisplayName("UserState 应正确存储用户信息")
    fun userStateShouldCorrectlyStoreUserInfo() {
        // Given: 创建用户状态
        val userState = UserState(
            isLogin = true,
            name = "测试用户",
            mid = 12345L,
            level = 6,
            isVip = true
        )
        
        // Then: 所有字段应正确
        assertTrue(userState.isLogin)
        assertEquals("测试用户", userState.name)
        assertEquals(12345L, userState.mid)
        assertEquals(6, userState.level)
        assertTrue(userState.isVip)
    }
    
    @Test
    @DisplayName("HomeCategory 枚举应包含正确的标签")
    fun homeCategoryShouldHaveCorrectLabels() {
        assertEquals("推荐", HomeCategory.RECOMMEND.label)
        assertEquals("热门", HomeCategory.POPULAR.label)
        assertEquals("直播", HomeCategory.LIVE.label)
    }
    
    @Test
    @DisplayName("HomeUiState 默认值应正确")
    fun homeUiStateDefaultsShouldBeCorrect() {
        val state = HomeUiState()
        
        assertTrue(state.videos.isEmpty())
        assertFalse(state.isLoading)
        assertEquals(null, state.error)
        assertEquals(HomeCategory.RECOMMEND, state.currentCategory)
    }
}
