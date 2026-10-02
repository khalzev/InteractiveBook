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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interactivebook.app.core.model.Information
import com.interactivebook.app.core.model.InformationType

/**
 * Журнал героя (раздел 42): цели — куда двигаться игроку, и заметки — вольные наблюдения
 * и рассуждения, как очерк. Связанные записи (linkedTo) показаны подписью друг у друга.
 */
@Composable
fun JournalScreen(
    information: List<Information>,
    onBack: () -> Unit
) {
    val goals = information.filter { it.type == InformationType.GOAL }
    val notes = information.filter { it.type == InformationType.NOTE }
    val titleById = information.associateBy({ it.id }, { it.title })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Назад") }
        Text("Журнал героя", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        if (information.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Журнал пока пуст",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        LazyColumn {
            if (goals.isNotEmpty()) {
                item {
                    Text("Цели", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                }
                items(goals, key = { it.id }) { entry ->
                    JournalCard(entry, titleById)
                    Spacer(Modifier.height(12.dp))
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
            if (notes.isNotEmpty()) {
                item {
                    Text("Заметки", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                }
                items(notes, key = { it.id }) { entry ->
                    JournalCard(entry, titleById)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun JournalCard(entry: Information, titleById: Map<String, String>) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (entry.topic.isNotBlank()) {
                Text(
                    text = entry.topic.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(entry.title, style = MaterialTheme.typography.titleLarge)
            if (entry.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(entry.description, style = MaterialTheme.typography.bodyLarge)
            }
            if (entry.linkedTo.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                val linkedTitles = entry.linkedTo.mapNotNull { titleById[it] }
                if (linkedTitles.isNotEmpty()) {
                    Text(
                        text = "🔗 Связано: " + linkedTitles.joinToString(", "),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
