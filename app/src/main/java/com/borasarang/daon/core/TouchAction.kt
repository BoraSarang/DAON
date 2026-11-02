package com.borasarang.daon.core

/**
 * 자동 전환 중 화면을 한 번 탭했을 때의 동작.
 *
 * - [DASHBOARD]: 달력(메인)으로 돌아간다. 기본값.
 * - [PAUSE_TOGGLE]: 그 화면에서 멈추고, 다시 탭하면 자동 전환을 다시 시작한다.
 * - [FIXED]: 그 화면에 고정한다. 탭해도 움직이지 않고, 두 번 탭하거나 `달력` 버튼으로 나간다.
 *
 * 어느 모드에서든 두 번 탭하면 달력으로 돌아간다(라디오·TV 제외).
 */
enum class TouchAction {
    DASHBOARD,
    PAUSE_TOGGLE,
    FIXED,
}
