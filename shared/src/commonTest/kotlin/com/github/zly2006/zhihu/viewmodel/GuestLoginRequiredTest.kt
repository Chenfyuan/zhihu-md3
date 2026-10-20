package com.github.zly2006.zhihu.viewmodel

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.viewmodel.comment.RootCommentViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HotListViewModel
import com.github.zly2006.zhihu.viewmodel.feed.SearchViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 游客模式：知乎对匿名请求返回 401/403 时，各页面的 ViewModel 必须标记 loginRequired（界面显示「登录后可看」），
 * 而不是把错误体当成正常数据、导致界面无限转圈。
 */
class GuestLoginRequiredTest {
    /** 真实抓包：未登录搜索 → 403，code 40353，need_login，并在响应体里给出 /account/unhuman 跳转。 */
    private val searchNeedLogin403 =
        """{"error":{"code":40353,"message":"请登录后查看","need_login":true,"redirect":"https://www.zhihu.com/account/unhuman?type=unhuman&message=need_login&need_login=true"}}"""
    private val unauthorized401 = """{"error":{"code":100,"message":"请求需要登录","name":"AuthenticationInvalidRequest"}}"""
    private val serverError500 = """{"error":{"code":10003,"message":"服务器开小差了"}}"""

    private fun environment(status: HttpStatusCode, body: String) = object : PaginationEnvironment {
        override fun httpClient() = HttpClient(
            MockEngine {
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        )

        override fun authenticatedCookies() = emptyMap<String, String>()

        override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
    }

    private class Search : SearchViewModel("Material 3") {
        // 与 loadMore() 相同的错误处理路径（不依赖 Main 调度器）
        suspend fun fetch(env: PaginationEnvironment) = try { fetchFeeds(env) } catch (e: Exception) { errorHandle(e) }
    }

    private class Hot : HotListViewModel() {
        // 与 loadMore() 相同的错误处理路径（不依赖 Main 调度器）
        suspend fun fetch(env: PaginationEnvironment) = try { fetchFeeds(env) } catch (e: Exception) { errorHandle(e) }
    }

    private class Comments : RootCommentViewModel(Article(type = ArticleType.Answer, id = 1L)) {
        // 与 loadMore() 相同的错误处理路径（不依赖 Main 调度器）
        suspend fun fetch(env: PaginationEnvironment) = try { fetchFeeds(env) } catch (e: Exception) { errorHandle(e) }
    }

    private fun check(status: HttpStatusCode, body: String, expectLogin: Boolean) = runBlocking {
        val env = environment(status, body)
        val results = mapOf(
            "search" to Search().also { it.fetch(env) },
            "hotlist" to Hot().also { it.fetch(env) },
            "comments" to Comments().also { it.fetch(env) },
        )
        results.forEach { (page, vm) ->
            assertNotNull(vm.errorMessage, "$page 应有错误信息（$status）")
            assertFalse(vm.isLoading, "$page 不应继续转圈（$status）")
            if (expectLogin) {
                assertTrue(vm.loginRequired, "$page 应显示登录后可看（$status）")
            } else {
                assertFalse(vm.loginRequired, "$page 不应误判为需要登录（$status）")
            }
        }
    }

    @Test
    fun forbidden403WithNeedLoginRedirectMarksLoginRequired() = check(HttpStatusCode.Forbidden, searchNeedLogin403, true)

    @Test
    fun unauthorized401MarksLoginRequired() = check(HttpStatusCode.Unauthorized, unauthorized401, true)

    @Test
    fun otherErrorsShowNormalErrorNotLoginPrompt() = check(HttpStatusCode.InternalServerError, serverError500, false)
}
