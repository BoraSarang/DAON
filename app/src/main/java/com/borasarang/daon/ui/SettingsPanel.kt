package com.borasarang.daon.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.borasarang.daon.R
import com.borasarang.daon.core.AppPrefs
import com.borasarang.daon.core.ClockFace
import com.borasarang.daon.core.DebugLogger
import com.borasarang.daon.core.KioskMode
import com.borasarang.daon.core.LocaleController
import com.borasarang.daon.data.City
import com.borasarang.daon.data.PlaceResult
import com.borasarang.daon.ui.theme.Kiosk
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * v0.2 설정 패널. 자동 전환·화면별 시간·터치 동작·글씨 크기·날씨 움직임을 더했다.
 * 다이얼로그 창을 쓰지 않아 시스템 바가 다시 나타나지 않는다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsPanel(
    vm: DashboardViewModel,
    onDismiss: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onRequestCalendarPermission: () -> Unit,
) {
    val activity = LocalContext.current as Activity
    val city by vm.city.collectAsStateWithLifecycle()
    val radioChannels by vm.radioChannels.collectAsStateWithLifecycle()
    val tvChannels by vm.tvChannels.collectAsStateWithLifecycle()
    val korean = java.util.Locale.getDefault().language == "ko"
    val settingsScroll = rememberScrollState()

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
                        .verticalScroll(settingsScroll),
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

                    SettingsSection(stringResource(R.string.settings_radio)) {
                        ToggleRow(
                            title = stringResource(R.string.settings_radio_rotation),
                            desc = stringResource(R.string.settings_radio_rotation_desc),
                            checked = vm.radioInRotation,
                            onToggle = { vm.updateRadioInRotation(it) },
                            stateText = stringResource(
                                if (vm.radioInRotation) R.string.motion_on else R.string.motion_off,
                            ),
                        )
                        DwellRow(
                            label = stringResource(R.string.settings_radio_dwell),
                            seconds = vm.dwellRadio,
                            options = AppPrefs.RADIO_DWELL_OPTIONS,
                            onSelect = { vm.updateDwell(ScreenMode.RADIO, it) },
                        )
                        if (radioChannels.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.settings_radio_default_channel),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Kiosk.color.textPrimary,
                            )
                            Text(
                                text = stringResource(R.string.settings_radio_default_channel_desc),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = Kiosk.color.textMuted,
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(Kiosk.sm)) {
                                radioChannels.forEach { channel ->
                                    SettingChip(
                                        selected = vm.radioChannelId == channel.id,
                                        label = "${channel.displayName(korean)} ${channel.freq}",
                                        onClick = { vm.setDefaultRadioChannel(channel) },
                                    )
                                }
                            }
                        }
                        ToggleRow(
                            title = stringResource(R.string.settings_radio_autoplay),
                            desc = stringResource(R.string.settings_radio_autoplay_desc),
                            checked = vm.radioAutoplay,
                            onToggle = { vm.updateRadioAutoplay(it) },
                            stateText = stringResource(
                                if (vm.radioAutoplay) R.string.motion_on else R.string.motion_off,
                            ),
                        )
                        Text(
                            text = stringResource(R.string.settings_radio_note),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Kiosk.color.textMuted,
                        )
                    }

                    SettingsSection(stringResource(R.string.settings_tv)) {
                        ToggleRow(
                            title = stringResource(R.string.settings_tv_rotation),
                            desc = stringResource(R.string.settings_tv_rotation_desc),
                            checked = vm.tvInRotation,
                            onToggle = { vm.updateTvInRotation(it) },
                            stateText = stringResource(
                                if (vm.tvInRotation) R.string.motion_on else R.string.motion_off,
                            ),
                        )
                        DwellRow(
                            label = stringResource(R.string.settings_tv_dwell),
                            seconds = vm.dwellTv,
                            options = AppPrefs.TV_DWELL_OPTIONS,
                            onSelect = { vm.updateDwell(ScreenMode.TV, it) },
                        )
                        if (tvChannels.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.settings_tv_default_channel),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Kiosk.color.textPrimary,
                            )
                            Text(
                                text = stringResource(R.string.settings_tv_default_channel_desc),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = Kiosk.color.textMuted,
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(Kiosk.sm)) {
                                tvChannels.forEach { channel ->
                                    SettingChip(
                                        selected = vm.tvChannelId == channel.id,
                                        label = channel.displayName(korean),
                                        onClick = { vm.setDefaultTvChannel(channel) },
                                    )
                                }
                            }
                        }
                        Text(
                            text = stringResource(R.string.settings_tv_note),
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

                    SettingsSection(stringResource(R.string.label_location)) {
                        ToggleRow(
                            title = stringResource(R.string.settings_location_use),
                            desc = stringResource(R.string.settings_location_desc),
                            checked = vm.locationEnabled,
                            onToggle = { vm.updateLocationEnabled(it) },
                            stateText = stringResource(
                                if (vm.locationEnabled) {
                                    R.string.settings_location_on
                                } else {
                                    R.string.settings_location_off
                                },
                            ),
                        )
                        Text(
                            text = locationStatusText(vm, city),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Kiosk.color.textMuted,
                        )
                        if (vm.locationEnabled) {
                            Row(horizontalArrangement = Arrangement.spacedBy(Kiosk.sm)) {
                                if (vm.locationStatus == LocationStatus.NO_PERMISSION) {
                                    SettingChip(
                                        selected = false,
                                        label = stringResource(R.string.location_grant),
                                        onClick = onRequestLocationPermission,
                                    )
                                    SettingChip(
                                        selected = false,
                                        label = stringResource(R.string.location_open_settings),
                                        onClick = {
                                            val intent = Intent(
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.parse("package:${activity.packageName}"),
                                            )
                                            activity.startActivity(intent)
                                        },
                                    )
                                } else {
                                    SettingChip(
                                        selected = false,
                                        label = stringResource(R.string.location_retry),
                                        onClick = { vm.refreshLocation() },
                                    )
                                }
                            }
                        }
                        PlaceSearchBlock(vm, settingsScroll)

                        if (!vm.locationEnabled || vm.locationStatus in FALLBACK_STATUSES) {
                            Text(
                                text = stringResource(R.string.settings_fallback_city),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = Kiosk.color.textMuted,
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(Kiosk.sm)) {
                                City.entries.forEach { item ->
                                    SettingChip(
                                        selected = city == item && vm.savedPlaceName == null,
                                        label = stringResource(item.labelRes),
                                        onClick = { vm.setCity(item) },
                                    )
                                }
                            }
                        }
                    }

                    SettingsSection(stringResource(R.string.settings_events)) {
                        ToggleRow(
                            title = stringResource(R.string.settings_device_calendar),
                            desc = stringResource(R.string.settings_device_calendar_desc),
                            checked = vm.deviceCalendarEnabled,
                            onToggle = { vm.updateDeviceCalendarEnabled(it) },
                            stateText = stringResource(
                                if (vm.deviceCalendarEnabled) R.string.motion_on else R.string.motion_off,
                            ),
                        )
                        Text(
                            text = calendarStatusText(vm),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Kiosk.color.textMuted,
                        )
                        if (vm.deviceCalendarEnabled) {
                            Row(horizontalArrangement = Arrangement.spacedBy(Kiosk.sm)) {
                                if (vm.calendarStatus == CalendarStatus.NO_PERMISSION) {
                                    SettingChip(
                                        selected = false,
                                        label = stringResource(R.string.calendar_grant),
                                        onClick = onRequestCalendarPermission,
                                    )
                                }
                                if (vm.calendarStatus == CalendarStatus.AVAILABLE) {
                                    SettingChip(
                                        selected = false,
                                        label = stringResource(R.string.calendar_reload),
                                        onClick = { vm.refreshDeviceCalendar() },
                                    )
                                }
                            }
                        }
                    }

                    SettingsSection(stringResource(R.string.settings_kiosk)) {
                        var lockActive by remember {
                            mutableStateOf(KioskMode.isLockTaskActive(activity))
                        }
                        val owner = remember { KioskMode.isDeviceOwner(activity) }
                        // 시스템 확인 창을 닫고 돌아왔을 때 실제 고정 상태를 다시 읽고,
                        // 실제 상태만 저장한다. 창을 취소했는데 `고정됨`으로 남지 않게 한다.
                        val lifecycleOwner = LocalLifecycleOwner.current
                        DisposableEffect(lifecycleOwner) {
                            val observer = LifecycleEventObserver { _, event ->
                                if (event == Lifecycle.Event.ON_RESUME) {
                                    val active = KioskMode.isLockTaskActive(activity)
                                    lockActive = active
                                    if (AppPrefs.kioskLock != active) {
                                        vm.updateKioskLock(active)
                                        DebugLogger.i("KIOSK", "saved active=$active")
                                    }
                                }
                            }
                            lifecycleOwner.lifecycle.addObserver(observer)
                            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                        }
                        ToggleRow(
                            title = stringResource(R.string.settings_kiosk_lock),
                            desc = stringResource(R.string.settings_kiosk_lock_desc),
                            checked = lockActive,
                            onToggle = { enabled ->
                                if (enabled) {
                                    // 시스템 확인 창이 뜬다. 실제 고정은 ON_RESUME 에서 확인해 저장한다.
                                    KioskMode.start(activity)
                                } else {
                                    KioskMode.stop(activity)
                                    vm.updateKioskLock(false)
                                }
                                lockActive = KioskMode.isLockTaskActive(activity)
                                DebugLogger.i("KIOSK", "requested=$enabled owner=$owner")
                            },
                            stateText = stringResource(
                                if (lockActive) R.string.settings_kiosk_on else R.string.settings_kiosk_off,
                            ),
                        )
                        Text(
                            text = stringResource(
                                if (owner) R.string.settings_kiosk_owner else R.string.settings_kiosk_note,
                            ),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Kiosk.color.textMuted,
                        )
                        ToggleRow(
                            title = stringResource(R.string.settings_kiosk_boot),
                            desc = stringResource(R.string.settings_kiosk_boot_desc),
                            checked = vm.kioskBootStart,
                            onToggle = { vm.updateBootStart(it) },
                            stateText = stringResource(
                                if (vm.kioskBootStart) R.string.motion_on else R.string.motion_off,
                            ),
                        )
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

/** 위치를 못 찾아 도시로 대신 보여 주는 상태. 이때만 도시 선택을 노출한다. */
private val FALLBACK_STATUSES = setOf(
    LocationStatus.NO_PERMISSION,
    LocationStatus.SERVICE_OFF,
    LocationStatus.UNAVAILABLE,
)

@Composable
private fun locationStatusText(vm: DashboardViewModel, fallbackCity: City): String {
    // 실제로 쓰는 지점을 알려 준다. 저장한 동네가 있으면 도시가 아니라 그 이름이다.
    val placeName = vm.savedPlaceName ?: stringResource(fallbackCity.labelRes)
    val accuracy = vm.locationAccuracy
    return when (vm.locationStatus) {
        LocationStatus.OFF -> stringResource(R.string.location_status_off, placeName)
        LocationStatus.SEARCHING -> stringResource(R.string.location_status_searching)
        LocationStatus.READY -> if (accuracy != null && accuracy > 0f) {
            stringResource(R.string.location_status_ready, formatAccuracy(accuracy))
        } else {
            stringResource(R.string.location_status_ready_unknown)
        }
        LocationStatus.NO_PERMISSION -> stringResource(R.string.location_status_permission, placeName)
        LocationStatus.SERVICE_OFF -> stringResource(R.string.location_status_service_off, placeName)
        LocationStatus.UNAVAILABLE -> stringResource(R.string.location_status_unavailable, placeName)
    }
}

private fun formatAccuracy(meters: Float): String =
    if (meters < 1_000f) {
        "${meters.roundToInt()}m"
    } else {
        String.format(java.util.Locale.getDefault(), "%.1fkm", meters / 1_000f)
    }

@Composable
private fun calendarStatusText(vm: DashboardViewModel): String = when (vm.calendarStatus) {
    CalendarStatus.OFF -> stringResource(R.string.settings_calendar_off)
    CalendarStatus.CHECKING -> stringResource(R.string.settings_calendar_checking)
    CalendarStatus.AVAILABLE -> stringResource(R.string.settings_calendar_on)
    CalendarStatus.NO_PERMISSION -> stringResource(R.string.settings_calendar_permission)
    CalendarStatus.UNAVAILABLE -> stringResource(R.string.settings_calendar_unavailable)
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
private fun DwellRow(
    label: String,
    seconds: Int,
    onSelect: (Int) -> Unit,
    options: List<Int> = AppPrefs.DWELL_OPTIONS,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Kiosk.color.textPrimary,
            modifier = Modifier.width(66.dp),
        )
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
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

/**
 * 동네 직접 찾기. Fire 태블릿은 GPS·네트워크 위치가 모두 동작하지 않아
 * 사용자가 한 번 검색해 좌표를 저장한다. 검색은 누를 때만 호출한다.
 */
@Composable
private fun PlaceSearchBlock(vm: DashboardViewModel, settingsScroll: ScrollState) {
    val hint = stringResource(R.string.place_hint)
    val focusManager = LocalFocusManager.current
    // 검색이 끝나면 키보드를 내리고 결과가 보이도록 설정 화면을 끝까지 내린다.
    LaunchedEffect(vm.placeResults, vm.placeSearchError) {
        if (vm.placeResults.isNotEmpty() || vm.placeSearchError != null) {
            focusManager.clearFocus()
            delay(120)
            settingsScroll.animateScrollTo(settingsScroll.maxValue)
        }
    }
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_place_desc),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Kiosk.color.textMuted,
        )
        Spacer(Modifier.height(Kiosk.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Kiosk.sm),
        ) {
            BasicTextField(
                value = vm.placeQuery,
                onValueChange = vm::updatePlaceQuery,
                singleLine = true,
                textStyle = TextStyle(fontSize = 19.sp, color = Kiosk.color.textPrimary),
                cursorBrush = SolidColor(Kiosk.color.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.searchPlace() }),
                modifier = Modifier
                    .weight(1f)
                    .background(Kiosk.color.surfaceAlt, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp)
                    .semantics { contentDescription = hint },
                decorationBox = { innerTextField ->
                    if (vm.placeQuery.isEmpty()) {
                        Text(hint, fontSize = 19.sp, color = Kiosk.color.textMuted)
                    }
                    innerTextField()
                },
            )
            SettingChip(
                selected = false,
                label = stringResource(
                    if (vm.placeSearching) R.string.place_searching else R.string.place_search,
                ),
                onClick = { vm.searchPlace() },
            )
        }
        vm.placeSearchError?.let { messageRes ->
            Spacer(Modifier.height(Kiosk.sm))
            Text(
                text = stringResource(messageRes),
                fontSize = 15.sp,
                color = Kiosk.color.danger,
            )
        }
        if (vm.placeResults.isNotEmpty()) {
            Spacer(Modifier.height(Kiosk.sm))
            Column(verticalArrangement = Arrangement.spacedBy(Kiosk.xs)) {
                vm.placeResults.forEach { result ->
                    PlaceResultRow(result) { vm.saveSearchedPlace(result) }
                }
            }
        }
        vm.savedPlaceName?.let { name ->
            Spacer(Modifier.height(Kiosk.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Kiosk.sm),
            ) {
                Text(
                    text = stringResource(R.string.place_saved, name),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Kiosk.color.ok,
                    modifier = Modifier.weight(1f),
                )
                SettingChip(
                    selected = false,
                    label = stringResource(R.string.place_clear),
                    onClick = { vm.clearSavedPlace() },
                )
            }
        }
    }
}

@Composable
private fun PlaceResultRow(result: PlaceResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Kiosk.color.surfaceAlt)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = result.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Kiosk.color.textPrimary,
            )
            if (result.detail.isNotBlank()) {
                Text(
                    text = result.detail,
                    fontSize = 14.sp,
                    color = Kiosk.color.textMuted,
                )
            }
        }
        Text(
            text = stringResource(R.string.place_use),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Kiosk.color.accent,
        )
    }
}
