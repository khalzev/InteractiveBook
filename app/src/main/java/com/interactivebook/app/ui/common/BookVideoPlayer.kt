package com.interactivebook.app.ui.common

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Проигрывает видео из assets/ui/ по имени файла на весь экран.
 *
 * - Если файла нет — тихо вызывает onFinished() сразу.
 * - `playWhenReady = true` — стартует автоматически.
 * - `repeatMode = OFF` — проигрывается один раз.
 * - Плеер освобождается в DisposableEffect.
 */
@Composable
fun BookVideoPlayer(
    fileName: String,
    modifier: Modifier = Modifier,
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

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    onFinished()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}