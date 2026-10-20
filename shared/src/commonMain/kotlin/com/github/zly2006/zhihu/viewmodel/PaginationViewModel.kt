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

package com.github.zly2006.zhihu.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.AigcVoteVoter
import com.github.zly2006.zhihu.data.ContentDetailCache
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.data.OnlineHistoryDeletePair
import com.github.zly2006.zhihu.data.QualityFilterSettings
import com.github.zly2006.zhihu.data.ZhihuJson.decodeJson
import com.github.zly2006.zhihu.data.ZhihuPaging
import com.github.zly2006.zhihu.data.executeZhihuAuthenticatedRequest
import com.github.zly2006.zhihu.data.fetchZhihuAuthenticatedJson
import com.github.zly2006.zhihu.data.fetchZhihuContentDetail
import com.github.zly2006.zhihu.data.getOrFetchContentDetail
import com.github.zly2006.zhihu.navigation.AnswerNavigator
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.platform.platformName
import com.github.zly2006.zhihu.ui.ArticleAnswerSwitchState
import com.github.zly2006.zhihu.ui.ArticleAnswerTransitionDirection
import com.github.zly2006.zhihu.util.Log
import com.github.zly2006.zhihu.util.ZhihuCredentialRefresher
import com.github.zly2006.zhihu.util.signZhihuFetchRequest
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent
import com.github.zly2006.zhihu.viewmodel.local.LocalRecommendationEngine
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import kotlin.reflect.KType

