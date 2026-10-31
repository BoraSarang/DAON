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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.borasarang.daon.R
import com.borasarang.daon.core.AppPrefs
import com.borasarang.daon.core.ClockFace
import com.borasarang.daon.core.LocaleController
import com.borasarang.daon.data.City
import com.borasarang.daon.ui.theme.Kiosk

/**
 * v0.2 설정 패널. 자동 전환·화면별 시간·터치 동작·글씨 크기·날씨 움직임을 더했다.
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
                        Text(
                            text = stringResource(R.string.label_close),
                            fontSize = 18.sp,
                            color = Kiosk.color.textSecondary,
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Kiosk.lg),
                ) {
                    SettingsGroup {
                        RotationRow(
                            checked = vm.rotationEnabled,
                            onToggle = { vm.updateRotationEnabled(it) },
                        )
                        Spacer(Modifier.height(4.dp))
                        DwellRow(
                            label = stringResource(R.string.screen_calendar),
                            seconds = vm.dwellCalendar,
                            onSelect = { vm.updateDwell(ScreenMode.CALENDAR, it) },
                        )
                        DwellRow(
                            label = stringResource(R.string.screen_weather),
                            seconds = vm.dwellWeather,
                            onSelect = { vm.updateDwell(ScreenMode.WEATHER, it) },
                        )
                        DwellRow(
                            label = stringResource(R.string.screen_clock),
                            seconds = vm.dwellClock,
                            onSelect = { vm.updateDwell(ScreenMode.CLOCK, it) },
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.settings_touch),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Kiosk.color.textPrimary,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Kiosk.sm)) {
                            SettingChip(
                                selected = !vm.touchStop,
                                label = stringResource(R.string.touch_dashboard),
                                onClick = { vm.updateTouchStop(false) },
                                modifier = Modifier.weight(1f),
                            )
                            SettingChip(
                                selected = vm.touchStop,
                                label = stringResource(R.string.touch_pause),
                                onClick = { vm.updateTouchStop(true) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Text(
                            text = stringResource(
                                if (vm.touchStop) R.string.touch_note_pause else R.string.touch_note_dashboard,
                            ),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Kiosk.color.textMuted,
                        )
                    }

                    SettingsSection(stringResource(R.string.settings_clock_face)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Kiosk.sm)) {
                            ClockFace.entries.forEach { face ->
                                SettingChip(
                                    selected = vm.clockFace == face,
                                    label = stringResource(face.labelRes),
                                    onClick = { vm.updateClockFace(face) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    SettingsSection(stringResource(R.string.settings_text_size)) {
                        Text(
                            text = stringResource(R.string.settings_text_size_desc),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Kiosk.color.textMuted,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Kiosk.sm)) {
                            listOf(
                                AppPrefs.TEXT_SCALE_OPTIONS[0] to R.string.text_normal,
                                AppPrefs.TEXT_SCALE_OPTIONS[1] to R.string.text_large,
                                AppPrefs.TEXT_SCALE_OPTIONS[2] to R.string.text_xlarge,
                            ).forEach { (percent, labelRes) ->
                                SettingChip(
                                    selected = vm.textScale == percent,
                                    label = stringResource(labelRes),
                                    onClick = { vm.updateTextScale(percent) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    SettingsSection(stringResource(R.string.settings_weather)) {
                        MotionRow(
                            checked = vm.weatherMotion,
                            onToggle = { vm.updateWeatherMotion(it) },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Kiosk.sm)) {
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
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Kiosk.color.accentSoft)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.label_close),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Kiosk.color.accent,
                    )
                }
            }
        }
    }
}

private val ClockFace.labelRes: Int
    get() = when (this) {
        ClockFace.ANALOG -> R.string.clock_face_analog
        ClockFace.DIGITAL -> R.string.clock_face_digital
        ClockFace.HYBRID -> R.string.clock_face_hybrid
    }

@Composable
private fun RotationRow(checked: Boolean, onToggle: (Boolean) -> Unit) {
    ToggleRow(
        title = stringResource(R.string.settings_rotation),
        desc = stringResource(R.string.settings_rotation_desc),
        checked = checked,
        onToggle = onToggle,
        stateText = stringResource(
            if (checked) R.string.settings_rotation_on else R.string.settings_rotation_off,
        ),
    )
}

@Composable
private fun MotionRow(checked: Boolean, onToggle: (Boolean) -> Unit) {
    ToggleRow(
        title = stringResource(R.string.settings_weather_motion),
        desc = stringResource(R.string.settings_weather_motion_desc),
        checked = checked,
        onToggle = onToggle,
        stateText = stringResource(if (checked) R.string.motion_on else R.string.motion_off),
    )
}

@Composable
private fun ToggleRow(
    title: String,
    desc: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    stateText: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .semantics { contentDescription = "$title $stateText" }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Kiosk.color.textPrimary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = desc,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Kiosk.color.textMuted,
            )
        }
        Spacer(Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .width(68.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(if (checked) Kiosk.color.accent else Kiosk.color.surfaceAlt),
        ) {
            Box(
                Modifier
                    .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(4.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (checked) Kiosk.color.bg else Kiosk.color.textMuted),
            )
        }
    }
}

@Composable
private fun DwellRow(label: String, seconds: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Kiosk.color.textPrimary,
            modifier = Modifier.width(66.dp),
        )
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppPrefs.DWELL_OPTIONS.forEach { option ->
                SettingChip(
                    selected = seconds == option,
                    label = stringResource(R.string.dwell_seconds, option),
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SettingChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier.height(46.dp),
        label = {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Kiosk.color.surfaceAlt,
            labelColor = Kiosk.color.textSecondary,
            selectedContainerColor = Kiosk.color.accentSoft,
            selectedLabelColor = Kiosk.color.accent,
        ),
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Kiosk.color.surfaceAlt.copy(alpha = 0.45f))
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(Kiosk.sm),
    ) {
        Box(Modifier.padding(horizontal = 14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(Kiosk.sm)) {
                content()
            }
        }
    }
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
