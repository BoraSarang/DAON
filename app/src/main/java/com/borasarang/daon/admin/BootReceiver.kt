package com.borasarang.daon.admin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.borasarang.daon.MainActivity
import com.borasarang.daon.core.AppPrefs
import com.borasarang.daon.core.DebugLogger
import com.borasarang.daon.core.ErrorCode

/**
 * 부팅이 끝나면 대시보드를 연다. 설정에서 `부팅 시 자동 시작`을 켠 경우에만 동작한다.
 * Fire OS 는 백그라운드 Activity 시작을 막을 수 있어, 실패해도 조용히 로그만 남긴다.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!AppPrefs.kioskBootStart) return
        DebugLogger.i("BOOT", "boot completed, opening dashboard")
        val launch = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        try {
            context.startActivity(launch)
        } catch (t: Throwable) {
            DebugLogger.e(ErrorCode.BOOT_START, "start failed: ${t.javaClass.simpleName}")
        }
    }
}
