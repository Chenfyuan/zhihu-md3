/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.ui

import android.graphics.Bitmap
import androidx.activity.BackEventCompat
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.viewmodel.sharedArticleAnswerSwitchState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * 阅读页（回答/文章/想法）的返回手势与转场。
 *
 * 手机上这些页面放在详情栈里、盖在列表之上。以前列表和详情互斥组合，打开和返回都是硬切，手势缩小时背后也没有列表；
 * 沉浸式模式下文章页自己的普通返回拦截还会把手势吞掉。这里用真实 MainActivity 驱动系统返回手势验证。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReadingPagePredictiveBackTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val outDir = File(
        System.getProperty("zhihu.shots.dir") ?: System.getenv("ZHIHU_SHOTS_DIR") ?: "build/zhihu-shots",
    )

    @After
    fun resetImmersive() {
        compose.runOnIdle { sharedArticleAnswerSwitchState.reset() }
    }

    private fun count(tag: String) =
        compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false).size

    private fun detailWidth(): Float =
        compose.onAllNodesWithTag("detail_pane", useUnmergedTree = true).fetchSemanticsNodes().first().boundsInRoot.width

    private fun settle(ms: Long = 1_500) {
        compose.mainClock.advanceTimeBy(ms)
        compose.waitForIdle()
    }

    private fun openReadingPage(destination: NavDestination) {
        compose.waitUntil(timeoutMillis = 20_000) { count("list_pane") > 0 }
        compose.runOnUiThread { compose.activity.navigate(destination) }
        compose.waitUntil(timeoutMillis = 10_000) { count(DETAIL_OVERLAY_TAG) > 0 }
        settle()
    }

    private fun startGesture(progress: Float) {
        val dispatcher = compose.activity.onBackPressedDispatcher
        compose.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 800f, 0f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(200f, 800f, progress, BackEventCompat.EDGE_LEFT))
        }
        settle(100)
    }

    private fun shoot(name: String) {
        outDir.mkdirs()
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        File(outDir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun assertGestureShrinksAndCloses(shotName: String) {
        assertTrue("阅读页打开时列表应留在下层", count("list_pane") > 0)
        val fullWidth = detailWidth()
        startGesture(0.5f)
        val midWidth = detailWidth()
        assertEquals("手势进度 0.5 时页面应缩小到 95%", fullWidth * predictiveBackScale(0.5f), midWidth, fullWidth * 0.01f)
        shoot(shotName)
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        settle()
        assertEquals("松手后阅读页应关闭", 0, count(DETAIL_OVERLAY_TAG))
        assertTrue("返回后列表可见", count("list_pane") > 0)
    }

    @Test
    fun pinPageFollowsBackGestureAndKeepsListBehind() {
        openReadingPage(Pin(id = 1L))
        assertGestureShrinksAndCloses("reading-back-mid-pin")
    }

    @Test
    fun answerPageFollowsBackGesture() {
        openReadingPage(Article(type = ArticleType.Answer, id = 1L, title = "离线回答"))
        assertGestureShrinksAndCloses("reading-back-mid-answer")
    }

    @Test
    fun immersiveAnswerStillFollowsBackGesture() {
        compose.runOnIdle { sharedArticleAnswerSwitchState.isImmersiveMode = true }
        openReadingPage(Article(type = ArticleType.Answer, id = 2L, title = "沉浸式回答"))
        assertGestureShrinksAndCloses("reading-back-mid-immersive")
        assertTrue("离开页面后应退出沉浸式", !sharedArticleAnswerSwitchState.isImmersiveMode)
    }

    @Test
    fun cancelledGestureRestoresPage() {
        openReadingPage(Pin(id = 3L))
        val fullWidth = detailWidth()
        startGesture(0.6f)
        assertTrue(detailWidth() < fullWidth)
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
        settle()
        assertEquals(fullWidth, detailWidth(), 1f)
        assertTrue("取消后阅读页仍在", count(DETAIL_OVERLAY_TAG) > 0)
    }
}
