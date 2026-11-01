package com.borasarang.daon.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.borasarang.daon.R
import com.borasarang.daon.data.news.NewsItem
import com.borasarang.daon.ui.theme.Kiosk
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 달력 상단의 뉴스 티커.
 *
 * 27px(18sp) 제목이 왼쪽으로 45px/s(30dp/s)로 천천히 흐른다. 리컴포지션 없이
 * [graphicsLayer] 로만 움직이고, 30fps 상한을 둬 상시 켜짐 기기의 발열을 줄인다.
 * [paused] 면 오프셋을 그대로 두고 멈춘다(패널·설정·다른 화면).
 */
@Composable
fun NewsTicker(
    items: List<NewsItem>,
    updatedAt: Instant?,
    failed: Boolean,
    paused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val display = remember(items) { repeatToMinimum(items, MIN_TICKER_ITEMS) }
    val labelColor = if (failed) Kiosk.color.textMuted else Kiosk.color.accent
    val surface = Kiosk.color.surface

    Box(
        modifier = modifier
            .height(TOUCH_HEIGHT)
            .clip(RoundedCornerShape(12.dp))
            // 목록이 비어 있어도 눌러서 `다시 시도`를 할 수 있어야 한다.
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(VISUAL_HEIGHT),
            color = surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Kiosk.color.rule),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.padding(start = 15.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(6.dp).background(labelColor, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.news_ticker_label),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = labelColor,
                        maxLines = 1,
                    )
                }
                Box(
                    Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(Kiosk.color.rule),
                )
                if (display.isEmpty()) {
                    Text(
                        text = stringResource(R.string.news_ticker_empty),
                        modifier = Modifier.padding(start = 16.dp),
                        fontSize = 18.sp,
                        color = Kiosk.color.textMuted,
                        maxLines = 1,
                    )
                } else {
                    Marquee(
                        items = display,
                        paused = paused,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                if (failed && updatedAt != null) {
                    Text(
                        text = clockLabel(updatedAt),
                        modifier = Modifier.padding(end = 14.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Kiosk.color.textMuted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun Marquee(
    items: List<NewsItem>,
    paused: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val speedPxPerSecond = with(density) { SPEED.toPx() }
    val contentKey = remember(items) { items.joinToString("|") { it.id } }

    var halfWidth by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableFloatStateOf(0f) }
    val surface = Kiosk.color.surface

    // 새 소식을 받으면 처음부터 다시 흐른다.
    LaunchedEffect(contentKey) { offset = 0f }

    LaunchedEffect(contentKey, paused, halfWidth) {
        if (paused || halfWidth <= 1f) return@LaunchedEffect
        var lastApplied = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val seconds = (now - lastApplied) / 1_000_000_000f
            // 30fps 상한. 33ms 가 지나기 전에는 레이어를 건드리지 않는다.
            if (seconds < 1f / TARGET_FPS) continue
            lastApplied = now
            offset = (offset + speedPxPerSecond * seconds) % halfWidth
        }
    }

    Box(
        // 제목 줄을 티커 높이의 세로 가운데에 둔다. 기본(Top)이면 글자가 위로 붙는다.
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .clipToBounds()
            .drawWithContent {
                drawContent()
                val left = with(density) { LEFT_FADE.toPx() }
                val right = with(density) { RIGHT_FADE.toPx() }
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(surface, surface.copy(alpha = 0f)),
                        startX = 0f,
                        endX = left,
                    ),
                    size = Size(left, size.height),
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(surface.copy(alpha = 0f), surface),
                        startX = size.width - right,
                        endX = size.width,
                    ),
                    topLeft = Offset(size.width - right, 0f),
                    size = Size(right, size.height),
                )
            },
    ) {
        Row(
            modifier = Modifier.graphicsLayer { translationX = -offset },
        ) {
            MarqueeHalf(
                items = items,
                gap = GAP,
                modifier = Modifier.onSizeChanged { halfWidth = it.width.toFloat() },
            )
            MarqueeHalf(items = items, gap = GAP)
        }
    }
}

@Composable
private fun MarqueeHalf(
    items: List<NewsItem>,
    gap: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    Row(modifier) {
        items.forEach { item ->
            Text(
                text = item.title,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                color = Kiosk.color.textPrimary,
                maxLines = 1,
                softWrap = false,
            )
            Spacer(Modifier.width(gap))
        }
    }
}

/** 제목이 너무 적으면 티커가 비어 보이므로 최소 몇 건이 되도록 되풀이한다. */
private fun repeatToMinimum(items: List<NewsItem>, minimum: Int): List<NewsItem> {
    if (items.isEmpty() || items.size >= minimum) return items
    return buildList {
        while (size < minimum) addAll(items)
    }
}

private fun clockLabel(instant: Instant): String =
    DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
        .withZone(ZoneId.systemDefault())
        .format(instant)

private const val TARGET_FPS = 30f
private const val MIN_TICKER_ITEMS = 5

private val VISUAL_HEIGHT = 43.dp
private val TOUCH_HEIGHT = 48.dp
private val SPEED = 30.dp
private val GAP = 51.dp
private val LEFT_FADE = 23.dp
private val RIGHT_FADE = 41.dp
