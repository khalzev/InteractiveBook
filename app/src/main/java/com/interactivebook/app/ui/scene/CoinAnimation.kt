package com.interactivebook.app.ui.scene

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.interactivebook.app.ui.common.AssetImage
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * Одна летящая монетка. Летит по дуге от start к end за durationMs, с задержкой delayMs.
 * Вращается (rotationZ) и "переворачивается" (scaleX) для объёма.
 * По окончании вызывает onFinished.
 */
data class CoinFlight(
    val id: Long,
    val start: Offset,
    val end: Offset,
    val delayMs: Long,
    val durationMs: Int = 600
)

private val CoinSize = 44.dp
private const val ARC_HEIGHT_PX = 140f

@Composable
fun CoinFlightLayer(
    coins: List<CoinFlight>,
    onCoinFinished: (Long) -> Unit
) {
    coins.forEach { coin ->
        key(coin.id) {
            CoinFlightItem(coin = coin, onFinished = { onCoinFinished(coin.id) })
        }
    }
}

@Composable
private fun CoinFlightItem(
    coin: CoinFlight,
    onFinished: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    val density = LocalDensity.current
    val coinSizePx = with(density) { CoinSize.toPx() }

    LaunchedEffect(Unit) {
        delay(coin.delayMs)
        progress.animateTo(1f, tween(coin.durationMs, easing = LinearEasing))
        onFinished()
    }

    val t = progress.value

    // Позиция по дуге.
    val x = coin.start.x + (coin.end.x - coin.start.x) * t
    val baseY = coin.start.y + (coin.end.y - coin.start.y) * t
    val arc = -sin(t * PI.toFloat()) * ARC_HEIGHT_PX
    val y = baseY + arc

    // Вращение и "3D-переворот".
    val rotation = t * 720f
    val flipScaleX = abs(cos(t * PI.toFloat() * 3f))
    val scaleX = 0.35f + 0.65f * flipScaleX

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (x - coinSizePx / 2f).roundToInt(),
                    (y - coinSizePx / 2f).roundToInt()
                )
            }
            .size(CoinSize)
            .graphicsLayer {
                rotationZ = rotation
                this.scaleX = scaleX
            }
    ) {
        AssetImage(
            fileName = "coin.png",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}