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

package com.github.zly2006.zhihu.util

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ThemeMode
import com.github.zly2006.zhihu.theme.customTabToolbarColor
import com.github.zly2006.zhihu.theme.resolveBaseColorScheme
import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.github.zly2006.zhihu.account.androidZhihuAccountStore
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.platform.androidSettingsStore
import com.github.zly2006.zhihu.util.signZhihuFetchRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.contentType
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.security.MessageDigest

fun HttpRequestBuilder.signFetchRequest() {
    signZhihuFetchRequest(AccountData.data.cookies)
}

@OptIn(DelicateCoroutinesApi::class)
fun telemetry(context: Context, usage: String) {
    require(usage in listOf("start", "login")) {
        "Usage must be either 'start' or 'login', but was '$usage'."
    }
    val settings = androidSettingsStore(context)
    val data = AccountData.loadData(context)
    if (settings.getBoolean("allowTelemetry", true)) {
        val versionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "unknown"
        GlobalScope.launch {
            @OptIn(ExperimentalStdlibApi::class)
            runCatching {
                val hash = MessageDigest
                    .getInstance("MD5")
                    .apply {
                        data.self!!
                            .userType
                            .toByteArray()
                            .let(this::update)
                        data.self.urlToken
                            ?.toByteArray()
                            ?.let(this::update)
                    }.digest(data.self!!.id.toByteArray())
                    .toHexString()
                androidZhihuAccountStore(context)
                    .client
                    .httpClient()
                    .post("https://redenmc.com/api/zhihu/usage?client_hash=$hash&usage=$usage") {
                        contentType(ContentType.Application.Json)
                        header(
                            HttpHeaders.UserAgent,
                            "Zhihu++/$versionName",
                        )
                    }
            }
        }
    }
}

/**
 * 非 Compose 环境下解析 Custom Tab 工具栏色：动态取色开启时读 Material You 系统中性色（surface 近似），
 * 否则走 [resolveBaseColorScheme]（自定义种子色 / 内置配色），浅色与深色各自独立。
 */
fun resolveCustomTabToolbarArgb(context: Context, dark: Boolean): Int {
    val snap = ThemeManager.snapshot()
    if (snap.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Material You 系统色：neutral1_10 ≈ 浅色 surface，neutral1_900 ≈ 深色 surface
        val id = if (dark) android.R.color.system_neutral1_900 else android.R.color.system_neutral1_10
        return context.getColor(id)
    }
    return customTabToolbarColor(
        resolveBaseColorScheme(
            useDynamicColor = snap.useDynamicColor,
            platformDynamicColorScheme = null,
            seedColor = Color(snap.customColor),
            darkTheme = dark,
        ),
    ).toArgb()
}

/**
 * 用应用内浏览器（Custom Tab）打开链接。工具栏颜色跟随应用主题（动态取色/自定义种子色），
 * 同时下发浅色与深色两套参数，并按应用的深浅色模式设置 colorScheme，保证深色下图标文字可读。
 */
fun luoTianYiUrlLauncher(context: Context, uri: Uri) {
    if (uri.host == "link.zhihu.com") {
        Url(uri.toString()).parameters["target"]?.let {
            luoTianYiUrlLauncher(context, it.toUri())
            return
        }
    }
    fun toolbar(dark: Boolean): Int = resolveCustomTabToolbarArgb(context, dark)
    val intent = CustomTabsIntent
        .Builder()
        .setColorScheme(
            when (ThemeManager.snapshot().themeMode) {
                ThemeMode.LIGHT -> CustomTabsIntent.COLOR_SCHEME_LIGHT
                ThemeMode.DARK -> CustomTabsIntent.COLOR_SCHEME_DARK
                ThemeMode.SYSTEM -> CustomTabsIntent.COLOR_SCHEME_SYSTEM
            },
        ).setColorSchemeParams(
            CustomTabsIntent.COLOR_SCHEME_LIGHT,
            CustomTabColorSchemeParams.Builder().setToolbarColor(toolbar(false)).build(),
        ).setColorSchemeParams(
            CustomTabsIntent.COLOR_SCHEME_DARK,
            CustomTabColorSchemeParams.Builder().setToolbarColor(toolbar(true)).build(),
        ).build()
    intent.launchUrl(context, uri)
}

val Context.clipboardManager
    get() = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
