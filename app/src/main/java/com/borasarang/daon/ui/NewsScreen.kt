package com.borasarang.daon.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.borasarang.daon.R
import com.borasarang.daon.data.news.NewsSource
import com.borasarang.daon.ui.theme.Home
import com.borasarang.daon.ui.theme.Kiosk
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 뉴스 화면에서 한 기사를 보여 주는 시간. 달력·뉴스 화면의 전체 머무는 시간과는 별개다. */
private const val ARTICLE_MS = 10_000L

/** 좌우로 이만큼(px) 넘기면 이전·다음 기사로 본다. */
private const val SWIPE_THRESHOLD = 90f

/**
 * 자동 전환에 들어가는 뉴스 화면.
 *
 * 제목 아래에 기사 전문을 큰 글씨로 보여 준다. 본문이 길면 위아래로 스크롤해 읽고,
 * 스크롤을 시작하면 자동 전환이 멈춘다. 멈춘 상태에서는 좌우로 밀어 이전·다음 기사를 본다.
 */
@Composable
fun NewsScreen(
    nowState: State<LocalDateTime>,
    news: NewsUiState,
    article: NewsArticleUiState?,
    index: Int,
    paused: Boolean,
    korean: Boolean,
    includeRadio: Boolean = true,
    includeTv: Boolean = true,
    includeNews: Boolean = true,
    onOpenSettings: () -> Unit,
    onGoHome: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val now by nowState
    val count = news.items.size
    val safeIndex = if (count == 0) 0 else index % count
    val item = news.items.getOrNull(safeIndex)
    val content = article?.content

    // 기사마다 스크롤과 진행 막대를 새로 시작한다. 기사가 바뀌면 본문은 맨 위부터 보인다.
    val scrollState = remember(item?.id) { ScrollState(0) }
    val progress = remember(item?.id) { Animatable(0f) }
    var dragTotal by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(item?.id, paused) {
        if (paused) {
            progress.stop()
        } else if (progress.value < 1f) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (ARTICLE_MS * (1f - progress.value)).toInt(),
                    easing = LinearEasing,
                ),
            )
        }
    }
    // 본문을 손으로 밀어 읽기 시작하면 화면이 넘어가지 않게 멈춘다.
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) onPause()
    }

    val updated = news.updatedAt?.let { feedClockLabel(it) }
    val sources = NewsSource.entries.joinToString(" · ") { it.displayName(korean) }
    val subtitle = listOfNotNull(
        sources,
        updated?.let { if (korean) "$it 기준" else "as of $it" },
    ).joinToString(" · ")

    Column(
        Modifier
            .fillMaxSize()
            .background(Kiosk.color.bg)
            .padding(Home.screenPadding),
    ) {
        ScreenHeader(
            title = stringResource(R.string.screen_news),
            subtitle = subtitle,
            current = ScreenMode.NEWS,
            onOpenSettings = onOpenSettings,
            onGoHome = onGoHome,
            includeRadio = includeRadio,
            includeTv = includeTv,
            includeNews = includeNews,
        )
        Spacer(Modifier.height(Home.panelGap))

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            color = Kiosk.color.surface,
            shape = RoundedCornerShape(Home.panelRadiusLarge),
            border = BorderStroke(1.dp, Kiosk.color.rule),
        ) {
            Box(Modifier.fillMaxSize()) {
                // 왼쪽 강조선. 달력 홈의 카드와 같은 결을 유지한다.
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(6.dp)
                        .background(Kiosk.color.accent),
                )
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(start = 40.dp, end = 40.dp, top = 30.dp, bottom = 22.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (item != null) {
                            SourceChip(item.source, korean)
                            Text(
                                text = item.timeLabel(now, korean),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Kiosk.color.textMuted,
                                maxLines = 1,
                            )
                            Text(
                                text = item.source.displayCategory(korean),
                                fontSize = 15.sp,
                                color = Kiosk.color.textMuted,
                                maxLines = 1,
                            )
                        }
                        if (paused) {
                            Surface(
                                color = Kiosk.color.accentSoft,
                                shape = RoundedCornerShape(7.dp),
                                border = BorderStroke(1.dp, Kiosk.color.accent.copy(alpha = 0.42f)),
                            ) {
                                Text(
                                    text = stringResource(R.string.news_feed_paused),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Kiosk.color.accent,
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        if (item != null) {
                            Text(
                                text = stringResource(
                                    R.string.news_article_index,
                                    safeIndex + 1,
                                    count,
                                ),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Kiosk.color.textMuted,
                                maxLines = 1,
                            )
                        }
                    }

                    if (item == null) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.news_feed_empty),
                                fontSize = 24.sp,
                                color = Kiosk.color.textSecondary,
                            )
                        }
                    } else {
                        Text(
                            text = item.title,
                            modifier = Modifier.padding(top = 18.dp),
                            fontSize = 40.sp,
                            lineHeight = 54.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.8).sp,
                            color = Kiosk.color.textPrimary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Box(Modifier.fillMaxWidth().weight(1f).padding(top = 18.dp)) {
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .padding(end = 12.dp),
                            ) {
                                val paragraphs = content?.paragraphs.orEmpty()
                                if (paragraphs.isEmpty()) {
                                    // 본문을 받는 동안에도 요약을 먼저 보여 줘 화면이 비지 않는다.
                                    Text(
                                        text = item.summary,
                                        fontSize = 24.sp,
                                        lineHeight = 38.sp,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = (-0.3).sp,
                                        color = Kiosk.color.textSecondary,
                                    )
                                } else {
                                    paragraphs.forEach { paragraph ->
                                        Text(
                                            text = paragraph,
                                            fontSize = 24.sp,
                                            lineHeight = 38.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = (-0.3).sp,
                                            color = Kiosk.color.textSecondary,
                                        )
                                        Spacer(Modifier.height(14.dp))
                                    }
                                }
                                Spacer(Modifier.height(40.dp))
                            }
                            // 아래로 더 있다는 것을 알리는 부드러운 페이드.
                            Box(
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Kiosk.color.surface),
                                        ),
                                    ),
                            )
                        }

                        if (paused) {
                            PausedFooter(onResume = onResume)
                        } else {
                            RunningFooter(progress = progress.value)
                        }
                    }
                }

                // 멈춘 상태에서만 좌우 밀기로 기사를 넘긴다. 자동 전환 중에는 오작동을 막는다.
                if (paused && count > 1) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .pointerInput(item?.id) {
                                detectHorizontalDragGestures(
                                    onDragStart = { dragTotal = 0f },
                                    onHorizontalDrag = { _, amount -> dragTotal += amount },
                                    onDragCancel = { dragTotal = 0f },
                                    onDragEnd = {
                                        if (dragTotal > SWIPE_THRESHOLD) {
                                            onPrevious()
                                        } else if (dragTotal < -SWIPE_THRESHOLD) {
                                            onNext()
                                        }
                                        dragTotal = 0f
                                    },
                                )
                            },
                    )
                }
            }
        }
    }
}

private fun feedClockLabel(instant: Instant): String =
    DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
        .withZone(ZoneId.systemDefault())
        .format(instant)

@Composable
private fun RunningFooter(progress: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Kiosk.color.surfaceAlt),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Kiosk.color.accent),
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = stringResource(R.string.news_feed_progress),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Kiosk.color.textMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun PausedFooter(onResume: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.news_feed_swipe_hint),
            modifier = Modifier.weight(1f),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Kiosk.color.textSecondary,
            maxLines = 1,
        )
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .clickable(onClick = onResume),
            color = Kiosk.color.accentSoft,
            shape = RoundedCornerShape(9.dp),
            border = BorderStroke(1.dp, Kiosk.color.accent.copy(alpha = 0.42f)),
        ) {
            Text(
                text = stringResource(R.string.news_feed_resume),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Kiosk.color.accent,
                maxLines = 1,
            )
        }
    }
}
