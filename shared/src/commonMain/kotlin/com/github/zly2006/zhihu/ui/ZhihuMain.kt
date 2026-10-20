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
import androidx.compose.ui.graphics.TransformOrigin
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
    val duo3HomeAccount = preferenceState.duo3HomeAccount
    val tapToScrollToTopEnabled = preferenceState.tapToScrollToTopEnabled
    val autoHideBottomBar = preferenceState.autoHideBottomBar
    val collectionDirectBrowseEnabled = preferenceState.collectionDirectBrowseEnabled
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
    // 开启时详情页（问题/回答/搜索结果打开的详情等）跟手缩小+轻微右移，松手后再出栈；关闭时保持普通返回。
    val predictiveBackEnabled by rememberObservedSetting(settings, "enable_predictive_back") { getBoolean("enable_predictive_back", true) }
    val detailBackProgress = remember { Animatable(0f) }
    val detailBackScope = rememberCoroutineScope()
    val mainDetailBackEnabled = navEntry != null && !showMainNavigation && !showDetailPane

    // 窄屏时阅读页（回答/文章/想法）由详情栈承载并覆盖在列表之上。详情栈只剩这一页时，
    // 出栈会让整个详情层消失，NavHost 自己的出场动画来不及播放，所以这里先把详情层滑出再出栈。
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

    fun popDetail() {
        val leavesOverlay = isNarrowDetailOverlay &&
            detailNavController.previousBackStackEntry?.destination?.hasRoute<EmptyDetail>() == true
        if (!leavesOverlay) {
            detailNavController.popBackStack()
            detailBackScope.launch { detailBackProgress.snapTo(0f) }
            return
        }
        if (detailOverlayExiting) return
        detailOverlayExiting = true
        detailBackScope.launch {
            detailOverlaySlide.animateTo(1f, tween(250))
            detailNavController.popBackStack()
            detailBackProgress.snapTo(0f)
        }
    }

    val detailBackGesture = if (predictiveBackEnabled) {
        DetailBackGesture(
            onProgress = { p -> detailBackScope.launch { detailBackProgress.snapTo(p) } },
            onCancel = { detailBackScope.launch { detailBackProgress.animateTo(0f, tween(180)) } },
            onBack = {
                if (showDetailPane) {
                    popDetail()
                } else {
                    navController.popBackStack()
                    detailBackScope.launch { detailBackProgress.snapTo(0f) }
                }
            },
        )
    } else {
        null
    }

    /**
     * 必须放在对应 NavHost 之后组合：NavHost 自己也注册了返回回调（返回栈多于一页时启用），
     * 后注册的回调优先。以前这里写在 NavHost 之前，手势被 NavHost 抢走，页面缩小从未生效；
     * 阅读页又在详情栈里，NavHost 出栈后整层直接消失，看起来就是没有返回动画。
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
            PlatformBackHandler(enabled = enabled) {
                if (showDetailPane) popDetail() else navController.popBackStack()
            }
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
                    .predictiveBackLayer { detailBackProgress.value }
                    .testTag("detail_pane")
                    .background(MaterialTheme.colorScheme.background),
                enterTransition = { slideInHorizontally(tween(300)) { it } },
                exitTransition = { listExitTransition() },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
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
            DetailBackHandler(enabled = showDetailPane)
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
                            modifier = Modifier.predictiveBackLayer { if (mainDetailBackEnabled) detailBackProgress.value else 0f },
                            startDestination = MainTabs,
                            enterTransition = {
                                slideInHorizontally(tween(300)) { it }
                            },
                            exitTransition = {
                                listExitTransition()
                            },
                            popEnterTransition = {
                                EnterTransition.None
                            },
                            popExitTransition = {
                                slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300))
                            },
                        ) {
                            composable<MainTabs> {
                                MainTabsPager(
                                    pagerState = mainPagerState,
                                    pages = mainTabPages,
                                    scrollToTopTrigger = scrollToTopTrigger,
                                    innerPadding = innerPadding,
                                    collectionDirectBrowseEnabled = collectionDirectBrowseEnabled,
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
                                        fadeOut(animationSpec = tween(durationMillis = 180)) +
                                            slideOutVertically(animationSpec = tween(durationMillis = 220)) { it / 20 } +
                                            scaleOut(
                                                animationSpec = tween(durationMillis = 220),
                                                targetScale = 0.985f,
                                            )
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
                        DetailBackHandler(enabled = mainDetailBackEnabled)
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
    collectionDirectBrowseEnabled: Boolean,
    showHomeTopActions: Boolean,
) {
    val glassProvider = LocalGlassBottomBarProvider.current
    val glassSource = LocalGlassBarSource.current
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        pageNestedScrollConnection = NoOpPagerNestedScrollConnection,
    ) { pageIndex ->
        val page = pages.getOrNull(pageIndex) ?: return@HorizontalPager
        val pageModifier =
            if (glassProvider != null && glassSource != null && page != MainTabPage.HomePage) {
                glassProvider.contentSource(Modifier.fillMaxSize(), glassSource)
            } else {
                Modifier.fillMaxSize()
            }
        Box(pageModifier) {
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
                isActive = pagerState.currentPage == pageIndex,
            )
            MainTabPage.OnlineHistoryPage -> OnlineHistoryScreen(
                scrollToTopTrigger = scrollToTopTrigger,
                isActive = pagerState.currentPage == pageIndex,
            )
            MainTabPage.MyCollectionsPage -> MyCollectionsTopLevelPage(
                scrollToTopTrigger = scrollToTopTrigger,
                innerPadding = innerPadding,
                collectionDirectBrowseEnabled = collectionDirectBrowseEnabled,
                isActive = pagerState.currentPage == pageIndex,
            )
            MainTabPage.AccountPage -> AccountSettingScreen(
                innerPadding = innerPadding,
                isActive = pagerState.currentPage == pageIndex,
            )
        }
        } // Box contentSource / fillMaxSize
    }
}

@Composable
private fun MyCollectionsTopLevelPage(
    scrollToTopTrigger: Int,
    innerPadding: PaddingValues,
    collectionDirectBrowseEnabled: Boolean,
    isActive: Boolean,
) {
    val account = rememberAccountSettingsAccountState().value
    if (collectionDirectBrowseEnabled) {
        CollectionBrowseScreen(
            urlToken = account.urlToken,
            contentPadding = innerPadding,
            showBackButton = false,
            scrollToTopTrigger = scrollToTopTrigger,
            isActive = isActive,
        )
    } else {
        CollectionScreen(
            urlToken = account.urlToken,
            contentPadding = innerPadding,
            showBackButton = false,
            isActive = isActive,
        )
    }
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


/** 列表 → 详情：列表轻微缩小 + 淡出，避免原先 ExitTransition.None 的突兀感。 */
fun listExitTransition(): ExitTransition =
    fadeOut(tween(durationMillis = 220), targetAlpha = 0.6f) +
        scaleOut(tween(durationMillis = 300), targetScale = 0.94f)

