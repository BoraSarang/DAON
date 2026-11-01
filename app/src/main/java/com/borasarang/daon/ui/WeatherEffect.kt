package com.borasarang.daon.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.borasarang.daon.ui.theme.Kiosk
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

internal enum class EffectKind { NONE, RAIN, SNOW, THUNDER }

/** 코드 분류는 [WeatherScene] 의 하늘 판정과 공유한다. */
internal fun effectOf(code: Int): EffectKind = when (code) {
    95, 96, 99 -> EffectKind.THUNDER
    71, 73, 75, 77, 85, 86 -> EffectKind.SNOW
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> EffectKind.RAIN
    else -> EffectKind.NONE
}

private data class Drop(
    val x: Float,
    val offset: Float,
    val speed: Float,
)

private data class Flake(
    val x: Float,
    val offset: Float,
    val speed: Float,
    val radius: Float,
    val phase: Float,
)

private const val THUNDER_CYCLE_MS = 6500f

/**
 * 날씨 화면 배경 효과. 비·눈·천둥만 움직인다(맑음·흐림은 정적).
 * 설정에서 [motion]을 끄면 아무것도 그리지 않는다.
 *
 * Compose 애니메이션 대신 프레임 시계로 직접 시간을 재기 때문에,
 * 기기의 '애니메이션 끄기'(animator_duration_scale=0) 설정과 무관하게 움직인다.
 * 화면이 바뀌면 컴포지션에서 빠져 루프도 함께 멈춘다.
 */
@Composable
fun WeatherEffect(code: Int?, motion: Boolean, modifier: Modifier = Modifier) {
    if (!motion || code == null) return
    when (effectOf(code)) {
        EffectKind.RAIN -> RainEffect(modifier)
        EffectKind.SNOW -> SnowEffect(modifier)
        EffectKind.THUNDER -> Box(modifier.fillMaxSize()) {
            RainEffect(Modifier.fillMaxSize())
            ThunderFlash(Modifier.fillMaxSize())
        }
        EffectKind.NONE -> Unit
    }
}

/** 컴포지션에 있는 동안 프레임마다 갱신되는 경과 시간(ms). 읽는 쪽이 그리기 단계면 재구성 없이 다시 그린다. */
@Composable
private fun rememberFrameTimeMillis(): State<Long> {
    val time = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now -> time.longValue = (now - start) / 1_000_000L }
        }
    }
    return time
}

@Composable
private fun RainEffect(modifier: Modifier) {
    val time = rememberFrameTimeMillis()
    val drops = remember {
        val random = Random(7)
        List(64) {
            Drop(
                x = random.nextFloat(),
                offset = random.nextFloat(),
                speed = 0.7f + random.nextFloat() * 0.6f,
            )
        }
    }
    Canvas(modifier) {
        val elapsed = time.value
        val length = 26.dp.toPx()
        val stroke = 1.6.dp.toPx()
        drops.forEach { drop ->
            val travel = elapsed / 1300f * drop.speed
            val y = ((drop.offset + travel) % 1f) * (size.height + length) - length
            val x = drop.x * size.width
            drawLine(
                // 라이트 테마의 흰 카드 위에서는 빗줄기가 글자를 가리지 않도록 더 옅게 그린다.
                color = Kiosk.color.rain.copy(alpha = if (Kiosk.isLight) 0.16f else 0.26f),
                start = Offset(x, y),
                end = Offset(x - length * 0.25f, y + length),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun SnowEffect(modifier: Modifier) {
    val time = rememberFrameTimeMillis()
    val flakes = remember {
        val random = Random(11)
        List(64) {
            Flake(
                x = random.nextFloat(),
                offset = random.nextFloat(),
                speed = 0.5f + random.nextFloat() * 0.5f,
                radius = 2f + random.nextFloat() * 3f,
                phase = random.nextFloat() * 2f * PI.toFloat(),
            )
        }
    }
    val flakeColor = if (Kiosk.isLight) {
        Kiosk.color.textSecondary.copy(alpha = 0.30f)
    } else {
        Color.White.copy(alpha = 0.42f)
    }
    Canvas(modifier) {
        val elapsed = time.value
        val drift = 16.dp.toPx()
        flakes.forEach { flake ->
            val travel = elapsed / 7000f * flake.speed
            val y = ((flake.offset + travel) % 1f) * size.height
            val x = flake.x * size.width +
                sin(elapsed / 7000f * 2f * PI.toFloat() + flake.phase) * drift
            drawCircle(
                color = flakeColor,
                radius = flake.radius.dp.toPx(),
                center = Offset(x, y),
            )
        }
    }
}

@Composable
private fun ThunderFlash(modifier: Modifier) {
    val time = rememberFrameTimeMillis()
    Canvas(modifier) {
        val phase = (time.value % THUNDER_CYCLE_MS.toLong()) / THUNDER_CYCLE_MS
        val alpha = flashAlpha(phase)
        if (alpha > 0f) {
            drawRect(Color.White.copy(alpha = alpha))
        }
    }
}

/** 한 번 번쩍였다가 잠시 뒤 약하게 한 번 더. 6.5초 주기. */
private fun flashAlpha(phase: Float): Float = when {
    phase < 0.80f -> 0f
    phase < 0.84f -> 0.18f * (phase - 0.80f) / 0.04f
    phase < 0.865f -> 0.18f
    phase < 0.885f -> 0.02f + 0.16f * (1f - (phase - 0.865f) / 0.02f)
    phase < 0.905f -> 0.02f + 0.08f * (phase - 0.885f) / 0.02f
    phase < 0.935f -> 0.10f * (1f - (phase - 0.905f) / 0.03f)
    else -> 0f
}
