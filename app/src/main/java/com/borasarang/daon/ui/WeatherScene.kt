package com.borasarang.daon.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 날씨 화면 배경 장면.
 *
 * Weawow 의 실사 사진 대신 **하늘을 그린다**(사진 라이선스·용량·대비 문제를 피한다).
 * - 시간대(새벽·아침·낮·해질녘·밤)에 따라 하늘·해·달이 바뀐다.
 * - 비·눈·천둥·흐림·안개면 하늘이 슬레이트색으로 가라앉는다.
 * - 구름은 0.4초 틱으로 아주 느리게 흐른다(60fps 애니메이션 금지, 상시 켜짐 대비).
 *
 * 비·눈 입자는 [WeatherEffect] 가 이 장면 **위에** 그린다.
 */
@Composable
fun WeatherScene(
    code: Int?,
    isDay: Boolean,
    motion: Boolean,
    light: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val hour = rememberHourOfDay()
    val millis = rememberSceneMillis(motion)
    val palette = remember(hour.value, code, light) {
        val base = if (light) lightSkyFor(hour.value) else skyFor(hour.value)
        base.under(code, light)
    }

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        drawRect(Brush.verticalGradient(listOf(palette.top, palette.middle, palette.horizon), 0f, h))
        drawField(palette, w, h)

        val seconds = millis.value / 1000f
        if (isDay) {
            drawSceneSun(palette, seconds, w, h)
        } else {
            drawSceneStars(palette, seconds, w, h)
            drawSceneMoon(palette, w, h)
        }
        drawSceneClouds(palette, seconds, w, h, drift = motion)
    }
}

// ---------- 색 ----------

private data class SkyPalette(
    val top: Color,
    val middle: Color,
    val horizon: Color,
    val sun: Color = Color(0xFFFFE9C4),
    val cloud: Color = Color(0xFFBFD0E0),
    val sunAlpha: Float = 1f,
    /** 아래쪽 들판을 덮는 색. 라이트 테마는 어둡게 덮지 않는다. */
    val field: Color = Color(0xFF070D0C),
    val fieldAlpha: Float = 0.92f,
    val moon: Color = Color(0xFFEDE6D2),
    val moonGlow: Color = Color(0xFFDCE6FF),
    val starAlpha: Float = 1f,
)

/** 1920×1200 장면 기준 시간대 색. 지평선은 밝게 두되 카드가 덮으므로 무늬 없이 매끈하게 유지한다. */
private fun skyFor(hour: Int): SkyPalette = when (hour) {
    in 0..4 -> SkyPalette(
        top = Color(0xFF05080F),
        middle = Color(0xFF0A1220),
        horizon = Color(0xFF17253A),
        cloud = Color(0xFF5C6C93),
        sunAlpha = 0f,
    )
    in 5..6 -> SkyPalette(
        top = Color(0xFF080C14),
        middle = Color(0xFF223049),
        horizon = Color(0xFF8A6A55),
        sun = Color(0xFFFFD3A0),
    )
    in 7..10 -> SkyPalette(
        top = Color(0xFF0B1622),
        middle = Color(0xFF21415A),
        horizon = Color(0xFF8FA7A2),
    )
    in 11..15 -> SkyPalette(
        top = Color(0xFF0C1622),
        middle = Color(0xFF1F425C),
        horizon = Color(0xFF93A99B),
    )
    in 16..18 -> SkyPalette(
        top = Color(0xFF161020),
        middle = Color(0xFF4A2F3E),
        horizon = Color(0xFFC08A5A),
        sun = Color(0xFFFFC07A),
    )
    else -> SkyPalette(
        top = Color(0xFF05080F),
        middle = Color(0xFF0B1424),
        horizon = Color(0xFF1B2A3A),
        cloud = Color(0xFF5C6C93),
        sunAlpha = 0f,
    )
}

/**
 * 라이트 테마용 하늘. 흰 카드·종이색 바탕과 이어지도록 하늘도 옅게 둔다.
 * 밤에는 별을 그리지 않고, 달은 회청색으로 낮은 대비만 준다.
 */
