package com.github.zly2006.zhihu.ui.glass

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Navigator
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.MainTabsPagerForTest
import com.github.zly2006.zhihu.ui.components.GlassBarTab
import com.github.zly2006.zhihu.ui.components.LocalGlassBarSource
import com.github.zly2006.zhihu.ui.components.LocalGlassBottomBarProvider
import com.github.zly2006.zhihu.ui.mainTabPageTag
import com.github.zly2006.zhihu.ui.MAIN_TABS_GLASS_SOURCE_TAG
import org.junit.Assert.assertTrue
import com.github.zly2006.zhihu.viewmodel.feed.FollowRecommendViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HotListViewModel
import com.github.zly2006.zhihu.viewmodel.za.MixedHomeFeedViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * 主壳四个 tab（主页/关注/热榜/账号）都要成为底栏玻璃的模糊源，而且同一时刻只有当前页在录制。
 * 依次切页截图（离线无法录视频，用截图序列代替）。
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class GlassTabsBase(private val suffix: String) {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val outDir = File(System.getProperty("zhihu.shots.dir") ?: System.getenv("ZHIHU_SHOTS_DIR") ?: "build/zhihu-shots")
    private val keys = listOf("home", "follow", "hotlist", "account")
    private val tabs = listOf(
        GlassBarTab("主页", Icons.Default.Home, Icons.Default.Home, "nav_tab_home"),
        GlassBarTab("关注", Icons.Default.Group, Icons.Default.Group, "nav_tab_follow"),
        GlassBarTab("热榜", Icons.Default.Whatshot, Icons.Default.Whatshot, "nav_tab_hot"),
        GlassBarTab("账号", Icons.Default.Person, Icons.Default.Person, "nav_tab_account"),
    )
    private lateinit var pager: PagerState

    @Before
    fun offline() {
        com.github.zly2006.zhihu.account.replaceAndroidZhihuAccountStoreForTesting(null)
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        com.github.zly2006.zhihu.data.AccountData.loadData(ctx)
        com.github.zly2006.zhihu.screenshot.applyReleaseLookIfRequested(ctx)
        com.github.zly2006.zhihu.theme.AndroidThemeSettings.initialize(ctx)
        com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase(ctx)
        com.github.zly2006.zhihu.account.androidZhihuAccountStore(ctx)
        com.github.zly2006.zhihu.updater.UpdateManager.updateState.value =
            com.github.zly2006.zhihu.updater.UpdateManager.UpdateState.NoUpdate
        outDir.mkdirs()
    }

    private fun items(label: String) = List(60) { i ->
        FeedDisplayItem(
            title = "$label 玻璃底栏测试 $i：内容应从悬浮底栏后面滚过",
            summary = "第 $i 条摘要，用来看底栏模糊是否采样到当前页。",
            details = "${(i + 1) * 10} 赞同",
            feed = null,
            authorName = "作者$i",
            navDestinationJson = null,
        )
    }

    private fun setTabs() {
        compose.runOnUiThread {
            val vp = ViewModelProvider(compose.activity)
            vp[MixedHomeFeedViewModel::class.java].addDisplayItems(items("主页"))
            vp[FollowRecommendViewModel::class.java].addDisplayItems(items("关注"))
            vp[HotListViewModel::class.java].addDisplayItems(items("热榜"))
        }
        compose.setContent {
            ZhihuTheme {
                val p = AndroidGlassBottomBarProvider
                val source = p.rememberSource()
                pager = rememberPagerState { keys.size }
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides compose.activity,
                    LocalNavigator provides Navigator({}, {}, {}),
                    LocalGlassBottomBarProvider provides p,
                    LocalGlassBarSource provides source,
                ) {
                    Scaffold(bottomBar = { p.Bar(source, tabs, pager.currentPage, {}, Modifier) }) { inner ->
                        MainTabsPagerForTest(pager, keys, inner)
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(1_500)
        compose.waitForIdle()
    }

    private fun go(index: Int) {
        compose.runOnUiThread { runBlocking { pager.scrollToPage(index) } }
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
    }

    @Test
    fun everyTabIsInsideTheSingleGlassSource() {
        setTabs()
        keys.indices.forEach { i ->
            go(i)
            val sources = compose.onAllNodesWithTag(MAIN_TABS_GLASS_SOURCE_TAG, useUnmergedTree = true).fetchSemanticsNodes()
            assertEquals("底栏玻璃源只能有一个录制节点（第 $i 个 tab）", 1, sources.size)
            val src = sources.single().boundsInRoot
            val page = compose.onAllNodesWithTag(mainTabPageTag(i), useUnmergedTree = true).fetchSemanticsNodes().single().boundsInRoot
            assertTrue("第 $i 个 tab (${keys[i]}) 当前时整页都在玻璃源录制范围内: page=$page src=$src",
                page.left >= src.left - 1 && page.right <= src.right + 1 && page.top >= src.top - 1 && page.bottom <= src.bottom + 1)
            assertTrue("页面要延伸到底栏下方（内容可从底栏后滚过）", page.bottom >= src.bottom - 1)
        }
    }

    @Test
    fun tabsScrolledSequence() {
        setTabs()
        keys.indices.forEach { i ->
            go(i)
            runCatching {
                repeat(2) {
                    compose.onRoot().performTouchInput { swipeUp(startY = bottom * 0.8f, endY = bottom * 0.3f, durationMillis = 500) }
                    compose.mainClock.advanceTimeBy(700)
                }
            }
            compose.mainClock.advanceTimeBy(1_000)
            compose.waitForIdle()
            val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
            File(outDir, "tabglass-${i + 1}-${keys[i]}-$suffix.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}

@Config(sdk = [35], qualifiers = "w393dp-h852dp-notnight-xxhdpi")
class GlassTabsLightTest : GlassTabsBase("light")

@Config(sdk = [35], qualifiers = "w393dp-h852dp-night-xxhdpi")
class GlassTabsDarkTest : GlassTabsBase("dark")
