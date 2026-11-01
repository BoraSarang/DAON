package com.borasarang.daon.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * 테마 색 구성표. 설정에서 블랙/라이트를 바꾸면 [Kiosk.color] 가 바뀌고,
 * 이 함수가 그 상태를 읽으므로 화면 전체가 다시 그려진다.
 */
@Composable
private fun kioskScheme() = with(Kiosk.color) {
    if (Kiosk.isLight) {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            secondary = textSecondary,
            onSecondary = surface,
            secondaryContainer = surfaceAlt,
            onSecondaryContainer = textPrimary,
            primaryContainer = accentSoft,
            onPrimaryContainer = accent,
            background = bg,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceAlt,
            onSurfaceVariant = textSecondary,
            outline = rule,
            error = danger,
            onError = surface,
        )
    } else {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            secondary = textSecondary,
            onSecondary = bg,
            secondaryContainer = surfaceAlt,
            onSecondaryContainer = textSecondary,
            primaryContainer = accentSoft,
            onPrimaryContainer = accent,
            background = bg,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceAlt,
            onSurfaceVariant = textSecondary,
            outline = rule,
            error = danger,
            onError = bg,
        )
    }
}

@Composable
fun DaonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = kioskScheme(),
        typography = KioskTypography,
        content = content,
    )
}
