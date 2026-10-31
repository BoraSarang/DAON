package com.borasarang.daon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.borasarang.daon.R
import com.borasarang.daon.ui.theme.Home
import com.borasarang.daon.ui.theme.Kiosk

/** 화면 점. [includeRadio] 가 false 면 라디오를 뺀 3개만 보여 준다. */
@Composable
fun ScreenDots(current: ScreenMode, includeRadio: Boolean = true) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        ScreenMode.order(includeRadio).forEach { mode ->
            Box(
                Modifier
                    .size(Home.dot)
                    .clip(CircleShape)
                    .background(if (mode == current) Kiosk.color.accent else Kiosk.color.rule),
            )
        }
    }
}

/** 상단 바 오른쪽 설정 버튼. 세 화면이 같은 크기·위치를 쓴다. */
@Composable
fun SettingsButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(Home.settingsHeight)
            .clip(RoundedCornerShape(10.dp))
            .background(Kiosk.color.surface)
            .border(1.dp, Kiosk.color.rule, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.label_settings),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Kiosk.color.textSecondary,
        )
    }
}

/**
 * 날씨·시계 화면의 상단 바. 달력 화면은 날짜·음력·절기를 함께 보여주므로 자체 상단 바를 쓴다.
 *
 * [onGoHome] 이 있으면 설정 왼쪽에 `달력` 버튼을 둔다. `그 화면에서 멈추기` 모드에서도
 * 대시보드로 돌아갈 길이 항상 보이게 하기 위해서다.
 */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String?,
    current: ScreenMode,
    onOpenSettings: () -> Unit,
    onGoHome: (() -> Unit)? = null,
    includeRadio: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = Home.topBar),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = Home.date,
                color = Kiosk.color.textPrimary,
                maxLines = 1,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = Home.sub,
                    color = Kiosk.color.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onGoHome != null) {
            HomeButton(onClick = onGoHome)
            Spacer(Modifier.width(10.dp))
        }
        ScreenDots(current, includeRadio)
        Spacer(Modifier.width(18.dp))
        SettingsButton(onClick = onOpenSettings)
    }
}

/** 날씨·시계 화면에서 달력 홈으로 돌아가는 버튼. */
@Composable
private fun HomeButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(Home.settingsHeight)
            .clip(RoundedCornerShape(10.dp))
            .background(Kiosk.color.accentSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.screen_calendar),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Kiosk.color.accent,
        )
    }
}

/** 자동 전환 상태 안내. 화면 아래쪽에 작게 띄운다. */
@Composable
fun RotationHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Kiosk.color.textMuted,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Kiosk.color.surface.copy(alpha = 0.82f))
                .padding(horizontal = 16.dp, vertical = 7.dp),
        )
    }
}

/**
 * 달력 화면 상단의 `♪ 채널` 표시. 라디오가 재생 중일 때만 나오고,
 * 누르면 라디오 화면으로 돌아간다. 70대가 발견하기 쉽게 글씨를 키웠다.
 */
@Composable
fun RadioPill(name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(Home.settingsHeight)
            .clip(RoundedCornerShape(10.dp))
            .background(Kiosk.color.accentSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Equalizer(barWidth = 2.dp, maxHeight = 14.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Kiosk.color.accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 210.dp),
        )
    }
}

/** 재생 중 표시용 작은 이퀄라이저. 소리가 나는 느낌을 준다. */
@Composable
fun Equalizer(
    modifier: Modifier = Modifier,
    color: Color = Kiosk.color.accent,
    barWidth: Dp = 3.dp,
    maxHeight: Dp = 18.dp,
) {
    val transition = rememberInfiniteTransition(label = "equalizer")
    val fractions = listOf(0.45f, 1f, 0.62f, 0.82f)
    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        fractions.forEachIndexed { index, fraction ->
            val scale by transition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 520 + index * 90,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "bar$index",
            )
            Box(
                Modifier
                    .width(barWidth)
                    .height(maxHeight * fraction * scale)
                    .clip(RoundedCornerShape(barWidth / 2f))
                    .background(color),
            )
        }
    }
}
