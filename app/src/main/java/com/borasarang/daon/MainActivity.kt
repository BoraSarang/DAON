package com.borasarang.daon

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.borasarang.daon.core.DebugLogger
import com.borasarang.daon.core.LocaleController
import com.borasarang.daon.ui.DashboardScreen
import com.borasarang.daon.ui.DashboardViewModel
import com.borasarang.daon.ui.SettingsPanel
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

        setContent {
            DaonTheme {
                val viewModel: DashboardViewModel = viewModel()
                var settingsOpen by remember { mutableStateOf(false) }

                DashboardScreen(vm = viewModel, onOpenSettings = { settingsOpen = true })
                if (settingsOpen) {
                    SettingsPanel(vm = viewModel, onDismiss = { settingsOpen = false })
                }
            }
        }

        logColdStart()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
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
