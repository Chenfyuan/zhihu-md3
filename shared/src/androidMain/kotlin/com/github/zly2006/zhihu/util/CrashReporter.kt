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

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 纯本地的崩溃日志存储：只写应用私有目录，不做任何网络请求。
 *
 * 每个文件不超过 [maxFileBytes]（超出部分截断），最多保留 [maxFiles] 个（最旧的先删）。
 */
class CrashLogWriter(
    private val dir: File,
    private val maxFiles: Int = MAX_CRASH_LOG_FILES,
    private val maxFileBytes: Int = MAX_CRASH_LOG_BYTES,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun logs(): List<File> =
        dir
            .listFiles { file -> file.isFile && file.name.startsWith(PREFIX) && file.name.endsWith(SUFFIX) }
            ?.sortedByDescending { it.name }
            ?: emptyList()

    fun write(header: Map<String, String>, threadName: String, throwable: Throwable): File? = runCatching {
        dir.mkdirs()
        val now = clock()
        val text = buildString {
            appendLine("时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.ROOT).format(Date(now))}")
            header.forEach { (k, v) -> appendLine("$k: $v") }
            appendLine("线程: $threadName")
            appendLine()
            append(throwable.stackTraceToString())
        }
        var name = "$PREFIX${now.toString().padStart(15, '0')}$SUFFIX"
        var n = 1
        while (File(dir, name).exists()) {
            name = "$PREFIX${now.toString().padStart(15, '0')}-${n++}$SUFFIX"
        }
        val file = File(dir, name)
        file.writeBytes(truncateUtf8(text, maxFileBytes))
        rotate()
        file
    }.getOrNull()

    fun clear(): Int = logs().count { it.delete() }

    private fun rotate() {
        logs().drop(maxFiles).forEach { it.delete() }
    }

    companion object {
        const val PREFIX = "crash-"
        const val SUFFIX = ".txt"
        const val TRUNCATED_MARKER = "\n…（已截断）\n"

        fun truncateUtf8(text: String, maxBytes: Int): ByteArray {
            val bytes = text.toByteArray(Charsets.UTF_8)
            if (bytes.size <= maxBytes) return bytes
            val marker = TRUNCATED_MARKER.toByteArray(Charsets.UTF_8)
            var end = (maxBytes - marker.size).coerceAtLeast(0)
            // 不要切断多字节 UTF-8 字符
            while (end > 0 && (bytes[end].toInt() and 0xC0) == 0x80) end--
            return bytes.copyOf(end) + marker
        }
    }
}

const val MAX_CRASH_LOG_FILES = 10
const val MAX_CRASH_LOG_BYTES = 256 * 1024

/** 崩溃日志入口：未捕获异常时写本地文件；查看、分享（FileProvider）、清除。绝不联网。 */
object CrashReporter {
    private const val TAG = "CrashReporter"
    private const val CRASH_LOG_DIRECTORY = "crash-logs"

    fun crashLogDir(context: Context): File = File(context.applicationContext.filesDir, CRASH_LOG_DIRECTORY)

    fun writer(context: Context): CrashLogWriter = CrashLogWriter(crashLogDir(context))

    fun recordCrash(context: Context, thread: Thread, throwable: Throwable) {
        runCatching {
            val appContext = context.applicationContext
            val info = runCatching { appContext.packageManager.getPackageInfo(appContext.packageName, 0) }.getOrNull()
            writer(appContext).write(
                header = linkedMapOf(
                    "应用版本" to "${info?.versionName ?: "unknown"} (${info?.longVersionCode ?: 0})",
                    "包名" to appContext.packageName,
                    "设备" to "${Build.MANUFACTURER} ${Build.MODEL}",
                    "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                ),
                threadName = thread.name,
                throwable = throwable,
            )
        }.onFailure { Log.w(TAG, "写入崩溃日志失败", it) }
    }

    fun logs(context: Context): List<File> = writer(context).logs()

    fun clear(context: Context): Int = writer(context).clear()

    fun share(context: Context, file: File): Boolean {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching {
            context.startActivity(
                Intent.createChooser(intent, "分享崩溃日志").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure { Log.w(TAG, "分享崩溃日志失败", it) }.isSuccess
    }
}
