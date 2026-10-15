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
import kotlin.math.sqrt
import kotlinx.coroutines.delay

/**
 * Тип летящей награды.
 */
enum class RewardKind {
    COIN,       // монета (в плашку золота)
    ITEM,       // предмет (в закладку-сумку)
    SPARK       // информация — письмо (в закладку-дневник)
}

/**
 * Одна летящая награда.
 */
data class RewardFlight(
    val id: Long,
    val kind: RewardKind,
    val start: Offset,
    val end: Offset,
    val delayMs: Long,
    val durationMs: Int = 1000
)

private val RewardSize = 44.dp
private val ItemRewardSize = 56.dp
private val SparkSize = 56.dp
private const val ARC_HEIGHT_PX = 140f

// Витиеватый полёт искры: амплитуда и частота змейки вдоль пути.
private const val SPARK_WIGGLE_AMPL = 45f
private const val SPARK_WIGGLE_FREQ = 3f

@Composable
fun FlyingRewardsLayer(
    rewards: List<RewardFlight>,
    onRewardFinished: (Long) -> Unit
) {
    rewards.forEach { reward ->
        key(reward.id) {
            RewardFlightItem(reward = reward, onFinished = { onRewardFinished(reward.id) })
        }
    }
}

@Composable
private fun RewardFlightItem(
    reward: RewardFlight,
    onFinished: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    val density = LocalDensity.current
    val sizeDp = when (reward.kind) {
        RewardKind.COIN -> RewardSize
        RewardKind.ITEM -> ItemRewardSize
        RewardKind.SPARK -> SparkSize
    }
    val sizePx = with(density) { sizeDp.toPx() }

    LaunchedEffect(Unit) {
        delay(reward.delayMs)
        progress.animateTo(1f, tween(reward.durationMs, easing = LinearEasing))
        onFinished()
    }

    val t = progress.value
    val position = computePosition(reward, t)

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (position.x - sizePx / 2f).roundToInt(),
                    (position.y - sizePx / 2f).roundToInt()
                )
            }
            .size(sizeDp)
            .graphicsLayer {
                when (reward.kind) {
                    RewardKind.COIN -> {
                        rotationZ = t * 720f
                        val flip = abs(cos(t * PI.toFloat() * 3f))
                        scaleX = 0.35f + 0.65f * flip
                    }
                    RewardKind.ITEM -> {
                        rotationZ = sin(t * PI.toFloat() * 4f) * 12f
                    }
                    RewardKind.SPARK -> {
                        // Письмо летит так же, как мешочек — легкое покачивание.
                        rotationZ = sin(t * PI.toFloat() * 4f) * 12f
                    }
                }
            }
    ) {
        when (reward.kind) {
            RewardKind.COIN -> AssetImage(
                fileName = "coin.png",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            RewardKind.ITEM -> AssetImage(
                fileName = "item_icon.png",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            RewardKind.SPARK -> AssetImage(
                fileName = "information_icon.png",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/**
 * Позиция награды в момент t.
 *  - COIN, ITEM: простая дуга.
 *  - SPARK: дуга + змейка поперёк пути (витиеватый полёт).
 */
private fun computePosition(reward: RewardFlight, t: Float): Offset {
    val baseX = reward.start.x + (reward.end.x - reward.start.x) * t
    val baseY = reward.start.y + (reward.end.y - reward.start.y) * t

    if (reward.kind != RewardKind.SPARK) {
        val arc = -sin(t * PI.toFloat()) * ARC_HEIGHT_PX
        return Offset(baseX, baseY + arc)
    }

    val dx = reward.end.x - reward.start.x
    val dy = reward.end.y - reward.start.y
    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val perpX = -dy / len
    val perpY = dx / len

    val arc = -sin(t * PI.toFloat()) * ARC_HEIGHT_PX
    val wiggle = sin(t * PI.toFloat() * SPARK_WIGGLE_FREQ) * SPARK_WIGGLE_AMPL
    val offset = arc + wiggle

    return Offset(
        baseX + perpX * offset,
        baseY + perpY * offset
    )
}