abstract class PaginationViewModel<T : Any>(
    val dataType: KType,
) : ViewModel() {
    val allData = mutableStateListOf<T>()
    val debugData: MutableList<JsonElement> = mutableListOf()
    var isLoading: Boolean by mutableStateOf(false)
        protected set
    var errorMessage: String? = null
        protected set
    /** 服务器以 401/403 拒绝了匿名请求：界面显示「登录后可看」，并停止自动加载，避免无限转圈。 */
    var loginRequired: Boolean by mutableStateOf(false)
    var rateLimited: Boolean by mutableStateOf(false)
        protected set
    var allowGuestAccess = false
    protected var lastPaging: ZhihuPaging? by mutableStateOf(null)
    open val isEnd: Boolean get() = lastPaging?.isEnd == true
    protected abstract val initialUrl: String
    private var currentJob: Job? = null
    protected open val shouldLogDecodeFailures: Boolean = true

    protected open fun resolvePageUrl(): String = lastPaging?.next ?: initialUrl

    /**
     * Generally used fields to include in the API request.
     * This can be overridden in subclasses to include more specific fields.
     */
    open val include = "data[*].content,excerpt,headline,target.author.badge_v2"

    open fun refresh(environment: PaginationEnvironment) {
        currentJob?.cancel()
        currentJob = null
        isLoading = false
        errorMessage = null
        loginRequired = false
        rateLimited = false
        debugData.clear()
        allData.clear()
        lastPaging = null // 重置 lastPaging
        loadMore(environment)
    }

    protected open fun handlePageMetadata(json: JsonObject) = Unit

    protected open fun processResponse(environment: PaginationEnvironment, data: List<T>, rawData: JsonArray) {
        debugData.addAll(rawData) // 保存原始JSON
        allData.addAll(data) // 保存未flatten的数据
    }

    protected open fun decodePage(
        environment: PaginationEnvironment,
        rawData: JsonArray,
    ): List<T> = rawData.mapNotNull {
        if ("type" in it.jsonObject &&
            it.jsonObject["type"]?.jsonPrimitive?.content in listOf(
                "invited_answer",
                "tab_list",
                "feed_item_index_group",
            )
        ) {
            return@mapNotNull null
        }
        try {
            @Suppress("UNCHECKED_CAST")
            decodeJson(serializer(dataType) as KSerializer<T>, it)
        } catch (e: Exception) {
            if (shouldLogDecodeFailures) {
                environment.logDecodeFailure(this::class.simpleName, it, e)
            }
            null
        }
    }

    protected open suspend fun fetchFeeds(environment: PaginationEnvironment) {
        try {
            val url = resolvePageUrl()

            @Suppress("HttpUrlsUsage")
            val json = environment.fetchJson(url.replace("http://", "https://"), include)
                ?: throw RuntimeException("您可能已被风控，请重新登录。", Exception("cause: not json object."))

            val jsonArray = json["data"] as? JsonArray
                ?: throw RuntimeException("您可能已被风控，请重新登录。", Exception("cause: no $.data"))
            processResponse(environment, decodePage(environment, jsonArray), jsonArray)
            if ("paging" in json) {
                lastPaging = decodeJson(json["paging"]!!)
            }
            handlePageMetadata(json)
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (_: NoTransformationFoundException) {
            throw RuntimeException("您可能已被风控，请重新登录。")
        } catch (e: Exception) {
            environment.handleFetchFailure(this::class.simpleName, e)
            errorHandle(e)
        } finally {
            isLoading = false
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    open fun loadMore(environment: PaginationEnvironment) {
        if (isLoading || isEnd || loginRequired) return // 使用新的isEnd getter
        rateLimited = false
        isLoading = true
        currentJob = viewModelScope.launch {
            try {
                fetchFeeds(environment)
            } catch (e: Exception) {
                errorHandle(e)
            }
        }
    }

    /** 与 loadMore() 相同的取数 + 错误处理路径，但不依赖 Main 调度器；仅供测试。 */
    internal suspend fun fetchOnceForTest(environment: PaginationEnvironment) {
        rateLimited = false
        try { fetchFeeds(environment) } catch (e: Exception) { errorHandle(e) }
    }

    protected fun errorHandle(e: Exception) {
        if (e !is CancellationException) {
            when (classifyAccessError(e)) {
                AccessErrorKind.LoginRequired -> loginRequired = true
                AccessErrorKind.RateLimited -> rateLimited = true
                null -> Unit
            }
            errorMessage = e.message
            isLoading = false
        }
    }
}

open class ArticleAnswerSwitchData :
    ViewModel(),
    ArticleAnswerSwitchState {
    /** 活跃的导航器：管理来源、历史记录和预取 */
    override var navigator: AnswerNavigator? by mutableStateOf(null)

    /**
     * 导航前由来源界面设置（如 CollectionContentScreen）。
     * [reset] 时会将其应用到 [navigator]。
     */
    override var pendingNavigator: AnswerNavigator? = null

    // 用于消除切换闪动：导航前设置，新页面用它初始化
    override var pendingInitialContent: CachedAnswerContent? = null

    // 标记是否从回答切换导航进入（避免被 LaunchedEffect 重置方向后误判）
    @kotlin.concurrent.Volatile
    override var navigatingFromAnswerSwitch = false

    // 由 DisposableEffect.onDispose 消费，不受 LaunchedEffect 时序影响
    override var answerSwitchDisposeInProgress = false

    // 导航动画方向
    override var answerTransitionDirection = ArticleAnswerTransitionDirection.DEFAULT

    // 沉浸式阅读模式
    override var isImmersiveMode by mutableStateOf(false)

    override fun reset() {
        navigator = pendingNavigator
        pendingNavigator = null
        pendingInitialContent = null
        navigatingFromAnswerSwitch = false
        isImmersiveMode = false
    }

    override fun promoteForNavigation(direction: ArticleAnswerTransitionDirection) = Unit
}

val sharedArticleAnswerSwitchState = ArticleAnswerSwitchData()

interface PreparedArticleExportContent

interface ArticleImageExportRenderer {
    suspend fun prepareExportWebView(htmlContent: String, timeoutMs: Long): PreparedArticleExportContent

    suspend fun captureExportBitmap(preparedWebView: PreparedArticleExportContent): Any

    suspend fun destroyExportWebView(preparedWebView: PreparedArticleExportContent)

    fun recycleExportBitmap(bitmap: Any)
}

interface ZhihuApiEnvironment {
    fun httpClient(): HttpClient

    fun authenticatedCookies(): Map<String, String>

    suspend fun <T> withAuthenticatedClient(
        block: suspend (client: HttpClient, cookies: Map<String, String>) -> T,
    ): T = block(httpClient(), authenticatedCookies())

    suspend fun fetchJson(
        url: String,
        include: String,
    ): JsonObject? = withAuthenticatedClient { client, cookies ->
        fetchZhihuAuthenticatedJson(client, url) {
            method = HttpMethod.Get
            url {
                protocol = URLProtocol.HTTPS
                if (include.isNotEmpty()) {
                    parameters["include"] = include
                }
            }
            signZhihuFetchRequest(cookies)
        }
    }

    suspend fun signedGetText(url: String): String = withAuthenticatedClient { client, cookies ->
        executeZhihuAuthenticatedRequest(client, url) {
            method = HttpMethod.Get
            signZhihuFetchRequest(cookies)
        }.bodyAsText()
    }

    suspend fun refreshToken() {
        val client = httpClient()
        ZhihuCredentialRefresher.refreshZhihuToken(
            ZhihuCredentialRefresher.fetchRefreshToken(client),
            client,
        )
    }

    suspend fun handleFetchFailure(
        tag: String?,
        error: Exception,
    )

    fun xsrfToken(): String = ""

    fun logDecodeFailure(
        tag: String?,
        item: JsonElement,
        error: Exception,
    ) {
        Log.e(tag ?: "PaginationViewModel", "Failed to decode item: $item", error)
    }
}

suspend fun ZhihuApiEnvironment.fetchContentDetail(destination: NavDestination): DataHolder.Content? =
    runCatching {
        fetchZhihuContentDetail(destination) { url, include ->
            fetchJson(url, include)
        }
    }.getOrElse { error ->
        if (error is CancellationException) throw error
        Log.e("ZhihuApiEnvironment", "Failed to fetch content detail for $destination", error)
        null
    }

suspend fun ZhihuApiEnvironment.getOrFetchContentDetail(destination: NavDestination): DataHolder.Content? =
    runCatching {
        ContentDetailCache.getOrFetchContentDetail(destination) { url, include ->
            fetchJson(url, include)
        }
    }.getOrElse { error ->
        if (error is CancellationException) throw error
        Log.e("ZhihuApiEnvironment", "Failed to fetch content detail for $destination", error)
        null
    }

suspend fun ZhihuApiEnvironment.addReadHistory(
    contentToken: String,
    contentTypeName: String,
) {
    if (authenticatedCookies()["d_c0"] == null) return
    runCatching {
        postSigned("https://www.zhihu.com/api/v4/read_history/add") {
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("content_token", contentToken)
                    put("content_type", contentTypeName)
                }.toString(),
            )
        }
    }
}

internal suspend fun ZhihuApiEnvironment.deleteOnlineHistoryItem(item: OnlineHistoryDeletePair) {
    val response = postSigned("https://api.zhihu.com/read_history/batch_del") {
        contentType(ContentType.Application.Json)
        setBody(
            buildJsonObject {
                put(
                    "pairs",
                    JsonArray(
                        listOf(
                            buildJsonObject {
                                put("content_token", item.contentToken)
                                put("content_type", item.contentType)
                            },
                        ),
                    ),
                )
                put("clear", false)
            }.toString(),
        )
    }
    check(response.status.isSuccess()) { "删除在线历史记录失败: ${response.status}" }
}

suspend fun ZhihuApiEnvironment.postSigned(
    url: String,
    block: HttpRequestBuilder.() -> Unit = {},
): HttpResponse = withAuthenticatedClient { client, cookies ->
    client.post(url) {
        block()
        signZhihuFetchRequest(cookies)
    }
}

suspend fun ZhihuApiEnvironment.deleteSigned(
    url: String,
    block: HttpRequestBuilder.() -> Unit = {},
): HttpResponse = withAuthenticatedClient { client, cookies ->
    client.delete(url) {
        block()
        signZhihuFetchRequest(cookies)
    }
}

interface MobileHomeFeedEnvironment : ZhihuApiEnvironment {
    fun mobileHomeFeedHttpClient(): HttpClient = httpClient()

    suspend fun handleMobileHomeFeedFailure(error: Exception) {
        handleFetchFailure("AndroidHomeFeedViewModel", error)
    }
}

interface FeedDisplayEnvironment {
    fun feedDisplaySettings(): FeedDisplaySettings = FeedDisplaySettings()

    suspend fun applyForegroundHomeFeedFilter(items: List<FeedDisplayItem>): List<FeedDisplayItem> = items

    suspend fun applyBackgroundHomeFeedFilter(items: List<FeedDisplayItem>): List<FeedDisplayItem> = items
}

interface HistoryEnvironment {
    fun localHistory(): List<NavDestination> = emptyList()

    suspend fun clearAllHistory() = Unit

    suspend fun postHistoryDestination(destination: NavDestination) = Unit
}

interface ContentInteractionEnvironment : ZhihuApiEnvironment {
    suspend fun recordContentInteraction(feed: Feed) = Unit
}

interface ContentOpenEnvironment {
    suspend fun recordContentOpenEvent(
        destination: NavDestination,
        questionId: Long? = null,
        openFrom: String = "",
    ) = Unit

    suspend fun recordOpenEvent(
        destination: Article,
        questionId: Long?,
    ) = Unit
}

interface AigcVoteEnvironment {
    fun isAigcVoteEnabled(): Boolean = false

    fun aigcVoteHttpClient(): HttpClient = error("AIGC 内容标记客户端不可用")

    fun aigcVoteBaseUrl(): String = ""

    fun aigcVoteClientId(): String = ""

    fun aigcVoteVoter(): AigcVoteVoter? = null
}

interface ContentBlocklistEnvironment {
    suspend fun isUserBlocked(userId: String): Boolean = false

    suspend fun isQuestionAuthorBlocked(userId: String): Boolean = false

    fun blockedUserIds(): Set<String> = emptySet()

    suspend fun addBlockedUser(
        userId: String,
        userName: String,
        urlToken: String? = null,
        avatarUrl: String? = null,
    ) = Unit

    suspend fun addBlockedQuestionAuthor(
        userId: String,
        userName: String,
        urlToken: String? = null,
        avatarUrl: String? = null,
    ) = Unit

    suspend fun removeBlockedUser(userId: String) = Unit

    suspend fun removeBlockedQuestionAuthor(userId: String) = Unit
}

interface LocalRecommendationEnvironment : ZhihuApiEnvironment {
    fun localRecommendationEngine(): LocalRecommendationEngine? = null

    suspend fun handleLocalRecommendationFailure(error: Exception) {
        handleFetchFailure("LocalHomeFeedViewModel", error)
    }

    suspend fun showLocalRecommendationDatabaseError() = Unit
}

interface ClipboardEnvironment {
    fun setPlainTextClipboard(
        label: String,
        text: String,
    ) = Unit
}

interface ArticleExportEnvironment {
    fun hasImageExportPermission(): Boolean = false

    fun requiresHtmlExportPermission(): Boolean = false

    fun requestImageExportPermission() = Unit

    fun loadExportAssetText(fileName: String): String = ""

    fun buildArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        extraSectionsHtml: String,
    ): String = ""

    suspend fun buildOfflineArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        httpClient: HttpClient,
    ): String = ""

    fun saveHtmlToDownloads(
        displayName: String,
        htmlContent: String,
    ): String = ""

    fun saveImageToMediaStore(
        displayName: String,
        bitmap: Any,
    ) = Unit

    fun articleImageExportRenderer(): ArticleImageExportRenderer =
        error("$platformName 暂不支持文章图片导出")
}

