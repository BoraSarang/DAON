package com.borasarang.daon.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/**
 * 화면에 보이는 현재 시각.
 *
 * [perSecond] 가 true 면 초 경계마다, false 면 분 경계마다 갱신한다. 초를 보여 주지 않는
 * 화면(달력·날씨)에서 1초마다 다시 그리면 42칸 달력까지 매초 재구성되어 CPU·배터리를
 * 낭비한다. 값은 [State] 로 돌려주므로 실제로 읽는 컴포저블만 다시 그린다.
 *
 * 갱신 시각을 분·초 경계에 맞춰, 화면 숫자가 최대 1초 늦게 바뀌는 일이 없게 한다.
 */
@Composable
fun rememberNow(perSecond: Boolean): State<LocalDateTime> {
    val state = remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(perSecond) {
        // 화면을 바꿔 들어온 순간에는 값이 최대 1분 늦을 수 있다. 바로 한 번 맞춘다.
        state.value = LocalDateTime.now()
        while (true) {
            val now = LocalDateTime.now()
            val millisIntoUnit = if (perSecond) {
                now.nano / 1_000_000L % 1_000L
            } else {
                now.second * 1_000L + now.nano / 1_000_000L
            }
            val unitMillis = if (perSecond) 1_000L else 60_000L
            delay((unitMillis - millisIntoUnit).coerceAtLeast(50L))
            state.value = LocalDateTime.now()
        }
    }
    return state
}
