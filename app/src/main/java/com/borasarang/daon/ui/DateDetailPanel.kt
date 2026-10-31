package com.borasarang.daon.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.borasarang.daon.R
import com.borasarang.daon.data.calendar.CalendarData
import com.borasarang.daon.data.calendar.Holiday
import com.borasarang.daon.data.calendar.SolarTerm
import com.borasarang.daon.data.calendar.TermDetail
import com.borasarang.daon.data.calendar.descriptionFor
import com.borasarang.daon.data.calendar.displayName
import com.borasarang.daon.data.event.CalendarEvent
import com.borasarang.daon.data.event.EventSource
import com.borasarang.daon.data.event.RepeatRule
import com.borasarang.daon.ui.theme.Home
import com.borasarang.daon.ui.theme.Kiosk
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

/**
 * 날짜를 눌렀을 때 오른쪽에서 열리는 설명 패널.
 * 다이얼로그를 쓰지 않아 키오스크 전체화면이 유지된다.
 */
@Composable
fun DateDetailPanel(
    date: LocalDate,
    calendar: CalendarData,
    korean: Boolean,
    locale: Locale,
    events: List<CalendarEvent>,
    calendarEnabled: Boolean,
    calendarStatus: CalendarStatus,
    onAddEvent: (String, RepeatRule, Boolean) -> Unit,
    onRemoveEvent: (String) -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onClose: () -> Unit,
) {
    val term = calendar.termsOn(date).firstOrNull()
    val active = if (term == null) calendar.activeTerm(date)?.second else null
    val holidays = calendar.holidaysOn(date)
    val holiday = holidays.firstOrNull()
    val lunar = calendar.lunarOn(date)
    val weekday = date.dayOfWeek.getDisplayName(DateTextStyle.FULL, locale)
    val hasEntry = term != null || holiday != null
    val dateLine = buildString {
        if (hasEntry) {
            if (korean) {
                append(date.monthValue).append("월 ").append(date.dayOfMonth).append("일")
            } else {
                append(date.format(DateTimeFormatter.ofPattern("MMM d", locale)))
            }
            append(" (").append(weekday).append(")")
        } else {
            append(weekday)
        }
        if (lunar != null) {
            if (korean) {
                append(" · 음력 ").append(lunar.month).append("월 ").append(lunar.day).append("일")
                if (lunar.leap) append(" 윤달")
            } else {
                append(" · Lunar ${lunar.month}/${lunar.day}")
                if (lunar.leap) append(" leap")
            }
        }
    }

    val kicker = when {
        term != null -> stringResource(
            R.string.detail_term_kicker,
            calendar.termDetail(term.index, korean)?.season.orEmpty(),
        )
        holiday != null -> stringResource(R.string.detail_holiday_kicker)
        else -> stringResource(R.string.detail_date_kicker)
    }
    val title = term?.displayName(korean)
        ?: holiday?.displayName(korean)
        ?: if (korean) "${date.monthValue}월 ${date.dayOfMonth}일"
        else date.format(DateTimeFormatter.ofPattern("MMM d", locale))
    val hanja = term?.hanja

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(onClick = onClose),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(520.dp),
            color = Kiosk.color.surface,
            shape = RoundedCornerShape(topStart = Home.panelRadiusLarge, bottomStart = Home.panelRadiusLarge),
            border = BorderStroke(1.dp, Kiosk.color.rule),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 26.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DetailHead(
                    kicker = kicker,
                    title = title,
                    hanja = hanja,
                    dateLine = dateLine,
                    onClose = onClose,
                )

                EventSection(
                    date = date,
                    calendar = calendar,
                    events = events,
                    calendarEnabled = calendarEnabled,
                    calendarStatus = calendarStatus,
                    onAdd = onAddEvent,
                    onRemove = onRemoveEvent,
                    onRequestCalendarPermission = onRequestCalendarPermission,
                )

                when {
                    term != null -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TermSection(term, calendar.termDetail(term.index, korean))
                        if (holiday != null) {
                            SubTitle(stringResource(R.string.detail_holiday))
                            HolidayDescription(holiday, korean)
                        }
                    }
                    holiday != null -> HolidayDescription(holiday, korean)
                    else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.detail_no_special),
                            style = Home.detailBody,
                            color = Kiosk.color.textSecondary,
                        )
                        if (active != null) {
                            SubTitle(stringResource(R.string.detail_active_term))
                            TermSection(active, calendar.termDetail(active.index, korean))
                        }
                    }
                }

                val month = YearMonth.from(date)
                val monthTerms = calendar.termsIn(month)
                if (monthTerms.isNotEmpty()) {
                    SubTitle(stringResource(R.string.detail_terms_this_month))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        monthTerms.forEach { (termDate, item) ->
                            TermChip(
                                text = item.displayName(korean) +
                                    " ${termDate.monthValue}/${termDate.dayOfMonth}",
                                highlighted = termDate == date,
                            )
                        }
                    }
                }

                val monthHolidays = calendar.holidaysIn(month)
                if (monthHolidays.isNotEmpty()) {
                    HolidayCard(monthHolidays, korean, locale)
                }

                Text(
                    text = stringResource(
                        when {
                            term != null -> R.string.detail_note_term
                            holiday != null -> R.string.detail_note_holiday
                            active != null -> R.string.detail_note_term
                            else -> R.string.detail_note_date
                        },
                    ),
                    style = Home.detailNote,
                    color = Kiosk.color.textMuted,
                )
            }
        }
    }
}

