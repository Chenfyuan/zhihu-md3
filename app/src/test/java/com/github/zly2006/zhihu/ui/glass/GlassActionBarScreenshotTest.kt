package com.github.zly2006.zhihu.ui.glass

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.github.zly2006.zhihu.ui.article.GLASS_ACTION_COMMENT_TAG
import com.github.zly2006.zhihu.ui.article.GLASS_ACTION_FAVORITE_TAG
import com.github.zly2006.zhihu.ui.article.GLASS_ACTION_MORE_TAG
import com.github.zly2006.zhihu.ui.article.GLASS_ACTION_VOTE_TAG
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Navigator
import com.github.zly2006.zhihu.platform.androidSettingsStore
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.ARTICLE_GLASS_ACTION_BAR_TAG
import com.github.zly2006.zhihu.ui.ArticleScreen
import com.github.zly2006.zhihu.ui.components.LocalGlassBottomBarProvider
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.data.VoteUpState
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.max
import kotlin.math.min

/** 玻璃面板底色在最坏背景（纯黑/纯白正文）上的文字对比度，保证图标和数字可读。 */
class GlassActionPanelContrastTest {
    private fun contrast(a: Color, b: Color): Float {
        val la = a.luminance() + 0.05f
        val lb = b.luminance() + 0.05f
        return max(la, lb) / min(la, lb)
    }

    @Test
    fun panelKeepsTextLegibleOverAnyContent() {
        val schemes = listOf(
            Color(0xFFF3EDF7) to Color(0xFF1D1B20), // 浅色 surfaceContainer / onSurface
            Color(0xFF211F26) to Color(0xFFE6E0E9), // 深色
        )
        for ((surface, onSurface) in schemes) {
            // 真玻璃下背景先被 blur(8dp)：正文文字被抹成"页面底色为主、少量文字色"的平均色
            for (ink in listOf(Color.Black, Color.White, Color(0xFF6750A4))) {
                val behind = lerp(surface, ink, 0.25f)
                val tinted = glassPanelTint(surface).compositeOver(behind)
                val fallback = glassPanelFallback(surface).compositeOver(ink)
                assertTrue("玻璃底色 over $behind 对比度不足", contrast(onSurface, tinted) >= 3f)
                assertTrue("降级底色 over $ink 对比度不足", contrast(onSurface, fallback) >= 4.5f)
            }
        }
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class GlassActionBarScreenshotBase(private val suffix: String) {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val outDir = File(
        System.getProperty("zhihu.shots.dir") ?: System.getenv("ZHIHU_SHOTS_DIR") ?: "build/zhihu-shots",
    )

    @Before
    fun offline() {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        com.github.zly2006.zhihu.account.replaceAndroidZhihuAccountStoreForTesting(null)
        com.github.zly2006.zhihu.data.AccountData.loadData(ctx)
        com.github.zly2006.zhihu.theme.AndroidThemeSettings.initialize(ctx)
        com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase(ctx)
        com.github.zly2006.zhihu.account.androidZhihuAccountStore(ctx)
        outDir.mkdirs()
    }

    private val body = buildString {
        repeat(6) { i ->
            append("<h2>第 ${i + 1} 节：玻璃操作栏叠在正文上</h2>")
            append("<p><b>加粗段落</b>用来检查模糊后底栏图标和数字是否仍然清楚。")
            append("这段文字会一直滚到操作栏下面，深浅色都要看得清赞同数和评论数。</p>")
            append("<blockquote>引用块有不同底色，模拟图片等高对比内容。</blockquote>")
            append("<pre><code>val glass = drawBackdrop(blur = 8.dp) // 代码块深色背景</code></pre>")
        }
        append("<p>全文完：最后一段必须能滚到悬浮按钮上方。</p>")
    }

    private fun open(type: ArticleType, duo3: Boolean, vote: VoteUpState, favorited: Boolean, name: String, scrollToEnd: Boolean = false) {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        androidSettingsStore(ctx).putBoolean("duo3_article_actions", duo3)
        val article = Article(type = type, id = 900L + name.hashCode(), title = "液态玻璃底部操作栏")
        val vm = ArticleViewModel(article = article, httpClient = null)
        compose.runOnUiThread {
            vm.title = article.title
            vm.authorName = "Tavi"
            vm.content = body
            vm.voteUpCount = 2048
            vm.commentCount = 128
            vm.voteUpState = vote
            vm.createdAt = 1_760_000_000L
            vm.updatedAt = 1_760_000_000L
        }
        compose.setContent {
            ZhihuTheme {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides compose.activity,
                    LocalNavigator provides Navigator(onNavigate = {}, onNavigateBack = {}, onNavigateTopLevel = {}),
                    LocalGlassBottomBarProvider provides AndroidGlassBottomBarProvider,
                ) { ArticleScreen(article = article, viewModel = remember { vm }) }
            }
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        compose.onNodeWithTag(ARTICLE_GLASS_ACTION_BAR_TAG, useUnmergedTree = true).assertIsDisplayed()
        for (tag in listOf(GLASS_ACTION_VOTE_TAG, GLASS_ACTION_FAVORITE_TAG, GLASS_ACTION_COMMENT_TAG, GLASS_ACTION_MORE_TAG)) {
            compose.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed()
        }
        val vote = compose.onNodeWithTag(GLASS_ACTION_VOTE_TAG, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val more = compose.onNodeWithTag(GLASS_ACTION_MORE_TAG, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("悬浮按钮应离开底边（导航栏+间距）", vote.bottom < root.bottom - 8f)
        assertTrue("更多按钮在右侧", more.left > vote.right)
        if (scrollToEnd) {
            repeat(20) {
                compose.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeUp() }
                compose.mainClock.advanceTimeBy(600)
            }
            compose.waitForIdle()
            val last = compose.onAllNodesWithText("全文完", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().first().boundsInRoot
            assertTrue("滚到底时最后一段 (${last.bottom}) 应在悬浮按钮 (${vote.top}) 之上", last.bottom <= vote.top)
        }
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        File(outDir, "glassbar-$name-$suffix.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun answer() = open(ArticleType.Answer, duo3 = false, vote = VoteUpState.Up, favorited = true, name = "answer")

    @Test
    fun answerDuo3() = open(ArticleType.Answer, duo3 = true, vote = VoteUpState.Up, favorited = false, name = "answer-duo3")

    @Test
    fun answerScrolledToEnd() = open(ArticleType.Answer, duo3 = false, vote = VoteUpState.Neutral, favorited = false, name = "answer-end", scrollToEnd = true)

    @Test
    fun article() = open(ArticleType.Article, duo3 = false, vote = VoteUpState.Neutral, favorited = false, name = "article")
}

@Config(sdk = [35], qualifiers = "w393dp-h852dp-notnight-xxhdpi")
class GlassActionBarLightTest : GlassActionBarScreenshotBase("light")

@Config(sdk = [35], qualifiers = "w393dp-h852dp-night-xxhdpi")
class GlassActionBarDarkTest : GlassActionBarScreenshotBase("dark")
