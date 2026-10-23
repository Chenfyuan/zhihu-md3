package com.github.zly2006.zhihu.screenshot

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Navigator
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.subscreens.CRASH_LOGS_CLEAR_TAG
import com.github.zly2006.zhihu.ui.subscreens.CrashLogEntry
import com.github.zly2006.zhihu.ui.subscreens.CrashLogStore
import com.github.zly2006.zhihu.ui.subscreens.CrashLogsScreen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

private class FakeCrashLogStore : CrashLogStore {
    val logs = mutableListOf(
        CrashLogEntry("crash-2.txt", "2026-10-23 12:41:07.512 +0800", 6_144),
        CrashLogEntry("crash-1.txt", "2026-10-22 21:03:55.020 +0800", 4_096),
    )
    var shared = 0

    override fun list() = logs.toList()

    override fun read(entry: CrashLogEntry) =
        """
        时间: ${entry.title}
        应用版本: 0.30.4 (751)
        包名: com.sjtech.zhihu
        设备: Google Pixel 8
        Android: 15 (API 35)
        线程: main

        java.lang.IllegalStateException: 示例崩溃
        	at com.github.zly2006.zhihu.ui.HomeScreenKt.HomeScreen(HomeScreen.kt:312)
        	at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.kt:109)
        """.trimIndent()

    override fun share(entry: CrashLogEntry): Boolean {
        shared++
        return true
    }

    override fun clear(): Int = logs.size.also { logs.clear() }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class CrashLogsScreenshotBase(private val suffix: String) {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val outDir = File(System.getProperty("zhihu.shots.dir") ?: System.getenv("ZHIHU_SHOTS_DIR") ?: "build/zhihu-shots")

    @Before
    fun init() {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        com.github.zly2006.zhihu.theme.AndroidThemeSettings.initialize(ctx)
    }

    private fun shoot(name: String) {
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        outDir.mkdirs()
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        File(outDir, "$name-$suffix.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun crashLogsPage() {
        val store = FakeCrashLogStore()
        compose.setContent {
            ZhihuTheme {
                CompositionLocalProvider(LocalNavigator provides Navigator(onNavigate = {}, onNavigateBack = {})) {
                    CrashLogsScreen(store)
                }
            }
        }
        shoot("crash-logs")
        compose.onNodeWithTag(CRASH_LOGS_CLEAR_TAG).performClick()
        shoot("crash-logs-clear-confirm")
        compose.onNodeWithText("清除").performClick()
        compose.waitForIdle()
        assertEquals(0, store.logs.size)
        compose.onNodeWithText("暂无崩溃日志").assertExists()
        shoot("crash-logs-empty")
    }
}

@Config(sdk = [35], qualifiers = "w393dp-h852dp-notnight-xxhdpi")
class CrashLogsScreenshotLightTest : CrashLogsScreenshotBase("light")

@Config(sdk = [35], qualifiers = "w393dp-h852dp-night-xxhdpi")
class CrashLogsScreenshotDarkTest : CrashLogsScreenshotBase("dark")
