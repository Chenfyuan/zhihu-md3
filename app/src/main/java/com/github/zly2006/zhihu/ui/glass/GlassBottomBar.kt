package com.github.zly2006.zhihu.ui.glass

import androidx.compose.ui.semantics.selected

import androidx.compose.ui.semantics.semantics

import androidx.compose.runtime.compositionLocalOf

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.github.zly2006.zhihu.ui.glass.DampedDragAnimation
import com.github.zly2006.zhihu.ui.glass.InteractiveHighlight
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.testTag
import com.github.zly2006.zhihu.ui.components.GlassBarTab
import com.github.zly2006.zhihu.ui.components.GlassBottomBarProvider
import com.kyant.backdrop.backdrops.LayerBackdrop

/**
 * 与清瓣（com.tavi.douban）同款的液态玻璃底栏，算法与视觉参数移植自其 Glass.kt
 * （基于 Kyant0/AndroidLiquidGlass 的 LiquidBottomTabs 示例）。
 * - Android 12+（RenderEffect）：vibrancy + blur(8dp) + lens(24dp) 折射背后内容，可拖动的选中胶囊
 * - Android 12 以下：降级为半透明 surfaceContainer 胶囊
 */
object AndroidGlassBottomBarProvider : GlassBottomBarProvider {
    @Composable
    override fun rememberSource(): Any = rememberLayerBackdrop()

    override fun contentSource(modifier: Modifier, source: Any): Modifier =
        if (isRenderEffectSupported()) modifier.layerBackdrop(source as LayerBackdrop) else modifier

    @Composable
    override fun Bar(
        source: Any,
        tabs: List<GlassBarTab>,
        selectedIndex: Int,
        onSelect: (Int) -> Unit,
        modifier: Modifier,
    ) {
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp + bottomInset)) {
            LiquidTabs(
                selectedTabIndex = { selectedIndex.coerceAtLeast(0) },
                pageOffset = { selectedIndex.coerceAtLeast(0).toFloat() },
                onTabSelected = onSelect,
                backdrop = source as Backdrop,
                tabsCount = tabs.size.coerceAtLeast(1),
            ) {
                val cs = MaterialTheme.colorScheme
                tabs.forEachIndexed { i, tab ->
                    val sel = selectedIndex == i
                    CompositionLocalProvider(LocalTabIndex provides i) {
                    LiquidTab(onClick = { onSelect(i) }, modifier = Modifier.testTag(tab.testTag)) {
                        Icon(if (sel) tab.selectedIcon else tab.unselectedIcon, tab.label, tint = cs.onSurface, modifier = Modifier.size(24.dp))
                        Text(tab.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = cs.onSurface, maxLines = 1)
                    }
                    }
                }
            }
        }
    }
}

/** Robolectric 不支持 AGSL 时可关掉玻璃着色器，走降级外观 */
val LocalLiquidGlassEnabled = staticCompositionLocalOf { true }

private val LocalTabScale = staticCompositionLocalOf { { 1f } }

/** 玻璃高亮（胶囊）当前停靠的 Tab 下标，用于无障碍 selected 语义和测试 */
private val LocalIndicatorIndex = compositionLocalOf { -1 }
private val LocalTabIndex = compositionLocalOf { -1 }

