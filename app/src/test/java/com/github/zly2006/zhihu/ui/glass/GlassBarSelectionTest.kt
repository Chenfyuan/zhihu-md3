package com.github.zly2006.zhihu.ui.glass

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.ui.components.GlassBarTab
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 回归：先停在「账号」，再切到「主页」后，玻璃高亮必须跟到主页，账号不再处于选中态。 */
@RunWith(AndroidJUnit4::class)
class GlassBarSelectionTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val tabs = listOf(
        GlassBarTab("主页", Icons.Default.Home, Icons.Default.Home, "nav_tab_home"),
        GlassBarTab("关注", Icons.Default.Person, Icons.Default.Person, "nav_tab_follow"),
        GlassBarTab("账号", Icons.Default.Person, Icons.Default.Person, "nav_tab_account"),
    )

    @Test
    fun highlightFollowsHomeAfterAccount() {
        var selected by mutableIntStateOf(2) // 冷启动停在账号
        compose.setContent {
            MaterialTheme {
                val p = AndroidGlassBottomBarProvider
                val source = p.rememberSource()
                Box(p.contentSource(Modifier, source)) {
                    p.Bar(source, tabs, selected, { selected = it }, Modifier)
                }
            }
        }
        compose.onNodeWithTag("nav_tab_account").assertIsSelected()
        compose.onNodeWithTag("nav_tab_home").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("nav_tab_home").assertIsSelected()
        compose.onNodeWithTag("nav_tab_account").assertIsNotSelected()
        // 外部状态（如分页滑动 / 返回键）改变选中项时同样要跟随
        compose.runOnIdle { selected = 1 }
        compose.waitForIdle()
        compose.onNodeWithTag("nav_tab_follow").assertIsSelected()
        compose.onNodeWithTag("nav_tab_home").assertIsNotSelected()
    }
}
