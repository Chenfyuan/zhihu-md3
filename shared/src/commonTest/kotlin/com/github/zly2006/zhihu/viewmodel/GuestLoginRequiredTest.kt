package com.github.zly2006.zhihu.viewmodel

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.viewmodel.comment.RootCommentViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HotListViewModel
import com.github.zly2006.zhihu.viewmodel.feed.QuestionFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.SearchViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * 游客模式错误分流（只做提示，不做任何签名/反爬绕过）：
 * - 401、403 + need_login / 40353 → 「登录后可看」
 * - 40362、10003 → 「请求被限制，稍后重试」+ 重试，不显示登录
 * - 其它（如 500 无已知 code）→ 普通错误
 * 每个页面都必须结束加载，不能无限转圈。
 */
class GuestLoginRequiredTest {
    /** 真实抓包：未登录搜索 → 403，code 40353，need_login，并在响应体里给出 /account/unhuman 跳转（不跟随）。 */
    private val needLogin40353 =
        """{"error":{"code":40353,"message":"请登录后查看","need_login":true,"redirect":"https://www.zhihu.com/account/unhuman?type=unhuman&message=need_login&need_login=true"}}"""
    private val limited40362 = """{"error":{"code":40362,"message":"您当前请求存在异常，暂时限制本次访问"}}"""
    private val limited10003 = """{"error":{"code":10003,"message":"请求参数异常，请升级客户端后重试"}}"""
    private val unauthorized401 = """{"error":{"code":100,"message":"请求需要登录","name":"AuthenticationInvalidRequest"}}"""
    private val serverError500 = """{"error":{"code":500,"message":"服务器开小差了"}}"""

    private enum class Expect { Login, Limited, Plain }

    private fun environment(status: HttpStatusCode, body: String) = object : PaginationEnvironment {
        override fun httpClient() = HttpClient(
            MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) },
        )

        override fun authenticatedCookies() = emptyMap<String, String>()

        override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
    }

    private fun lists(): Map<String, PaginationViewModel<*>> = linkedMapOf(
        "搜索" to SearchViewModel("Material 3"),
        "热榜" to HotListViewModel(),
        "评论" to RootCommentViewModel(Article(type = ArticleType.Answer, id = 1L)),
        "问题详情(回答列表)" to QuestionFeedViewModel(1L),
        "收藏夹" to CollectionsViewModel("someone"),
        "通知" to NotificationViewModel(),
    )

    private fun check(status: HttpStatusCode, body: String, expect: Expect) = runBlocking {
        val env = environment(status, body)
        lists().forEach { (page, vm) ->
            vm.fetchOnceForTest(env)
            assertNotNull(vm.errorMessage, "$page 应有错误信息（$status）")
            assertFalse(vm.isLoading, "$page 不应继续转圈（$status）")
            assertEquals(expect == Expect.Login, vm.loginRequired, "$page loginRequired（$status $body）")
            assertEquals(expect == Expect.Limited, vm.rateLimited, "$page rateLimited（$status $body）")
        }
        // 回答 / 文章详情走 ArticleViewModel，单独校验
        for (type in listOf(ArticleType.Answer, ArticleType.Article)) {
            val vm = ArticleViewModel(Article(type = type, id = 1L), env.httpClient())
            vm.loadArticleContent(env)
            val want = when (expect) {
                Expect.Login -> AccessErrorKind.LoginRequired
                Expect.Limited -> AccessErrorKind.RateLimited
                Expect.Plain -> null
            }
            assertEquals(want, vm.accessError, "${type.name} 详情 accessError（$status $body）")
            assertFalse(vm.content.isBlank(), "${type.name} 详情应显示提示而不是空白（$status）")
        }
    }

    @Test fun forbidden403NeedLogin40353ShowsLogin() = check(HttpStatusCode.Forbidden, needLogin40353, Expect.Login)

    @Test fun forbidden403Code40362ShowsRateLimited() = check(HttpStatusCode.Forbidden, limited40362, Expect.Limited)

    @Test fun forbidden403Code10003ShowsRateLimited() = check(HttpStatusCode.Forbidden, limited10003, Expect.Limited)

    @Test fun unauthorized401ShowsLogin() = check(HttpStatusCode.Unauthorized, unauthorized401, Expect.Login)

    @Test fun serverError500ShowsPlainError() = check(HttpStatusCode.InternalServerError, serverError500, Expect.Plain)
}
