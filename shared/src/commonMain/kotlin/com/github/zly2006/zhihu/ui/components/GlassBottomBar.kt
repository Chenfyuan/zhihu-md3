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

    /**
     * 圆形玻璃悬浮按钮。必须画在 [contentSource] 标记的模糊层之外（与底栏同层），
     * 否则会采样到自己。玻璃底色透明度固定。
     */
    @Composable
    fun Fab(
        source: Any,
        onClick: () -> Unit,
        modifier: Modifier,
        content: @Composable () -> Unit,
    )

    /**
     * 与 [Fab] 同款的玻璃胶囊（宽度随内容，圆形时传正方形尺寸）。用于内容页底部的赞踩/收藏/评论/更多悬浮按钮。
     * [source] 必须是按钮之外、被 [contentSource] 标记的正文层，避免采样到自己。Android 12 以下降级为半透明 surfaceContainer。
     */
    @Composable
    fun Capsule(
        source: Any,
        modifier: Modifier,
        content: @Composable () -> Unit,
    )
}

val LocalGlassBottomBarProvider = staticCompositionLocalOf<GlassBottomBarProvider?> { null }

/** 与底栏共用的模糊源；由主壳提供，悬浮按钮在模糊层外读取它。 */
val LocalGlassBarSource = staticCompositionLocalOf<Any?> { null }
