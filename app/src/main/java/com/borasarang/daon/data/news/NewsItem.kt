package com.borasarang.daon.data.news

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 뉴스 한 건. 목록·티커·기사 화면이 같은 값을 쓴다.
 *
 * [title] 은 정리된 제목(꼬리표 `(종합)` 제거), [summary] 는 RSS 요약이다.
 * 본문은 저장하지 않는다 — 기사를 열 때 한 번 받는다.
 */
data class NewsItem(
    val source: NewsSource,
    val title: String,
    val link: String,
    val summary: String,
    val publishedAt: Instant,
) {
    /** 같은 기사인지 판단하는 키. 주소가 그대로면 같은 기사다. */
    val id: String get() = link

    /** 48시간이 지났는지. 표시·저장에서 함께 쓴다. */
    fun isExpired(now: Instant, maxAge: Duration = MAX_AGE): Boolean =
        publishedAt.isBefore(now.minus(maxAge))

    /**
     * 목록·기사 상단의 시각 표기.
     * 오늘이면 `18:49`, 어제면 `어제 18:49`, 그 이전이면 `10/30 18:49`.
     */
    fun timeLabel(
        now: LocalDateTime,
        korean: Boolean,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val time = LocalDateTime.ofInstant(publishedAt, zone)
        val clock = String.format(java.util.Locale.ROOT, "%02d:%02d", time.hour, time.minute)
        val days = java.time.temporal.ChronoUnit.DAYS.between(time.toLocalDate(), now.toLocalDate())
        return when {
            days <= 0L -> clock
            days == 1L -> if (korean) "어제 $clock" else "Yesterday $clock"
            else -> String.format(
                java.util.Locale.ROOT,
                "%02d/%02d %s",
                time.monthValue,
                time.dayOfMonth,
                clock,
            )
        }
    }

    companion object {
        /** 표시·저장 상한. 밤사이 갱신이 한두 번 실패해도 아침에 비지 않게 하는 여유. */
        val MAX_AGE: Duration = Duration.ofHours(48)

        /** 한 출처가 가질 수 있는 최대 보관 건수. 표시는 20건이라 잠깐 밀려도 목록이 비지 않는다. */
        const val MAX_PER_SOURCE = 30

        /** 목록에 보여 줄 출처별 건수. */
        const val PER_SOURCE = 10

        /** 목록 전체 건수. */
        const val LIST_SIZE = 20
    }
}

/** [NewsItem.publishedAt] 을 현지 날짜로 바꿀 때 쓰는 도우미. */
fun NewsItem.localDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    LocalDateTime.ofInstant(publishedAt, zone).toLocalDate()
