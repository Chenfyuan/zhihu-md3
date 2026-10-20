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
