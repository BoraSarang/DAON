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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.borasarang.daon.R
import com.borasarang.daon.core.monthGrid
import com.borasarang.daon.data.calendar.CalendarData
import com.borasarang.daon.data.calendar.displayName
import com.borasarang.daon.ui.theme.Home
import com.borasarang.daon.ui.theme.Kiosk
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as DateTextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private val TodayInk = Color(0xFF15181D)
private val TodayLunarInk = Color(0xFF4B3A1D)

/**
 * 달력 홈의 왼쪽 70% 패널. 6주 고정 그리드에 양력·음력·공휴일·절기를 함께 보여준다.
 */
@Composable
fun CalendarPanel(
    today: LocalDate,
    calendar: CalendarData,
    korean: Boolean,
    locale: Locale,
    eventDates: Set<LocalDate> = emptySet(),
    modifier: Modifier = Modifier,
    onDateClick: (LocalDate) -> Unit = {},
) {
    var month by remember(today) { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    val weekStart = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val days = remember(month, weekStart) { monthGrid(month, weekStart) }
    val monthPattern = stringResource(R.string.pattern_month)
    val monthText = remember(month, monthPattern, locale) {
        month.format(Formatters.of(monthPattern, locale))
    }
    val weekdays = remember(weekStart) { (0 until 7).map { weekStart.plus(it.toLong()) } }
    val prevLabel = stringResource(R.string.calendar_prev)
    val nextLabel = stringResource(R.string.calendar_next)

    Surface(
        modifier = modifier,
        color = Kiosk.color.surface,
        shape = RoundedCornerShape(Home.panelRadius),
        border = BorderStroke(1.dp, Kiosk.color.rule),
    ) {
        Column(Modifier.fillMaxSize()) {
            CalendarHead(
                year = month.year,
                monthText = monthText,
                korean = korean,
                prevLabel = prevLabel,
                nextLabel = nextLabel,
                onPrev = { month = month.minusMonths(1) },
                onNext = { month = month.plusMonths(1) },
                onToday = {
                    month = YearMonth.from(today)
                    selected = today
                },
            )

            Row(Modifier.fillMaxWidth().height(Home.weekdays)) {
                weekdays.forEach { day ->
                    Text(
                        text = day.getDisplayName(DateTextStyle.NARROW, locale),
                        modifier = Modifier.weight(1f),
                        style = Home.weekday,
                        color = when (day) {
                            DayOfWeek.SUNDAY -> Kiosk.color.sunday
                            DayOfWeek.SATURDAY -> Kiosk.color.saturday
                            else -> Kiosk.color.textSecondary
                        },
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Kiosk.color.rule),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                days.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        week.forEach { date ->
                            DayCell(
                                date = date,
                                today = today,
                                inMonth = YearMonth.from(date) == month,
                                calendar = calendar,
                                korean = korean,
                                selected = selected == date,
                                hasEvents = date in eventDates,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onClick = {
                                    selected = date
                                    onDateClick(date)
                                    if (YearMonth.from(date) != month) {
                                        month = YearMonth.from(date)
                                    }
                                },
                            )
                        }
                    }
                }
            }

            FarmStrip(today = today, calendar = calendar, korean = korean)
        }
    }
}

@Composable
private fun CalendarHead(
    year: Int,
    monthText: String,
    korean: Boolean,
    prevLabel: String,
    nextLabel: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Home.calHead)
            .padding(horizontal = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavButton(label = "‹", description = prevLabel, onClick = onPrev)
        Spacer(Modifier.width(14.dp))
        if (korean) {
            Text(year.toString(), style = Home.year, color = Kiosk.color.textSecondary)
            Spacer(Modifier.width(7.dp))
            Text(monthText, style = Home.month, color = Kiosk.color.textPrimary)
        } else {
            Text(monthText, style = Home.month, color = Kiosk.color.textPrimary)
            Spacer(Modifier.width(7.dp))
            Text(year.toString(), style = Home.year, color = Kiosk.color.textSecondary)
        }
        Spacer(Modifier.width(14.dp))
        NavButton(label = "›", description = nextLabel, onClick = onNext)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .height(Home.todayButtonHeight)
                .clip(RoundedCornerShape(9.dp))
                .background(Kiosk.color.accentSoft)
                .border(1.dp, Kiosk.color.accent.copy(alpha = 0.55f), RoundedCornerShape(9.dp))
                .clickable(onClick = onToday)
                .padding(horizontal = 17.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.today),
                style = Home.weekday,
                color = Kiosk.color.accent,
            )
        }
    }
}

@Composable
private fun NavButton(
    label: String,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(Home.navSize)
            .clip(RoundedCornerShape(10.dp))
            .background(Kiosk.color.surfaceAlt)
            .border(1.dp, Kiosk.color.rule, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = Home.weekday, color = Kiosk.color.textSecondary)
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    today: LocalDate,
    inMonth: Boolean,
    calendar: CalendarData,
    korean: Boolean,
    selected: Boolean,
    hasEvents: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val isToday = date == today
    val holiday = calendar.holidaysOn(date).firstOrNull()
    val term = calendar.termsOn(date).firstOrNull()
    val lunar = calendar.lunarOn(date)

    val dayColor = when {
        isToday -> TodayInk
        holiday != null -> Kiosk.color.sunday
        date.dayOfWeek == DayOfWeek.SUNDAY -> Kiosk.color.sunday
        date.dayOfWeek == DayOfWeek.SATURDAY -> Kiosk.color.saturday
        else -> Kiosk.color.textPrimary
    }
    val tag = holiday?.displayName(korean) ?: term?.displayName(korean)
    val tagColor = when {
        isToday -> TodayInk
        holiday != null -> Kiosk.color.sunday
        term != null -> Kiosk.color.term
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .background(if (isToday) Kiosk.color.accent else Kiosk.color.surface)
            .clickable(onClick = onClick),
    ) {
        if (selected && !isToday) {
            Box(
                Modifier
                    .fillMaxSize()
                    .border(2.dp, Kiosk.color.accent),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Home.cellPadH, vertical = Home.cellPadV)
                .alpha(if (inMonth) 1f else 0.32f),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = Home.dayNumber,
                    color = dayColor,
                    maxLines = 1,
                )
                if (hasEvents) {
                    Spacer(Modifier.width(5.dp))
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isToday) TodayInk else Kiosk.color.accent),
                    )
                }
            }
            if (lunar != null) {
                Text(
                    text = if (korean) {
                        "음 ${lunar.month}/${lunar.day}"
                    } else {
                        "L ${lunar.month}/${lunar.day}"
                    },
                    style = Home.lunar,
                    color = if (isToday) TodayLunarInk else Kiosk.color.textSecondary,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.weight(1f))
            if (tag != null) {
                Text(
                    text = tag,
                    style = Home.tag,
                    color = tagColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FarmStrip(today: LocalDate, calendar: CalendarData, korean: Boolean) {
    val active = calendar.activeTerm(today) ?: return
    val detail = calendar.termDetail(active.second.index, korean)
    val task = detail?.tasks?.firstOrNull() ?: detail?.summary ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Home.farmStrip)
            .background(Kiosk.color.accentSoft)
            .padding(horizontal = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(34.dp)
                .background(Kiosk.color.accent, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(13.dp))
        Column {
            Text(
                text = stringResource(R.string.label_today_farm) + " · " +
                    active.second.displayName(korean),
                style = Home.farmLabel,
                color = Kiosk.color.accent,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = task,
                style = Home.farmText,
                color = Kiosk.color.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
