/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import com.github.zly2006.zhihu.ui.components.GlassBarTab
import com.github.zly2006.zhihu.ui.components.LocalGlassBarSource
import com.github.zly2006.zhihu.ui.components.LocalGlassBottomBarProvider
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.github.zly2006.zhihu.account.LoginScreen
import com.github.zly2006.zhihu.filter.ContentOpenFrom
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.ArticleTypeNavType
import com.github.zly2006.zhihu.navigation.CollectionContent
import com.github.zly2006.zhihu.navigation.Collections
import com.github.zly2006.zhihu.navigation.Daily
import com.github.zly2006.zhihu.navigation.EmptyDetail
import com.github.zly2006.zhihu.navigation.Follow
import com.github.zly2006.zhihu.navigation.History
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.HotList
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Login
import com.github.zly2006.zhihu.navigation.MainTabs
import com.github.zly2006.zhihu.navigation.MyCollections
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Navigator
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.Search
import com.github.zly2006.zhihu.navigation.SentenceSimilarityTest
import com.github.zly2006.zhihu.navigation.TopLevelDestination
import com.github.zly2006.zhihu.navigation.Topic
import com.github.zly2006.zhihu.navigation.Video
import com.github.zly2006.zhihu.navigation.WriteAnswer
import com.github.zly2006.zhihu.navigation.WritePin
import com.github.zly2006.zhihu.navigation.loginNavigationRequestFlow
import com.github.zly2006.zhihu.platform.PlatformBackHandler
import com.github.zly2006.zhihu.platform.PlatformPredictiveBackHandler
import androidx.compose.animation.core.Animatable
import com.github.zly2006.zhihu.platform.platformName
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.ui.components.NoOpPagerNestedScrollConnection
import com.github.zly2006.zhihu.ui.subscreens.AppearanceSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.ThemePreviewScreen
import com.github.zly2006.zhihu.ui.subscreens.BlockedFeedHistoryScreen
import com.github.zly2006.zhihu.ui.subscreens.ColorSchemeScreen
import com.github.zly2006.zhihu.ui.subscreens.ContentFilterSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.DeveloperSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.IdentityManagementScreen
import com.github.zly2006.zhihu.ui.subscreens.OpenSourceLicensesScreen
import com.github.zly2006.zhihu.ui.subscreens.SettingsSearchScreen
import com.github.zly2006.zhihu.ui.subscreens.SystemAndUpdateSettingsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

private sealed class MainTabPage(
    val bottomDestination: TopLevelDestination,
    val key: String,
) {
    data object HomePage : MainTabPage(Home, "home")

    data object FollowPage : MainTabPage(Follow, "follow")

    data object HotListPage : MainTabPage(HotList, "hotlist")

    data object DailyPage : MainTabPage(Daily, "daily")

    data object OnlineHistoryPage : MainTabPage(OnlineHistory, "online_history")

    data object MyCollectionsPage : MainTabPage(MyCollections, "my_collections")

    data object AccountPage : MainTabPage(Account, "account")
}

internal val LocalArticleNavController = staticCompositionLocalOf<NavHostController?> { null }

/**
 * Zhihu++ 的共享应用主壳。
 *
 * 这个 composable 是顶层体验的唯一所有者：渲染可配置底部导航栏，承载横向主 tab pager，向子页面提供 [LocalNavigator]，
 * 并注册跨平台共享的 typed [NavDestination] route。设计上把顶层 tab 收在 [MainTabs] 内部，而不是把每个 tab
 * 都作为独立 NavHost 页面 push，这样 tab 重选、回到顶部、顶/底栏自动隐藏和持久化 tab 选择都能使用同一套状态模型。
 *
 * 用户可见的主壳设置通过 [preferenceState] 流入。设置页退出时只 reload 这份状态，不重建 NavHost，从而在应用底栏和主题相关变更时
 * 保留已加载页面、返回栈和滚动位置。
 *
 * [navController] 是主内容 NavHost 的控制器；[navigate] 负责把目标交给平台主导航入口。
 * [navigateContent] 可将目标交给指定的内容控制器，用于横屏列表/详情布局的右侧 NavHost；未提供平台实现时会回退到 [navigate]。
 *
 * @param navController 主内容 NavHost；顶层 tab 和默认内容页使用它维护返回栈。
 * @param navigate 平台主导航入口，负责处理顶层 tab 与外部导航。
 * @param navigateContent 指定内容 NavHost 的导航入口；大屏列表/详情布局通过它打开右侧内容。
 */
