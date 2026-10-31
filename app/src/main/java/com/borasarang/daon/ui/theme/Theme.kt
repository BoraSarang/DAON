package com.borasarang.daon.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val KioskColorScheme = darkColorScheme(
    primary = DarkKioskColors.accent,
    onPrimary = DarkKioskColors.bg,
    secondary = DarkKioskColors.textSecondary,
    onSecondary = DarkKioskColors.bg,
    secondaryContainer = DarkKioskColors.surfaceAlt,
    onSecondaryContainer = DarkKioskColors.textSecondary,
    primaryContainer = DarkKioskColors.accentSoft,
    onPrimaryContainer = DarkKioskColors.accent,
    background = DarkKioskColors.bg,
    onBackground = DarkKioskColors.textPrimary,
    surface = DarkKioskColors.surface,
    onSurface = DarkKioskColors.textPrimary,
    surfaceVariant = DarkKioskColors.surfaceAlt,
    onSurfaceVariant = DarkKioskColors.textSecondary,
    outline = DarkKioskColors.rule,
    error = DarkKioskColors.danger,
    onError = DarkKioskColors.bg,
)

@Composable
fun DaonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KioskColorScheme,
        typography = KioskTypography,
        content = content,
    )
}
