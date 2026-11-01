package com.borasarang.daon.ui

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.borasarang.daon.R
import com.borasarang.daon.data.tv.TvChannel
import com.borasarang.daon.ui.theme.Home
import com.borasarang.daon.ui.theme.Kiosk
import kotlinx.coroutines.delay
import java.util.Locale

private const val TV_COLUMNS = 2

/**
 * TV 화면. 16:9 영상 + 채널 타일 + 재생/멈춤·전체 화면.
 *
 * 영상이 나오는 동안에는 자동 전환이 멈춘다(`DashboardScreen` 의 `paused`). 빈 곳을 눌러도
 * 화면이 넘어가지 않는다. 달력으로 돌아가는 길은 상단 `달력` 버튼 하나다.
 */
@Composable
fun TvScreen(
    channels: List<TvChannel>,
    state: TvUiState,
    korean: Boolean,
    player: ExoPlayer?,
    includeRadio: Boolean,
    includeTv: Boolean,
    onSelectChannel: (TvChannel) -> Unit,
    onTogglePlay: () -> Unit,
    onSetFullscreen: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onGoHome: () -> Unit,
    onYouTubeStarted: () -> Unit,
    onYouTubeFailed: (String) -> Unit,
) {
    val context = LocalContext.current
    // 유튜브 채널을 처음 볼 때만 WebView 를 만든다. 다른 채널·화면에서는 메모리를 쓰지 않는다.
    val youtube = remember(state.youtubeId != null) {
        if (state.youtubeId != null) YouTubePlayer(context) else null
    }
    DisposableEffect(youtube) {
        onDispose { youtube?.release() }
    }
    SideEffect {
        youtube?.onStarted = onYouTubeStarted
        youtube?.onError = onYouTubeFailed
    }
    LaunchedEffect(youtube, state.youtubeId) {
        val id = state.youtubeId
        if (youtube != null && id != null) youtube.load(id)
    }

    if (state.fullscreen && (state.playing || state.buffering)) {
        TvFullscreen(
            state = state,
            korean = korean,
            player = player,
            youtube = youtube,
            onExit = { onSetFullscreen(false) },
        )
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Kiosk.color.bg)
            .padding(Home.screenPadding),
    ) {
        ScreenHeader(
            title = stringResource(R.string.screen_tv),
            subtitle = stringResource(
                if (state.playing) R.string.tv_subtitle_playing else R.string.tv_subtitle_stopped,
            ),
            current = ScreenMode.TV,
            onOpenSettings = onOpenSettings,
            onGoHome = onGoHome,
            includeRadio = includeRadio,
            includeTv = includeTv,
        )
        Spacer(Modifier.height(Home.panelGap))

        Row(Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.weight(0.66f).fillMaxHeight()) {
                VideoStage(
                    state = state,
                    korean = korean,
                    player = player,
                    youtube = youtube,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                Spacer(Modifier.height(Home.panelGap))
                TvControlBar(
                    state = state,
                    korean = korean,
                    onTogglePlay = onTogglePlay,
                    onSetFullscreen = onSetFullscreen,
                    onRetry = onRetry,
                )
            }
            Spacer(Modifier.width(Home.panelGap))
            Column(Modifier.weight(0.34f).fillMaxHeight()) {
                Text(
                    text = stringResource(R.string.tv_channel_list),
                    style = Home.radioKicker,
                    color = Kiosk.color.textMuted,
                )
                Spacer(Modifier.height(10.dp))
                if (channels.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.tv_channels_loading),
                            style = Home.radioState,
                            color = Kiosk.color.textMuted,
                        )
                    }
                } else {
                    TvChannelGrid(
                        channels = channels,
                        currentId = state.channel?.id,
                        active = state.playing || state.buffering,
                        korean = korean,
                        onSelectChannel = onSelectChannel,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "▣", fontSize = 18.sp, color = Kiosk.color.textMuted)
            Spacer(Modifier.width(9.dp))
            Text(
                text = stringResource(
                    if (state.playing) R.string.tv_hint_playing else R.string.tv_hint_stopped,
                ),
                style = Home.radioHint,
                color = Kiosk.color.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 영상이 들어가는 검은 판. 16:9 를 넘으면 위아래를 검게 남긴다. */
@Composable
private fun VideoStage(
    state: TvUiState,
    korean: Boolean,
    player: ExoPlayer?,
    youtube: YouTubePlayer?,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(Home.panelRadiusLarge)
    Surface(
        modifier = modifier,
        color = Color.Black,
        shape = shape,
        border = BorderStroke(1.dp, Kiosk.color.rule),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val ratio = 16f / 9f
            val width: Dp
            val height: Dp
            if (maxWidth / maxHeight > ratio) {
                height = maxHeight
                width = maxHeight * ratio
            } else {
                width = maxWidth
                height = maxWidth / ratio
            }
            Box(Modifier.width(width).height(height), contentAlignment = Alignment.Center) {
                when {
                    state.playing || state.buffering -> {
                        if (youtube != null) {
                            YouTubeSurface(youtube, Modifier.fillMaxSize())
                        } else if (player != null) {
                            AndroidView(
                                factory = { context ->
                                    PlayerView(context).apply {
                                        useController = false
                                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                                        this.player = player
                                    }
                                },
                                update = { view -> if (view.player !== player) view.player = player },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.channel?.displayName(korean) ?: "—",
                            style = Home.radioTileName,
                            color = Kiosk.color.textSecondary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.tv_hint_stopped),
                            style = Home.radioState,
                            color = Kiosk.color.textMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvControlBar(
    state: TvUiState,
    korean: Boolean,
    onTogglePlay: () -> Unit,
    onSetFullscreen: (Boolean) -> Unit,
    onRetry: () -> Unit,
) {
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
        modifier = Modifier.fillMaxWidth().height(Home.tvControlBar),
        color = Kiosk.color.surface,
        shape = RoundedCornerShape(Home.panelRadiusLarge),
        border = BorderStroke(1.dp, Kiosk.color.rule),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(
                            if (active) R.string.tv_now_playing else R.string.tv_last_channel,
                        ),
                        style = Home.radioKicker,
                        color = Kiosk.color.textMuted,
                    )
                    if (state.playing) {
                        Spacer(Modifier.width(10.dp))
                        TvLiveBadge()
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = state.channel?.displayName(korean) ?: "—",
                    style = Home.tvName,
                    color = Kiosk.color.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                if (state.errorRes != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(state.errorRes),
                            style = Home.radioState,
                            color = Kiosk.color.danger,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(12.dp))
                        TvRetryChip(onClick = onRetry)
                    }
                } else {
                    Text(
                        text = when {
                            state.buffering -> stringResource(R.string.tv_state_connecting)
                            state.playing -> stringResource(
                                R.string.tv_state_playing,
                                formatTvElapsed(elapsed),
                            )
                            else -> stringResource(R.string.tv_state_stopped)
                        },
                        style = Home.radioState,
                        color = Kiosk.color.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.width(18.dp))
            TvPlayButton(active = active, onClick = onTogglePlay)
            Spacer(Modifier.width(18.dp))
            TvFullscreenButton(
                enabled = active,
                onClick = { onSetFullscreen(true) },
            )
        }
    }
}

@Composable
private fun TvLiveBadge() {
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
            text = stringResource(R.string.tv_live),
            style = Home.radioLive,
            color = Kiosk.color.danger,
        )
    }
}

@Composable
private fun TvRetryChip(onClick: () -> Unit) {
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
            text = stringResource(R.string.tv_retry),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Kiosk.color.accent,
        )
    }
}

@Composable
private fun TvPlayButton(active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(Home.tvPlay)
                .clip(CircleShape)
                .background(Kiosk.color.accent)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            TvPlayGlyph(stop = active)
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = stringResource(if (active) R.string.tv_stop else R.string.tv_play),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Kiosk.color.textSecondary,
        )
    }
}

