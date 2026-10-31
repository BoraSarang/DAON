package com.borasarang.daon.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.borasarang.daon.R
import com.borasarang.daon.core.LocaleController
import com.borasarang.daon.data.City
import com.borasarang.daon.ui.theme.Kiosk

/**
 * v0.1 설정 패널. v0.2 T-018에서 자동 전환·시계 모양·글씨 크기를 추가한다.
 * 다이얼로그 창을 쓰지 않아 시스템 바가 다시 나타나지 않는다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsPanel(vm: DashboardViewModel, onDismiss: () -> Unit) {
    val activity = LocalContext.current as Activity
    val city by vm.city.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onDismiss),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(680.dp),
            color = Kiosk.color.surface,
            shape = RoundedCornerShape(topStart = Kiosk.radiusLg, bottomStart = Kiosk.radiusLg),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Kiosk.xxl),
                verticalArrangement = Arrangement.spacedBy(Kiosk.lg),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.label_settings),
                        style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
                        color = Kiosk.color.textPrimary,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.label_close), color = Kiosk.color.textSecondary)
                    }
                }

                SettingsSection(stringResource(R.string.label_city)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Kiosk.sm)) {
                        City.entries.forEach { item ->
                            SettingChip(
                                selected = city == item,
                                label = stringResource(item.labelRes),
                                onClick = { vm.setCity(item) },
                            )
                        }
                    }
                }

                SettingsSection(stringResource(R.string.label_hour_format)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Kiosk.sm)) {
                        SettingChip(
                            selected = vm.hour24,
                            label = stringResource(R.string.hour_24),
                            onClick = { vm.updateHour24(true) },
                        )
                        SettingChip(
                            selected = !vm.hour24,
                            label = stringResource(R.string.hour_12),
                            onClick = { vm.updateHour24(false) },
                        )
                    }
                }

                SettingsSection(stringResource(R.string.label_units)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Kiosk.sm)) {
                        SettingChip(
                            selected = vm.celsius,
                            label = stringResource(R.string.unit_celsius),
                            onClick = { vm.updateCelsius(true) },
                        )
                        SettingChip(
                            selected = !vm.celsius,
                            label = stringResource(R.string.unit_fahrenheit),
                            onClick = { vm.updateCelsius(false) },
                        )
                    }
                }

                SettingsSection(stringResource(R.string.label_language)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Kiosk.sm)) {
                        listOf("system", "en", "ko").forEach { tag ->
                            SettingChip(
                                selected = vm.localeTag == tag,
                                label = stringResource(LocaleController.labelRes(tag)),
                                onClick = {
                                    vm.updateLocaleTag(tag)
                                    LocaleController.set(tag, activity)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Kiosk.color.surfaceAlt,
            labelColor = Kiosk.color.textSecondary,
            selectedContainerColor = Kiosk.color.accentSoft,
            selectedLabelColor = Kiosk.color.accent,
        ),
    )
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Kiosk.sm)) {
        Text(
            text = title.uppercase(java.util.Locale.getDefault()),
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = Kiosk.color.textMuted,
        )
        content()
    }
}
