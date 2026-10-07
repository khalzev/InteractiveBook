package com.interactivebook.app.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Оверлей для перелистывания страниц.
 *
 * Показывается ПОВЕРХ текущего экрана, пока проигрывается видео перелистывания
 * (page_turn.mp4). По окончании видео вызывается onFinished() — навигация
 * переключает экран, и оверлей закрывается.
 *
 * Если pageTurnEnabled = false — оверлей сразу вызывает onFinished() без показа
 * видео (мгновенный переход).
 *
 * Компонент НЕ блокирует нажатия клавиш "назад" — этим управляет навигация.
 */
@Composable
fun PageTurnOverlay(
    enabled: Boolean,
    onFinished: () -> Unit
) {
    if (!enabled) {
        // Перелистывание отключено в настройках — сразу завершаем.
        onFinished()
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BookVideoPlayer(
            fileName = "page_turn.mp4",
            allowSkip = false,
            onFinished = onFinished
        )
    }
}