package com.interactivebook.app.ui.common

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Проигрывает видео из assets/ui/ поверх холста book_page.png.
 *
 * Всегда блокирует тапы по экрану во время проигрывания.
 * Если allowSkip = true — тап пропускает видео.
 * Если allowSkip = false — тап игнорируется.
 */
@Composable
fun BookVideoPlayer(
    fileName: String,
    modifier: Modifier = Modifier,
    allowSkip: Boolean = false,
    onFinished: () -> Unit
) {
    val context = LocalContext.current

    val exists = remember(fileName) {
        try {
            context.assets.open("ui/$fileName").close()
            true
        } catch (e: Exception) {
            false
        }
    }

    if (!exists) {
        LaunchedEffect(fileName) {
            onFinished()
        }
        return
    }

    val exoPlayer = remember(fileName) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse("file:///android_asset/ui/$fileName")))
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
            volume = 0f
        }
    }

    var firstFrameRendered by remember(fileName) { mutableStateOf(false) }
    val fadeIn = remember(fileName) { Animatable(0f) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    onFinished()
                }
            }
            override fun onRenderedFirstFrame() {
                firstFrameRendered = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(firstFrameRendered) {
        if (firstFrameRendered) {
            fadeIn.animateTo(1f, tween(200))
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        BookPageBackground()

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = fadeIn.value
                    scaleX = 1.01f
                    scaleY = 1.01f
                }
        )

        // Перехватчик тапов. Всегда активен.
        // allowSkip = true — тап завершает видео.
        // allowSkip = false — тап блокируется (не проходит к элементам под видео).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (allowSkip) onFinished()
                    }
                )
        )
    }
}