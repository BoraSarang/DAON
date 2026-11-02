package com.borasarang.daon.ui

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.borasarang.daon.R
import com.borasarang.daon.data.radio.RadioChannel
import com.borasarang.daon.ui.theme.Home
import com.borasarang.daon.ui.theme.Kiosk
import kotlinx.coroutines.delay
import java.util.Locale

private const val CHANNEL_COLUMNS = 5

/**
 * 라디오 화면. 지금 방송 패널 + 채널 10칸(5×2) + 안내 한 줄.
 *
 * 화면 빈 곳을 눌러도 달력으로 가지 않는다. 터치는 채널 선택·재생에만 쓴다.
 * 달력으로 돌아가는 길은 상단 바의 `달력` 버튼 하나뿐이다.
 */
@Composable
fun RadioScreen(
    channels: List<RadioChannel>,
    state: RadioUiState,
    korean: Boolean,
    volume: Int,
    includeRadio: Boolean,
    includeTv: Boolean,
    includeNews: Boolean = true,
    onSelectChannel: (RadioChannel) -> Unit,
    onTogglePlay: () -> Unit,
    onSetVolume: (Int) -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onGoHome: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Kiosk.color.bg)
            .padding(Home.screenPadding),
    ) {
        ScreenHeader(
            title = stringResource(R.string.screen_radio),
            subtitle = stringResource(
                if (state.playing) R.string.radio_subtitle_playing else R.string.radio_subtitle_stopped,
            ),
            current = ScreenMode.RADIO,
            onOpenSettings = onOpenSettings,
            onGoHome = onGoHome,
            includeRadio = includeRadio,
            includeTv = includeTv,
            includeNews = includeNews,
        )
        Spacer(Modifier.height(Home.panelGap))

        NowPlayingPanel(
            state = state,
            korean = korean,
            volume = volume,
            onTogglePlay = onTogglePlay,
            onSetVolume = onSetVolume,
            onRetry = onRetry,
        )
        Spacer(Modifier.height(Home.panelGap))

        if (channels.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.radio_channels_loading),
                    style = Home.radioState,
                    color = Kiosk.color.textMuted,
                )
            }
        } else {
            ChannelGrid(
                channels = channels,
                currentId = state.channel?.id,
                active = state.playing || state.buffering,
                korean = korean,
                onSelectChannel = onSelectChannel,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "♪",
                fontSize = 19.sp,
                color = Kiosk.color.textMuted,
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = stringResource(
                    if (state.playing) R.string.radio_hint_playing else R.string.radio_hint_stopped,
                ),
                style = Home.radioHint,
                color = Kiosk.color.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NowPlayingPanel(
    state: RadioUiState,
    korean: Boolean,
    volume: Int,
    onTogglePlay: () -> Unit,
    onSetVolume: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    val channel = state.channel
    val active = state.playing || state.buffering
    var elapsed by remember { mutableStateOf(0L) }
    LaunchedEffect(state.playing, state.startedAtMs) {
        if (!state.playing || state.startedAtMs <= 0L) {
            elapsed = 0L
            return@LaunchedEffect
        }
        while (true) {
            elapsed = ((SystemClock.elapsedRealtime() - state.startedAtMs) / 1_000L)
                .coerceAtLeast(0L)
            delay(1_000L)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().height(Home.radioNowPanel),
        color = Kiosk.color.surface,
        shape = RoundedCornerShape(Home.panelRadiusLarge),
        border = BorderStroke(1.dp, Kiosk.color.rule),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 29.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(
                            if (state.playing || state.buffering) {
                                R.string.radio_now_playing
                            } else {
                                R.string.radio_last_channel
                            },
                        ),
                        style = Home.radioKicker,
                        color = Kiosk.color.textMuted,
                    )
                    if (state.playing) {
                        Spacer(Modifier.width(10.dp))
                        LiveBadge()
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = channel?.displayName(korean) ?: "—",
                    style = Home.radioName,
                    color = Kiosk.color.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.radio_freq, channel?.freq ?: "—"),
                        style = Home.radioFreq,
                        color = Kiosk.color.textSecondary,
                    )
                    Spacer(Modifier.width(14.dp))
                    Box(Modifier.width(1.dp).height(22.dp).background(Kiosk.color.rule))
                    Spacer(Modifier.width(14.dp))
                    if (state.errorRes != null) {
                        Text(
                            text = stringResource(state.errorRes),
                            style = Home.radioState,
                            color = Kiosk.color.danger,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(12.dp))
                        RetryChip(onClick = onRetry)
                    } else {
                        Text(
                            text = when {
                                state.buffering -> stringResource(R.string.radio_state_connecting)
                                state.playing -> stringResource(
                                    R.string.radio_state_playing,
                                    formatElapsed(elapsed),
                                )
                                else -> stringResource(R.string.radio_state_stopped)
                            },
                            style = Home.radioState,
                            color = Kiosk.color.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(Modifier.width(26.dp))
            PlayButton(active = active, onClick = onTogglePlay)
            Spacer(Modifier.width(26.dp))
            VolumeControl(volume = volume, onSetVolume = onSetVolume)
        }
    }
}

@Composable
private fun LiveBadge() {
    Row(
        modifier = Modifier
            .height(25.dp)
            .clip(CircleShape)
            .background(Kiosk.color.danger.copy(alpha = 0.14f))
            .border(1.dp, Kiosk.color.danger.copy(alpha = 0.45f), CircleShape)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Kiosk.color.danger),
        )
        Text(
            text = stringResource(R.string.radio_live),
            style = Home.radioLive,
            color = Kiosk.color.danger,
        )
    }
}

