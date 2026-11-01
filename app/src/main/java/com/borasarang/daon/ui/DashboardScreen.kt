package com.borasarang.daon.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.borasarang.daon.R
import com.borasarang.daon.core.AppPrefs
import com.borasarang.daon.data.DailyForecast
import com.borasarang.daon.data.Place
import com.borasarang.daon.data.WeatherSnapshot
import com.borasarang.daon.data.calendar.CalendarData
import com.borasarang.daon.data.calendar.displayName
import com.borasarang.daon.data.event.CalendarEvent
import com.borasarang.daon.core.DebugLogger
import com.borasarang.daon.ui.theme.Home
import com.borasarang.daon.ui.theme.Kiosk
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 키오스크 홈. 달력 → 라디오 → TV → 날씨 → 시계 화면을 자동 전환한다(라디오·TV는 설정에서 제외 가능).
 * 70대 사용자를 기준으로 큰 글씨와 큰 터치 영역을 쓴다.
 */
@Composable
fun DashboardScreen(vm: DashboardViewModel) {
    val weather by vm.weather.collectAsStateWithLifecycle()
    val place by vm.place.collectAsStateWithLifecycle()
    val calendar by vm.calendar.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val radio by vm.radio.collectAsStateWithLifecycle()
    val radioChannels by vm.radioChannels.collectAsStateWithLifecycle()
    val tv by vm.tv.collectAsStateWithLifecycle()
    val tvChannels by vm.tvChannels.collectAsStateWithLifecycle()
    val locale = Locale.getDefault()
    val korean = locale.language == "ko"
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        vm.onLocationPermissionResult(granted)
    }
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        vm.onCalendarPermissionResult(granted)
    }
    LaunchedEffect(Unit) {
        // 첫 실행 때 한 번만 묻는다. 거부하면 설정의 `권한 허용`으로 다시 요청할 수 있다.
        if (vm.locationEnabled && !vm.hasLocationPermission() && !AppPrefs.locationAsked) {
            AppPrefs.locationAsked = true
            DebugLogger.i("LOCATION", "request coarse permission")
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }
    LaunchedEffect(vm.deviceCalendarEnabled, vm.calendarStatus) {
        // 캘린더 provider 가 있는 기기에서만 권한을 묻는다(Fire OS 에는 캘린더 앱이 없다).
        if (vm.deviceCalendarEnabled &&
            vm.calendarStatus == CalendarStatus.NO_PERMISSION &&
            !AppPrefs.deviceCalendarAsked
        ) {
            AppPrefs.deviceCalendarAsked = true
            DebugLogger.i("CALENDAR", "request read permission")
            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    }
    var screen by rememberSaveable { mutableStateOf(ScreenMode.CALENDAR) }
    var detailDate by remember { mutableStateOf<LocalDate?>(null) }
    var settingsOpen by remember { mutableStateOf(false) }
    /** 터치 동작이 `그 화면에서 멈추기`일 때 사용자가 직접 멈춘 상태. */
    var rotationStopped by remember { mutableStateOf(false) }

    // 초가 필요한 화면(시계)에서만 1초마다 갱신한다. 달력·날씨는 분 경계에서만 바뀐다.
    val nowState = rememberNow(perSecond = screen == ScreenMode.CLOCK)
    // 날짜가 바뀔 때만 알린다. 매초 달력 42칸을 다시 그리지 않게 하는 핵심이다.
    val today by remember { derivedStateOf { nowState.value.toLocalDate() } }
    // 날짜별로 셀에 보여줄 일정 제목 1개. 목록은 시간순이라 첫 건이 그날 첫 일정이다.
    val eventTitles = remember(events) {
        events.groupBy { it.date }.mapValues { (_, list) -> list.first().title }
    }
    val todayEvents = remember(events, today) { events.filter { it.date == today } }
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, vm.textScale) {
        Density(baseDensity.density, baseDensity.fontScale * vm.textScale / 100f)
    }

    // 날씨·위치·기기 달력은 첫 화면에 필요 없다. 첫 프레임을 그린 뒤에 시작한다.
    LaunchedEffect(Unit) {
        withFrameNanos { }
        vm.startBackgroundWork()
    }

    /** TV 영상이 나오는 동안에는 화면을 넘기지 않는다(전체 화면 포함). */
    val tvActive = tv.playing || tv.buffering
    val paused = settingsOpen || detailDate != null || tvActive || tv.fullscreen
    val dwell = when (screen) {
        ScreenMode.CALENDAR -> vm.dwellCalendar
        ScreenMode.RADIO -> vm.dwellRadio
        ScreenMode.TV -> vm.dwellTv
        ScreenMode.WEATHER -> vm.dwellWeather
        ScreenMode.CLOCK -> vm.dwellClock
    }

    LaunchedEffect(
        screen,
        dwell,
        vm.rotationEnabled,
        vm.radioInRotation,
        vm.tvInRotation,
        paused,
        rotationStopped,
    ) {
        if (!vm.rotationEnabled || paused || rotationStopped) return@LaunchedEffect
        delay(dwell * 1_000L)
        val next = screen.next(vm.radioInRotation, vm.tvInRotation)
        DebugLogger.i("ROTATE", "auto ${screen.name} -> ${next.name} after ${dwell}s")
        screen = next
    }

    // TV 화면을 떠나면 재생을 멈춘다. 라디오처럼 백그라운드 재생은 두지 않는다.
    LaunchedEffect(screen) {
        if (screen != ScreenMode.TV) vm.stopTv()
    }

    // 앱이 화면에서 내려가면 방송을 멈춘다. 백그라운드 재생 서비스는 두지 않는다.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                vm.stopRadio()
                vm.stopTv()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(
        enabled = tv.fullscreen || detailDate != null || settingsOpen || screen != ScreenMode.CALENDAR,
    ) {
        when {
            tv.fullscreen -> vm.setTvFullscreen(false)
            detailDate != null -> detailDate = null
            settingsOpen -> settingsOpen = false
            else -> screen = ScreenMode.CALENDAR
        }
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Kiosk.color.bg)
                .pointerInput(vm.touchStop, screen, settingsOpen, detailDate) {
                    detectTapGestures {
                        // 설정·날짜 상세가 열려 있으면 화면 전환 터치로 보지 않는다.
                        if (settingsOpen || detailDate != null) return@detectTapGestures
                        // 라디오·TV 는 빈 곳을 눌러도 아무 일도 하지 않는다. 터치는 채널·재생에만 쓴다.
                        if (screen == ScreenMode.RADIO || screen == ScreenMode.TV) {
                            return@detectTapGestures
                        }
                        DebugLogger.i("ROTATE", "tap screen=${screen.name} touchStop=${vm.touchStop} stopped=$rotationStopped")
                        if (vm.touchStop) {
                            rotationStopped = !rotationStopped
                        } else if (screen != ScreenMode.CALENDAR) {
                            screen = ScreenMode.CALENDAR
                        }
                    }
                },
        ) {
            Crossfade(targetState = screen, animationSpec = tween(250), label = "screen") { mode ->
                when (mode) {
                    ScreenMode.CALENDAR -> CalendarHome(
                        nowState = nowState,
                        today = today,
                        weather = weather,
                        place = place,
                        calendar = calendar,
                        korean = korean,
                        locale = locale,
                        hour24 = vm.hour24,
                        celsius = vm.celsius,
                        eventTitles = eventTitles,
                        todayEvents = todayEvents,
                        onDateClick = { detailDate = it },
                        onOpenClock = {
                            rotationStopped = false
                            screen = ScreenMode.CLOCK
                        },
                        onOpenWeather = {
                            rotationStopped = false
                            screen = ScreenMode.WEATHER
                        },
                        onOpenSettings = { settingsOpen = true },
                        onRefreshWeather = { vm.refresh() },
                        radioName = radio.channel?.displayName(korean)?.takeIf { radio.playing },
                        includeRadio = vm.radioInRotation,
                        includeTv = vm.tvInRotation,
                        onOpenRadio = {
                            rotationStopped = false
                            screen = ScreenMode.RADIO
                        },
                        onOpenTv = {
                            rotationStopped = false
                            screen = ScreenMode.TV
                        },
                    )
                    ScreenMode.RADIO -> RadioScreen(
                        channels = radioChannels,
                        state = radio,
                        korean = korean,
                        volume = vm.radioVolume,
                        includeRadio = vm.radioInRotation,
                        includeTv = vm.tvInRotation,
                        onSelectChannel = { vm.playChannel(it) },
                        onTogglePlay = { vm.toggleRadio() },
                        onSetVolume = { vm.updateRadioVolume(it) },
                        onRetry = { vm.retryRadio() },
                        onOpenSettings = { settingsOpen = true },
                        onGoHome = {
                            rotationStopped = false
                            screen = ScreenMode.CALENDAR
                        },
                    )
                    ScreenMode.TV -> TvScreen(
                        channels = tvChannels,
                        state = tv,
                        korean = korean,
                        player = vm.tvPlayerInstance,
                        includeRadio = vm.radioInRotation,
                        includeTv = vm.tvInRotation,
                        onSelectChannel = { vm.playTvChannel(it) },
                        onTogglePlay = { vm.toggleTv() },
                        onSetFullscreen = { vm.setTvFullscreen(it) },
                        onRetry = { vm.retryTv() },
                        onOpenSettings = { settingsOpen = true },
                        onYouTubeStarted = { vm.onTvPlaybackStarted() },
                        onYouTubeFailed = { vm.onTvPlaybackFailed(it) },
                        onGoHome = {
                            rotationStopped = false
                            screen = ScreenMode.CALENDAR
                        },
                    )
                    ScreenMode.WEATHER -> WeatherScreen(
                        place = place,
                        state = weather,
                        celsius = vm.celsius,
                        locale = locale,
                        weatherMotion = vm.weatherMotion && !settingsOpen,
                        touchStop = vm.touchStop,
                        stopped = rotationStopped,
                        includeRadio = vm.radioInRotation,
                        includeTv = vm.tvInRotation,
                        onOpenSettings = { settingsOpen = true },
                        onGoHome = {
                            rotationStopped = false
                            screen = ScreenMode.CALENDAR
                        },
                        onRefresh = { vm.refresh() },
                    )
                    ScreenMode.CLOCK -> ClockScreen(
                        nowState = nowState,
                        hour24 = vm.hour24,
                        korean = korean,
                        locale = locale,
                        calendar = calendar,
                        place = place,
                        weather = weather,
                        celsius = vm.celsius,
                        clockFace = vm.clockFace,
                        touchStop = vm.touchStop,
                        stopped = rotationStopped,
                        includeRadio = vm.radioInRotation,
                        includeTv = vm.tvInRotation,
                        onOpenSettings = { settingsOpen = true },
                        onGoHome = {
                            rotationStopped = false
                            screen = ScreenMode.CALENDAR
                        },
                    )
                }
            }

            if (rotationStopped) {
                Box(Modifier.align(Alignment.BottomCenter)) {
                    RotationHint(stringResource(R.string.rotation_paused))
                }
            }

            detailDate?.let { date ->
                DateDetailPanel(
                    date = date,
                    calendar = calendar,
                    korean = korean,
                    locale = locale,
                    events = events.filter { it.date == date },
                    calendarEnabled = vm.deviceCalendarEnabled,
                    calendarStatus = vm.calendarStatus,
                    onAddEvent = { title, repeat, lunar ->
                        vm.addEvent(date, title, repeat, lunar)
                    },
                    onRemoveEvent = { id -> vm.removeEvent(id) },
                    onSkipEvent = { id, date -> vm.skipEventOccurrence(id, date) },
                    onRequestCalendarPermission = {
                        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                    },
                    onClose = { detailDate = null },
                )
            }

            if (settingsOpen) {
                SettingsPanel(
                    vm = vm,
                    onDismiss = { settingsOpen = false },
                    onRequestLocationPermission = {
                        permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    onRequestCalendarPermission = {
                        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                    },
                )
            }
        }
    }
}

