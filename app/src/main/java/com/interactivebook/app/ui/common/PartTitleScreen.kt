package com.interactivebook.app.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.interactivebook.app.core.model.PartTitle
import kotlinx.coroutines.delay

private const val HINT_DELAY_MS = 600L

private val HintColor = Color(0xFF6B5A44)

/**
 * Экран титула части — страница книги с двумя картинками:
 *   1. chapterImage — название главы (например "chapter_title.png"),
 *   2. partImage — название части (например "part_title_prologue.png").
 *
 * Контент лежит внутри BookPageContent — то есть внутри правой страницы книги.
 */
@Composable
fun PartTitleScreen(partTitle: PartTitle, onContinue: () -> Unit) {
    var hintVisible by remember(partTitle) { mutableStateOf(false) }
    var readyToContinue by remember(partTitle) { mutableStateOf(false) }

    LaunchedEffect(partTitle) {
        delay(HINT_DELAY_MS)
        hintVisible = true
        readyToContinue = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Фон: дерево + книга + рамка.
        BookPageBackground()

        // Контент внутри правой страницы.
        BookPageContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (readyToContinue) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onContinue
                            )
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Картинка главы.
                    partTitle.chapterImage?.let { image ->
                        AssetImage(
                            fileName = image,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.height(24.dp))
                    }

                    // Картинка части.
                    AssetImage(
                        fileName = partTitle.partImage,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        // Подсказка внизу (вне страницы — на фоне).
        AnimatedVisibility(
            visible = hintVisible,
            enter = fadeIn(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        ) {
            Text(
                text = "Нажмите на экран",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFE8DBC0)
            )
        }
    }
}