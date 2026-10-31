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

/**
 * 6주 고정 달력. 앞뒤 달 날짜까지 채워 42칸을 모두 실제 날짜로 만든다.
 * v0.2 홈은 인접 월을 흐리게 보여주므로 null 대신 날짜가 필요하다.
 */
fun monthGrid(month: YearMonth, weekStart: DayOfWeek): List<LocalDate> {
    val offset = (month.atDay(1).dayOfWeek.value - weekStart.value + 7) % 7
    val start = month.atDay(1).minusDays(offset.toLong())
    return (0 until CELL_COUNT).map { start.plusDays(it.toLong()) }
}

const val CELL_COUNT = 42
const val COLUMNS = 7
const val ROWS = 6
