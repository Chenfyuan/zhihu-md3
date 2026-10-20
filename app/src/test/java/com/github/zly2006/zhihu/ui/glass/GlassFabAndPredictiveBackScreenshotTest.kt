package com.github.zly2006.zhihu.ui.glass

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Navigator
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.HOME_CREATE_FAB_TAG
import com.github.zly2006.zhihu.ui.HOME_REFRESH_BUTTON_TAG
import com.github.zly2006.zhihu.ui.HomeScreen
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.GlassBarTab
import com.github.zly2006.zhihu.ui.components.LocalGlassBarSource
import com.github.zly2006.zhihu.ui.components.LocalGlassBottomBarProvider
import com.github.zly2006.zhihu.ui.predictiveBackLayer
import com.github.zly2006.zhihu.ui.predictiveBackScale
import com.github.zly2006.zhihu.viewmodel.za.MixedHomeFeedViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class GlassFabAndPredictiveBackBase(private val suffix: String) {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val outDir = File(
        System.getProperty("zhihu.shots.dir")
            ?: System.getenv("ZHIHU_SHOTS_DIR")
            ?: "build/zhihu-shots",
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

    private fun sampleItems() = List(8) { i ->
        FeedDisplayItem(
            title = "玻璃 FAB 叠字测试 $i：悬浮按钮应压在这段标题和摘要上",
            summary = "用来检查玻璃模糊与图标可读性的正文。第 $i 段内容足够长，方便看出 FAB 底下的字。",
            details = "${(i + 1) * 10} 赞同 · ${i + 3} 评论",
            feed = null,
            authorName = "作者$i",
            navDestinationJson = null,
        )
    }

    private fun shoot(name: String) {
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        File(outDir, "$name-$suffix.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun glassFabOverText() {
        compose.runOnUiThread {
            ViewModelProvider(compose.activity)[MixedHomeFeedViewModel::class.java].addDisplayItems(sampleItems())
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
        compose.waitForIdle()
        compose.onNodeWithTag(HOME_REFRESH_BUTTON_TAG).assertIsDisplayed()
        compose.onNodeWithTag(HOME_CREATE_FAB_TAG).assertIsDisplayed()
        shoot("glassfab")
    }

    @Test
    fun predictiveBackMidGesture() {
        compose.setContent {
            ZhihuTheme {
                // 外层铺底色，内层带 progress=0.5 的缩放；截图应能看出四周露出底色（缩小到 95%）
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Surface(
                        Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .predictiveBackLayer { 0.5f }
                            .testTag("predictive_back_page"),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "预测返回进度 0.5\n应缩小到 95%",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        assertEquals(0.95f, predictiveBackScale(0.5f), 1e-5f)
        shoot("predictive-back-mid")
    }

    @Test
    fun glassFabStaysClickableAndDraggable() {
        var clicks = 0
        compose.setContent {
            MaterialTheme {
                val p = AndroidGlassBottomBarProvider
                val source = p.rememberSource()
                CompositionLocalProvider(
                    LocalGlassBottomBarProvider provides p,
                    LocalGlassBarSource provides source,
                ) {
                    Box(Modifier.fillMaxSize().padding(8.dp)) {
                        Text("列表正文".repeat(40), modifier = Modifier.testTag("list_body"))
                        DraggableRefreshButton(
                            modifier = Modifier.testTag("drag_fab"),
                            preferenceName = "testGlassFabDrag_$suffix",
                            bottomAvoidance = 80.dp,
                            onClick = { clicks++ },
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        val fab = compose.onNodeWithTag("drag_fab")
        fab.assertIsDisplayed().assertIsEnabled()
        val before = fab.fetchSemanticsNode().boundsInRoot
        assertTrue("命中区应接近 56dp FAB，实际 ${before.width}x${before.height}", before.width >= 50f && before.height >= 50f)
        fab.performClick()
        assertEquals(1, clicks)
        fab.performTouchInput {
            swipe(start = center, end = center + Offset(-120f, -80f), durationMillis = 80)
        }
        compose.waitForIdle()
        val after = compose.onNodeWithTag("drag_fab").fetchSemanticsNode().boundsInRoot
        assertTrue("拖动后位置应改变", after.left != before.left || after.top != before.top)
        compose.onNodeWithTag("list_body").assertIsDisplayed().performClick()
    }
}

@Config(sdk = [35], qualifiers = "w393dp-h852dp-notnight-xxhdpi")
class GlassFabAndPredictiveBackLightTest : GlassFabAndPredictiveBackBase("light")

@Config(sdk = [35], qualifiers = "w393dp-h852dp-night-xxhdpi")
class GlassFabAndPredictiveBackDarkTest : GlassFabAndPredictiveBackBase("dark")
