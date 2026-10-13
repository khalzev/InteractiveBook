package com.interactivebook.app.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Оверлей перелистывания страницы.
 * Проигрывает page_turn.mp4 поверх текущего экрана.
 * Тап по экрану мгновенно пропускает видео.
 */
@Composable
fun PageTurnTransition(
    onFinished: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        BookVideoPlayer(
            fileName = "page_turn.mp4",
            allowSkip = true,
            onFinished = onFinished
        )
    }
}