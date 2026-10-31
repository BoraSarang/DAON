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
    const val PERM_LOCATION = "E-AND-PERM-0001"
    const val PERM_CALENDAR = "E-AND-PERM-0002"
    const val KIOSK_LOCK = "E-AND-KIOSK-0001"
    const val BOOT_START = "E-AND-BOOT-0001"

    /** 라디오 채널 정보가 없거나 목록이 깨졌다. */
    const val RADIO_CHANNEL = "E-AND-RADIO-0001"

    /** 방송사 스트림 주소 확인(리졸버) 실패. */
    const val RADIO_RESOLVE = "E-AND-RADIO-0002"

    /** 스트림 재생 실패. */
    const val RADIO_PLAYBACK = "E-AND-RADIO-0003"

    fun format(platform: String, category: String, number: Int): String =
        "E-$platform-$category-${number.toString().padStart(4, '0')}"
}
