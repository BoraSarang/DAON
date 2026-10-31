package com.borasarang.daon.data

import com.borasarang.daon.R

/**
 * 도시 목록. 위치 권한·GMS 없이 동작하도록 좌표를 내장한다.
 * 표시 이름은 리소스(en/ko)에서 가져온다.
 */
enum class City(
    val id: String,
    val labelRes: Int,
    val latitude: Double,
    val longitude: Double,
) {
    SEOUL("seoul", R.string.city_seoul, 37.5665, 126.9780),
    BUSAN("busan", R.string.city_busan, 35.1796, 129.0756),
    INCHEON("incheon", R.string.city_incheon, 37.4563, 126.7052),
    DAEGU("daegu", R.string.city_daegu, 35.8714, 128.6014),
    DAEJEON("daejeon", R.string.city_daejeon, 36.3504, 127.3845),
    GWANGJU("gwangju", R.string.city_gwangju, 35.1595, 126.8526),
    JEJU("jeju", R.string.city_jeju, 33.4996, 126.5312),
    TOKYO("tokyo", R.string.city_tokyo, 35.6762, 139.6503),
    OSAKA("osaka", R.string.city_osaka, 34.6937, 135.5023),
    NEW_YORK("new_york", R.string.city_new_york, 40.7128, -74.0060),
    LONDON("london", R.string.city_london, 51.5074, -0.1278),
    SYDNEY("sydney", R.string.city_sydney, -33.8688, 151.2093),
    SINGAPORE("singapore", R.string.city_singapore, 1.3521, 103.8198),
    ;

    companion object {
        fun fromId(id: String): City = entries.firstOrNull { it.id == id } ?: SEOUL
    }
}
