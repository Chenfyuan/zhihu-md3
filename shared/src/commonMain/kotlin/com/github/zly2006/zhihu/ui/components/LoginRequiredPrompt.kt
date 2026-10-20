package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.requestLoginNavigation

const val LOGIN_REQUIRED_PROMPT_TAG = "loginRequiredPrompt"
const val RATE_LIMITED_PROMPT_TAG = "rateLimitedPrompt"

/** 匿名访问被知乎拒绝时的温和提示：说明原因 + 登录按钮，不阻断其它页面。 */
@Composable
fun LoginRequiredPrompt(
    modifier: Modifier = Modifier,
    message: String = "这部分内容登录后可看",
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .testTag(LOGIN_REQUIRED_PROMPT_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(message, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            "知乎不向未登录用户提供这部分内容。如已登录，可尝试重新登录。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        FilledTonalButton(onClick = ::requestLoginNavigation) { Text("登录") }
    }
}

/** 知乎返回 40362 / 10003（请求被限制）时的提示：只给重试，不引导登录，也不尝试任何绕过。 */
@Composable
fun RateLimitedPrompt(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    message: String = "请求被限制，稍后重试",
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .testTag(RATE_LIMITED_PROMPT_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(message, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            "知乎暂时限制了这类请求，等一会儿再试通常就能恢复。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        FilledTonalButton(onClick = onRetry) { Text("重试") }
    }
}
