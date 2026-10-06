package com.interactivebook.app.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.interactivebook.app.core.model.PartTitle
import kotlinx.coroutines.delay

private const val CHAR_DELAY_MS = 45L
private const val LINE_PAUSE_MS = 320L
private const val HINT_DELAY_MS = 450L

// Цвета «чернил» на странице книги.
private val InkColor = Color(0xFF3B2A1A)
private val InkDimColor = Color(0xFF6B5A44)

/**
 * Экран заголовка части/акта — страница книги (book_open.png) с текстом по центру.
 * Буквы появляются одна за другой, после — подсказка «Нажмите на экран».
 */
@Composable
fun PartTitleScreen(partTitle: PartTitle, onContinue: () -> Unit) {
    var partLabelShown by remember(partTitle) { mutableStateOf("") }
    var titleShown by remember(partTitle) { mutableStateOf("") }
    var chapterLabelShown by remember(partTitle) { mutableStateOf("") }
    var hintVisible by remember(partTitle) { mutableStateOf(false) }
    var readyToContinue by remember(partTitle) { mutableStateOf(false) }

    LaunchedEffect(partTitle) {
        for (i in 1..partTitle.partLabel.length) {
            partLabelShown = partTitle.partLabel.take(i)
            delay(CHAR_DELAY_MS)
        }
        delay(LINE_PAUSE_MS)
        for (i in 1..partTitle.title.length) {
            titleShown = partTitle.title.take(i)
            delay(CHAR_DELAY_MS)
        }
        delay(LINE_PAUSE_MS)
        for (i in 1..partTitle.chapterLabel.length) {
            chapterLabelShown = partTitle.chapterLabel.take(i)
            delay(CHAR_DELAY_MS)
        }
        delay(HINT_DELAY_MS)
        hintVisible = true
        readyToContinue = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AssetImage(
            fileName = "book_open.png",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
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
                modifier = Modifier
                    .padding(start = 120.dp, end = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = partLabelShown,
                    style = MaterialTheme.typography.labelLarge,
                    color = InkDimColor,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = titleShown,
                    style = MaterialTheme.typography.headlineMedium,
                    color = InkColor,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = chapterLabelShown,
                    style = MaterialTheme.typography.labelLarge,
                    color = InkDimColor,
                    textAlign = TextAlign.Center
                )
            }
        }

        AnimatedVisibility(
            visible = hintVisible,
            enter = fadeIn(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp)
        ) {
            Text(
                text = "Нажмите на экран",
                style = MaterialTheme.typography.labelLarge,
                color = InkDimColor
            )
        }
    }
}