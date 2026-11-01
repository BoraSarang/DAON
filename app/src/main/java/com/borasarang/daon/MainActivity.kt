package com.borasarang.daon

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.borasarang.daon.core.AppPrefs
import com.borasarang.daon.core.DebugLogger
import com.borasarang.daon.core.KioskMode
import com.borasarang.daon.core.LocaleController
import com.borasarang.daon.core.ScreenBrightness
import com.borasarang.daon.ui.DashboardScreen
import com.borasarang.daon.ui.DashboardViewModel
import com.borasarang.daon.ui.theme.DaonTheme

/**
 * 단일 Activity 키오스크 셸.
 * - 가로 고정(manifest), 시스템 바 숨김, 화면 꺼짐 방지
 * - 첫 프레임에서 콜드 스타트를 [PERF] 로그로 남긴다 (예산 2.0s)
 */
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleController.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
        ScreenBrightness.apply(this, AppPrefs.screenBrightness)

        setContent {
            DaonTheme {
                val viewModel: DashboardViewModel = viewModel()
                DashboardScreen(vm = viewModel)
            }
        }

        logColdStart()
        restoreKioskLock()
    }

    /**
     * 앱을 다시 켰을 때 저장된 `이 앱에 고정` 을 되살린다.
     *
     * 화면 고정은 프로세스가 내려가면 풀리므로, 설정만 저장해 두면 재부팅 뒤 풀린 채로 남는다.
     * Device Owner 가 아니면 시스템 확인 창이 한 번 뜬다. 사용자가 고정을 풀 수 있도록
     * [onResume] 이 아니라 앱을 새로 시작할 때만 적용한다.
     */
    private fun restoreKioskLock() {
        if (!AppPrefs.kioskLock) return
        window.decorView.post {
            if (!KioskMode.isLockTaskActive(this)) {
                val started = KioskMode.start(this)
                DebugLogger.i("KIOSK", "restore lock=$started active=${KioskMode.isLockTaskActive(this)}")
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        // 기기(또는 키오스크 런처)가 밝기를 되돌리는 경우가 있어 화면에 돌아올 때마다 다시 적용한다.
        ScreenBrightness.apply(this, AppPrefs.screenBrightness)
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun logColdStart() {
        if (App.coldStartLogged) return
        val decor = window.decorView
        decor.viewTreeObserver.addOnDrawListener(object : ViewTreeObserver.OnDrawListener {
            private var logged = false

            override fun onDraw() {
                if (logged) return
                logged = true
                App.markColdStartLogged()
                val elapsed = SystemClock.uptimeMillis() - App.startUptimeMs
                DebugLogger.perf("STARTUP", "cold_start=${elapsed}ms budget=2000ms")
                reportFullyDrawn()
                // removeOnDrawListener 는 onDraw 안에서 호출할 수 없다 (API 28: IllegalStateException)
                decor.post {
                    if (decor.viewTreeObserver.isAlive) {
                        decor.viewTreeObserver.removeOnDrawListener(this)
                    }
                }
            }
        })
    }
}
