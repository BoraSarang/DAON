package com.borasarang.daon.core

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.Context

/**
 * 키오스크 화면 고정.
 *
 * - Device Owner 로 등록된 기기: 확인 창 없이 고정된다.
 * - 그 외: Android 화면 고정(스크린 피닝)으로 동작하며, 처음 한 번 사용자가 확인한다.
 * - Fire OS 는 화면 고정을 지원하지만 부팅 자동 시작은 막을 수 있다.
 *
 * UI 테마 토큰 `com.borasarang.daon.ui.theme.Kiosk` 와 이름이 겹치지 않도록 `KioskMode` 를 쓴다.
 */
object KioskMode {

    fun isLockTaskActive(context: Context): Boolean = try {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        manager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    } catch (t: Throwable) {
        false
    }

    fun isDeviceOwner(context: Context): Boolean = try {
        val manager =
            context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        manager.isDeviceOwnerApp(context.packageName)
    } catch (t: Throwable) {
        false
    }

    /** 성공 여부와 무관하게 시스템 확인 창이 뜰 수 있다. 실제 상태는 [isLockTaskActive] 로 다시 읽는다. */
    fun start(activity: Activity): Boolean = try {
        activity.startLockTask()
        true
    } catch (t: Throwable) {
        DebugLogger.e(ErrorCode.KIOSK_LOCK, "start failed: ${t.javaClass.simpleName}")
        false
    }

    fun stop(activity: Activity): Boolean = try {
        activity.stopLockTask()
        true
    } catch (t: Throwable) {
        DebugLogger.e(ErrorCode.KIOSK_LOCK, "stop failed: ${t.javaClass.simpleName}")
        false
    }
}
