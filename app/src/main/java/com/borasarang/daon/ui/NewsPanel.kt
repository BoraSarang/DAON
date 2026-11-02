package com.borasarang.daon.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.borasarang.daon.R
import com.borasarang.daon.data.news.NewsItem
import com.borasarang.daon.data.news.NewsSource
import com.borasarang.daon.ui.theme.Home
import com.borasarang.daon.ui.theme.Kiosk
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 티커를 눌렀을 때 오른쪽에서 열리는 뉴스 패널.
 *
 * 목록(출처별 10건 = 20건)과 기사 본문이 같은 600dp 패널 안에서 바뀐다.
 * 브라우저·다른 앱을 열지 않아 화면 고정(키오스크)이 유지된다.
 */
@Composable
fun NewsPanel(
    news: NewsUiState,
    article: NewsArticleUiState?,
    korean: Boolean,
    onSelect: (NewsItem) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Kiosk.color.scrim)
                .clickable(onClick = onClose),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(600.dp),
            color = Kiosk.color.surface,
            shape = RoundedCornerShape(
                topStart = Home.panelRadiusLarge,
                bottomStart = Home.panelRadiusLarge,
            ),
            border = BorderStroke(1.dp, Kiosk.color.rule),
        ) {
            if (article == null) {
                NewsListView(news, korean, onSelect, onClose, onRefresh)
            } else {
                NewsArticleView(news, article, korean, onSelect, onBack, onClose)
            }
        }
    }
}

@Composable
private fun NewsListView(
    news: NewsUiState,
    korean: Boolean,
    onSelect: (NewsItem) -> Unit,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
) {
    val now = LocalDateTime.now()
    val updated = news.updatedAt?.let { clockLabel(it) }

    Column(Modifier.fillMaxSize()) {
        PanelHead(
            title = stringResource(R.string.news_panel_title),
            onBack = null,
            onClose = onClose,
        )
        Text(
            text = listMeta(news, updated, korean),
            modifier = Modifier.padding(horizontal = 26.dp),
            fontSize = 13.5.sp,
            lineHeight = 18.sp,
            color = Kiosk.color.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))

        if (news.items.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.news_panel_empty),
                        fontSize = 16.sp,
                        color = Kiosk.color.textSecondary,
                    )
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onRefresh),
                        color = Kiosk.color.accentSoft,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Kiosk.color.accent.copy(alpha = 0.4f)),
                    ) {
                        Text(
                            text = stringResource(R.string.news_retry),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Kiosk.color.accent,
                        )
                    }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(news.items, key = { it.id }) { item ->
                    NewsRow(item = item, now = now, korean = korean, onClick = { onSelect(item) })
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Kiosk.color.rule),
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.news_panel_hint),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp, vertical = 18.dp),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = Kiosk.color.textMuted,
        )
    }
}

@Composable
private fun NewsRow(
    item: NewsItem,
    now: LocalDateTime,
    korean: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimeColumn(item = item, now = now, korean = korean)
        Spacer(Modifier.width(14.dp))
        Text(
            text = item.title,
            modifier = Modifier.weight(1f),
            fontSize = 18.sp,
            lineHeight = 23.sp,
            fontWeight = FontWeight.SemiBold,
            color = Kiosk.color.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(12.dp))
        SourceChip(item.source, korean)
        Spacer(Modifier.width(8.dp))
        Text(
            text = "›",
            fontSize = 24.sp,
            color = Kiosk.color.textMuted,
        )
    }
}

