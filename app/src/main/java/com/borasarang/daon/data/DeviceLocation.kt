package com.borasarang.daon.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.borasarang.daon.core.AppPrefs
import com.borasarang.daon.core.DebugLogger
import com.borasarang.daon.core.ErrorCode
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/** 기기에서 얻은 좌표. [timeMs]는 fix 시각(epoch ms). */
data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timeMs: Long,
) {
    val ageMillis: Long
        get() = if (timeMs <= 0L) Long.MAX_VALUE else System.currentTimeMillis() - timeMs

    val isValid: Boolean
        get() = latitude in -90.0..90.0 &&
            longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)
}

/**
 * GMS 없이 프레임워크 [LocationManager]만 쓰는 위치 조회.
 * Fire OS 에는 GPS 가 없고 `network`(HERE) 프로바이더만 있으므로 동네 수준 정확도다.
 * 권한이 없거나 위치 서비스가 꺼져 있으면 null 을 돌려주고, 호출부가 도시로 폴백한다.
 */
class DeviceLocation(private val context: Context) {

    private val manager: LocationManager?
        get() = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    fun isServiceEnabled(): Boolean {
        val lm = manager ?: return false
        return PROVIDERS.any { provider ->
            runCatching { lm.isProviderEnabled(provider) }.getOrDefault(false)
        }
    }

    fun cached(): LocationFix? {
        val latitude = AppPrefs.cachedLatitude() ?: return null
        val longitude = AppPrefs.cachedLongitude() ?: return null
        val fix = LocationFix(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = AppPrefs.cachedAccuracy(),
            timeMs = AppPrefs.cachedLocationTime(),
        )
        return fix.takeIf { it.isValid }
    }

    /** 마지막으로 알려진 좌표. 오래됐어도 있으면 쓴다. */
    @SuppressLint("MissingPermission")
    fun lastKnown(): LocationFix? {
        if (!hasPermission()) return null
        val lm = manager ?: return null
        return PROVIDERS
            .mapNotNull { provider ->
                runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
            }
            .mapNotNull { it.toFix() }
            .maxByOrNull { it.timeMs }
    }

    /**
     * 새 좌표를 요청한다. [force]가 false면 최근 10분 이내 마지막 좌표를 재사용한다.
     * 실패·시간 초과 시 null. 예외는 삼키지 않고 로그로 남긴다.
     */
    @SuppressLint("MissingPermission")
    suspend fun requestFix(timeoutMs: Long = 20_000L, force: Boolean = false): LocationFix? {
        if (!hasPermission()) {
            DebugLogger.e(ErrorCode.PERM_LOCATION, "permission not granted")
            return null
        }
        val known = lastKnown()
        if (!force && known != null && known.ageMillis in 0..RECENT_FIX_MS) {
            DebugLogger.cache("LOCATION", "last_known age=${known.ageMillis / 1000}s")
            return known
        }

        val lm = manager ?: return known
        DebugLogger.i(
            "LOCATION",
            PROVIDERS.joinToString(" ") { provider ->
                "$provider=${runCatching { lm.isProviderEnabled(provider) }.getOrDefault(false)}"
            },
        )
        val provider = if (lm.allProviders.contains(LocationManager.FUSED_PROVIDER)) {
            // Fire OS 에서 isProviderEnabled("fused") 가 false 로 나와도 실제 요청은 받는다.
            LocationManager.FUSED_PROVIDER
        } else {
            PROVIDERS.firstOrNull { provider ->
                runCatching { lm.isProviderEnabled(provider) }.getOrDefault(false)
            }
        }
        if (provider == null) {
            DebugLogger.i("LOCATION", "no provider enabled")
            return known
        }

        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        val fix = location.toFix() ?: return
                        DebugLogger.i(
                            "LOCATION",
                            "fix provider=$provider acc=${fix.accuracyMeters.roundToInt()}m",
                        )
                        if (cont.isActive) cont.resume(fix)
                        runCatching { lm.removeUpdates(this) }
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

                    override fun onProviderEnabled(provider: String) = Unit

                    override fun onProviderDisabled(provider: String) {
                        DebugLogger.i("LOCATION", "provider disabled=$provider")
                        if (cont.isActive) cont.resume(null)
                    }
                }
                try {
                    // 위치 서비스가 켜져 있어도 HERE 백엔드가 응답하지 않으면 timeout 까지 기다린다.
                    lm.requestLocationUpdates(
                        provider,
                        0L,
                        0f,
                        listener,
                        Looper.getMainLooper(),
                    )
                } catch (t: Throwable) {
                    DebugLogger.e(
                        ErrorCode.PERM_LOCATION,
                        "request failed: ${t.javaClass.simpleName}",
                    )
                    if (cont.isActive) cont.resume(known)
                    return@suspendCancellableCoroutine
                }
                cont.invokeOnCancellation {
                    runCatching { lm.removeUpdates(listener) }
                }
            }
        } ?: known
    }

    private fun Location.toFix(): LocationFix? {
        if (latitude == 0.0 && longitude == 0.0) return null
        val time = if (time <= 0L) System.currentTimeMillis() else time
        val fix = LocationFix(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = if (accuracy.isFinite() && accuracy > 0f) accuracy else -1f,
            timeMs = time,
        )
        return fix.takeIf { it.isValid }
    }

    companion object {
        /** Fire OS 는 fused 만 실제로 돌려주는 경우가 있어 fused 를 먼저 시도한다. */
        private val PROVIDERS = listOf(
            LocationManager.FUSED_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )

        /** 같은 자리에서 10분 안에 다시 요청하면 마지막 좌표를 쓴다. */
        const val RECENT_FIX_MS = 10 * 60 * 1_000L

        /** 저장된 좌표를 하루까지 그대로 쓴다. 키오스크는 거의 움직이지 않는다. */
        const val CACHE_MAX_AGE_MS = 24 * 60 * 60 * 1_000L
    }
}
