package com.borasarang.daon.ui

import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * 화면에서 쓰는 날짜·시간 포매터 캐시.
 *
 * [DateTimeFormatter.ofPattern] 은 패턴을 매번 해석하므로 재구성마다 부르면 비싸다.
 * 특히 1초마다 다시 그리는 시계 화면에서 효과가 크다. 패턴·로케일 조합은 몇 개뿐이라
 * 한 번 만든 것을 재사용한다.
 */
object Formatters {
    private val cache = ConcurrentHashMap<String, DateTimeFormatter>()

    fun of(pattern: String, locale: Locale): DateTimeFormatter {
        val key = "$pattern\u0000${locale.toLanguageTag()}"
        return cache[key] ?: DateTimeFormatter.ofPattern(pattern, locale).also { cache[key] = it }
    }
}