private fun lightSkyFor(hour: Int): SkyPalette = when (hour) {
    in 0..4 -> SkyPalette(
        top = Color(0xFFC6D1E2),
        middle = Color(0xFFDCE3EE),
        horizon = Color(0xFFEEF0F2),
        cloud = Color(0xFFFFFFFF),
        sunAlpha = 0f,
        field = Color(0xFFD8D0C0),
        fieldAlpha = 0.30f,
        moon = Color(0xFF9AA5B6),
        moonGlow = Color(0xFF8FA0BE),
        starAlpha = 0f,
    )
    in 5..6 -> SkyPalette(
        top = Color(0xFFB9CBDE),
        middle = Color(0xFFE3D3C6),
        horizon = Color(0xFFF6E4D2),
        sun = Color(0xFFFFCE8C),
        cloud = Color(0xFFFFFDF8),
        field = Color(0xFFD9C9B2),
        fieldAlpha = 0.28f,
        starAlpha = 0f,
    )
    in 7..10 -> SkyPalette(
        top = Color(0xFFAECBE4),
        middle = Color(0xFFD5E5EE),
        horizon = Color(0xFFF1EEE2),
        sun = Color(0xFFFFDA95),
        cloud = Color(0xFFFFFFFF),
        field = Color(0xFFD9CFBA),
        fieldAlpha = 0.26f,
        starAlpha = 0f,
    )
    in 11..15 -> SkyPalette(
        top = Color(0xFFA6C6E2),
        middle = Color(0xFFCFE2EC),
        horizon = Color(0xFFF0EDDF),
        sun = Color(0xFFFFE3A8),
        cloud = Color(0xFFFFFFFF),
        field = Color(0xFFD7CDB6),
        fieldAlpha = 0.26f,
        starAlpha = 0f,
    )
    in 16..18 -> SkyPalette(
        top = Color(0xFFC9BDD2),
        middle = Color(0xFFE8CBB6),
        horizon = Color(0xFFF6DDBE),
        sun = Color(0xFFFFC27F),
        cloud = Color(0xFFFFF8F0),
        field = Color(0xFFD8C3A6),
        fieldAlpha = 0.28f,
        starAlpha = 0f,
    )
    else -> SkyPalette(
        top = Color(0xFFC6D1E2),
        middle = Color(0xFFDCE3EE),
        horizon = Color(0xFFEEF0F2),
        cloud = Color(0xFFFFFFFF),
        sunAlpha = 0f,
        field = Color(0xFFD8D0C0),
        fieldAlpha = 0.30f,
        moon = Color(0xFF9AA5B6),
        moonGlow = Color(0xFF8FA0BE),
        starAlpha = 0f,
    )
}

/** 비·흐림·안개면 하늘을 가라앉히고 해를 가린다. 라이트 테마는 회청색으로만 흐린다. */
private fun SkyPalette.under(code: Int?, light: Boolean): SkyPalette {
    if (code == null) return this
    val wet = effectOf(code) != EffectKind.NONE
    val wetMiddle = if (light) Color(0xFF9FB0BC) else Color(0xFF22303A)
    val wetHorizon = if (light) Color(0xFFC6D0D4) else Color(0xFF47585B)
    val wetCloud = if (light) Color(0xFFF7F9FA) else Color(0xFF7D8C99)
    val overcastMiddle = if (light) Color(0xFFB4C2CC) else Color(0xFF33424C)
    val overcastHorizon = if (light) Color(0xFFD5DCDD) else Color(0xFF7A8A8C)
    return when {
        wet -> copy(
            middle = lerp(middle, wetMiddle, 0.55f),
            horizon = lerp(horizon, wetHorizon, 0.5f),
            cloud = lerp(cloud, wetCloud, 0.5f),
            sunAlpha = sunAlpha * 0.35f,
        )
        code == 3 || code == 45 || code == 48 -> copy(
            middle = lerp(middle, overcastMiddle, 0.5f),
            horizon = lerp(horizon, overcastHorizon, 0.4f),
            sunAlpha = sunAlpha * 0.45f,
        )
        else -> this
    }
}

// ---------- 그리기 ----------

/** 아래쪽을 어둡게 깔아 카드가 뜨지 않고 들판처럼 앉게 한다. 경계선은 만들지 않는다. */
private fun DrawScope.drawField(palette: SkyPalette, w: Float, h: Float) {
    drawRect(
        Brush.radialGradient(
            colors = listOf(
                palette.field.copy(alpha = palette.fieldAlpha),
                palette.field.copy(alpha = 0f),
            ),
            center = Offset(w * 0.5f, h * 1.12f),
            radius = h * 0.62f,
        ),
    )
}

