package com.borasarang.daon.core

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import com.borasarang.daon.R
import java.util.Locale

/**
 * 앱 내 언어 선택(시스템 / English / 한국어).
 * GMS·AppCompat 없이 attachBaseContext 에서 로케일을 감싼다.
 */
object LocaleController {
    private var systemLocale: Locale = Locale.getDefault()

    fun init() {
        systemLocale = Locale.getDefault()
    }

    fun wrap(base: Context): Context {
        val tag = AppPrefs.localeTag
        val locale = if (tag == AppPrefs.LOCALE_SYSTEM) systemLocale else Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    fun labelRes(tag: String): Int = when (tag) {
        "en" -> R.string.lang_english
        "ko" -> R.string.lang_korean
        else -> R.string.lang_system
    }

    fun set(tag: String, activity: Activity) {
        AppPrefs.localeTag = tag
        activity.recreate()
    }
}
