package com.github.zly2006.zhihu

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 冷启动回归：未登录（全新安装、无任何账号数据）时，MainActivity 应直接进入首页信息流，
 * 不跳转登录页、不崩溃。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GuestColdStartTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun loggedOutColdStartLandsOnHomeWithoutLogin() {
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithTag("home_feed_list", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.mainClock.advanceTimeBy(3_000)
        compose.waitForIdle()
        assertTrue(
            "首页信息流应可见",
            compose.onAllNodesWithTag("home_feed_list", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty(),
        )
        assertEquals(
            "未登录冷启动不应跳到登录须知页",
            0,
            compose.onAllNodesWithTag("login_notice_step_1", useUnmergedTree = true).fetchSemanticsNodes().size,
        )
        assertTrue("Activity 不应被结束", !compose.activity.isFinishing)
    }
}
