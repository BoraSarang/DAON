package com.borasarang.daon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.borasarang.daon.R
import com.borasarang.daon.ui.theme.Home
import com.borasarang.daon.ui.theme.Kiosk

/** 화면 점 3개. 현재 화면만 액센트로 켠다. */
@Composable
fun ScreenDots(current: ScreenMode) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        ScreenMode.entries.forEach { mode ->
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
 */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String?,
    current: ScreenMode,
    onOpenSettings: () -> Unit,
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
        ScreenDots(current)
        Spacer(Modifier.width(18.dp))
        SettingsButton(onClick = onOpenSettings)
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
