package com.github.zly2006.zhihu.ui.glass

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Navigator
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.HOME_FEED_LIST_TAG
import com.github.zly2006.zhihu.ui.HOME_NOTIFICATION_BADGE_TAG
import com.github.zly2006.zhihu.ui.HOME_NOTIFICATION_BUTTON_TAG
import com.github.zly2006.zhihu.ui.HOME_SEARCH_BUTTON_TAG
import com.github.zly2006.zhihu.ui.HOME_TOP_ACTIONS_TAG
import com.github.zly2006.zhihu.ui.HomeScreen
import com.github.zly2006.zhihu.ui.components.GlassBarTab
import com.github.zly2006.zhihu.ui.components.GlassHomeTopBar
import com.github.zly2006.zhihu.ui.components.LocalGlassBarSource
import com.github.zly2006.zhihu.ui.components.LocalGlassBottomBarProvider
import com.github.zly2006.zhihu.viewmodel.za.MixedHomeFeedViewModel
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * 首页顶部玻璃：搜索胶囊 + 独立通知圆钮，列表从后面和缝隙透出。
 * (1) 列表在顶部时首条不被遮挡；(2) 滚动后条目经过玻璃下方；(3) 带数字的红色角标可读。
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class GlassHomeTopBarBase(private val suffix: String) {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val outDir = File(
        System.getProperty("zhihu.shots.dir") ?: System.getenv("ZHIHU_SHOTS_DIR") ?: "build/zhihu-shots",
    )

    private val tabs = listOf(
        GlassBarTab("主页", Icons.Default.Home, Icons.Default.Home, "nav_tab_home"),
        GlassBarTab("账号", Icons.Default.Person, Icons.Default.Person, "nav_tab_account"),
    )

    @Before
    fun offline() {
        com.github.zly2006.zhihu.account.replaceAndroidZhihuAccountStoreForTesting(null)
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        com.github.zly2006.zhihu.data.AccountData.loadData(ctx)
        com.github.zly2006.zhihu.theme.AndroidThemeSettings.initialize(ctx)
        com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase(ctx)
        com.github.zly2006.zhihu.account.androidZhihuAccountStore(ctx)
        com.github.zly2006.zhihu.updater.UpdateManager.updateState.value =
            com.github.zly2006.zhihu.updater.UpdateManager.UpdateState.NoUpdate
        outDir.mkdirs()
    }

    private fun items() = List(60) { i ->
        FeedDisplayItem(
            title = "顶部玻璃叠字测试 $i：列表从搜索胶囊和通知圆钮后面滚过",
            summary = "第 $i 条摘要。用来检查玻璃模糊下文字仍可读，以及首条在顶部时不被挡住。",
            details = "${(i + 1) * 10} 赞同 · ${i + 3} 评论",
            feed = null,
            authorName = "作者$i",
            navDestinationJson = null,
        )
    }

    private fun shoot(name: String) {
        compose.mainClock.advanceTimeBy(1_500)
        compose.waitForIdle()
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        File(outDir, "topglass-$name-$suffix.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun setHome() {
        compose.runOnUiThread {
            ViewModelProvider(compose.activity)[MixedHomeFeedViewModel::class.java].addDisplayItems(items())
        }
        compose.setContent {
            ZhihuTheme {
                val p = AndroidGlassBottomBarProvider
                val source = p.rememberSource()
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides compose.activity,
                    LocalNavigator provides Navigator({}, {}, {}),
                    LocalGlassBottomBarProvider provides p,
                    LocalGlassBarSource provides source,
                ) {
                    Scaffold(bottomBar = { p.Bar(source, tabs, 0, {}, Modifier) }) { inner ->
                        HomeScreen(scrollToTopTrigger = 0, innerPadding = inner)
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(1_500)
        compose.waitForIdle()
    }

    private fun bounds(tag: String) =
        compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    @Test
    fun listAtTopFirstItemNotCovered() {
        setHome()
        compose.onNodeWithTag(HOME_SEARCH_BUTTON_TAG, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag(HOME_NOTIFICATION_BUTTON_TAG, useUnmergedTree = true).assertIsDisplayed()
        val search = bounds(HOME_SEARCH_BUTTON_TAG)
        val bell = bounds(HOME_NOTIFICATION_BUTTON_TAG)
        assertTrue("搜索和通知是两个分开的按钮，中间留缝", bell.left > search.right)
        val first = compose.onAllNodesWithText("顶部玻璃叠字测试 0", substring = true, useUnmergedTree = true)
            .fetchSemanticsNodes().first().boundsInRoot
        val topBar = bounds(HOME_TOP_ACTIONS_TAG)
        assertTrue("列表在顶部时首条 (${first.top}) 应在玻璃顶栏 (${topBar.bottom}) 之下", first.top >= topBar.bottom)
        shoot("list-top")
    }

    @Test
    fun listScrolledPassesBehindGlass() {
        setHome()
        repeat(2) {
            compose.onNodeWithTag(HOME_FEED_LIST_TAG).performTouchInput { swipeUp(durationMillis = 600) }
            compose.mainClock.advanceTimeBy(800)
        }
        compose.waitForIdle()
        val topBar = bounds(HOME_TOP_ACTIONS_TAG)
        // 有条目文字跨过顶栏区域 => 内容在玻璃后面滚动
        val behind = compose.onAllNodesWithText("顶部玻璃叠字测试", substring = true, useUnmergedTree = true)
            .fetchSemanticsNodes().any { it.boundsInRoot.top < topBar.bottom && it.boundsInRoot.bottom > topBar.top }
        assertTrue("滚动后应有条目经过玻璃顶栏后面", behind)
        // 离线时滚到列表尾会触发 loadMore，刷新 FAB 变成转圈、底部出现加载 footer；60 条只滚两屏不会到尾部
        assertTrue("截图时不应处于加载中（无转圈）",
            compose.onAllNodesWithContentDescription("刷新", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        shoot("list-scrolled")
    }

    @Test
    fun badgeWithNumberIsLegible() {
        compose.setContent {
            ZhihuTheme {
                val p = AndroidGlassBottomBarProvider
                val source = p.rememberSource()
                Scaffold(
                    topBar = {
                        GlassHomeTopBar(p, source, topInset = 0.dp, searchHint = "搜索内容", onSearch = {}, unreadCount = 12, onNotification = {})
                    },
                ) { inner ->
                    LazyColumn(
                        modifier = p.contentSource(Modifier.fillMaxSize(), source).background(MaterialTheme.colorScheme.background),
                        contentPadding = PaddingValues(top = 0.dp, bottom = inner.calculateBottomPadding()),
                    ) {
                        items(items()) { item ->
                            Box(Modifier.fillMaxWidth().padding(16.dp, 8.dp)) {
                                Text(item.title + "\n" + item.summary, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag(HOME_NOTIFICATION_BADGE_TAG, useUnmergedTree = true).assertIsDisplayed()
        compose.onAllNodesWithText("12", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty().let { assertTrue("角标显示数字 12", it) }
        val badge = bounds(HOME_NOTIFICATION_BADGE_TAG)
        val bell = bounds(HOME_NOTIFICATION_BUTTON_TAG)
        assertTrue("角标在通知圆钮右上，没有被玻璃裁掉", badge.right >= bell.right - 2f && badge.top <= bell.top + 2f)
        shoot("badge")
    }
}

@Config(sdk = [35], qualifiers = "w393dp-h852dp-notnight-xxhdpi")
class GlassHomeTopBarLightTest : GlassHomeTopBarBase("light")

@Config(sdk = [35], qualifiers = "w393dp-h852dp-night-xxhdpi")
class GlassHomeTopBarDarkTest : GlassHomeTopBarBase("dark")
