package com.github.zly2006.zhihu.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class PredictiveBackScaleTest {
    @Test
    fun scaleAndTranslationFollowProgress() {
        assertEquals(1.0f, predictiveBackScale(0f), 1e-5f)
        assertEquals(0.95f, predictiveBackScale(0.5f), 1e-5f)
        assertEquals(0.9f, predictiveBackScale(1f), 1e-5f)
        assertEquals(0.0f, predictiveBackTranslationFraction(0f), 1e-5f)
        assertEquals(0.03f, predictiveBackTranslationFraction(0.5f), 1e-5f)
        assertEquals(0.06f, predictiveBackTranslationFraction(1f), 1e-5f)
    }
}
