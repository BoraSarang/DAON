package com.borasarang.daon

import android.app.Application
import android.os.SystemClock
import com.borasarang.daon.core.AppPrefs
import com.borasarang.daon.core.DebugLogger
import com.borasarang.daon.core.LocaleController
import com.borasarang.daon.ui.theme.Kiosk

/**
 * 프로세스 진입점. 콜드 스타트 측정 기준 시각을 남긴다.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startUptimeMs = SystemClock.uptimeMillis()
        AppPrefs.init(this)
        // 첫 프레임부터 저장된 테마로 그린다(블랙 → 라이트 깜빡임 방지).
        Kiosk.applyTheme(AppPrefs.themeLight)
        LocaleController.init()
        DebugLogger.i("APP", "onCreate")
    }

    companion object {
        /** 프로세스 시작 기준 uptime(ms). MainActivity 첫 프레임에서 [PERF] 로그로 남긴다. */
        var startUptimeMs: Long = 0L
            private set

        /** 프로세스당 1회만 콜드 스타트를 기록한다(언어 변경 등 Activity 재생성 제외). */
        var coldStartLogged: Boolean = false
            private set

        fun markColdStartLogged() {
            coldStartLogged = true
        }
    }
}
