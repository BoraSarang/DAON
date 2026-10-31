package com.borasarang.daon.admin

import android.app.admin.DeviceAdminReceiver

/**
 * Device Owner 등록용 관리자 수신자.
 *
 * `adb shell dpm set-device-owner com.borasarang.daon/.admin.AdminReceiver`
 * 로 등록하면 화면 고정 확인 창 없이 키오스크로 동작한다. 등록하지 않아도
 * 앱은 일반 화면 고정(스크린 피닝)으로 동작한다.
 */
class AdminReceiver : DeviceAdminReceiver()
