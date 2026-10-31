package com.borasarang.daon.core

/** 시계 화면 모양. 저장은 이름 문자열로 하고, 읽을 때 안전하게 변환한다. */
enum class ClockFace {
    ANALOG,
    DIGITAL,
    HYBRID,
    ;

    companion object {
        fun fromName(name: String?): ClockFace =
            entries.firstOrNull { it.name == name } ?: ANALOG
    }
}
