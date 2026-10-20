package com.github.zly2006.zhihu.screenshot

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.captureToImage
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.account.accountHttpClientEngineForTesting
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Navigator
import com.github.zly2006.zhihu.navigation.Search
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.ArticleScreen
import com.github.zly2006.zhihu.ui.HomeScreen
import com.github.zly2006.zhihu.ui.HotListScreen
import com.github.zly2006.zhihu.ui.SearchScreen
import com.github.zly2006.zhihu.ui.components.GlassBarTab
import com.github.zly2006.zhihu.ui.glass.AndroidGlassBottomBarProvider
import com.github.zly2006.zhihu.ui.components.LocalGlassBottomBarProvider
import com.github.zly2006.zhihu.ui.components.LocalGlassBarSource
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HotListViewModel
import com.github.zly2006.zhihu.viewmodel.feed.SearchViewModel
import com.github.zly2006.zhihu.viewmodel.za.MixedHomeFeedViewModel
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * 无模拟器截图：Robolectric 原生图形渲染 + 本地假数据（所有网络请求由 MockEngine 返回 404，不出网）。
 * 输出目录由系统属性 zhihu.shots.dir 指定，默认 build/zhihu-shots。
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class GuestScreenshotBase(private val suffix: String) {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val outDir = File(System.getProperty("zhihu.shots.dir") ?: System.getenv("ZHIHU_SHOTS_DIR") ?: "build/zhihu-shots")

    @Before
    fun offline() {
        // 同一 JVM 里前面的测试可能已建好真实网络的账号存储，先清掉，保证本测试只走 MockEngine。
        com.github.zly2006.zhihu.account.replaceAndroidZhihuAccountStoreForTesting(null)
        accountHttpClientEngineForTesting = MockEngine { request ->
            // 真实抓包：未登录搜索 → 403，code 40353，need_login，并在响应体里给出 /account/unhuman 跳转（不跟随）。
            if ("/search" in request.url.encodedPath) {
                return@MockEngine respond(
                    """{"error":{"code":40353,"message":"请登录后查看","need_login":true,"redirect":"https://www.zhihu.com/account/unhuman?type=unhuman&message=need_login&need_login=true"}}""",
                    HttpStatusCode.Forbidden,
                    headersOf("Content-Type", "application/json"),
                )
            }
            respond("""{"error":"offline screenshot"}""", HttpStatusCode.NotFound, headersOf("Content-Type", "application/json"))
        }
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        com.github.zly2006.zhihu.data.AccountData.loadData(ctx)
        com.github.zly2006.zhihu.theme.AndroidThemeSettings.initialize(ctx)
        com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase(ctx)
        com.github.zly2006.zhihu.account.androidZhihuAccountStore(ctx)
        com.github.zly2006.zhihu.updater.UpdateManager.updateState.value =
            com.github.zly2006.zhihu.updater.UpdateManager.UpdateState.NoUpdate
    }

    @After
    fun restore() {
        accountHttpClientEngineForTesting = null
        com.github.zly2006.zhihu.account.replaceAndroidZhihuAccountStoreForTesting(null)
    }

    private val tabs = listOf(
        GlassBarTab("主页", Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
        GlassBarTab("关注", Icons.Filled.Group, Icons.Outlined.Group, "nav_tab_follow"),
        GlassBarTab("热榜", Icons.Filled.Whatshot, Icons.Outlined.Whatshot, "nav_tab_hotlist"),
        GlassBarTab("收藏夹", Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks, "nav_tab_mycollections"),
        GlassBarTab("账号", Icons.Filled.ManageAccounts, Icons.Outlined.ManageAccounts, "nav_tab_account"),
    )

    private fun sampleItems(prefix: String) = listOf(
        "如何评价 Material 3 Expressive 的设计语言？" to "动态取色和更有弹性的动效让界面更接近系统原生，但信息密度需要克制……",
        "为什么越来越多的应用开始使用底部悬浮导航栏？" to "悬浮导航把内容区域延伸到屏幕底部，配合模糊背景能保留层次感。",
        "新加坡有哪些适合周末徒步的路线？" to "麦里芝蓄水池环线、南部山脊步道和武吉知马自然保护区都很适合入门。",
        "Kotlin Multiplatform 现在适合在生产环境使用吗？" to "共享业务逻辑已经相当成熟，UI 层是否共享取决于团队对平台一致性的要求。",
        "有哪些冷门但好用的安卓应用？" to "开源、无广告、尊重隐私的小工具往往比大而全的应用更耐用。",
        "如何系统地学习数据结构与算法？" to "先掌握数组、链表、哈希表与树，再按专题刷题，并定期复盘。",
    ).mapIndexed { i, (t, s) ->
        FeedDisplayItem(
            title = t,
            summary = s,
            details = "${(i + 1) * 137} 赞同 · ${(i + 2) * 11} 评论",
            feed = null,
            authorName = listOf("林间", "Tavi", "阿哲", "Kai", "小雨", "老周")[i],
            navDestinationJson = null,
        ).let { if (prefix.isEmpty()) it else it.copy(title = "$prefix${it.title}") }
    }

    private fun shoot(name: String) {
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        outDir.mkdirs()
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        File(outDir, "$name-$suffix.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun setShell(selected: Int, content: @Composable (PaddingValues) -> Unit) {
        compose.setContent {
            ZhihuTheme {
                val p = AndroidGlassBottomBarProvider
                val source = p.rememberSource()
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides compose.activity,
                    LocalNavigator provides Navigator(onNavigate = {}, onNavigateBack = {}, onNavigateTopLevel = {}),
                    LocalGlassBottomBarProvider provides p,
                    LocalGlassBarSource provides source,
                ) {
                    Scaffold(
                        bottomBar = { p.Bar(source, tabs, selected, {}, Modifier) },
                    ) { inner ->
                        // 首页自己拆分模糊层与 FAB；其它页由调用方决定
                        content(inner)
                    }
                }
            }
        }
    }

    @Test
    fun home() {
        compose.runOnUiThread {
            ViewModelProvider(compose.activity)[MixedHomeFeedViewModel::class.java].addDisplayItems(sampleItems(""))
        }
        setShell(0) { HomeScreen(scrollToTopTrigger = 0, innerPadding = it) }
        shoot("home")
    }

    @Test
    fun hotList() {
        compose.runOnUiThread {
            ViewModelProvider(compose.activity)[HotListViewModel::class.java].addDisplayItems(sampleItems(""))
        }
        setShell(2) { HotListScreen(innerPadding = it) }
        shoot("hotlist")
    }

    @Test
    fun search() {
        compose.runOnUiThread {
            ViewModelProvider(
                compose.activity,
                viewModelFactory { initializer { SearchViewModel("Material 3", "") } },
            )[SearchViewModel::class.java] // 不预填数据：让页面真正发起搜索请求，走 403 降级路径
        }
        compose.setContent {
            ZhihuTheme {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides compose.activity,
                    LocalNavigator provides Navigator(onNavigate = {}, onNavigateBack = {}, onNavigateTopLevel = {}),
                ) { SearchScreen(Search(query = "Material 3")) }
            }
        }
        // 403 need_login 后应显示「登录后可看」+ 登录按钮，而不是无限转圈。
        // 网络回调在真实 IO 线程完成，需要推进 Compose 时钟才会重组，所以手动轮询。
        var shown = false
        repeat(100) {
            if (!shown) {
                Thread.sleep(100)
                compose.mainClock.advanceTimeBy(200)
                compose.waitForIdle()
                shown = compose.onAllNodesWithText("这部分内容登录后可看", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }
        org.junit.Assert.assertTrue("未登录搜索 403 后应显示「登录后可看」", shown)
        shoot("search")
    }

    @Test
    fun answer() {
        val article = Article(type = ArticleType.Answer, id = 777L, title = "为什么越来越多的应用开始使用底部悬浮导航栏？")
        val vm = ArticleViewModel(article = article, httpClient = null)
        compose.runOnUiThread {
            vm.title = article.title
            vm.authorName = "Tavi"
            vm.authorId = "sample-author"
            vm.authorUrlToken = "sample-author"
            vm.content = listOf(
                "<p>悬浮导航栏把内容区域延伸到了屏幕底部，配合背景模糊，能在不牺牲可读性的前提下保留层次感。</p>",
                "<p>从交互上看，它让拇指可达区域更集中；从视觉上看，它也更贴近系统原生的设计语言。</p>",
                "<h2>需要注意的地方</h2>",
                "<p>列表底部必须留出足够的留白，否则最后一项会被遮挡；低版本系统没有实时模糊时，应降级为半透明的表面色。</p>",
                "<p>配色要跟随动态取色，在浅色和深色模式下都保证图标与文字的对比度。</p>",
            ).joinToString("")
            vm.voteUpCount = 1024
            vm.commentCount = 86
            vm.questionId = 123456L
            vm.createdAt = 1_760_000_000L
            vm.updatedAt = 1_760_000_600L
            vm.ipInfo = "新加坡"
        }
        compose.setContent {
            ZhihuTheme {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides compose.activity,
                    LocalNavigator provides Navigator(onNavigate = {}, onNavigateBack = {}, onNavigateTopLevel = {}),
                ) { ArticleScreen(article = article, viewModel = remember { vm }) }
            }
        }
        shoot("answer")
    }
}

@Config(sdk = [35], qualifiers = "w393dp-h852dp-notnight-xxhdpi")
class GuestScreenshotLightTest : GuestScreenshotBase("light")

@Config(sdk = [35], qualifiers = "w393dp-h852dp-night-xxhdpi")
class GuestScreenshotDarkTest : GuestScreenshotBase("dark")
