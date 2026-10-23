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

package com.github.zly2006.zhihu.ui.subscreens

import android.content.Context
import com.github.zly2006.zhihu.util.CrashReporter
import java.io.File
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.updater.UpdateManager
import com.github.zly2006.zhihu.updater.UpdateManager.UpdateState
import com.github.zly2006.zhihu.util.PowerSaveModeCompat
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Composable
actual fun rememberDeveloperInfo(): DeveloperInfoSnapshot {
    val context = LocalContext.current
    return produceState(initialValue = DeveloperInfoSnapshot(), context) {
        while (true) {
            value = DeveloperInfoSnapshot(
                networkStatus = context.networkStatusText(),
                powerSaveModeText =
                    when (PowerSaveModeCompat.getPowerSaveMode(context)) {
                        PowerSaveModeCompat.POWER_SAVE -> "省电模式：已开启"
                        PowerSaveModeCompat.HUAWEI_POWER_SAVE -> "省电模式：华为傻逼模式已开启"
                        else -> null
                    },
            )
            delay(1_000L)
        }
    }.value
}

private fun Context.networkStatusText(): String {
    val connectivityManager = getSystemService(ConnectivityManager::class.java)
    val activeNetwork = connectivityManager.activeNetwork
    return buildString {
        append("网络状态：")
        if (activeNetwork != null) {
            append("已连接")
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> append(" (移动数据)")
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> append(" (Wi-Fi)")
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> append(" (VPN)")
            }
        } else {
            append("未连接")
        }
    }
}

@Composable
actual fun rememberSystemUpdateState(): StateFlow<SystemUpdateState> {
    val scope = rememberCoroutineScope()
    return remember(scope) {
        UpdateManager.updateState.map { it.toSystemUpdateState() }.stateIn(
            scope,
            SharingStarted.Eagerly,
            UpdateManager.updateState.value.toSystemUpdateState(),
        )
    }
}

@Composable
actual fun rememberSystemUpdateChecker(): SystemUpdateChecker {
    val context = LocalContext.current
    return remember(context) {
        object : SystemUpdateChecker {
            override suspend fun check() = UpdateManager.checkForUpdate(context)
        }
    }
}

@Composable
actual fun rememberSystemUpdateVersionSkipper(): SystemUpdateVersionSkipper {
    val context = LocalContext.current
    return remember(context) {
        object : SystemUpdateVersionSkipper {
            override fun skip(version: String) {
                UpdateManager.skipVersion(context, version)
                UpdateManager.updateState.value = UpdateState.Latest
            }
        }
    }
}

@Composable
actual fun rememberSystemUpdateDownloader(): SystemUpdateDownloader {
    val context = LocalContext.current
    return remember(context) {
        object : SystemUpdateDownloader {
            override suspend fun download(url: String) = UpdateManager.downloadUpdate(context, url)
        }
    }
}

@Composable
actual fun rememberDownloadedSystemUpdateInstaller(): DownloadedSystemUpdateInstaller {
    val context = LocalContext.current
    return remember(context) {
        object : DownloadedSystemUpdateInstaller {
            override suspend fun install() {
                val state = UpdateManager.updateState.value
                if (state is UpdateState.Downloaded) {
                    UpdateManager.installUpdate(context, state.file)
                }
            }
        }
    }
}

actual fun resetSystemUpdateState() {
    UpdateManager.updateState.value = UpdateState.NoUpdate
}

actual fun setSystemUpdateError(message: String) {
    UpdateManager.updateState.value = UpdateState.Error(message)
}

actual val isApkUpdateInstallSupported: Boolean = true

private fun UpdateState.toSystemUpdateState(): SystemUpdateState = when (this) {
    UpdateState.NoUpdate -> SystemUpdateState.NoUpdate
    UpdateState.Checking -> SystemUpdateState.Checking
    UpdateState.Latest -> SystemUpdateState.Latest
    is UpdateState.UpdateAvailable -> SystemUpdateState.UpdateAvailable(
        version = version.toString(),
        isNightly = isNightly,
        releaseNotes = releaseNotes,
        downloadUrl = downloadUrl,
        cnDownloadUrl = cnDownloadUrl,
    )
    UpdateState.Downloading -> SystemUpdateState.Downloading
    is UpdateState.Downloaded -> SystemUpdateState.Downloaded
    is UpdateState.Error -> SystemUpdateState.Error(message)
}

@Composable
actual fun rememberOpenSourceLicensesLibraries(): Libs {
    val context = LocalContext.current
    return remember(context) {
        Libs
            .Builder()
            .withContext(context)
            .build()
    }
}

private class AndroidCrashLogStore(
    private val context: Context,
) : CrashLogStore {
    private fun file(entry: CrashLogEntry): File = File(CrashReporter.crashLogDir(context), entry.id)

    override fun list(): List<CrashLogEntry> = CrashReporter.logs(context).map { file ->
        val title = runCatching { file.useLines { it.firstOrNull() } }.getOrNull()?.removePrefix("时间: ") ?: file.name
        CrashLogEntry(id = file.name, title = title, sizeBytes = file.length())
    }

    override fun read(entry: CrashLogEntry): String = runCatching { file(entry).readText() }.getOrDefault("")

    override fun share(entry: CrashLogEntry): Boolean = CrashReporter.share(context, file(entry))

    override fun clear(): Int = CrashReporter.clear(context)
}

@Composable
actual fun rememberCrashLogStore(): CrashLogStore {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidCrashLogStore(context) }
}