@Composable
private fun DetailHead(
    kicker: String,
    title: String,
    hanja: String?,
    dateLine: String,
    onClose: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(kicker, style = Home.detailKicker, color = Kiosk.color.textMuted)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = title,
                    style = Home.detailTitle,
                    color = Kiosk.color.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (hanja != null) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = hanja,
                        style = Home.detailHanja,
                        color = Kiosk.color.textMuted,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(dateLine, style = Home.detailDate, color = Kiosk.color.textSecondary)
        }
        Spacer(Modifier.width(12.dp))
        val closeLabel = stringResource(R.string.label_close)
        Box(
            modifier = Modifier
                .size(Home.navSize)
                .clip(RoundedCornerShape(10.dp))
                .background(Kiosk.color.surfaceAlt)
                .border(1.dp, Kiosk.color.rule, RoundedCornerShape(10.dp))
                .clickable(onClick = onClose)
                .semantics { contentDescription = closeLabel },
            contentAlignment = Alignment.Center,
        ) {
            Text("×", fontSize = 24.sp, color = Kiosk.color.textSecondary)
        }
    }
}

@Composable
private fun TermSection(term: SolarTerm, detail: TermDetail?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (detail != null) {
            Text(
                text = detail.description,
                style = Home.detailBody,
                color = Kiosk.color.textPrimary,
            )
            Spacer(Modifier.height(2.dp))
            SubTitle(stringResource(R.string.detail_tasks))
            detail.tasks.forEach { task -> TaskRow(task) }
        } else {
            Text(
                text = term.name,
                style = Home.detailBody,
                color = Kiosk.color.textPrimary,
            )
        }
    }
}

@Composable
private fun TaskRow(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Kiosk.color.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text("✓", fontSize = 12.sp, color = Kiosk.color.accent)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = Home.taskText,
            color = Kiosk.color.textSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HolidayDescription(holiday: Holiday, korean: Boolean) {
    Text(
        text = holiday.descriptionFor(korean),
        style = Home.detailBody,
        color = Kiosk.color.textPrimary,
    )
}

@Composable
private fun HolidayCard(
    days: List<Pair<LocalDate, List<Holiday>>>,
    korean: Boolean,
    locale: Locale,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Kiosk.color.surfaceAlt)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = stringResource(R.string.detail_holidays_this_month),
            style = Home.detailSub,
            color = Kiosk.color.textMuted,
        )
        days.forEach { (date, items) ->
            items.forEach { holiday ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = holiday.displayName(korean),
                        style = Home.detailRow,
                        color = Kiosk.color.textSecondary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (korean) {
                            "${date.monthValue}월 ${date.dayOfMonth}일 (" +
                                date.dayOfWeek.getDisplayName(DateTextStyle.NARROW, locale) + ")"
                        } else {
                            date.format(DateTimeFormatter.ofPattern("MMM d (EEE)", locale))
                        },
                        style = Home.detailRow,
                        color = Kiosk.color.textPrimary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun TermChip(text: String, highlighted: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (highlighted) Kiosk.color.accentSoft else Color.Transparent)
            .border(
                1.dp,
                if (highlighted) Kiosk.color.accent.copy(alpha = 0.7f) else Kiosk.color.rule,
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 11.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = Home.chipText,
            color = if (highlighted) Kiosk.color.accent else Kiosk.color.textSecondary,
        )
    }
}

@Composable
private fun SubTitle(text: String) {
    Text(text, style = Home.detailSub, color = Kiosk.color.textMuted)
}

