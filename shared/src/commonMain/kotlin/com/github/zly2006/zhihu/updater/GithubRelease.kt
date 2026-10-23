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

package com.github.zly2006.zhihu.updater

import com.github.zly2006.zhihu.util.raiseForStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val ZHIHU_MD3_GITHUB_REPO = "Chenfyuan/zhihu-md3"
const val ZHIHU_MD3_GITHUB_URL = "https://github.com/$ZHIHU_MD3_GITHUB_REPO"
const val ZHIHU_PLUS_PLUS_GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/$ZHIHU_MD3_GITHUB_REPO/releases/latest"
const val ZHIHU_PLUS_PLUS_GITHUB_NIGHTLY_RELEASE_URL = "https://api.github.com/repos/$ZHIHU_MD3_GITHUB_REPO/releases/tags/nightly"

/** 仓库尚无 Release（或仓库不可匿名访问）时 GitHub API 返回 404，此时提示“暂无更新信息”，不当作失败。 */
const val NO_RELEASE_INFO_MESSAGE = "暂无更新信息"

class NoReleaseInfoException : Exception(NO_RELEASE_INFO_MESSAGE)

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("html_url") val htmlUrl: String? = null,
    val body: String? = null,
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
data class GithubAsset(
    val name: String,
    @SerialName("content_type") val contentType: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    @SerialName("cn_download_url") val cnDownloadUrl: String? = null,
)

fun extractGithubReleaseNotes(body: String): String = body
    .replace("\r\n", "\n")
    .substringAfter("## What's Changed\n")
    .substringBefore("\n**Full Changelog**:")
    .trimEnd('\n')

suspend fun fetchLatestZhihuRelease(
    client: HttpClient,
    githubToken: String?,
): GithubRelease = fetchGithubRelease(client, ZHIHU_PLUS_PLUS_GITHUB_LATEST_RELEASE_URL, githubToken)

suspend fun fetchNightlyZhihuRelease(
    client: HttpClient,
    githubToken: String?,
): GithubRelease = fetchGithubRelease(client, ZHIHU_PLUS_PLUS_GITHUB_NIGHTLY_RELEASE_URL, githubToken)

private suspend fun fetchGithubRelease(
    client: HttpClient,
    url: String,
    githubToken: String?,
): GithubRelease {
    val response = client.get(url) {
        githubToken?.let { token ->
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }
    if (response.status == HttpStatusCode.NotFound) throw NoReleaseInfoException()
    return response.raiseForStatus().body<GithubRelease>()
}

internal data class AndroidReleaseDownloadInfo(
    val browserDownloadUrl: String,
    val cnDownloadUrl: String? = null,
    val opensExternally: Boolean = false,
)

internal fun GithubRelease.extractAndroidDownloadInfo(isLiteVariant: Boolean): AndroidReleaseDownloadInfo {
    val apkAssets = assets.filter { it.contentType == "application/vnd.android.package-archive" }
    val variant = if (isLiteVariant) "lite" else "full"
    val selectedAsset = apkAssets.firstOrNull { it.name.contains(variant, ignoreCase = true) } ?: apkAssets.firstOrNull()
    if (selectedAsset != null) {
        return AndroidReleaseDownloadInfo(selectedAsset.browserDownloadUrl, selectedAsset.cnDownloadUrl)
    }
    val quarkUrl = Regex("""https://pan\.quark\.cn/s/[A-Za-z0-9_-]+(?:\?[A-Za-z0-9._~%!$&*+,;=:@/?-]+)?""")
        .find(body.orEmpty())
        ?.value
        ?: error("发布中未找到 APK 或夸克网盘下载链接")
    return AndroidReleaseDownloadInfo(browserDownloadUrl = quarkUrl, opensExternally = true)
}