interface ArticleExportContentEnvironment :
    ArticleExportEnvironment,
    ZhihuApiEnvironment

interface ContentLoadEnvironment :
    ZhihuApiEnvironment,
    HistoryEnvironment,
    ContentOpenEnvironment,
    AigcVoteEnvironment

interface ProfileLoadEnvironment :
    ContentLoadEnvironment,
    ContentBlocklistEnvironment

interface ArticleLoadEnvironment :
    ZhihuApiEnvironment,
    ContentLoadEnvironment

interface PaginationEnvironment :
    ZhihuApiEnvironment,
    MobileHomeFeedEnvironment,
    FeedDisplayEnvironment,
    ContentInteractionEnvironment,
    LocalRecommendationEnvironment,
    ClipboardEnvironment,
    ProfileLoadEnvironment,
    ArticleLoadEnvironment,
    ArticleExportContentEnvironment

data class FeedDisplaySettings(
    val qualityFilterMode: QualityFilterMode = QualityFilterMode.RULES,
    val qualityFilter: QualityFilterSettings = QualityFilterSettings(),
    val reverseBlock: Boolean = false,
)

enum class QualityFilterMode {
    OFF,
    RULES,
    HIDE,
}

const val QUALITY_FILTER_MODE_PREFERENCE_KEY = "qualityFilterMode"
const val ANSWER_VOTEUP_THRESHOLD_PREFERENCE_KEY = "answerVoteupThreshold"
const val ARTICLE_VOTEUP_THRESHOLD_PREFERENCE_KEY = "articleVoteupThreshold"
const val ARTICLE_FOLLOWERS_THRESHOLD_PREFERENCE_KEY = "articleFollowersThreshold"
const val VIDEO_VOTE_THRESHOLD_PREFERENCE_KEY = "videoVoteThreshold"
const val VIDEO_FOLLOWERS_THRESHOLD_PREFERENCE_KEY = "videoFollowersThreshold"
const val QUESTION_ANSWER_THRESHOLD_PREFERENCE_KEY = "questionAnswerThreshold"
const val QUESTION_FOLLOWERS_THRESHOLD_PREFERENCE_KEY = "questionFollowersThreshold"

