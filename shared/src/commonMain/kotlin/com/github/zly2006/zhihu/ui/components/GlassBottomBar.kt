package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/** 底栏的一个导航项（交给平台的玻璃底栏实现绘制）。 */
data class GlassBarTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

/**
 * 平台提供的液态玻璃底栏。Android 端由 app 模块注入（Kyant backdrop：Android 12+ 真模糊，
 * 以下降级为半透明 surfaceContainer）；其它平台为 null，继续使用 Material 3 NavigationBar。
 */
interface GlassBottomBarProvider {
    /** 返回一个模糊源句柄；主内容用 [contentSource] 标记为被模糊的背景。 */
    @Composable
    fun rememberSource(): Any

    fun contentSource(modifier: Modifier, source: Any): Modifier

    @Composable
    fun Bar(
        source: Any,
        tabs: List<GlassBarTab>,
        selectedIndex: Int,
        onSelect: (Int) -> Unit,
        modifier: Modifier,
    )
}

val LocalGlassBottomBarProvider = staticCompositionLocalOf<GlassBottomBarProvider?> { null }
