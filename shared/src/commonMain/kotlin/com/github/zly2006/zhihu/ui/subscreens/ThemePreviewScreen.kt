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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ThemeMode
import com.github.zly2006.zhihu.ui.components.SettingItem
import com.github.zly2006.zhihu.ui.components.SettingItemGroup
import com.github.zly2006.zhihu.ui.components.pageTurnViewportWithGuide
import com.github.zly2006.zhihu.ui.components.rememberPageTurnTarget

const val THEME_PREVIEW_SCROLL_TAG = "theme_preview_scroll"

/**
 * QA 用主题预览页：切换明暗 / 动态取色，并展示 M3 色阶、字体与圆角。
 *
 * 改动会写入 ThemeManager 与 SettingsStore，与外观设置页同一套状态。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePreviewScreen() {
    val navigator = LocalNavigator.current
    val settings = rememberSettingsStore()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollState = rememberScrollState()
    val pageTurnTarget = rememberPageTurnTarget(scrollState, enabled = true)
    val cs = MaterialTheme.colorScheme
    val shapes = MaterialTheme.shapes
    val typography = MaterialTheme.typography
    val useDynamicColor = ThemeManager.getUseDynamicColor()
    val themeMode = ThemeManager.getThemeMode()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .testTag(THEME_PREVIEW_SCROLL_TAG),
        containerColor = cs.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = { Text("主题预览") },
                navigationIcon = {
                    IconButton(
                        onClick = navigator.onNavigateBack,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = cs.surfaceVariant,
                            contentColor = cs.onSurfaceVariant,
                        ),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = cs.surfaceContainer,
                    scrolledContainerColor = cs.surfaceContainerHigh,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .pageTurnViewportWithGuide(pageTurnTarget)
                .verticalScroll(scrollState)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingItemGroup(title = "快速切换") {
                SettingItem(
                    title = { Text("主题模式") },
                    description = { Text("浅色 / 深色 / 跟随系统，立即作用于全应用。") },
                    bottomAction = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ) {
                            listOf(
                                ThemeMode.SYSTEM to "自动",
                                ThemeMode.LIGHT to "浅色",
                                ThemeMode.DARK to "深色",
                            ).forEach { (mode, label) ->
                                FilterChip(
                                    selected = themeMode == mode,
                                    onClick = {
                                        ThemeManager.setThemeMode(mode)
                                        settings.putString("themeMode", mode.name)
                                    },
                                    label = { Text(label) },
                                )
                            }
                        }
                    },
                )
                SettingItem(
                    title = { Text("Material You 动态取色") },
                    description = { Text("关闭后使用自定义种子色生成完整 ColorScheme。") },
                    endAction = {
                        Switch(
                            checked = useDynamicColor,
                            onCheckedChange = {
                                ThemeManager.setUseDynamicColor(it)
                                settings.putBoolean("useDynamicColor", it)
                            },
                        )
                    },
                )
                SettingItem(
                    title = { Text("完整 ColorScheme") },
                    description = { Text("查看全部 token（开发者配色页）。") },
                    onClick = { navigator.onNavigate(Account.DeveloperSettings.ColorScheme) },
                )
            }

            SettingItemGroup(title = "色阶（surfaceContainer）") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SurfaceSwatch("background", cs.background, cs.onBackground)
                    SurfaceSwatch("surface", cs.surface, cs.onSurface)
                    SurfaceSwatch("surfaceContainerLowest", cs.surfaceContainerLowest, cs.onSurface)
                    SurfaceSwatch("surfaceContainerLow", cs.surfaceContainerLow, cs.onSurface)
                    SurfaceSwatch("surfaceContainer", cs.surfaceContainer, cs.onSurface)
                    SurfaceSwatch("surfaceContainerHigh", cs.surfaceContainerHigh, cs.onSurface)
                    SurfaceSwatch("surfaceContainerHighest", cs.surfaceContainerHighest, cs.onSurface)
                    SurfaceSwatch("primary", cs.primary, cs.onPrimary)
                    SurfaceSwatch("secondaryContainer", cs.secondaryContainer, cs.onSecondaryContainer)
                }
            }

            SettingItemGroup(title = "字体") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Headline Small", style = typography.headlineSmall, color = cs.onSurface)
                    Text("Title Large", style = typography.titleLarge, color = cs.onSurface)
                    Text("Title Medium", style = typography.titleMedium, color = cs.onSurface)
                    Text("Body Large — 正文样例，用于核对行高与字重。", style = typography.bodyLarge, color = cs.onSurface)
                    Text("Body Medium — 次级说明文字。", style = typography.bodyMedium, color = cs.onSurfaceVariant)
                    Text("Label Large", style = typography.labelLarge, color = cs.primary)
                }
            }

            SettingItemGroup(title = "圆角与组件") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ShapeChip("XS", shapes.extraSmall)
                        ShapeChip("S", shapes.small)
                        ShapeChip("M", shapes.medium)
                        ShapeChip("L", shapes.large)
                        ShapeChip("XL", shapes.extraLarge)
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh),
                        shape = shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("示例卡片", style = typography.titleMedium)
                            Text(
                                "使用 surfaceContainerHigh + medium shape，验证导航栏 / 设置页同一色阶。",
                                style = typography.bodyMedium,
                                color = cs.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(onClick = {}) { Text("FilledTonal") }
                                OutlinedButton(onClick = {}) { Text("Outlined") }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SurfaceSwatch(
    name: String,
    background: Color,
    foreground: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            name,
            color = foreground,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun ShapeChip(
    label: String,
    shape: androidx.compose.ui.graphics.Shape,
) {
    Surface(
        modifier = Modifier.size(48.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
