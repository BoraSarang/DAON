package com.borasarang.daon.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * 월간 달력 셀(6행 × 7열 = 42칸)을 만든다. 빈 칸은 null.
 * @param weekStart 로케일 기준 주 시작 요일 (WeekFields.firstDayOfWeek)
 */
fun monthCells(month: YearMonth, weekStart: DayOfWeek): List<LocalDate?> {
    val offset = (month.atDay(1).dayOfWeek.value - weekStart.value + 7) % 7
    val cells = ArrayList<LocalDate?>(CELL_COUNT)
    repeat(offset) { cells.add(null) }
    for (day in 1..month.lengthOfMonth()) cells.add(month.atDay(day))
    while (cells.size < CELL_COUNT) cells.add(null)
    return cells
}

const val CELL_COUNT = 42
const val COLUMNS = 7
const val ROWS = 6