/** 달력 홈. 달력 70% + 오른쪽 시계·날씨 30%. */
@Composable
private fun CalendarHome(
    nowState: State<LocalDateTime>,
    today: LocalDate,
    weather: WeatherUiState,
    place: Place,
    calendar: CalendarData,
    korean: Boolean,
    locale: Locale,
    hour24: Boolean,
    celsius: Boolean,
    eventTitles: Map<LocalDate, String>,
    todayEvents: List<CalendarEvent>,
    onDateClick: (LocalDate) -> Unit,
    onOpenClock: () -> Unit,
    onOpenWeather: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefreshWeather: () -> Unit,
    radioName: String?,
    includeRadio: Boolean,
    includeTv: Boolean,
    onOpenRadio: () -> Unit,
    onOpenTv: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(Home.screenPadding),
    ) {
        HomeTopBar(
            nowState = nowState,
            calendar = calendar,
            korean = korean,
            locale = locale,
            todayEvents = todayEvents,
            onOpenToday = { onDateClick(today) },
            radioName = radioName,
            includeRadio = includeRadio,
            includeTv = includeTv,
            onOpenRadio = onOpenRadio,
            onOpenTv = onOpenTv,
            onOpenSettings = onOpenSettings,
        )
        Spacer(Modifier.height(Home.panelGap))

        Row(Modifier.fillMaxWidth().weight(1f)) {
            CalendarPanel(
                today = today,
                calendar = calendar,
                korean = korean,
                locale = locale,
                eventTitles = eventTitles,
                modifier = Modifier.weight(0.7f).fillMaxHeight(),
                onDateClick = onDateClick,
            )
            Spacer(Modifier.width(Home.panelGap))
            Column(
                modifier = Modifier.weight(0.3f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(Home.panelGap),
            ) {
                ClockCard(
                    nowState = nowState,
                    hour24 = hour24,
                    korean = korean,
                    locale = locale,
                    onClick = onOpenClock,
                    modifier = Modifier.fillMaxWidth().height(Home.clockCard),
                )
                WeatherCard(
                    place = place,
                    state = weather,
                    celsius = celsius,
                    locale = locale,
                    onClick = onOpenWeather,
                    onRefresh = onRefreshWeather,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    nowState: State<LocalDateTime>,
    calendar: CalendarData,
    korean: Boolean,
    locale: Locale,
    todayEvents: List<CalendarEvent>,
    onOpenToday: () -> Unit,
    radioName: String?,
    includeRadio: Boolean,
    includeTv: Boolean,
    onOpenRadio: () -> Unit,
    onOpenTv: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val now by nowState
    val today = now.toLocalDate()
    val headerPattern = stringResource(R.string.pattern_date_header)
    val dateText = remember(today, headerPattern, locale) {
        now.format(Formatters.of(headerPattern, locale))
    }
    val weekday = remember(today, locale) {
        today.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    }
    val lunar = calendar.lunarOn(today)
    val active = calendar.activeTerm(today)
    val activeDetail = active?.let { calendar.termDetail(it.second.index, korean) }
    val subText = buildAnnotatedString {
        if (lunar != null) {
            append(
                if (korean) {
                    "음력 ${lunar.month}월 ${lunar.day}일"
                } else {
                    "Lunar ${lunar.month}/${lunar.day}"
                },
            )
            if (lunar.leap) {
                append(if (korean) " 윤달" else " leap")
            }
        }
        if (active != null) {
            if (length > 0) append(" · ")
            withStyle(SpanStyle(color = Kiosk.color.term, fontWeight = FontWeight.Bold)) {
                append(active.second.displayName(korean))
            }
            if (activeDetail != null) {
                append(" · ")
                append(activeDetail.summary)
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = Home.topBar),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (korean) {
                Row {
                    Text(dateText, style = Home.date, color = Kiosk.color.textPrimary)
                    Text(" $weekday", style = Home.date, color = Kiosk.color.accent)
                }
            } else {
                Text(
                    text = "$weekday, $dateText",
                    style = Home.date,
                    color = Kiosk.color.textPrimary,
                    maxLines = 1,
                )
            }
            if (subText.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subText,
                    style = Home.sub,
                    color = Kiosk.color.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (todayEvents.isNotEmpty()) {
            TodayAgendaPill(events = todayEvents, onClick = onOpenToday)
            Spacer(Modifier.width(12.dp))
        }
        if (radioName != null) {
            RadioPill(name = radioName, onClick = onOpenRadio)
            Spacer(Modifier.width(12.dp))
        }
        ScreenDots(ScreenMode.CALENDAR, includeRadio, includeTv)
        Spacer(Modifier.width(18.dp))
        // 자동 전환이 꺼져 있어도 라디오·TV 로 들어갈 길을 항상 남긴다.
        NavButton(
            label = stringResource(R.string.screen_radio),
            active = radioName != null,
            onClick = onOpenRadio,
        )
        Spacer(Modifier.width(8.dp))
        NavButton(
            label = stringResource(R.string.screen_tv),
            onClick = onOpenTv,
        )
        Spacer(Modifier.width(8.dp))
        SettingsButton(onClick = onOpenSettings)
    }
}

@Composable
private fun ClockCard(
    nowState: State<LocalDateTime>,
    hour24: Boolean,
    korean: Boolean,
    locale: Locale,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val now by nowState
    val timeText = now.format(
        Formatters.of(if (hour24) "HH:mm" else "h:mm", locale),
    )
    val amPm = now.format(Formatters.of("a", locale))
    val clockDatePattern = stringResource(R.string.pattern_clock_date)
    val clockDate = now.format(Formatters.of(clockDatePattern, locale))
    val weekday = now.dayOfWeek.getDisplayName(TextStyle.FULL, locale)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(Home.panelRadius))
            .clickable(onClick = onClick),
        color = Kiosk.color.surface,
        shape = RoundedCornerShape(Home.panelRadius),
        border = androidx.compose.foundation.BorderStroke(1.dp, Kiosk.color.rule),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Home.cardPadding),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.label_now),
                style = Home.clockLabel,
                color = Kiosk.color.textMuted,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                if (!hour24) {
                    Text(
                        text = amPm,
                        style = Home.clockAmPm,
                        color = Kiosk.color.textSecondary,
                        modifier = Modifier.padding(end = 8.dp, bottom = 10.dp),
                    )
                }
                Text(
                    text = timeText,
                    style = Home.clockTime,
                    color = Kiosk.color.textPrimary,
                )
            }
            Text(
                text = stringResource(R.string.label_seconds, now.second),
                style = Home.clockSeconds,
                color = Kiosk.color.textMuted,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "$weekday · $clockDate",
                style = Home.clockDay,
                color = Kiosk.color.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WeatherCard(
    place: Place,
    state: WeatherUiState,
    celsius: Boolean,
    locale: Locale,
    onClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(Home.panelRadius))
            .clickable(onClick = onClick),
        color = Kiosk.color.surface,
        shape = RoundedCornerShape(Home.panelRadius),
        border = androidx.compose.foundation.BorderStroke(1.dp, Kiosk.color.rule),
    ) {
        Column(Modifier.fillMaxSize().padding(Home.cardPadding)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${place.label()} · ${stringResource(R.string.label_now)}",
                    style = Home.clockLabel,
                    color = Kiosk.color.textMuted,
                    maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onRefresh)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.label_refresh),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Kiosk.color.accent,
                    )
                }
            }

            val snapshot = state.snapshot
            when {
                snapshot != null -> WeatherContent(snapshot, celsius, locale)
                state.isLoading -> Text(
                    text = stringResource(R.string.weather_loading),
                    style = Home.condition,
                    color = Kiosk.color.textMuted,
                )
                else -> Text(
                    text = stringResource(state.errorRes ?: R.string.error_weather_network),
                    style = Home.condition,
                    color = Kiosk.color.danger,
                )
            }

            if (state.errorRes != null && snapshot != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.weather_offline),
                    fontSize = 13.sp,
                    color = Kiosk.color.danger,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.WeatherContent(snapshot: WeatherSnapshot, celsius: Boolean, locale: Locale) {
    val current = snapshot.current
    val today = snapshot.daily.firstOrNull()
    val unit = stringResource(if (celsius) R.string.unit_celsius else R.string.unit_fahrenheit)

    Spacer(Modifier.height(2.dp))
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "${current.temperature.roundToInt()}°",
            style = Home.temp,
            color = Kiosk.color.textPrimary,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.padding(bottom = 10.dp)) {
            Text(
                text = stringResource(weatherLabelRes(current.weatherCode)),
                style = Home.condition,
                color = Kiosk.color.textSecondary,
                maxLines = 1,
            )
            if (today != null) {
                Text(
                    text = stringResource(
                        R.string.label_today_range,
                        "${today.max.roundToInt()}°",
                        "${today.min.roundToInt()}°",
                    ),
                    style = Home.hiLo,
                    color = Kiosk.color.textMuted,
                    maxLines = 1,
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth()) {
        WeatherMetric(
            label = stringResource(R.string.label_feels),
            value = "${current.feelsLike.roundToInt()}$unit",
            modifier = Modifier.weight(1f),
        )
        WeatherMetric(
            label = stringResource(R.string.label_humidity),
            value = "${current.humidity}%",
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth()) {
        WeatherMetric(
            label = stringResource(R.string.label_wind),
            value = String.format(locale, "%.1f m/s", current.windSpeed),
            modifier = Modifier.weight(1f),
        )
        WeatherMetric(
            label = stringResource(R.string.label_range),
            value = today?.let { "${(it.max - it.min).roundToInt()}°" } ?: "—",
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(Modifier.height(12.dp))
    FarmAdviceBox(snapshot)

    Spacer(Modifier.weight(1f))
    Box(Modifier.fillMaxWidth().height(1.dp).background(Kiosk.color.rule))
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth()) {
        snapshot.daily.drop(1).take(3).forEach { day ->
            MiniForecast(day, locale, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniForecast(day: DailyForecast, locale: Locale, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = day.date.format(Formatters.of("E", locale)),
            style = Home.miniDay,
            color = Kiosk.color.textSecondary,
            maxLines = 1,
        )
        Text(
            text = stringResource(weatherLabelRes(day.weatherCode)),
            style = Home.miniCondition,
            color = Kiosk.color.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${day.max.roundToInt()}° / ${day.min.roundToInt()}°",
            style = Home.miniTemp,
            color = Kiosk.color.textPrimary,
            maxLines = 1,
        )
    }
}
