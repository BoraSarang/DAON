package com.borasarang.daon.core

import android.app.Activity
import android.view.WindowManager

/**
 * 앱 창의 밝기만 바꾼다.
 *
 * 키오스크로 고정하면 기기 설정의 `화면 밝기` 에 들어갈 수 없어서 앱 안에 둔다.
 * `WindowManager.LayoutParams.screenBrightness` 는 이 창에만 적용되므로 기기의
 * 자동 밝기·잠금 화면 밝기는 건드리지 않고, 앱을 나가면 원래대로 돌아간다.
 */
object ScreenBrightness {

    /** [percent] 0..100, 또는 [AppPrefs.BRIGHTNESS_SYSTEM](-1) 이면 기기 설정을 따른다. */
    fun apply(activity: Activity, percent: Int) {
        val params = activity.window.attributes
        params.screenBrightness = if (percent < 0) {
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        } else {
            // 0f 는 화면이 꺼진 것처럼 보이므로 최소 1% 로 둔다.
            percent.coerceIn(1, 100) / 100f
        }
        activity.window.attributes = params
    }
}
