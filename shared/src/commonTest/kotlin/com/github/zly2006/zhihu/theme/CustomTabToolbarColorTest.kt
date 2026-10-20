package com.github.zly2006.zhihu.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CustomTabToolbarColorTest {
    @Test
    fun dynamicColorUsesPlatformSchemeSurface() {
        val wallpaper = lightColorScheme(surface = Color(0xFFFFF0E0))
        val scheme = resolveBaseColorScheme(true, wallpaper, Color.Blue, darkTheme = false)
        assertEquals(Color(0xFFFFF0E0), customTabToolbarColor(scheme))
    }

    @Test
    fun customSeedFollowsSeedAndDarkMode() {
        val light = customTabToolbarColor(resolveBaseColorScheme(false, null, Color(0xFF2196F3), false))
        val dark = customTabToolbarColor(resolveBaseColorScheme(false, null, Color(0xFF2196F3), true))
        val otherSeed = customTabToolbarColor(resolveBaseColorScheme(false, null, Color(0xFFE91E63), false))
        assertTrue(light.luminance() > 0.5f, "浅色工具栏应为浅底")
        assertTrue(dark.luminance() < 0.2f, "深色工具栏应为深底，保证图标文字可读")
        assertNotEquals(light, otherSeed, "工具栏颜色应随种子色变化")
    }

    @Test
    fun dynamicWithoutPlatformSupportFallsBackToBuiltIn() {
        val dark = resolveBaseColorScheme(true, null, Color.Red, darkTheme = true)
        assertTrue(customTabToolbarColor(dark).luminance() < 0.2f)
    }
}
