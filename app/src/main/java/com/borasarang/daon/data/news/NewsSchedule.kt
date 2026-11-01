package com.borasarang.daon.data.news

/**
 * 갱신 주기와 실패 시 물러서는 간격.
 *
 * 성공하면 설정한 간격(기본 1시간), 실패하면 5분 → 15분 → 1시간으로 물러난다.
 * 상시 켜진 기기에서 피드가 죽었을 때 5분마다 두드리지 않게 하려는 것이다.
 */
object NewsSchedule {

    const val DEFAULT_INTERVAL_MIN = 60

    /** 설정에서 고를 수 있는 간격(분). */
    val INTERVAL_OPTIONS = listOf(30, 60, 180, 360)

    private val BACKOFF_MIN = listOf(5, 15, 60)

    fun normalizedInterval(minutes: Int): Int =
        if (minutes in INTERVAL_OPTIONS) minutes else DEFAULT_INTERVAL_MIN

    /** 연속 실패 횟수에 따른 다음 시도까지의 시간(분). */
    fun delayMinutes(failures: Int, intervalMinutes: Int): Int {
        if (failures <= 0) return normalizedInterval(intervalMinutes)
        return BACKOFF_MIN[(failures - 1).coerceAtMost(BACKOFF_MIN.lastIndex)]
    }

    fun delayMs(failures: Int, intervalMinutes: Int): Long =
        delayMinutes(failures, intervalMinutes) * 60_000L
}