/** 详情页返回手势的回调，阅读页内部的返回拦截（如沉浸式）可以借此让手势继续驱动页面缩小。 */
class DetailBackGesture(
    val onProgress: (Float) -> Unit,
    val onCancel: () -> Unit,
    val onBack: () -> Unit,
)

/** 只有开启「预测性返回」时才有值。 */
val LocalDetailBackGesture = staticCompositionLocalOf<DetailBackGesture?> { null }

const val DETAIL_OVERLAY_TAG = "detail_overlay"

/** 预测性返回：进度 0→1 时缩放到 1→0.9 */
fun predictiveBackScale(progress: Float): Float = 1f - 0.1f * progress.coerceIn(0f, 1f)

/** 预测性返回：进度 0→1 时水平位移比例（相对宽度） */
fun predictiveBackTranslationFraction(progress: Float): Float = 0.06f * progress.coerceIn(0f, 1f)

/** 预测性返回手势进度 0..1 → 页面缩小到 90%、右移 */
fun Modifier.predictiveBackLayer(progress: () -> Float): Modifier = graphicsLayer {
    val p = progress().coerceIn(0f, 1f)
    if (p > 0f) {
        val s = predictiveBackScale(p)
        scaleX = s
        scaleY = s
        translationX = size.width * predictiveBackTranslationFraction(p)
        transformOrigin = TransformOrigin(1f, 0.5f)
    }
}
