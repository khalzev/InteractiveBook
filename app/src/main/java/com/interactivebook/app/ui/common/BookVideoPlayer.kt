package com.interactivebook.app.ui.common

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * Проигрывает видео из assets/ui/ по имени файла на весь экран,
 * поверх подложки background.png (текстура дерева).
 *
 * Особенности:
 * - Если файла нет — тихо вызывает onFinished() сразу.
 * - playWhenReady = true, repeatMode = OFF, volume = 0.
 * - resizeMode = FIT — видео вписывается целиком.
 * - allowSkip = true → тап по экрану мгновенно завершает видео.
 * - allowSkip = false → тап игнорируется.
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

    // Корневой Box — фон дерева, поверх — видео, поверх — слой для тапа (если allowSkip).
    Box(modifier = modifier.fillMaxSize()) {

        // Слой 0: фон — текстура дерева.
        AssetImage(
            fileName = "background.png",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Слой 1: видео.
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Слой 2: если allowSkip — невидимая зона нажатия на весь экран.
        // Тап мгновенно завершает видео.
        if (allowSkip) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onFinished() }
                    )
            )
        }
    }
}

