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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.LocalNavigator

const val CRASH_LOGS_SCREEN_TAG = "crash_logs_screen"
const val CRASH_LOGS_SHARE_TAG = "crash_logs_share"
const val CRASH_LOGS_CLEAR_TAG = "crash_logs_clear"

/** 一条本地崩溃日志。[id] 为文件名，[title] 为时间等摘要。 */
data class CrashLogEntry(
    val id: String,
    val title: String,
    val sizeBytes: Long,
)

/** 本地崩溃日志存取，纯本地，不联网。 */
interface CrashLogStore {
    fun list(): List<CrashLogEntry>

    fun read(entry: CrashLogEntry): String

    fun share(entry: CrashLogEntry): Boolean

    fun clear(): Int
}

object EmptyCrashLogStore : CrashLogStore {
    override fun list(): List<CrashLogEntry> = emptyList()

    override fun read(entry: CrashLogEntry): String = ""

    override fun share(entry: CrashLogEntry): Boolean = false

    override fun clear(): Int = 0
}

@Composable
expect fun rememberCrashLogStore(): CrashLogStore

/**
 * 崩溃日志页：列出本机保存的崩溃记录（最多 10 条），默认展开最新一条，可分享（系统分享面板）或全部清除。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashLogsScreen(store: CrashLogStore = rememberCrashLogStore()) {
    val navigator = LocalNavigator.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var version by remember { mutableIntStateOf(0) }
    val entries = remember(version) { store.list() }
    var selectedId by remember(version) { mutableStateOf(entries.firstOrNull()?.id) }
    val selected = entries.firstOrNull { it.id == selectedId } ?: entries.firstOrNull()
    val selectedText = remember(selected?.id, version) { selected?.let(store::read).orEmpty() }
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .testTag(CRASH_LOGS_SCREEN_TAG),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text("崩溃日志") },
                navigationIcon = {
                    IconButton(
                        onClick = navigator.onNavigateBack,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item("intro") {
                Text(
                    "应用闪退时会在本机记录一份日志（最多保留 10 份，每份不超过 256 KB），不会上传到任何服务器。需要反馈问题时可以分享给开发者。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (entries.isEmpty()) {
                item("empty") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        Text("暂无崩溃日志", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                item("actions") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            modifier = Modifier.testTag(CRASH_LOGS_SHARE_TAG),
                            onClick = { selected?.let(store::share) },
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null)
                            Text("分享", Modifier.padding(start = 8.dp))
                        }
                        OutlinedButton(
                            modifier = Modifier.testTag(CRASH_LOGS_CLEAR_TAG),
                            onClick = { confirmClear = true },
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = null)
                            Text("全部清除", Modifier.padding(start = 8.dp))
                        }
                    }
                }
                items(entries, key = { it.id }) { entry ->
                    val isSelected = entry.id == selected?.id
                    Card(
                        onClick = { selectedId = entry.id },
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (isSelected) {
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        } else {
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        },
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(entry.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${(entry.sizeBytes + 1023) / 1024} KB",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            if (isSelected) {
                                SelectionContainer {
                                    Text(
                                        selectedText.take(MAX_PREVIEW_CHARS),
                                        modifier = Modifier
                                            .padding(top = 12.dp)
                                            .horizontalScroll(rememberScrollState()),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清除崩溃日志") },
            text = { Text("将删除本机保存的全部 ${entries.size} 份崩溃日志，无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    store.clear()
                    confirmClear = false
                    version++
                }) { Text("清除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("取消") }
            },
        )
    }
}

private const val MAX_PREVIEW_CHARS = 20_000
