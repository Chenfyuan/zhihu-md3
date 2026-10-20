package com.github.zly2006.zhihu.ui

import kotlin.test.Test
import kotlin.test.assertEquals

/** 返回手势改为横向滑出：只有位移，没有缩放。 */
class PredictiveBackScaleTest {
    @Test
    fun detailSlidesWithProgressWithoutScale() {
        assertEquals(0f, predictiveBackTranslationFraction(0f), 1e-5f)
        assertEquals(0.5f, predictiveBackTranslationFraction(0.5f), 1e-5f)
        assertEquals(1f, predictiveBackTranslationFraction(1f), 1e-5f)
        assertEquals(1f, predictiveBackTranslationFraction(1.4f), 1e-5f)
    }

    @Test
    fun listParallaxAndScrimFollowDetailSlide() {
        assertEquals(-0.3f, listParallaxFraction(0f), 1e-5f)
        assertEquals(-0.15f, listParallaxFraction(0.5f), 1e-5f)
        assertEquals(0f, listParallaxFraction(1f), 1e-5f)
        assertEquals(0.18f, listScrimAlpha(0f), 1e-5f)
        assertEquals(0.09f, listScrimAlpha(0.5f), 1e-5f)
        assertEquals(0f, listScrimAlpha(1f), 1e-5f)
    }
}