@Composable
private fun RowScope.LiquidTab(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val scale = LocalTabScale.current
    val indicatorOnMe = LocalIndicatorIndex.current == LocalTabIndex.current
    Column(
        modifier
            .semantics { selected = indicatorOnMe }
            .clip(Capsule())
            .clickable(interactionSource = null, indication = null, role = Role.Tab, onClick = onClick)
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer { val s = scale(); scaleX = s; scaleY = s },
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
private fun LiquidTabs(
    selectedTabIndex: () -> Int,
    pageOffset: () -> Float,
    onTabSelected: (Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    content: @Composable RowScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val dark = cs.surface.luminance() < 0.5f
    // 跟随动态取色：选中色用 primary，底色用 surfaceContainer。
    val accent = cs.primary
    val supported = isRenderEffectSupported() && LocalLiquidGlassEnabled.current
    // 真模糊时保留清瓣的 0.4 透明度略加厚到 0.5，保证浅/深色下文字对比度；降级时为近乎不透明的 surfaceContainer。
    val container = cs.surfaceContainer.copy(alpha = if (supported) 0.5f else 0.94f)
    val tabsBackdrop = rememberLayerBackdrop()
    val combined = rememberCombinedBackdrop(backdrop, tabsBackdrop)

    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        val density = LocalDensity.current
        val maxW = constraints.maxWidth.toFloat()
        val tabWidth = with(density) { (maxW - 8.dp.toPx()) / tabsCount }
        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density, maxW) {
            derivedStateOf {
                val f = (offsetAnimation.value / maxW).fastCoerceIn(-1f, 1f)
                with(density) { 4.dp.toPx() * f.sign * EaseOut.transform(abs(f)) }
            }
        }
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var currentIndex by remember { mutableIntStateOf(selectedTabIndex()) }
        var dragging by remember { mutableStateOf(false) }
        val last = (tabsCount - 1).toFloat().coerceAtLeast(0.0001f)
        val damped = remember(animationScope, tabsCount) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().toFloat(),
                valueRange = 0f..last,
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = { dragging = true },
                onDragStopped = {
                    val target = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    dragging = false
                    currentIndex = target
                    animateToValue(target.toFloat())
                    onTabSelected(target)
                    animationScope.launch { offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f)) }
                },
                onDrag = { _, drag ->
                    updateValue((targetValue + drag.x / tabWidth * if (isLtr) 1f else -1f).fastCoerceIn(0f, last))
                    animationScope.launch { offsetAnimation.snapTo(offsetAnimation.value + drag.x) }
                },
            )
        }
        // 修复：LaunchedEffect 只启动一次，直接用参数 lambda 会一直读首次组合时捕获的旧 selectedIndex，
        // 导致切换 Tab 后高亮停在旧位置。用 rememberUpdatedState 始终读最新值。
        val latestSelected by rememberUpdatedState(selectedTabIndex)
        val latestPageOffset by rememberUpdatedState(pageOffset)
        LaunchedEffect(damped) {
            snapshotFlow { latestSelected() }.collectLatest { i ->
                if (i != currentIndex) { currentIndex = i; if (!dragging) damped.animateToValue(i.toFloat()) }
            }
        }
        LaunchedEffect(damped) {
            snapshotFlow { latestPageOffset() }.collectLatest { p ->
                if (!dragging && abs(p - p.fastRoundToInt()) > 0.001f) damped.updateValue(p)
            }
        }
        val highlight = remember(animationScope, damped) {
            InteractiveHighlight(animationScope = animationScope, enabled = supported, position = { size, _ ->
                Offset(
                    if (isLtr) (damped.value + 0.5f) * tabWidth + panelOffset
                    else size.width - (damped.value + 0.5f) * tabWidth + panelOffset,
                    size.height / 2f,
                )
            })
        }

        Row(
            Modifier
                .graphicsLayer { translationX = panelOffset }
                .glassOr(supported, container) {
                    drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = { if (supported) { vibrancy(); blur(8.dp.toPx()); lens(24.dp.toPx(), 24.dp.toPx()) } },
                    layerBlock = {
                        val s = lerp(1f, 1f + 16.dp.toPx() / size.width, damped.pressProgress)
                        scaleX = s; scaleY = s
                    },
                    highlight = { if (supported) Highlight.Default else null },
                    shadow = { Shadow(alpha = if (dark) 0.6f else 0.25f) },
                    onDrawSurface = { drawRect(container) },
                ) }
                .then(highlight.modifier)
                .height(64.dp)
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = { CompositionLocalProvider(LocalIndicatorIndex provides damped.targetValue.fastRoundToInt()) { content() } },
        )

        CompositionLocalProvider(
            LocalTabScale provides { lerp(1f, 1.2f, damped.pressProgress) },
        ) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .then(if (supported) Modifier.layerBackdrop(tabsBackdrop) else Modifier)
                    .graphicsLayer { translationX = panelOffset }
                    .glassOr(supported, container) {
                    drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            val p = damped.pressProgress
                            if (supported) { vibrancy(); blur(8.dp.toPx()); lens(24.dp.toPx() * p, 24.dp.toPx() * p) }
                        },
                        highlight = { if (supported) Highlight.Default.copy(alpha = damped.pressProgress) else null },
                        onDrawSurface = { drawRect(container) },
                    ) }
                    .then(highlight.modifier)
                    .height(56.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .graphicsLayer(colorFilter = ColorFilter.tint(accent)),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }

        Box(
            Modifier
                .padding(horizontal = 4.dp)
                .graphicsLayer {
                    translationX = if (isLtr) damped.value * tabWidth + panelOffset
                    else size.width - (damped.value + 1f) * tabWidth + panelOffset
                }
                .then(highlight.gestureModifier)
                .then(damped.modifier)
                .glassOr(supported, accent.copy(alpha = if (dark) 0.28f else 0.16f)) {
                    drawBackdrop(
                    backdrop = combined,
                    shape = { Capsule() },
                    effects = {
                        val p = damped.pressProgress
                        if (supported) lens(10.dp.toPx() * p, 14.dp.toPx() * p, chromaticAberration = true)
                    },
                    highlight = { if (supported) Highlight.Default.copy(alpha = damped.pressProgress) else null },
                    shadow = { Shadow(alpha = damped.pressProgress) },
                    innerShadow = { InnerShadow(radius = 8.dp * damped.pressProgress, alpha = damped.pressProgress) },
                    layerBlock = {
                        scaleX = damped.scaleX; scaleY = damped.scaleY
                        val v = damped.velocity / 10f
                        scaleX /= 1f - (v * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (v * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val p = damped.pressProgress
                        drawRect(if (dark) Color.White.copy(0.1f) else Color.Black.copy(0.1f), alpha = 1f - p)
                        drawRect(Color.Black.copy(alpha = 0.03f * p))
                    },
                ) }
                .height(56.dp)
                .fillMaxWidth(1f / tabsCount),
        )
    }
}





/** 支持玻璃时用 backdrop 绘制；否则（Android 12 以下）退化为半透明实色胶囊 */
private fun Modifier.glassOr(supported: Boolean, fallback: Color, glass: Modifier.() -> Modifier): Modifier =
    if (supported) glass() else clip(Capsule()).background(fallback)
