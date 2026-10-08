package com.interactivebook.app.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.interactivebook.app.R
import com.interactivebook.app.core.checks.CheckOutcome
import com.interactivebook.app.core.checks.CheckResolution
import com.interactivebook.app.core.effects.Effect
import com.interactivebook.app.core.model.Action
import com.interactivebook.app.core.model.GameResource
import com.interactivebook.app.core.model.Item
import com.interactivebook.app.ui.check.CheckRollOverlay
import com.interactivebook.app.ui.common.BookPageBackground
import com.interactivebook.app.ui.common.BookPageContent

private val InkColor = Color(0xFF3B2A1A)
private val InkDimColor = Color(0xFF6B5A44)
private val BurgundyColor = Color(0xFF7A2525)

/**
 * Инвентарь — страница книги. Контент внутри BookPageContent.
 */
@Composable
fun InventoryScreen(
    items: List<Item>,
    resources: Map<String, GameResource>,
    examineActionFor: (Item) -> Action?,
    onSimpleAction: (Action) -> Unit,
    onResolveCheck: (Action) -> CheckResolution?,
    onApplyCheckOutcome: (Action, CheckOutcome) -> Unit,
    outcomeEffects: (Action, CheckOutcome) -> List<Effect>,
    onBack: () -> Unit
) {
    var pendingCheck by remember { mutableStateOf<Pair<Action, CheckResolution>?>(null) }

    fun containsTransition(effects: List<Effect>) = effects.any { it is Effect.Transition }

    fun examine(item: Item) {
        val action = examineActionFor(item) ?: return
        if (action.check != null) {
            val resolution = onResolveCheck(action)
            if (resolution != null) pendingCheck = action to resolution
        } else {
            onSimpleAction(action)
            if (containsTransition(action.effects)) onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BookPageBackground()

        BookPageContent {
            Column(modifier = Modifier.fillMaxSize()) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.back), color = InkDimColor)
                }

                Text(
                    text = "Инвентарь",
                    style = MaterialTheme.typography.headlineMedium,
                    color = InkColor
                )
                Spacer(Modifier.height(12.dp))

                if (resources.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        resources.values.forEach { resource ->
                            Text(
                                text = "${resourceIcon(resource.id)} ${resource.name}: ${resource.currentValue}",
                                style = MaterialTheme.typography.labelLarge,
                                color = InkColor
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Пока пусто",
                            style = MaterialTheme.typography.bodyLarge,
                            color = InkDimColor
                        )
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(items, key = { it.id }) { item ->
                            val examinable = examineActionFor(item) != null
                            ItemRow(item = item, examinable = examinable, onClick = {
                                if (examinable) examine(item)
                            })
                        }
                    }
                }
            }
        }

        pendingCheck?.let { (action, resolution) ->
            CheckRollOverlay(
                resolution = resolution,
                onContinue = {
                    onApplyCheckOutcome(action, resolution.outcome)
                    val transitions = containsTransition(outcomeEffects(action, resolution.outcome))
                    pendingCheck = null
                    if (transitions) onBack()
                }
            )
        }
    }
}

@Composable
private fun ItemRow(item: Item, examinable: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = examinable, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "◆",
            color = BurgundyColor,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(end = 10.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (item.quantity > 1) "${item.name} ×${item.quantity}" else item.name,
                style = MaterialTheme.typography.titleLarge,
                color = InkColor
            )
            if (item.description.isNotBlank()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkDimColor
                )
            }
            if (examinable) {
                Text(
                    text = "Рассмотреть",
                    style = MaterialTheme.typography.labelLarge,
                    color = BurgundyColor
                )
            }
        }
    }
}

private fun resourceIcon(resourceId: String): String = when (resourceId) {
    "gold" -> "🪙"
    "time" -> "⏳"
    else -> "◆"
}