private fun DrawScope.drawSceneSun(palette: SkyPalette, seconds: Float, w: Float, h: Float) {
    if (palette.sunAlpha <= 0f) return
    val center = Offset(w * 0.62f, h * 0.07f)
    val glow = w * 0.16f

    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                palette.sun.copy(alpha = 0.50f * palette.sunAlpha),
                palette.sun.copy(alpha = 0.14f * palette.sunAlpha),
                Color.Transparent,
            ),
            center = center,
            radius = glow,
        ),
        radius = glow,
        center = center,
    )
    drawCircle(palette.sun.copy(alpha = 0.85f * palette.sunAlpha), w * 0.014f, center)

    rotate(seconds * 1.2f, center) {
        repeat(14) { index ->
            val angle = index * 2f * PI.toFloat() / 14f
            val inner = w * 0.030f
            val outer = w * (0.082f + 0.010f * sin(index * 1.7f))
            drawLine(
                color = palette.sun.copy(alpha = 0.09f * palette.sunAlpha),
                start = Offset(center.x + cos(angle) * inner, center.y + sin(angle) * inner),
                end = Offset(center.x + cos(angle) * outer, center.y + sin(angle) * outer),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawSceneMoon(palette: SkyPalette, w: Float, h: Float) {
    val center = Offset(w * 0.44f, h * 0.06f)
    val radius = w * 0.019f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(palette.moonGlow.copy(alpha = 0.16f), Color.Transparent),
            center = center,
            radius = radius * 5f,
        ),
        radius = radius * 5f,
        center = center,
    )
    drawCircle(palette.moon, radius, center)
    drawCircle(palette.middle.copy(alpha = 0.94f), radius * 0.92f, Offset(center.x + radius * 0.42f, center.y - radius * 0.30f))
}

/** 화면 비율로 고정한 별 18개. 움직임이 켜져 있으면 천천히 밝아졌다 어두워진다. */
private val STARS = listOf(
    0.062f to 0.075f, 0.135f to 0.125f, 0.219f to 0.050f, 0.292f to 0.108f,
    0.365f to 0.058f, 0.458f to 0.108f, 0.531f to 0.046f, 0.615f to 0.092f,
    0.688f to 0.058f, 0.766f to 0.117f, 0.833f to 0.050f, 0.906f to 0.100f,
    0.047f to 0.183f, 0.177f to 0.217f, 0.312f to 0.200f, 0.469f to 0.233f,
    0.640f to 0.183f, 0.902f to 0.208f,
)

private fun DrawScope.drawSceneStars(palette: SkyPalette, seconds: Float, w: Float, h: Float) {
    if (palette.starAlpha <= 0f) return
    STARS.forEachIndexed { index, (x, y) ->
        val alpha = (0.26f + 0.20f * sin(seconds * 0.6f + index)) * palette.starAlpha
        drawCircle(
            color = Color(0xFFEAF0FF).copy(alpha = alpha),
            radius = if (index % 4 == 0) 2.2.dp.toPx() else 1.4.dp.toPx(),
            center = Offset(x * w, y * h),
        )
    }
}

private data class SceneCloud(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val speed: Float,
)

private val CLOUDS = listOf(
    SceneCloud(0.10f, 0.050f, 0.34f, 0.13f, 0.0040f),
    SceneCloud(0.58f, 0.015f, 0.30f, 0.11f, 0.0026f),
    SceneCloud(0.30f, 0.160f, 0.40f, 0.15f, 0.0018f),
    SceneCloud(0.82f, 0.120f, 0.28f, 0.10f, 0.0032f),
)

private fun DrawScope.drawSceneClouds(
    palette: SkyPalette,
    seconds: Float,
    w: Float,
    h: Float,
    drift: Boolean,
) {
    CLOUDS.forEach { cloud ->
        val shift = if (drift) (seconds * cloud.speed) % 0.35f else 0f
        val cx = (cloud.x + shift) * w
        val cy = cloud.y * h
        val width = cloud.width * w
        val height = cloud.height * h
        drawOval(
            brush = Brush.radialGradient(
                listOf(palette.cloud.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(cx, cy),
                radius = width * 0.55f,
            ),
            topLeft = Offset(cx - width * 0.5f, cy - height * 0.5f),
            size = Size(width, height),
        )
    }
}

// ---------- 시간 ----------

/** 1분에 한 번만 확인해 시간대(낮/밤)가 바뀌면 장면을 갱신한다. */
@Composable
private fun rememberHourOfDay(): State<Int> {
    val hour = remember { mutableIntStateOf(LocalTime.now().hour) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            val now = LocalTime.now().hour
            if (now != hour.intValue) hour.intValue = now
        }
    }
    return hour
}

/**
 * 장면용 느린 시계(ms). 0.4초마다만 갱신해 60fps 로 그리지 않는다.
 * [motion] 이 꺼져 있으면 0으로 고정 → 구름·별이 멈춘다.
 */
@Composable
private fun rememberSceneMillis(motion: Boolean): State<Long> {
    val time = remember { mutableLongStateOf(0L) }
    LaunchedEffect(motion) {
        if (!motion) {
            time.longValue = 0L
            return@LaunchedEffect
        }
        val start = android.os.SystemClock.elapsedRealtime()
        while (true) {
            kotlinx.coroutines.delay(400)
            time.longValue = android.os.SystemClock.elapsedRealtime() - start
        }
    }
    return time
}
