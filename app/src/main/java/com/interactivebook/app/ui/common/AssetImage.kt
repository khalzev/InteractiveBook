package com.interactivebook.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Загружает картинку из assets/ui/ по имени файла.
 *
 * Пример: AssetImage("scene_background.png", ...).
 *
 * Если файл отсутствует — показывается пустой Box или fallbackColor.
 * Это позволяет собирать проект и смотреть его даже до того, как все PNG сгенерированы:
 * код не падает, просто визуально "пропускает" отсутствующий элемент.
 *
 * Картинка читается из assets/ через Coil: "file:///android_asset/ui/<name>".
 */
@Composable
fun AssetImage(
    fileName: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackColor: Color? = null
) {
    val context = LocalContext.current
    val hasAsset = remember(fileName) {
        try {
            context.assets.open("ui/$fileName").close()
            true
        } catch (e: Exception) {
            false
        }
    }

    Box(modifier = modifier) {
        if (hasAsset) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/ui/$fileName")
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else if (fallbackColor != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fallbackColor)
            )
        }
    }
}