@OptIn(ExperimentalFoundationApi::class)
@Suppress("RestrictedApi")
@Composable
fun ZhihuMain(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    mainTabNavigationTarget: TopLevelDestination?,
    navigate: (NavDestination) -> Unit,
    navigateContent: (NavDestination, NavHostController) -> Unit = { destination, _ -> navigate(destination) },
    enableLandscapeListDetail: Boolean = false,
    isListDetailCapable: Boolean = true,
    setCurrentMainTabOpenFrom: (String?) -> Unit,
    consumeMainTabNavigationTarget: (TopLevelDestination) -> Unit,
    preferenceState: ZhihuMainPreferenceState,
    isDarkTheme: Boolean,
    articleContent: @Composable (Article, NavBackStackEntry) -> Unit,
    showMainNavigationBar: Boolean = true,
    showHomeTopActions: Boolean = true,
    onCurrentMainTabDestinationChange: (TopLevelDestination) -> Unit = {},
    sentenceSimilarityContent: @Composable () -> Unit = {
        error("$platformName 暂不支持句子相似度测试")
    },
    blocklistSettingsNlpContent: @Composable (onNavigateBack: () -> Unit) -> Unit = {
        error("$platformName 暂不支持 NLP 智能屏蔽设置")
    },
    articleEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
    articleExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = null,
) = BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val containerWidth = maxWidth
    val isLargeLandscape = canShowLandscapeListDetail(
        enabled = enableLandscapeListDetail,
        deviceSupportsListDetail = isListDetailCapable,
        preferenceEnabled = preferenceState.landscapeListDetailEnabled,
        width = containerWidth,
        height = maxHeight,
    )
    val bottomPadding = ScaffoldDefaults.contentWindowInsets.asPaddingValues().calculateBottomPadding()
    val tapToScrollToTopEnabled = preferenceState.tapToScrollToTopEnabled
    val autoHideBottomBar = preferenceState.autoHideBottomBar
    val selectedBottomBarItemKeys = preferenceState.selectedBottomBarItemKeys
    val startDestination = preferenceState.startDestination
    val reloadBottomBarPreferences = preferenceState::reload
    val settings = rememberSettingsStore()

    /** 右侧详情独立持有返回栈；栏内导航保留上级，从左侧重新选项时清空。 */
    val paneStateHolder = rememberSaveableStateHolder()
    val detailNavController = rememberNavController()
    val detailEntry by detailNavController.currentBackStackEntryAsState()
    val selectedContentDestination = detailEntry.readingDestinationOrNull()
    val hasOpenSecondaryDetail = detailEntry?.destination?.hasRoute<EmptyDetail>() == false
    val density = LocalDensity.current
    val currentOnMainTabDestinationChange by rememberUpdatedState(onCurrentMainTabDestinationChange)

    val navEntry by navController.currentBackStackEntryAsState()
    val showMainNavigation = navEntry?.destination?.hasRoute<MainTabs>() == true
    val isListPaneContext = navEntry.isListPaneDestination()
    // 窄屏从详情进入主栈页面时隐藏详情；返回原列表项后恢复同一详情和页面状态。
    var detailOwnerEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    val showListDetail = isLargeLandscape && isListPaneContext
    val showDetailPane = hasOpenSecondaryDetail &&
        (showListDetail || navEntry?.id == detailOwnerEntryId)
    val hasOpenDetail = showDetailPane && selectedContentDestination != null
    val useSecondaryContentNavigation = enableLandscapeListDetail && isListPaneContext
    val detailLifecycleOwner = rememberLifecycleOwner(
        maxLifecycle = if (showListDetail || showDetailPane) Lifecycle.State.RESUMED else Lifecycle.State.CREATED,
    )
    val listLifecycleOwner = rememberLifecycleOwner(
        maxLifecycle = if (showDetailPane && !showListDetail) Lifecycle.State.CREATED else Lifecycle.State.RESUMED,
    )

    LaunchedEffect(isLargeLandscape, navEntry) {
        if (showListDetail) detailOwnerEntryId = navEntry?.id
    }

    fun openListDetail(destination: NavDestination) {
        detailOwnerEntryId = navController.currentBackStackEntry?.id
        detailNavController.popBackStack(detailNavController.graph.startDestinationId, inclusive = false)
        navigateContent(destination, detailNavController)
    }

    // 离开文章页时恢复系统状态栏（只在实际切换时触发）
    val isOnArticle = (hasOpenDetail && detailEntry?.destination?.hasRoute<Article>() == true) ||
        navEntry?.destination?.hasRoute<Article>() == true
    var wasOnArticle by remember { mutableStateOf(false) }
    if (!isOnArticle && wasOnArticle) {
        LeaveImmersiveModeCleanup()
    }
    SideEffect {
        wasOnArticle = isOnArticle
    }

    var scrollToTopTrigger by remember { mutableIntStateOf(0) }
    // 滚动时自动隐藏底部导航栏
    var isBottomBarVisible by remember { mutableStateOf(true) }
    val bottomBarScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                when {
                    available.y < -3f -> isBottomBarVisible = false
                    available.y > 3f -> isBottomBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    val allBottomBarItems = listOf(
        BottomBarItem(Home, "主页", Icons.Filled.Home, Icons.Outlined.Home),
        BottomBarItem(Follow, "关注", Icons.Filled.Group, Icons.Outlined.Group),
        BottomBarItem(HotList, "热榜", Icons.Filled.Whatshot, Icons.Outlined.Whatshot),
        BottomBarItem(Daily, "日报", Icons.Filled.Newspaper, Icons.Outlined.Newspaper),
        BottomBarItem(OnlineHistory, "历史", Icons.Filled.History, Icons.Outlined.History),
        BottomBarItem(MyCollections, "收藏夹", Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks),
        BottomBarItem(Account, "账号", Icons.Filled.ManageAccounts, Icons.Outlined.ManageAccounts),
    )
    val bottomBarItems = selectedBottomBarItemKeys.mapNotNull { key ->
        allBottomBarItems.firstOrNull { it.destination.name == key }
    }

    val mainTabPages = remember(bottomBarItems) {
        bottomBarItems.flatMap { item ->
            when (item.destination) {
                Home -> listOf(MainTabPage.HomePage)
                Follow -> listOf(MainTabPage.FollowPage)
                HotList -> listOf(MainTabPage.HotListPage)
                Daily -> listOf(MainTabPage.DailyPage)
                OnlineHistory -> listOf(MainTabPage.OnlineHistoryPage)
                MyCollections -> listOf(MainTabPage.MyCollectionsPage)
                Account -> listOf(MainTabPage.AccountPage)
                else -> emptyList()
            }
        }
    }

    fun pageIndexForDestination(destination: TopLevelDestination): Int = mainTabPages
        .indexOfFirst {
            it.bottomDestination::class == destination::class
        }.takeIf { it >= 0 } ?: mainTabPages
        .indexOfFirst {
            it.bottomDestination::class == startDestination::class
        }.takeIf { it >= 0 } ?: 0

    val mainPagerState = rememberPagerState(
        initialPage = pageIndexForDestination(startDestination),
        pageCount = { mainTabPages.size },
    )
    val coroutineScope = rememberCoroutineScope()

    var currentMainTabDestination by remember { mutableStateOf(startDestination) }
    val isAccountSurface = currentMainTabDestination == Account

    // 预测性返回：读取设置「启用预测性返回」（enable_predictive_back，默认开）。
    // 开启时返回手势让详情页跟手横向滑出、下层列表带视差跟进，松手后滑完剩余距离再出栈，取消则回弹；不缩放、不加圆角。
    val predictiveBackEnabled by rememberObservedSetting(settings, "enable_predictive_back") { getBoolean("enable_predictive_back", true) }
    val detailBackScope = rememberCoroutineScope()

    // 窄屏时阅读页（回答/文章/想法）由详情栈承载并覆盖在列表之上。详情栈只剩这一页时，
    // 出栈会让整个详情层消失，NavHost 自己的出场动画来不及播放，所以由这里驱动详情层的横向位移。
    // detailOverlaySlide：0 = 详情完全展示，1 = 详情完全滑出到右侧。
    val detailOverlaySlide = remember { Animatable(1f) }
    var detailOverlayExiting by remember { mutableStateOf(false) }
    val isNarrowDetailOverlay = showDetailPane && !showListDetail
    LaunchedEffect(isNarrowDetailOverlay) {
        if (isNarrowDetailOverlay) {
            detailOverlaySlide.animateTo(0f, tween(300))
        } else {
            detailOverlaySlide.snapTo(1f)
            detailOverlayExiting = false
        }
    }
    // 只有「返回会离开整层详情」时由这里接管手势；详情栈内部的多页返回、宽屏分栏交给 NavHost 自带的跟手转场。
    val overlayBackActive = isNarrowDetailOverlay &&
        detailEntry != null &&
        detailNavController.previousBackStackEntry?.destination?.hasRoute<EmptyDetail>() == true

    fun popDetail() {
        if (!overlayBackActive) {
            detailNavController.popBackStack()
            return
        }
        if (detailOverlayExiting) return
        detailOverlayExiting = true
        detailBackScope.launch {
            val remaining = 1f - detailOverlaySlide.value
            detailOverlaySlide.animateTo(1f, tween((280 * remaining).toInt().coerceAtLeast(90)))
            detailNavController.popBackStack()
        }
    }

    val detailBackGesture = if (predictiveBackEnabled && overlayBackActive) {
        DetailBackGesture(
            onProgress = { p ->
                if (!detailOverlayExiting) detailBackScope.launch { detailOverlaySlide.snapTo(predictiveBackTranslationFraction(p)) }
            },
            onCancel = {
                detailBackScope.launch { detailOverlaySlide.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
            },
            onBack = { popDetail() },
        )
    } else {
        null
    }

    /**
     * 必须放在详情 NavHost 之后组合：NavHost 自己也注册了返回回调（返回栈多于一页时启用），
     * 后注册的回调优先，否则手势会被 NavHost 抢走，详情层整层消失、没有跟手动画。
     */
    @Composable
    fun DetailBackHandler(enabled: Boolean) {
        if (detailBackGesture != null) {
            PlatformPredictiveBackHandler(
                enabled = enabled,
                onProgress = detailBackGesture.onProgress,
                onCancel = detailBackGesture.onCancel,
                onBack = detailBackGesture.onBack,
            )
        } else {
            PlatformBackHandler(enabled = enabled) { popDetail() }
        }
    }

    fun navigateTopLevel(destination: TopLevelDestination) {
        if (destination == Account && hasOpenDetail) {
            detailNavController.popBackStack(
                detailNavController.graph.startDestinationId,
                inclusive = false,
            )
        }
        val targetPage = pageIndexForDestination(destination)
        coroutineScope.launch {
            mainPagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(navController) {
        loginNavigationRequestFlow.collect {
            navController.navigate(Login) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(mainPagerState.currentPage, mainTabPages) {
        mainTabPages.getOrNull(mainPagerState.currentPage)?.bottomDestination?.let { destination ->
            currentMainTabDestination = destination
            setCurrentMainTabOpenFrom(destination.openFrom)
            currentOnMainTabDestinationChange(destination)
        }
    }

    LaunchedEffect(currentMainTabDestination, navEntry) {
        if (isAccountSurface && hasOpenDetail) {
            // A reading detail belongs to the feed that opened it. Do not carry it into
            // the account split pane when the user switches tabs.
            detailNavController.popBackStack(
                detailNavController.graph.startDestinationId,
                inclusive = false,
            )
        }
    }

    PlatformBackHandler(
        showMainNavigation &&
            mainPagerState.currentPage != 0 &&
            !showDetailPane,
    ) {
        coroutineScope.launch {
            mainPagerState.animateScrollToPage(0)
        }
    }

    LaunchedEffect(mainTabNavigationTarget, mainTabPages) {
        mainTabNavigationTarget?.let { destination ->
            // 平台适配层会把旧的顶层 route 请求映射到 MainTabs。这里消费该请求，
            // 让 deeplink 等调用方仍能选中 Home/Follow 等 tab，而不是把旧 route 压入返回栈。
            mainPagerState.scrollToPage(pageIndexForDestination(destination))
            consumeMainTabNavigationTarget(destination)
        }
    }

    LaunchedEffect(mainTabPages) {
        if (mainTabPages.isNotEmpty()) {
            val currentDestinationStillVisible = mainTabPages.any {
                it.bottomDestination::class == currentMainTabDestination::class
            }
            val targetDestination = if (currentDestinationStillVisible) {
                currentMainTabDestination
            } else {
                startDestination
            }
            val targetPage = pageIndexForDestination(targetDestination)
            if (mainPagerState.currentPage != targetPage || mainPagerState.currentPage !in mainTabPages.indices) {
                mainPagerState.scrollToPage(targetPage)
            }
        }
    }

    var listPaneRatio by remember {
        mutableFloatStateOf(
            settings.getFloat(LANDSCAPE_LIST_PANE_RATIO_KEY, DEFAULT_LIST_PANE_RATIO),
        )
    }

    @Composable
    fun DetailPane(modifier: Modifier) {
        CompositionLocalProvider(
            LocalLifecycleOwner provides detailLifecycleOwner,
            LocalArticleNavController provides detailNavController,
            LocalNavigator provides Navigator(
                onNavigate = { destination ->
                    if (
                        destination is Video || destination.isDetailPaneDestination() || destination.isAccountDetailDestination()
                    ) {
                        navigateContent(destination, detailNavController)
                    } else {
                        navigate(destination)
                    }
                },
                onNavigateBack = ::popDetail,
                onNavigateTopLevel = ::navigateTopLevel,
            ),
            LocalSelectedContentDestination provides selectedContentDestination,
            LocalDetailBackGesture provides detailBackGesture,
        ) {
            NavHost(
                navController = detailNavController,
                startDestination = EmptyDetail,
                modifier = modifier
                    .testTag("detail_pane")
                    .background(MaterialTheme.colorScheme.background),
                enterTransition = { slideInHorizontally(tween(300)) { it } },
                exitTransition = { listExitTransition() },
                popEnterTransition = { listPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() },
            ) {
                composable<EmptyDetail> {
                    EmptyDetailPane()
                }
                composable<Article>(
                    typeMap = mapOf(typeOf<ArticleType>() to ArticleTypeNavType),
                    enterTransition = articleEnterTransition,
                    exitTransition = articleExitTransition,
                ) { entry ->
                    articleContent(entry.toRoute(), entry)
                }
                composable<Pin> { entry ->
                    PinScreen(entry.toRoute())
                }
                accountSettings(reloadBottomBarPreferences, blocklistSettingsNlpContent)
            }
            DetailBackHandler(enabled = overlayBackActive)
        }
    }

    Box(Modifier.fillMaxSize()) {
        val listPaneWidth = if (showListDetail) {
            normalizedListPaneWidth(containerWidth, listPaneRatio)
        } else {
            containerWidth
        }
        val listPaneModifier = if (showListDetail) {
            Modifier.width(listPaneWidth).fillMaxHeight().align(Alignment.CenterStart)
        } else {
            Modifier.fillMaxSize()
        }
        LaunchedEffect(navEntry, isLargeLandscape, hasOpenSecondaryDetail, isListPaneContext) {
            val directReadingDestination = navEntry.readingDestinationOrNull()?.takeIf { it.isDetailPaneDestination() }
            if (enableLandscapeListDetail && directReadingDestination != null) {
                navController.popBackStack()
                if (navController.currentBackStackEntry == null) navController.navigate(MainTabs)
                detailOwnerEntryId = navController.currentBackStackEntry?.id
                detailNavController.popBackStack(detailNavController.graph.startDestinationId, inclusive = false)
                // 平台入口已经处理历史、评论交接等副作用，这里只转移页面归属。
                detailNavController.navigate(directReadingDestination)
            }
        }
        val glassBarProvider = LocalGlassBottomBarProvider.current
        val glassBarSource = glassBarProvider?.rememberSource()
        // 窄屏打开阅读页时列表继续留在下层（生命周期降为 CREATED），返回手势缩小详情时能看到列表，
        // 详情层也能做滑入/滑出动画；以前列表和详情互斥组合，切换时没有任何过渡。
        run {
            paneStateHolder.SaveableStateProvider("list") {
                Scaffold(
                    modifier = listPaneModifier
                        .then(
                            if (isNarrowDetailOverlay) {
                                Modifier
                                    .graphicsLayer {
                                        translationX = size.width * listParallaxFraction(detailOverlaySlide.value)
                                    }.drawWithContent {
                                        drawContent()
                                        drawRect(Color.Black, alpha = listScrimAlpha(detailOverlaySlide.value))
                                    }
                            } else {
                                Modifier
                            },
                        )
                        .testTag("list_pane")
                        .nestedScroll(bottomBarScrollConnection),
                    bottomBar = {
                        if (showMainNavigationBar && navEntry != null) {
                            // 页面切换时重置底部导航栏可见状态
                            LaunchedEffect(navEntry) { isBottomBarVisible = true }
                            val currentBottomDestination = mainTabPages
                                .getOrNull(mainPagerState.targetPage)
                                ?.bottomDestination
                            AnimatedVisibility(
                                visible = showMainNavigation && (!autoHideBottomBar || isBottomBarVisible),
                                enter = slideInVertically(tween(200)) { it },
                                exit = slideOutVertically(tween(200)) { it },
                            ) {
                                fun onBottomItemClick(destination: TopLevelDestination, selected: Boolean) {
                                    if (!selected) {
                                        navigateTopLevel(destination)
                                    } else if (tapToScrollToTopEnabled) {
                                        scrollToTopTrigger++
                                    }
                                }
                                if (glassBarProvider != null && glassBarSource != null) {
                                    val selectedIndex = bottomBarItems.indexOfFirst { item ->
                                        currentBottomDestination?.let { it::class == item.destination::class } == true
                                    }
                                    glassBarProvider.Bar(
                                        source = glassBarSource,
                                        tabs = bottomBarItems.map { item ->
                                            GlassBarTab(
                                                label = item.label,
                                                selectedIcon = item.selectedIcon,
                                                unselectedIcon = item.unselectedIcon,
                                                testTag = "nav_tab_${item.destination.name.lowercase()}",
                                            )
                                        },
                                        selectedIndex = selectedIndex,
                                        onSelect = { index ->
                                            bottomBarItems.getOrNull(index)?.let { item ->
                                                onBottomItemClick(item.destination, index == selectedIndex)
                                            }
                                        },
                                        modifier = Modifier,
                                    )
                                } else NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.height(
                                        64.dp + bottomPadding,
                                    ),
                                ) {
                                    @Composable
                                    fun Item(item: BottomBarItem) {
                                        val destination = item.destination
                                        val selected =
                                            currentBottomDestination?.let { it::class == destination::class } == true
                                        val tag = "nav_tab_${destination.name.lowercase()}"
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = {
                                                if (!selected) {
                                                    navigateTopLevel(destination)
                                                } else if (tapToScrollToTopEnabled) {
                                                    scrollToTopTrigger++
                                                }
                                            },
                                            label = { Text(item.label) },
                                            alwaysShowLabel = true,
                                            colors = if (!isDarkTheme) {
                                                NavigationBarItemDefaults.colors().copy(
                                                    selectedIndicatorColor =
                                                        MaterialTheme.colorScheme.secondaryContainer
                                                            .copy(alpha = 0.92f)
                                                            .compositeOver(MaterialTheme.colorScheme.secondary),
                                                )
                                            } else {
                                                NavigationBarItemDefaults.colors()
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) {
                                                        item.selectedIcon
                                                    } else {
                                                        item.unselectedIcon
                                                    },
                                                    contentDescription = item.label,
                                                )
                                            },
                                            modifier = Modifier.padding(top = 4.dp).testTag(tag),
                                        )
                                    }

                                    bottomBarItems.forEach { item ->
                                        Item(item)
                                    }
                                }
                            }
                        }
                    },
                ) { innerPadding ->
                    CompositionLocalProvider(
                        LocalGlassBarSource provides glassBarSource,
                        LocalLifecycleOwner provides listLifecycleOwner,
                        LocalArticleNavController provides navController,
                        LocalNavigator provides Navigator(
                            onNavigate = { destination ->
                                if (
                                    useSecondaryContentNavigation &&
                                    (
                                        destination.isDetailPaneDestination() ||
                                            (showListDetail && currentMainTabDestination == Account && destination.isAccountDetailDestination())
                                    )
                                ) {
                                    openListDetail(destination)
                                } else {
                                    navigate(destination)
                                }
                            },
                            onNavigateBack = navController::popBackStack,
                            onNavigateTopLevel = ::navigateTopLevel,
                        ),
                        LocalSelectedContentDestination provides selectedContentDestination,
                        LocalDetailBackGesture provides detailBackGesture,
                    ) {
                        NavHost(
                            navController,
                            startDestination = MainTabs,
                            enterTransition = {
                                slideInHorizontally(tween(300)) { it }
                            },
                            exitTransition = {
                                listExitTransition()
                            },
                            // 问题/搜索等主栈页面的返回手势由 NavHost 自带的预测返回驱动这两段转场（跟手横滑 + 列表视差）。
                            popEnterTransition = {
                                listPopEnterTransition()
                            },
                            popExitTransition = {
                                detailPopExitTransition()
                            },
                        ) {
                            composable<MainTabs> {
                                MainTabsPager(
                                    pagerState = mainPagerState,
                                    pages = mainTabPages,
                                    scrollToTopTrigger = scrollToTopTrigger,
                                    innerPadding = innerPadding,
                                    showHomeTopActions = showHomeTopActions,
                                )
                            }
                            composable<Login> {
                                LoginScreen(
                                    onLoginComplete = { navController.popBackStack() },
                                    onOpenTelemetrySettings = {
                                        navController.navigate(Account.SystemAndUpdateSettings("allowTelemetry"))
                                    },
                                )
                            }
                            composable<Question> { navEntry ->
                                val question: Question = navEntry.toRoute()
                                QuestionScreen(question)
                            }
                            composable<Topic> { navEntry ->
                                TopicScreen(navEntry.toRoute())
                            }
                            composable<WriteAnswer> { navEntry ->
                                val args: WriteAnswer = navEntry.toRoute()
                                WriteAnswerScreen(args)
                            }
                            composable<WritePin> { navEntry ->
                                WritePinScreen(navEntry.toRoute())
                            }
                            composable<Article>(
                                typeMap = mapOf(typeOf<ArticleType>() to ArticleTypeNavType),
                                enterTransition = articleEnterTransition,
                                exitTransition = articleExitTransition,
                            ) { navEntry ->
                                val article: Article = navEntry.toRoute()
                                // 启用自适应导航时由 effect 转交详情栈，不创建会抢先消费一次性交接数据的临时页面。
                                if (!enableLandscapeListDetail) articleContent(article, navEntry)
                            }
                            composable<HotList> {
                                HotListScreen(innerPadding)
                            }
                            composable<Follow> {
                                FollowScreen(
                                    scrollToTopTrigger = scrollToTopTrigger,
                                    innerPadding = innerPadding,
                                    parentPagerState = mainPagerState,
                                )
                            }
                            composable<Daily> {
                                DailyScreen()
                            }
                            composable<History> {
                                LegacyLocalHistoryScreen(innerPadding)
                            }
                            composable<OnlineHistory> {
                                OnlineHistoryScreen()
                            }
                            composable<Account> {
                                AccountSettingScreen(innerPadding)
                            }
                            composable<Search>(
                                enterTransition = {
                                    if (initialState.destination.hasRoute<Search>()) {
                                        EnterTransition.None
                                    } else {
                                        fadeIn(animationSpec = tween(durationMillis = 240)) +
                                            slideInVertically(animationSpec = tween(durationMillis = 280)) { it / 16 } +
                                            scaleIn(
                                                animationSpec = tween(durationMillis = 280),
                                                initialScale = 0.985f,
                                            )
                                    }
                                },
                                popExitTransition = {
                                    if (targetState.destination.hasRoute<Search>()) {
                                        ExitTransition.None
                                    } else {
                                        detailPopExitTransition()
                                    }
                                },
                            ) { navEntry ->
                                val search: Search = navEntry.toRoute()
                                SearchScreen(search)
                            }
                            composable<Collections> { navEntry ->
                                val data: Collections = navEntry.toRoute()
                                CollectionScreen(
                                    urlToken = data.userToken,
                                    contentPadding = innerPadding,
                                )
                            }
                            composable<CollectionContent> { navEntry ->
                                val content: CollectionContent = navEntry.toRoute()
                                CollectionContentScreen(content.collectionId)
                            }
                            composable<Person> { navEntry ->
                                val person: Person = navEntry.toRoute()
                                PeopleScreen(person)
                            }
                            composable<Pin> { navEntry ->
                                val pin: Pin = navEntry.toRoute()
                                if (!enableLandscapeListDetail) PinScreen(pin)
                            }
                            accountSettings(reloadBottomBarPreferences, blocklistSettingsNlpContent)
                            composable<Notification> {
                                NotificationScreen()
                            }
                            composable<Notification.Entry> { navEntry ->
                                val entry: Notification.Entry = navEntry.toRoute()
                                NotificationTimelineScreen(entry.entryName, entry.title)
                            }
                            composable<Notification.Invitations> {
                                NotificationTimelineScreen("invite", "邀请回答")
                            }
                            composable<Notification.Message> { navEntry ->
                                PrivateMessageScreen(navEntry.toRoute())
                            }
                            composable<Notification.NotificationSettings> { navEntry ->
                                NotificationSettingsScreen(
                                    setting = navEntry.toRoute<Notification.NotificationSettings>().setting,
                                )
                            }
                            composable<SentenceSimilarityTest> {
                                sentenceSimilarityContent()
                            }
                        }
                    }
                }
            }
        }

        if (showListDetail || showDetailPane || detailEntry == null) {
            paneStateHolder.SaveableStateProvider("detail") {
                // 尺寸变化只调整同一宿主的布局；隐藏时保存整个导航子树，独立弹窗也随页面离开组合。
                Row(
                    if (showListDetail || showDetailPane) {
                        Modifier
                            .fillMaxSize()
                            .then(
                                if (isNarrowDetailOverlay) {
                                    Modifier
                                        .testTag(DETAIL_OVERLAY_TAG)
                                        .graphicsLayer { translationX = size.width * detailOverlaySlide.value }
                                        // 详情覆盖在列表上，空白处也不能把点击漏给下层列表。
                                        .pointerInput(Unit) {
                                            awaitPointerEventScope {
                                                while (true) awaitPointerEvent(PointerEventPass.Initial)
                                            }
                                        }
                                } else {
                                    Modifier
                                },
                            )
                    } else {
                        Modifier.size(0.dp)
                    },
                ) {
                    if (showListDetail) {
                        Spacer(Modifier.width(listPaneWidth))
                        ListDetailDivider(
                            onDrag = { deltaPx ->
                                val usableWidthPx = with(density) {
                                    (containerWidth - LIST_DETAIL_DIVIDER_WIDTH).toPx()
                                }
                                if (usableWidthPx > 0f) {
                                    listPaneRatio = (listPaneRatio + deltaPx / usableWidthPx).coerceIn(0f, 1f)
                                }
                            },
                            onAdjustBy = { ratioDelta ->
                                listPaneRatio = (listPaneRatio + ratioDelta).coerceIn(0f, 1f)
                            },
                            onDragStopped = {
                                val normalizedWidth = normalizedListPaneWidth(containerWidth, listPaneRatio)
                                listPaneRatio = normalizedWidth.value /
                                    (containerWidth - LIST_DETAIL_DIVIDER_WIDTH).value
                                settings.putFloat(LANDSCAPE_LIST_PANE_RATIO_KEY, listPaneRatio)
                            },
                        )
                    }
                    DetailPane(Modifier.weight(1f).fillMaxSize())
                }
            }
        }
    }
}

/** 测试入口：按页面 key（home/follow/hotlist/daily/online_history/my_collections/account）渲染主壳 pager。 */
@Composable
fun MainTabsPagerForTest(
    pagerState: PagerState,
    pageKeys: List<String>,
    innerPadding: PaddingValues,
) {
    val all = listOf(
        MainTabPage.HomePage,
        MainTabPage.FollowPage,
        MainTabPage.HotListPage,
        MainTabPage.DailyPage,
        MainTabPage.OnlineHistoryPage,
        MainTabPage.MyCollectionsPage,
        MainTabPage.AccountPage,
    )
    MainTabsPager(pagerState, pageKeys.mapNotNull { k -> all.firstOrNull { it.key == k } }, 0, innerPadding, true)
}

/**
 * 渲染可配置底部导航主壳内的页面。
 *
 * 每个页面都接收主壳给出的 [innerPadding]，保证系统栏、底部栏和子页面之间的留白一致。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainTabsPager(
    pagerState: PagerState,
    pages: List<MainTabPage>,
    scrollToTopTrigger: Int,
    innerPadding: PaddingValues,
    showHomeTopActions: Boolean,
) {
    val glassProvider = LocalGlassBottomBarProvider.current
    val glassSource = LocalGlassBarSource.current
    // 根因修复：以前每个非首页 tab 各自把页面录进同一个底栏 backdrop、首页又在内部单独录制；
    // pager 会预取/保留相邻页，多个节点抢写同一个 layer，底栏常采样到屏幕外或空白页（玻璃“消失”/像不透明）。
    // 现在由这里统一录制整个 pager：底栏看到的永远是屏幕上实际可见的内容（滑动切页时也一致），不随切页增删 modifier。
    // pager 内部不再暴露底栏源（页面内悬浮钮若采样它会把自己录进去），首页自带列表源给自己的悬浮钮用。
    Box(
        if (glassProvider != null && glassSource != null) {
            glassProvider.contentSource(Modifier.fillMaxSize(), glassSource).testTag(MAIN_TABS_GLASS_SOURCE_TAG)
        } else {
            Modifier.fillMaxSize()
        },
    ) {
    CompositionLocalProvider(LocalGlassBarSource provides null) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        pageNestedScrollConnection = NoOpPagerNestedScrollConnection,
    ) { pageIndex ->
        val page = pages.getOrNull(pageIndex) ?: return@HorizontalPager
        Box(Modifier.fillMaxSize().testTag(mainTabPageTag(pageIndex))) {
        when (page) {
            MainTabPage.HomePage -> HomeScreen(
                scrollToTopTrigger = scrollToTopTrigger,
                innerPadding = innerPadding,
                showTopActions = showHomeTopActions,
                isActive = pagerState.currentPage == pageIndex,
            )
            MainTabPage.FollowPage -> FollowScreen(
                scrollToTopTrigger = scrollToTopTrigger,
                innerPadding = innerPadding,
                parentPagerState = pagerState,
                isActive = pagerState.currentPage == pageIndex,
            )
            MainTabPage.HotListPage -> HotListScreen(
                innerPadding = innerPadding,
                scrollToTopTrigger = scrollToTopTrigger,
                isActive = pagerState.currentPage == pageIndex,
            )
            MainTabPage.DailyPage -> DailyScreen(
                scrollToTopTrigger = scrollToTopTrigger,
                bottomPadding = innerPadding.calculateBottomPadding(),
                isActive = pagerState.currentPage == pageIndex,
            )
            MainTabPage.OnlineHistoryPage -> OnlineHistoryScreen(
                scrollToTopTrigger = scrollToTopTrigger,
                bottomPadding = innerPadding.calculateBottomPadding(),
                isActive = pagerState.currentPage == pageIndex,
            )
            MainTabPage.MyCollectionsPage -> MyCollectionsTopLevelPage(
                scrollToTopTrigger = scrollToTopTrigger,
                innerPadding = innerPadding,
                isActive = pagerState.currentPage == pageIndex,
            )
            MainTabPage.AccountPage -> AccountSettingScreen(
                innerPadding = innerPadding,
                isActive = pagerState.currentPage == pageIndex,
            )
        }
        } // Box page
    }
    } // LocalGlassBarSource = null
    } // 底栏玻璃源：整个 pager
}

/** 测试用：底栏玻璃源录制容器（包住整个 tab pager）。 */
const val MAIN_TABS_GLASS_SOURCE_TAG = "main_tabs_glass_source"

/** 测试用：第 [pageIndex] 个 tab 页容器。 */
fun mainTabPageTag(pageIndex: Int): String = "main_tab_page_$pageIndex"

@Composable
private fun MyCollectionsTopLevelPage(
    scrollToTopTrigger: Int,
    innerPadding: PaddingValues,
    isActive: Boolean,
) {
    val account = rememberAccountSettingsAccountState().value
    CollectionScreen(
        urlToken = account.urlToken,
        contentPadding = innerPadding,
        showBackButton = false,
        isActive = isActive,
    )
}

private val TopLevelDestination.openFrom: String?
    get() = when (this) {
        Home -> ContentOpenFrom.HOME_FEED
        OnlineHistory -> ContentOpenFrom.HISTORY
        else -> null
    }

internal fun NavBackStackEntry?.hasRoute(cls: KClass<out NavDestination>): Boolean {
    val dest = this?.destination ?: return false
    return dest.hierarchy.any { it.hasRoute(cls) }
}

private fun NavBackStackEntry?.readingDestinationOrNull(): NavDestination? = when {
    this?.destination?.hasRoute<Article>() == true -> runCatching { toRoute<Article>() }.getOrNull()
    this?.destination?.hasRoute<Pin>() == true -> runCatching { toRoute<Pin>() }.getOrNull()
    this?.destination?.hasRoute<Question>() == true -> runCatching { toRoute<Question>() }.getOrNull()
    else -> null
}

private data class BottomBarItem(
    val destination: TopLevelDestination,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private fun NavDestination.isAccountDetailDestination(): Boolean = when (this) {
    is Account.AppearanceSettings,
    Account.ThemePreview,
    is Account.RecommendSettings,
    Account.RecommendSettings.Blocklist,
    Account.RecommendSettings.BlockedFeedHistory,
    Account.IdentityManagement,
    is Account.SystemAndUpdateSettings,
    Account.SettingsSearch,
    Account.OpenSourceLicenses,
    Account.DeveloperSettings,
    Account.DeveloperSettings.ColorScheme,
    -> true
    else -> false
}

private fun NavBackStackEntry?.isListPaneDestination(): Boolean = when {
    this == null -> false
    destination.hasRoute<MainTabs>() -> true
    destination.hasRoute<Search>() -> true
    destination.hasRoute<History>() -> true
    destination.hasRoute<Collections>() -> true
    destination.hasRoute<CollectionContent>() -> true
    destination.hasRoute<Question>() -> true
    destination.hasRoute<Person>() -> true
    destination.hasRoute<Topic>() -> true
    destination.hasRoute<Notification>() -> true
    destination.hasRoute<Notification.Entry>() -> true
    destination.hasRoute<Notification.Invitations>() -> true
    destination.hasRoute<Account.RecommendSettings.BlockedFeedHistory>() -> true
    else -> false
}

private fun NavGraphBuilder.accountSettings(
    reloadPreferences: () -> Unit,
    nlpContent: @Composable (onNavigateBack: () -> Unit) -> Unit,
) {
    composable<Account.RecommendSettings.Blocklist> {
        BlocklistSettingsScreen(nlpContent)
    }
    composable<Account.RecommendSettings.BlockedFeedHistory> {
        BlockedFeedHistoryScreen()
    }
    composable<Account.AppearanceSettings> { entry ->
        val args = entry.toRoute<Account.AppearanceSettings>()
        AppearanceSettingsScreen(
            setting = args.setting,
            onExit = reloadPreferences,
        )
    }
    composable<Account.ThemePreview> {
        ThemePreviewScreen()
    }
    composable<Account.RecommendSettings> { entry ->
        val args = entry.toRoute<Account.RecommendSettings>()
        ContentFilterSettingsScreen(args.setting)
    }
    composable<Account.IdentityManagement> {
        IdentityManagementScreen()
    }
    composable<Account.SystemAndUpdateSettings> { entry ->
        SystemAndUpdateSettingsScreen(
            setting = entry.toRoute<Account.SystemAndUpdateSettings>().setting,
        )
    }
    composable<Account.SettingsSearch> {
        SettingsSearchScreen()
    }
    composable<Account.OpenSourceLicenses> {
        OpenSourceLicensesScreen()
    }
    composable<Account.DeveloperSettings> {
        DeveloperSettingsScreen()
    }
    composable<Account.DeveloperSettings.ColorScheme> {
        ColorSchemeScreen()
    }
}


/** 列表 → 详情：列表向左轻微视差滑开，不缩放。 */
fun listExitTransition(): ExitTransition =
    slideOutHorizontally(tween(300)) { -(it * LIST_PARALLAX_FRACTION).toInt() }

/**
 * 返回时列表从视差位置滑回原位。用线性曲线：预测返回会按手势进度定位到转场的对应时刻，
 * 线性才能让页面位置和手指位置一一对应。
 */
fun listPopEnterTransition(): EnterTransition =
    slideInHorizontally(tween(300, easing = LinearEasing)) { -(it * LIST_PARALLAX_FRACTION).toInt() }

/** 返回时详情页向右整页滑出（线性，原因同上）。 */
fun detailPopExitTransition(): ExitTransition =
    slideOutHorizontally(tween(300, easing = LinearEasing)) { it }

/** 详情页返回手势的回调，阅读页内部的返回拦截（如沉浸式）可以借此让手势继续驱动页面缩小。 */
class DetailBackGesture(
    val onProgress: (Float) -> Unit,
    val onCancel: () -> Unit,
    val onBack: () -> Unit,
)

/** 只有开启「预测性返回」时才有值。 */
val LocalDetailBackGesture = staticCompositionLocalOf<DetailBackGesture?> { null }

const val DETAIL_OVERLAY_TAG = "detail_overlay"

/** 下层列表的视差距离（相对宽度）。 */
const val LIST_PARALLAX_FRACTION = 0.3f

/** 预测性返回：手势进度 0→1 对应详情页向右位移 0→整页宽度，不缩放。 */
fun predictiveBackTranslationFraction(progress: Float): Float = progress.coerceIn(0f, 1f)

/** 详情位移 0→1 时，下层列表从 -0.3 宽度滑回 0。 */
fun listParallaxFraction(detailSlide: Float): Float = -LIST_PARALLAX_FRACTION * (1f - detailSlide.coerceIn(0f, 1f))

/** 下层列表上的浅色遮罩，详情完全展示时最深，滑出过程中淡掉。 */
fun listScrimAlpha(detailSlide: Float): Float = 0.18f * (1f - detailSlide.coerceIn(0f, 1f))
