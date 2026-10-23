package com.github.zly2006.zhihu.ui.article

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.data.VoteUpState
import zhihu.shared.generated.resources.Res
import zhihu.shared.generated.resources.ic_vote_down_24dp
import zhihu.shared.generated.resources.ic_vote_up_24dp
import com.github.zly2006.zhihu.ui.components.GlassBottomBarProvider
import org.jetbrains.compose.resources.painterResource

/** 玻璃悬浮按钮高度（圆形按钮直径），底部留白按此计算。 */
val GLASS_ACTION_BUTTON_SIZE: Dp = 48.dp

/** 悬浮按钮距底边（导航栏之上）的间距。 */
val GLASS_ACTION_BUTTON_MARGIN: Dp = 12.dp

const val GLASS_ACTION_VOTE_TAG = "glass_action_vote"
const val GLASS_ACTION_FAVORITE_TAG = "glass_action_favorite"
const val GLASS_ACTION_COMMENT_TAG = "glass_action_comment"
const val GLASS_ACTION_MORE_TAG = "glass_action_more"

/**
 * 内容页底部的四个玻璃悬浮胶囊：赞踩合体胶囊（左），收藏圆钮、评论胶囊（带数）、更多圆钮（右）。
 * 与首页刷新/+ 悬浮按钮同款玻璃；选中态（赞/踩/收藏）保留原有实色，保证一眼可辨。
 */
@Composable
fun GlassArticleActionBar(
    provider: GlassBottomBarProvider,
    source: Any,
    voteUpState: VoteUpState,
    voteUpCount: Int,
    commentCount: Int,
    isFavorited: Boolean,
    selectedVoteColor: Color,
    onVote: (VoteUpState) -> Unit,
    onFavorite: () -> Unit,
    onComment: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        provider.Capsule(source, Modifier.height(GLASS_ACTION_BUTTON_SIZE).testTag(GLASS_ACTION_VOTE_TAG)) {
            Row(Modifier.fillMaxHeight().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                val up = voteUpState == VoteUpState.Up
                val down = voteUpState == VoteUpState.Down
                Segment(
                    selected = up,
                    selectedColor = selectedVoteColor,
                    contentDescription = "赞同",
                    onClick = { onVote(if (up) VoteUpState.Neutral else VoteUpState.Up) },
                ) {
                    Icon(painterResource(Res.drawable.ic_vote_up_24dp), "赞同", Modifier.size(22.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(voteUpCount.toString(), style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.width(2.dp))
                Segment(
                    selected = down,
                    selectedColor = selectedVoteColor,
                    contentDescription = "反对",
                    onClick = { onVote(if (down) VoteUpState.Neutral else VoteUpState.Down) },
                ) {
                    Icon(painterResource(Res.drawable.ic_vote_down_24dp), "反对", Modifier.size(22.dp))
                    if (down) {
                        Spacer(Modifier.width(4.dp))
                        Text("反对", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            provider.Capsule(
                source,
                Modifier.size(GLASS_ACTION_BUTTON_SIZE).testTag(GLASS_ACTION_FAVORITE_TAG).clip(RoundedCornerShape(50))
                    .clickable(role = Role.Button, onClick = onFavorite),
            ) {
                if (isFavorited) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Bookmark, "收藏", tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                } else {
                    Icon(Icons.Filled.BookmarkBorder, "收藏")
                }
            }
            provider.Capsule(
                source,
                Modifier.height(GLASS_ACTION_BUTTON_SIZE).testTag(GLASS_ACTION_COMMENT_TAG).clip(RoundedCornerShape(50))
                    .clickable(role = Role.Button, onClick = onComment),
            ) {
                Row(Modifier.padding(start = 14.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Comment, "评论", Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("$commentCount", style = MaterialTheme.typography.titleSmall)
                }
            }
            provider.Capsule(
                source,
                Modifier.size(GLASS_ACTION_BUTTON_SIZE).testTag(GLASS_ACTION_MORE_TAG).clip(RoundedCornerShape(50))
                    .clickable(role = Role.Button, onClick = onMore),
            ) {
                Icon(Icons.Filled.MoreVert, "更多选项")
            }
        }
    }
}

@Composable
private fun Segment(
    selected: Boolean,
    selectedColor: Color,
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(50))
            .then(if (selected) Modifier.background(selectedColor) else Modifier)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (selected) Color.White else LocalContentColor.current
        androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides tint) { content() }
    }
}
