package com.github.zly2006.zhihu.updater

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GithubReleaseFetchTest {
    private fun client(status: HttpStatusCode, body: String, seen: MutableList<String>) = HttpClient(
        MockEngine { req ->
            seen += req.url.toString()
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        },
    ) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    @Test
    fun latestReleasePointsToOwnRepo() = runBlocking {
        val seen = mutableListOf<String>()
        val release = fetchLatestZhihuRelease(client(HttpStatusCode.OK, """{"tag_name":"1.2.3"}""", seen), null)
        assertEquals("1.2.3", release.tagName)
        assertEquals(listOf("https://api.github.com/repos/Chenfyuan/zhihu-md3/releases/latest"), seen)
    }

    @Test
    fun notFoundMeansNoReleaseInfoNotCrash() = runBlocking {
        val seen = mutableListOf<String>()
        val e = assertFailsWith<NoReleaseInfoException> {
            fetchLatestZhihuRelease(client(HttpStatusCode.NotFound, """{"message":"Not Found"}""", seen), null)
        }
        assertEquals(NO_RELEASE_INFO_MESSAGE, e.message)
        assertFailsWith<NoReleaseInfoException> {
            fetchNightlyZhihuRelease(client(HttpStatusCode.NotFound, "{}", seen), null)
        }
        assertTrue(seen.all { it.startsWith("https://api.github.com/repos/Chenfyuan/zhihu-md3/") })
    }
}