@Composable
private fun EventSection(
    date: LocalDate,
    calendar: CalendarData,
    events: List<CalendarEvent>,
    calendarEnabled: Boolean,
    calendarStatus: CalendarStatus,
    onAdd: (String, RepeatRule, Boolean) -> Unit,
    onRemove: (String) -> Unit,
    onRequestCalendarPermission: () -> Unit,
) {
    var draft by remember(date) { mutableStateOf("") }
    var lunar by remember(date) { mutableStateOf(false) }
    var repeat by remember(date) { mutableStateOf(RepeatRule.NONE) }
    val lunarDate = calendar.lunarOn(date)
    val focusManager = LocalFocusManager.current
    val hint = stringResource(R.string.detail_event_hint)
    val submit = {
        val text = draft.trim()
        if (text.isNotEmpty()) {
            onAdd(text, if (lunar) RepeatRule.YEARLY else repeat, lunar)
            draft = ""
            focusManager.clearFocus()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SubTitle(stringResource(R.string.detail_events))
        if (events.isEmpty()) {
            Text(
                text = stringResource(R.string.detail_no_events),
                style = Home.detailBody,
                color = Kiosk.color.textMuted,
            )
        } else {
            events.forEach { event -> EventRow(event, onRemove) }
        }

        // 날짜 기준: 같은 날을 양력으로 볼지 음력으로 볼지. 음력은 매년 반복된다.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.detail_event_date_basis),
                style = Home.detailNote,
                color = Kiosk.color.textMuted,
            )
            OptionChip(
                label = stringResource(
                    R.string.detail_event_solar,
                    date.monthValue,
                    date.dayOfMonth,
                ),
                selected = !lunar,
                onClick = { lunar = false },
            )
            if (lunarDate != null) {
                OptionChip(
                    label = stringResource(
                        R.string.detail_event_lunar,
                        lunarDate.month,
                        lunarDate.day,
                    ),
                    selected = lunar,
                    onClick = { lunar = true },
                )
            }
        }

        if (lunar) {
            Text(
                text = stringResource(
                    R.string.detail_event_lunar_note,
                    lunarDate?.month ?: 0,
                    lunarDate?.day ?: 0,
                ),
                style = Home.detailNote,
                color = Kiosk.color.accent,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.detail_event_repeat),
                    style = Home.detailNote,
                    color = Kiosk.color.textMuted,
                )
                RepeatRule.entries.forEach { rule ->
                    OptionChip(
                        label = stringResource(repeatLabelRes(rule)),
                        selected = repeat == rule,
                        onClick = { repeat = rule },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value = draft,
                onValueChange = { draft = it.take(60) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 17.sp, color = Kiosk.color.textPrimary),
                cursorBrush = SolidColor(Kiosk.color.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Kiosk.color.surfaceAlt)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .semantics { contentDescription = hint },
                decorationBox = { innerTextField ->
                    if (draft.isEmpty()) {
                        Text(hint, fontSize = 17.sp, color = Kiosk.color.textMuted)
                    }
                    innerTextField()
                },
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Kiosk.color.accentSoft)
                    .clickable { submit() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.detail_event_add),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Kiosk.color.accent,
                )
            }
        }
        if (calendarEnabled) {
            when (calendarStatus) {
                CalendarStatus.NO_PERMISSION -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_calendar_permission),
                        style = Home.detailNote,
                        color = Kiosk.color.textMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.calendar_grant),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Kiosk.color.accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Kiosk.color.accentSoft)
                            .clickable(onClick = onRequestCalendarPermission)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                CalendarStatus.UNAVAILABLE -> Text(
                    text = stringResource(R.string.settings_calendar_unavailable),
                    style = Home.detailNote,
                    color = Kiosk.color.textMuted,
                )
                else -> Unit
            }
        }
    }
}

/** 일정 입력의 선택 칩. 터치 영역은 40dp 이상으로 둔다. */
@Composable
private fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Kiosk.color.accentSoft else Color.Transparent)
            .border(
                1.dp,
                if (selected) Kiosk.color.accent.copy(alpha = 0.7f) else Kiosk.color.rule,
                RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = Home.chipText,
            color = if (selected) Kiosk.color.accent else Kiosk.color.textSecondary,
            maxLines = 1,
        )
    }
}

private fun repeatLabelRes(rule: RepeatRule): Int = when (rule) {
    RepeatRule.NONE -> R.string.repeat_none
    RepeatRule.WEEKLY -> R.string.repeat_weekly
    RepeatRule.MONTHLY -> R.string.repeat_monthly
    RepeatRule.YEARLY -> R.string.repeat_yearly
}

@Composable
private fun EventRow(event: CalendarEvent, onRemove: (String) -> Unit) {
    val removeLabel = stringResource(R.string.detail_event_remove)
    val sourceText = if (event.source == EventSource.DEVICE) {
        stringResource(
            R.string.detail_event_device_source,
            event.calendarName ?: stringResource(R.string.screen_calendar),
        )
    } else {
        stringResource(R.string.detail_event_local_source)
    }
    val repeatText = when {
        event.lunar -> stringResource(
            R.string.detail_event_lunar_yearly,
            event.lunarMonth,
            event.lunarDay,
        )
        event.repeat != RepeatRule.NONE -> stringResource(repeatLabelRes(event.repeat))
        else -> null
    }
    val meta = listOfNotNull(sourceText, repeatText).joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Kiosk.color.surfaceAlt)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (event.time != null) {
            Text(
                text = event.time,
                style = Home.detailRow,
                color = Kiosk.color.accent,
                maxLines = 1,
            )
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = event.title,
                style = Home.detailBody,
                color = Kiosk.color.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = meta,
                style = Home.detailNote,
                color = Kiosk.color.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (event.source == EventSource.LOCAL) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onRemove(event.id) }
                    .semantics { contentDescription = removeLabel },
                contentAlignment = Alignment.Center,
            ) {
                Text("×", fontSize = 20.sp, color = Kiosk.color.textMuted)
            }
        }
    }
}
