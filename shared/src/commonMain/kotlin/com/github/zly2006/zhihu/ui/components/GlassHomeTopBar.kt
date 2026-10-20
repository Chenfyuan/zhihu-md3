package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.ui.HOME_NOTIFICATION_BADGE_TAG
import com.github.zly2006.zhihu.ui.HOME_NOTIFICATION_BUTTON_CONTENT_TAG
import com.github.zly2006.zhihu.ui.HOME_NOTIFICATION_BUTTON_TAG
import com.github.zly2006.zhihu.ui.HOME_SEARCH_BUTTON_TAG
import com.github.zly2006.zhihu.ui.HOME_TOP_ACTIONS_TAG

/** 顶部玻璃按钮高度（搜索胶囊与通知圆钮同高）。 */
val GLASS_TOP_BUTTON_SIZE: Dp = 48.dp

/**
 * 首页顶部：一个玻璃搜索胶囊 + 一个独立的玻璃通知圆钮（保留红色数字角标）。
 * 两者之间和周围没有底板，列表内容从后面和缝隙里透出来。玻璃样式与底部悬浮按钮一致（[GlassBottomBarProvider.Capsule]）。
 */
@Composable
fun GlassHomeTopBar(
    provider: GlassBottomBarProvider,
    source: Any,
    topInset: Dp,
    searchHint: String,
    onSearch: () -> Unit,
    unreadCount: Int,
    onNotification: () -> Unit,
) {
    Row(
        modifier = Modifier
            .testTag(HOME_TOP_ACTIONS_TAG)
            .fillMaxWidth()
            .padding(top = topInset)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        provider.Capsule(
            source,
            Modifier
                .weight(1f)
                .height(GLASS_TOP_BUTTON_SIZE)
                .testTag(HOME_SEARCH_BUTTON_TAG)
                .clip(RoundedCornerShape(50))
                .clickable(role = Role.Button, onClick = onSearch),
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = "搜索")
                Spacer(Modifier.width(12.dp))
                Text(searchHint, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.width(10.dp))
        // 角标画在玻璃圆钮外层，避免被胶囊裁剪
        Box(
            Modifier
                .size(GLASS_TOP_BUTTON_SIZE)
                .testTag(HOME_NOTIFICATION_BUTTON_TAG),
        ) {
            provider.Capsule(
                source,
                Modifier
                    .size(GLASS_TOP_BUTTON_SIZE)
                    .testTag(HOME_NOTIFICATION_BUTTON_CONTENT_TAG)
                    .clip(RoundedCornerShape(50))
                    .clickable(role = Role.Button, onClick = onNotification),
            ) {
                Icon(Icons.Default.Notifications, contentDescription = "通知")
            }
            if (unreadCount > 0) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-2).dp)
                        .testTag(HOME_NOTIFICATION_BADGE_TAG),
                ) {
                    Text("$unreadCount")
                }
            }
        }
    }
}
