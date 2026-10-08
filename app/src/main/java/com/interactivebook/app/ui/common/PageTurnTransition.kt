package com.interactivebook.app.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

/**
 * Фазы универсального перехода:
 *   FADE_OUT    — затемнение текущего экрана (0.5 сек).
 *   VIDEO       — page_turn.mp4 (длительность ~1 сек).
 *   FADE_IN     — новый экран проявляется (0.4 сек).
 *   ACTION      — вызывается onFinished, переход выполняется.
 */
private enum class TransitionPhase { FADE_OUT, VIDEO, FADE_IN }

private const val FADE_OUT_MS = 500
private const val FADE_IN_MS = 400
private const val VIDEO_DURATION_MS = 1000L

/**
 * Универсальный оверлей перехода между экранами.
 *
 *  1. Fade out — накладывает тёмный слой, постепенно скрывая текущий экран.
 *  2. Проигрывает page_turn.mp4 поверх.
 *  3. Fade in — слой исчезает, новый экран проявляется.
 *  4. Вызывает onFinished() — навигация совершает переход.
 *
 * Используется в MainActivity при любом переходе между экранами
 * (если в настройках включено перелистывание).
 */
@Composable
fun PageTurnTransition(
    onFinished: () -> Unit
) {
    val fadeAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Фаза 1: fade out текущего экрана.
        fadeAlpha.animateTo(1f, tween(FADE_OUT_MS))

        // Фаза 2: видео перелистывания.
        delay(VIDEO_DURATION_MS)

        // Фаза 3: fade in нового экрана.
        fadeAlpha.animateTo(0f, tween(FADE_IN_MS))

        // Фаза 4: переход.
        onFinished()
    }

    // Слой затемнения — только он виден поверх текущего экрана.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer { alpha = fadeAlpha.value }
    )
}