data class HomeFeedFilterResult(
    val foregroundItems: List<FeedDisplayItem>,
    val filteredItems: List<FeedDisplayItem>,
    val reverseBlock: Boolean,
)

@Composable
expect fun rememberPaginationEnvironment(allowGuestAccess: Boolean): PaginationEnvironment

/** 匿名/受限访问的错误分类：只做提示，不做任何签名或反爬绕过。 */
enum class AccessErrorKind {
    /** 401，或 403 且 need_login=true / code 40353：显示「登录后可看」+ 登录按钮 */
    LoginRequired,

    /** code 40362 / 10003：请求被限制，显示「请求被限制，稍后重试」+ 重试，不显示登录按钮 */
    RateLimited,
}

private val RATE_LIMITED_CODES = setOf(40362, 10003)
private const val NEED_LOGIN_CODE = 40353

fun classifyAccessError(e: Throwable?): AccessErrorKind? {
    var cur = e
    while (cur != null) {
        if (cur is com.github.zly2006.zhihu.util.HttpStatusException) {
            val error = runCatching {
                kotlinx.serialization.json.Json.parseToJsonElement(cur.bodyText)
                    .let { it as? JsonObject }?.get("error") as? JsonObject
            }.getOrNull()
            val code = (error?.get("code") as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull()
            val needLogin = (error?.get("need_login") as? kotlinx.serialization.json.JsonPrimitive)?.content == "true"
            return when {
                code in RATE_LIMITED_CODES -> AccessErrorKind.RateLimited
                cur.status.value == 401 -> AccessErrorKind.LoginRequired
                cur.status.value == 403 && (needLogin || code == NEED_LOGIN_CODE) -> AccessErrorKind.LoginRequired
                else -> null
            }
        }
        cur = cur.cause
    }
    return null
}

fun isLoginRequiredError(e: Throwable?): Boolean = classifyAccessError(e) == AccessErrorKind.LoginRequired

fun isRateLimitedError(e: Throwable?): Boolean = classifyAccessError(e) == AccessErrorKind.RateLimited

/** 与 [fetchContentDetail] 相同，但不吞异常，用于需要区分「登录后可看 / 请求被限制」的详情页。 */
suspend fun ZhihuApiEnvironment.fetchContentDetailOrThrow(destination: NavDestination): DataHolder.Content? =
    fetchZhihuContentDetail(destination) { url, include -> fetchJson(url, include) }
