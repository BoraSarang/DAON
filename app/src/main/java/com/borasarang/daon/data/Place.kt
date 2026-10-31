package com.borasarang.daon.data

import com.borasarang.daon.R
import kotlin.math.abs

/**
 * 날씨를 조회할 지점. 성격은 세 가지다.
 *
 * - **현재 위치**: [auto] 가 true. 기기에서 얻은 좌표이며 [city]·[name] 이 없다.
 * - **저장한 동네**: [name] 이 있다. 설정에서 검색해 한 번 저장한 지점이다.
 * - **도시**: [city] 가 있다. 위치도 저장한 동네도 없을 때 쓰는 마지막 폴백이다.
 *
 * 우선순위는 현재 위치 → 저장한 동네 → 도시 순서다.
 */
data class Place(
    val latitude: Double,
    val longitude: Double,
    val city: City? = null,
    val auto: Boolean = false,
    val accuracyMeters: Float? = null,
    val name: String? = null,
) {
    /** [name] 이 없을 때 쓰는 이름 리소스. 현재 위치면 "현재 위치"다. */
    val labelRes: Int
        get() = city?.labelRes ?: R.string.location_current

    /** 같은 지점으로 볼 수 있는지(약 100m). 잦은 재조회를 막는다. */
    fun sameAs(other: Place): Boolean =
        abs(latitude - other.latitude) < 0.001 &&
            abs(longitude - other.longitude) < 0.001 &&
            city?.id == other.city?.id &&
            name == other.name

    companion object {
        fun of(city: City): Place = Place(
            latitude = city.latitude,
            longitude = city.longitude,
            city = city,
            auto = false,
        )

        fun saved(latitude: Double, longitude: Double, name: String): Place = Place(
            latitude = latitude,
            longitude = longitude,
            city = null,
            auto = false,
            name = name,
        )
    }
}
