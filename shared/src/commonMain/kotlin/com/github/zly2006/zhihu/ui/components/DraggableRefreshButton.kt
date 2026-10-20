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

package com.github.zly2006.zhihu.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.ui.subscreens.DEFAULT_FAB_OPACITY
import com.github.zly2006.zhihu.ui.subscreens.PREF_FAB_OPACITY
import kotlin.math.roundToInt

/**
 * 可拖动并自动贴边的刷新按钮。
 *
 * 这个 FAB 用于首页和其他列表页的手动刷新入口。位置按 [preferenceName] 分别保存到 `-x`、`-y` 两个 preference key，
 * 拖动结束后会限制在屏幕内并贴近左右边缘，避免遮挡内容或被系统栏吞掉。需要多个可拖动按钮时必须使用不同的 [preferenceName]，
 * 并可用 [initiallyOnLeft] 把新按钮放到另一侧；用户已保存的位置仍始终优先。
 */
@Composable
fun DraggableRefreshButton(
    modifier: Modifier = Modifier,
    preferenceName: String = "fabRefresh",
    bottomAvoidance: Dp = 0.dp,
    initiallyOnLeft: Boolean = false,
    /** 为 true 时默认叠在右下角创作 FAB 正上方（bottomAvoidance 需包含底栏高度） */
    stackAboveFab: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit = {
        Icon(Icons.Default.Refresh, contentDescription = "刷新")
    },
) {
    val density = LocalDensity.current
    val screenSize = LocalWindowInfo.current.containerSize
    val settings = rememberSettingsStore()

    var offsetX by remember(preferenceName, initiallyOnLeft) {
        mutableFloatStateOf(
            settings.getFloat(
                "$preferenceName-x",
                if (initiallyOnLeft) 0f else Float.MAX_VALUE,
            ),
        )
    }
    var offsetY by remember { mutableFloatStateOf(settings.getFloat("$preferenceName-y", Float.MAX_VALUE)) }
    var pressing by remember { mutableStateOf(false) }
    // 默认位置：叠在右下角「创作」FAB 正上方，两者同列、间距 16dp，整体在底栏上方 16dp。
    // 创作 FAB 底边 = 底栏/系统栏高度(bottomAvoidance) + 16dp，高 56dp；刷新按钮再往上 16dp + 56dp。
    val maxStoredOffsetY = with(density) {
        (screenSize.height - (if (stackAboveFab) FAB_STACK_HEIGHT else 250.dp).toPx()).coerceAtLeast(0f)
    }
    val maxVisibleOffsetY = with(density) {
        (maxStoredOffsetY - bottomAvoidance.toPx()).coerceAtLeast(0f)
    }

    fun adjustFabPosition() {
        with(density) {
            // 左右各留 16dp，与创作 FAB 的 end padding 对齐，不再贴屏幕边缘
            val minX = FAB_EDGE_MARGIN.toPx()
            val maxX = (screenSize.width - 56.dp.toPx() - FAB_EDGE_MARGIN.toPx()).coerceAtLeast(minX)
            offsetX = offsetX.coerceIn(minX, maxX)
            offsetY = offsetY.coerceIn(0f, maxStoredOffsetY)
        }
    }

    adjustFabPosition()
    val visibleOffsetY = offsetY.coerceAtMost(maxVisibleOffsetY)

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(if (pressing) 1 else 300),
        label = "offsetX",
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = visibleOffsetY,
        animationSpec = tween(if (pressing) 1 else 300),
        label = "offsetY",
    )
    val displayedOffsetX = if (pressing) offsetX else animatedOffsetX
    val displayedOffsetY = if (pressing) visibleOffsetY else animatedOffsetY
    val hapticFeedback = LocalHapticFeedback.current

    val opacityFraction = remember(settings) {
        settings.getInt(PREF_FAB_OPACITY, DEFAULT_FAB_OPACITY).coerceIn(10, 100) / 100f
    }
    val dragModifier = modifier
        .offset { IntOffset(displayedOffsetX.roundToInt(), displayedOffsetY.roundToInt()) }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    offsetX = animatedOffsetX
                    offsetY = animatedOffsetY
                    pressing = true
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDragEnd = {
                    pressing = false
                    adjustFabPosition()
                    val screenWidth = screenSize.width.toFloat()
                    with(density) {
                        val minX = FAB_EDGE_MARGIN.toPx()
                        val maxX = (screenWidth - 56.dp.toPx() - FAB_EDGE_MARGIN.toPx()).coerceAtLeast(minX)
                        offsetX = if (offsetX < screenWidth / 2) minX else maxX
                    }
                    settings.putFloat("$preferenceName-x", offsetX)
                    settings.putFloat("$preferenceName-y", offsetY)
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    if (pressing) {
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                        adjustFabPosition()
                    }
                },
            )
        }
    val glassProvider = LocalGlassBottomBarProvider.current
    val glassSource = LocalGlassBarSource.current
    if (glassProvider != null && glassSource != null) {
        // 玻璃 FAB：必须在 contentSource 模糊层之外（首页已把列表和 FAB 拆开）
        glassProvider.Fab(
            source = glassSource,
            onClick = onClick,
            modifier = dragModifier,
            opacity = opacityFraction,
            content = content,
        )
    } else {
        FloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = FloatingActionButtonDefaults.containerColor.copy(alpha = opacityFraction),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = opacityFraction),
            elevation = if (opacityFraction < 1f) {
                FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
            } else {
                FloatingActionButtonDefaults.elevation()
            },
            modifier = dragModifier,
            content = content,
        )
    }
}


private val FAB_EDGE_MARGIN = 16.dp
/** 16(底边距) + 56(创作 FAB) + 16(间距) + 56(刷新 FAB) */
private val FAB_STACK_HEIGHT = 144.dp