@Composable
private fun TvPlayGlyph(stop: Boolean) {
    Canvas(Modifier.size(38.dp)) {
        if (stop) {
            val side = size.minDimension * 0.52f
            drawRoundRect(
                color = Kiosk.color.bg,
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
            drawPath(path, Kiosk.color.bg)
        }
    }
}

@Composable
private fun TvFullscreenButton(enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(Home.radioVolumeButton)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(12.dp))
            .background(Kiosk.color.surfaceAlt)
            .border(1.dp, Kiosk.color.rule, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(text = "⛶", fontSize = 21.sp, color = Kiosk.color.textSecondary)
        Text(
            text = stringResource(R.string.tv_fullscreen),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Kiosk.color.textSecondary,
        )
    }
}

@Composable
private fun TvChannelGrid(
    channels: List<TvChannel>,
    currentId: String?,
    active: Boolean,
    korean: Boolean,
    onSelectChannel: (TvChannel) -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Home.tvTileGap)) {
        channels.chunked(TV_COLUMNS).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Home.tvTileGap),
            ) {
                row.forEach { channel ->
                    TvChannelTile(
                        channel = channel,
                        active = active && channel.id == currentId,
                        korean = korean,
                        onClick = { onSelectChannel(channel) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                repeat(TV_COLUMNS - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TvChannelTile(
    channel: TvChannel,
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
        Box(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)) {
            Column(Modifier.align(Alignment.CenterStart)) {
                Text(
                    text = channel.displayName(korean),
                    style = Home.radioTileName,
                    color = if (active) Kiosk.color.accent else Kiosk.color.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (channel.category.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = channel.category,
                        style = Home.radioTileFreq,
                        color = Kiosk.color.textMuted,
                    )
                }
            }
            if (active) {
                Equalizer(Modifier.align(Alignment.TopEnd), barWidth = 3.dp, maxHeight = 18.dp)
                Text(
                    text = stringResource(R.string.tv_channel_playing),
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

/** 전체 화면. 영상만 남기고 `전체 화면 나가기` 버튼을 위에 얹는다. */
@Composable
private fun TvFullscreen(
    state: TvUiState,
    korean: Boolean,
    player: ExoPlayer?,
    youtube: YouTubePlayer?,
    onExit: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (youtube != null) {
            YouTubeSurface(youtube, Modifier.fillMaxSize())
        } else if (player != null) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        this.player = player
                    }
                },
                update = { view -> if (view.player !== player) view.player = player },
                modifier = Modifier.fillMaxSize(),
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Home.screenPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = state.channel?.displayName(korean) ?: "—",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.85f),
            )
            Box(
                modifier = Modifier
                    .height(Home.radioVolumeButton)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onExit)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.tv_exit_fullscreen),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
        if (state.buffering) {
            Text(
                text = stringResource(R.string.tv_state_connecting),
                style = Home.radioState,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

private fun formatTvElapsed(totalSeconds: Long): String {
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}
