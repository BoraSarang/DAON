package com.borasarang.daon.core

/**
 * 에러 코드 형식: E-{PLATFORM}-{CATEGORY}-{NUM4} (rules/quality.md).
 * 범위: NET(날씨 네트워크/파싱), STORE(설정 저장), DATA(번들 자산).
 */
object ErrorCode {
    const val NET_TIMEOUT = "E-AND-NET-0001"
    const val NET_PARSE = "E-AND-NET-0002"
    const val NET_HTTP = "E-AND-NET-0003"
    const val STORE_WRITE = "E-AND-STORE-0001"
    const val CALENDAR_ASSET = "E-AND-DATA-0001"

    fun format(platform: String, category: String, number: Int): String =
        "E-$platform-$category-${number.toString().padStart(4, '0')}"
}