@Composable
private fun RetryChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Kiosk.color.accentSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.radio_retry),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Kiosk.color.accent,
        )
    }
}

@Composable
private fun PlayButton(active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(Home.radioPlay)
                .clip(CircleShape)
                .background(Kiosk.color.accent)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            PlayGlyph(stop = active)
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = stringResource(if (active) R.string.radio_stop else R.string.radio_play),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Kiosk.color.textSecondary,
        )
    }
}

@Composable
private fun PlayGlyph(stop: Boolean) {
    Canvas(Modifier.size(50.dp)) {
        if (stop) {
            val side = size.minDimension * 0.52f
            drawRoundRect(
                color = Kiosk.color.onAccent,
                topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f),
                size = Size(side, side),
                cornerRadius = CornerRadius(side * 0.16f),
            )
        } else {
            val path = Path().apply {
                moveTo(size.width * 0.30f, size.height * 0.22f)
                lineTo(size.width * 0.80f, size.height * 0.50f)
                lineTo(size.width * 0.30f, size.height * 0.78f)
                close()
            }
            drawPath(path, Kiosk.color.onAccent)
        }
    }
}

@Composable
private fun VolumeControl(volume: Int, onSetVolume: (Int) -> Unit) {
    Column(Modifier.width(Home.radioVolumeWidth)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.radio_volume),
                style = Home.radioKicker,
                color = Kiosk.color.textMuted,
            )
            Text(
                text = "${volume * 10}%",
                style = Home.radioState,
                color = Kiosk.color.textSecondary,
            )
        }
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            VolumeButton(symbol = "−", description = stringResource(R.string.radio_volume_down)) {
                onSetVolume(volume - 1)
            }
            Spacer(Modifier.width(9.dp))
            Row(
                modifier = Modifier.weight(1f).height(Home.radioVolumeButton),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                val heights = listOf(8f, 11f, 15f, 18f, 21f, 25f, 29f, 33f, 37f, 41f)
                heights.forEachIndexed { index, height ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(height.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (index < volume) Kiosk.color.accent else Kiosk.color.surfaceAlt,
                            ),
                    )
                }
            }
            Spacer(Modifier.width(9.dp))
            VolumeButton(symbol = "+", description = stringResource(R.string.radio_volume_up)) {
                onSetVolume(volume + 1)
            }
        }
    }
}

@Composable
private fun VolumeButton(symbol: String, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(Home.radioVolumeButton)
            .clip(RoundedCornerShape(12.dp))
            .background(Kiosk.color.surfaceAlt)
            .border(1.dp, Kiosk.color.rule, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semanticsDescription(description),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            fontSize = 25.sp,
            fontWeight = FontWeight.SemiBold,
            color = Kiosk.color.textSecondary,
        )
    }
}

/** 접근성 설명. 시각적으로는 보이지 않는다. */
private fun Modifier.semanticsDescription(description: String): Modifier =
    this.then(
        Modifier.semantics { contentDescription = description },
    )

@Composable
private fun ChannelGrid(
    channels: List<RadioChannel>,
    currentId: String?,
    active: Boolean,
    korean: Boolean,
    onSelectChannel: (RadioChannel) -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Home.radioTileGap)) {
        channels.chunked(CHANNEL_COLUMNS).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Home.radioTileGap),
            ) {
                row.forEach { channel ->
                    ChannelTile(
                        channel = channel,
                        active = active && channel.id == currentId,
                        korean = korean,
                        onClick = { onSelectChannel(channel) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                repeat(CHANNEL_COLUMNS - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ChannelTile(
    channel: RadioChannel,
    active: Boolean,
    korean: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier.clip(shape).clickable(onClick = onClick),
        color = if (active) Kiosk.color.accentSoft else Kiosk.color.surface,
        shape = shape,
        border = BorderStroke(1.dp, if (active) Kiosk.color.accent else Kiosk.color.rule),
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 19.dp, vertical = 16.dp)) {
            Column(Modifier.align(Alignment.CenterStart)) {
                Text(
                    text = channel.displayName(korean),
                    style = Home.radioTileName,
                    color = if (active) Kiosk.color.accent else Kiosk.color.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    Text(
                        text = "FM ",
                        style = Home.radioTileFreq,
                        color = Kiosk.color.textMuted,
                    )
                    Text(
                        text = channel.freq,
                        style = Home.radioTileFreq,
                        color = if (active) Kiosk.color.accent else Kiosk.color.textSecondary,
                    )
                }
            }
            if (active) {
                Equalizer(Modifier.align(Alignment.TopEnd), barWidth = 3.dp, maxHeight = 18.dp)
                Text(
                    text = stringResource(R.string.radio_channel_playing),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp,
                    color = Kiosk.color.accent,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }
        }
    }
}

private fun formatElapsed(totalSeconds: Long): String {
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}
