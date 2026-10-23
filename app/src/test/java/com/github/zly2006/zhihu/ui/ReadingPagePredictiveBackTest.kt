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
import com.github.zly2006.zhihu.navigation.Question
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
 * 手机上这些页面放在详情栈里、盖在列表之上。以前列表和详情互斥组合，打开和返回都是硬切，手势时背后也没有列表；
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

    private fun bounds(tag: String) =
        compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().first().boundsInRoot

    private fun detailWidth(): Float = bounds("detail_pane").width

    private fun detailLeft(): Float = bounds("detail_pane").left

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

    private fun assertGestureSlidesAndCloses(shotName: String) {
        assertTrue("阅读页打开时列表应留在下层", count("list_pane") > 0)
        val fullWidth = detailWidth()
        assertEquals("打开动画结束后详情应贴齐左边", 0f, detailLeft(), 1f)
        assertEquals("详情展示时列表停在 -0.3 宽度的视差位置", -0.3f * fullWidth, bounds("list_pane").left, 2f)
        startGesture(0.5f)
        assertEquals("手势进度 0.5 时详情不缩放", fullWidth, detailWidth(), 1f)
        assertEquals("手势进度 0.5 时详情右移半屏", fullWidth * 0.5f, detailLeft(), 2f)
        assertEquals("列表视差跟到 -0.15 宽度", -0.15f * fullWidth, bounds("list_pane").left, 2f)
        shoot(shotName)
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.mainClock.advanceTimeBy(64)
        compose.waitForIdle()
        if (count(DETAIL_OVERLAY_TAG) > 0) {
            assertTrue("松手后从当前位置继续滑出，不能跳回", detailLeft() >= fullWidth * 0.5f - 1f)
        }
        settle()
        assertEquals("松手后阅读页应关闭", 0, count(DETAIL_OVERLAY_TAG))
        assertEquals("返回后列表回到原位", 0f, bounds("list_pane").left, 1f)
    }

    @Test
    fun pinPageFollowsBackGestureAndKeepsListBehind() {
        openReadingPage(Pin(id = 1L))
        assertGestureSlidesAndCloses("slideback-mid-pin")
    }

    @Test
    fun answerPageFollowsBackGesture() {
        openReadingPage(Article(type = ArticleType.Answer, id = 1L, title = "离线回答"))
        assertGestureSlidesAndCloses("slideback-mid-answer")
    }

    @Test
    fun immersiveAnswerStillFollowsBackGesture() {
        compose.runOnIdle { sharedArticleAnswerSwitchState.isImmersiveMode = true }
        openReadingPage(Article(type = ArticleType.Answer, id = 2L, title = "沉浸式回答"))
        assertGestureSlidesAndCloses("slideback-mid-immersive")
        assertTrue("离开页面后应退出沉浸式", !sharedArticleAnswerSwitchState.isImmersiveMode)
    }

    @Test
    fun articlePageFollowsBackGesture() {
        openReadingPage(Article(type = ArticleType.Article, id = 4L, title = "离线文章"))
        assertGestureSlidesAndCloses("slideback-mid-article")
    }

    @Test
    fun questionPageInMainStackSlidesWithGesture() {
        compose.waitUntil(timeoutMillis = 20_000) { count("list_pane") > 0 }
        compose.runOnUiThread { compose.activity.navigate(Question(questionId = 5L, title = "离线问题")) }
        compose.waitUntil(timeoutMillis = 10_000) { count(QUESTION_SCREEN_LIST_TAG) > 0 }
        settle()
        val width = bounds("list_pane").width
        assertEquals(0f, bounds(QUESTION_SCREEN_LIST_TAG).left, 2f)
        startGesture(0.5f)
        val left = bounds(QUESTION_SCREEN_LIST_TAG).left
        assertTrue("问题页应随手势右移而不是缩小，实际 left=$left", left > width * 0.4f && left < width * 0.6f)
        assertEquals(
            "不缩放（屏外部分被裁掉，看可见宽度 = 屏宽 - 左移量）",
            width - left,
            bounds(QUESTION_SCREEN_LIST_TAG).width,
            2f,
        )
        shoot("slideback-mid-question")
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        settle()
        assertEquals("松手后问题页关闭", 0, count(QUESTION_SCREEN_LIST_TAG))
    }

    @Test
    fun cancelledGestureRestoresPage() {
        openReadingPage(Pin(id = 3L))
        startGesture(0.6f)
        assertTrue("取消前详情已右移", detailLeft() > 100f)
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
        settle()
        assertEquals("取消后回弹到原位", 0f, detailLeft(), 1f)
        assertTrue("取消后阅读页仍在", count(DETAIL_OVERLAY_TAG) > 0)
    }
}
