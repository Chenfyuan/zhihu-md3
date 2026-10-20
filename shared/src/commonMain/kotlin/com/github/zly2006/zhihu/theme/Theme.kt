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

package com.github.zly2006.zhihu.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamicColorScheme
import kotlin.math.pow

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
)

/**
 * Apply the optional user background without flattening the M3 surface ladder.
 *
 * Previously both `background` and `surface` were forced to the same custom color,
 * which collapsed `surfaceContainer*` tonal roles. Only `background` (and a
 * matching `onBackground`) is overridden; surface containers stay scheme-derived.
 */
private fun Color.relativeLuminance(): Float {
    fun linearize(channel: Float): Float =
        if (channel <= 0.04045f) {
            channel / 12.92f
        } else {
            ((channel + 0.055f) / 1.055f).pow(2.4f)
        }
    return 0.2126f * linearize(red) + 0.7152f * linearize(green) + 0.0722f * linearize(blue)
}

internal fun ColorScheme.withOptionalBackground(customBackground: Color): ColorScheme {
    val onBackground = if (customBackground.relativeLuminance() > 0.5f) {
        Color(0xFF1C1B1F)
    } else {
        Color(0xFFE6E1E5)
    }
    return copy(
        background = customBackground,
        onBackground = onBackground,
    )
}

@Composable
fun ZhihuTheme(
    content: @Composable () -> Unit,
) {
    val useDynamicColor = ThemeManager.getUseDynamicColor()
    val customBackgroundColor = ThemeManager.getBackgroundColor()
    val darkTheme = ThemeManager.isDarkTheme()
    val platformDynamicColorScheme = platformDynamicColorScheme(darkTheme)

    val baseColorScheme = resolveBaseColorScheme(
        useDynamicColor = useDynamicColor,
        platformDynamicColorScheme = platformDynamicColorScheme,
        seedColor = ThemeManager.getCustomColor(),
        darkTheme = darkTheme,
    )

    val colorScheme = baseColorScheme.withOptionalBackground(customBackgroundColor)

    PlatformSystemBarEffect(darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ZhihuShapes,
        content = content,
    )
}

@Composable
expect fun currentSystemInDarkTheme(): Boolean

@Composable
expect fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme?

@Composable
expect fun PlatformSystemBarEffect(darkTheme: Boolean)

/**
 * 应用基础配色的唯一来源（不含自定义背景覆盖），供 [ZhihuTheme] 与 Compose 之外的调用方（如应用内浏览器工具栏）共用。
 */
fun resolveBaseColorScheme(
    useDynamicColor: Boolean,
    platformDynamicColorScheme: ColorScheme?,
    seedColor: Color,
    darkTheme: Boolean,
): ColorScheme = when {
    useDynamicColor && platformDynamicColorScheme != null -> platformDynamicColorScheme
    !useDynamicColor -> dynamicColorScheme(seedColor = seedColor, isDark = darkTheme, isAmoled = false)
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
}

/** 应用内浏览器（Custom Tab）工具栏色：与应用顶栏一致取 surface，Custom Tab 会据此自动选择可读的图标/文字颜色。 */
fun customTabToolbarColor(scheme: ColorScheme): Color = scheme.surface
