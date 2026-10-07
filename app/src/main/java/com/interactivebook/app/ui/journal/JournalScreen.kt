package com.interactivebook.app.ui.journal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.interactivebook.app.R
import com.interactivebook.app.core.model.Information
import com.interactivebook.app.core.model.InformationType
import com.interactivebook.app.ui.common.AssetImage

private val InkColor = Color(0xFF3B2A1A)
private val InkDimColor = Color(0xFF6B5A44)
private val GoldColor = Color(0xFFB8925A)

/**
 * Журнал героя — страница книги (book_open.png) с записями поверх.
 * Цели — куда двигаться. Заметки — вольные наблюдения.
 *
 * Визуал позже: страница под заметки будет отдельной (book_notes.png).
 */
@Composable
fun JournalScreen(
    information: List<Information>,
    onBack: () -> Unit
) {
    val goals = information.filter { it.type == InformationType.GOAL }
    val notes = information.filter { it.type == InformationType.NOTE }
    val titleById = information.associateBy({ it.id }, { it.title })

    Box(modifier = Modifier.fillMaxSize()) {
        // Слой 0: фон — текстура дерева на весь экран.
        AssetImage(
            fileName = "background.png",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Слой 1: книга — вписывается целиком (Fit).
        AssetImage(
            fileName = "book_open.png",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(start = 118.dp, end = 28.dp, top = 130.dp, bottom = 40.dp)
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.back), color = InkDimColor)
            }

            Text(
                text = "Заметки",
                style = MaterialTheme.typography.headlineMedium,
                color = InkColor
            )
            Spacer(Modifier.height(12.dp))

            if (information.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Журнал пока пуст",
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkDimColor
                    )
                }
                return@Column
            }

            LazyColumn {
                if (goals.isNotEmpty()) {
                    item {
                        Text("Цели", style = MaterialTheme.typography.titleLarge, color = GoldColor)
                        Spacer(Modifier.height(6.dp))
                    }
                    items(goals, key = { it.id }) { entry ->
                        JournalEntry(entry, titleById)
                        Spacer(Modifier.height(10.dp))
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
                if (notes.isNotEmpty()) {
                    item {
                        Text("Заметки", style = MaterialTheme.typography.titleLarge, color = GoldColor)
                        Spacer(Modifier.height(6.dp))
                    }
                    items(notes, key = { it.id }) { entry ->
                        JournalEntry(entry, titleById)
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalEntry(entry: Information, titleById: Map<String, String>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (entry.topic.isNotBlank()) {
            Text(
                text = entry.topic.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = GoldColor
            )
            Spacer(Modifier.height(2.dp))
        }
        Text(entry.title, style = MaterialTheme.typography.titleLarge, color = InkColor)
        if (entry.description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(entry.description, style = MaterialTheme.typography.bodyLarge, color = InkColor)
        }
        if (entry.linkedTo.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            val linkedTitles = entry.linkedTo.mapNotNull { titleById[it] }
            if (linkedTitles.isNotEmpty()) {
                Text(
                    text = "🔗 Связано: " + linkedTitles.joinToString(", "),
                    style = MaterialTheme.typography.labelLarge,
                    color = InkDimColor
                )
            }
        }
    }
}