/** `어제 17:53` 처럼 긴 표기는 두 줄로 쌓아 52dp 열 안에 넣는다. */
@Composable
private fun TimeColumn(item: NewsItem, now: LocalDateTime, korean: Boolean) {
    val parts = item.timeLabel(now, korean).split(' ')
    Column(
        modifier = Modifier.widthIn(min = 52.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        if (parts.size == 2) {
            Text(
                text = parts[0],
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Kiosk.color.textMuted,
                maxLines = 1,
            )
            Text(
                text = parts[1],
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Kiosk.color.textSecondary,
                maxLines = 1,
            )
        } else {
            Text(
                text = parts[0],
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Kiosk.color.textSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun SourceChip(source: NewsSource, korean: Boolean) {
    val sbs = source == NewsSource.SBS
    Surface(
        color = if (sbs) Kiosk.color.accentSoft else Kiosk.color.surfaceAlt,
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(
            1.dp,
            if (sbs) Kiosk.color.accent.copy(alpha = 0.42f) else Kiosk.color.rule,
        ),
    ) {
        Text(
            text = source.displayName(korean),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (sbs) Kiosk.color.accent else Kiosk.color.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun NewsArticleView(
    news: NewsUiState,
    article: NewsArticleUiState,
    korean: Boolean,
    onSelect: (NewsItem) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    val item = article.item
    val now = LocalDateTime.now()
    val index = news.items.indexOfFirst { it.id == item.id }
    val previous = if (index > 0) news.items.getOrNull(index - 1) else null
    val next = if (index >= 0) news.items.getOrNull(index + 1) else null

    Column(Modifier.fillMaxSize()) {
        PanelHead(
            title = null,
            onBack = onBack,
            onClose = onClose,
        )
        Text(
            text = listOf(
                item.source.displayName(korean),
                item.timeLabel(now, korean),
                item.source.displayCategory(korean),
            ).joinToString(" · "),
            modifier = Modifier.padding(horizontal = 26.dp),
            fontSize = 13.5.sp,
            color = Kiosk.color.textMuted,
            maxLines = 1,
        )
        if (article.content?.usedSummary == true) {
            Text(
                text = stringResource(R.string.news_article_summary_note),
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 2.dp),
                fontSize = 12.sp,
                color = Kiosk.color.textMuted,
            )
        }
        Spacer(Modifier.height(10.dp))

        val content = article.content
        when {
            article.loading || content == null -> Box(
                Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.news_article_loading),
                    fontSize = 15.sp,
                    color = Kiosk.color.textMuted,
                )
            }

            content.paragraphs.isEmpty() -> Box(
                Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.news_article_empty),
                    fontSize = 15.sp,
                    color = Kiosk.color.textMuted,
                )
            }

            else -> Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 26.dp),
            ) {
                Text(
                    text = item.title,
                    fontSize = 33.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = Kiosk.color.textPrimary,
                )
                Spacer(Modifier.height(18.dp))
                content.paragraphs.forEach { paragraph ->
                    Text(
                        text = paragraph,
                        fontSize = 18.sp,
                        lineHeight = 29.sp,
                        color = Kiosk.color.textPrimary,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArticleNavButton(
                text = stringResource(R.string.news_article_previous),
                enabled = previous != null,
                onClick = { previous?.let(onSelect) },
            )
            Text(
                text = if (index >= 0) {
                    stringResource(R.string.news_article_index, index + 1, news.items.size)
                } else {
                    ""
                },
                modifier = Modifier.width(64.dp),
                fontSize = 13.sp,
                color = Kiosk.color.textMuted,
                maxLines = 1,
            )
            ArticleNavButton(
                text = stringResource(R.string.news_article_next),
                enabled = next != null,
                onClick = { next?.let(onSelect) },
                alignEnd = true,
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ArticleNavButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    alignEnd: Boolean = false,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) Kiosk.color.accent else Kiosk.color.textMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun PanelHead(
    title: String?,
    onBack: (() -> Unit)?,
    onClose: () -> Unit,
) {
    val closeLabel = stringResource(R.string.label_close)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "‹",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                    color = Kiosk.color.textSecondary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.news_article_back),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Kiosk.color.textSecondary,
                    maxLines = 1,
                )
            }
        } else {
            Text(
                text = title.orEmpty(),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                color = Kiosk.color.textPrimary,
                maxLines = 1,
            )
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClose)
                .semantics { contentDescription = closeLabel },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✕",
                fontSize = 20.sp,
                color = Kiosk.color.textSecondary,
            )
        }
    }
}

private fun listMeta(news: NewsUiState, updated: String?, korean: Boolean): String {
    val sources = NewsSource.entries.joinToString(" · ") { it.displayName(korean) }
    val status = when {
        news.loading -> if (korean) "갱신 중" else "Updating"
        news.failed && updated != null ->
            if (korean) "마지막 소식 $updated" else "Last news $updated"
        updated != null -> if (korean) "$updated 갱신" else "Updated $updated"
        else -> ""
    }
    val count = if (korean) "${news.items.size}건" else "${news.items.size} items"
    return listOf(sources, status, count).filter { it.isNotEmpty() }.joinToString(" · ")
}

private fun clockLabel(instant: Instant): String =
    DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
        .withZone(ZoneId.systemDefault())
        .format(instant)
