package com.borasarang.daon.core

import android.util.Log

/**
 * 앱 전용 로거. 사용자 노출 문자열과 달리 로그는 영어 태그를 쓴다.
 * 레벨 접두사: [INFO] / [PERF] / [CACHE] / [ERROR] (rules/quality.md).
 * 시크릿·토큰·개인정보는 절대 로그에 남기지 않는다.
 */
object DebugLogger {
    private const val TAG = "DAON"

    fun i(feature: String, message: String) {
        Log.i(TAG, "[INFO] [$feature] $message")
    }

    fun perf(feature: String, message: String) {
        Log.i(TAG, "[PERF] [$feature] $message")
    }

    fun cache(feature: String, message: String) {
        Log.d(TAG, "[CACHE] [$feature] $message")
    }

    fun e(code: String, message: String) {
        Log.e(TAG, "[ERROR] $code $message")
    }
}
