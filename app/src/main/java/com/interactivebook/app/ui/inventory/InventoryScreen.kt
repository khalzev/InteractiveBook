package com.interactivebook.app.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interactivebook.app.core.checks.CheckOutcome
import com.interactivebook.app.core.checks.CheckResolution
import com.interactivebook.app.core.effects.Effect
import com.interactivebook.app.core.model.Action
import com.interactivebook.app.core.model.GameResource
import com.interactivebook.app.core.model.Item
import com.interactivebook.app.ui.check.CheckRollOverlay

/**
 * Инвентарь (раздел 35): сетка карточек предметов. Предметы, у которых есть examineAction
 * (см. GameViewModel.examineAction), можно осмотреть тапом — это обычное Action движка: может
 * содержать Check и Effects, в том числе Transition — если после осмотра сцена меняется,
 * экран автоматически возвращается назад, чтобы игрок увидел новую сцену.
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            TextButton(onClick = onBack) { Text("← Назад") }
            Text("Инвентарь", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            if (resources.isNotEmpty()) {
                Text("Ресурсы", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    resources.values.forEach { resource ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${resourceIcon(resource.id)} ${resource.name}: ${resource.currentValue}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            Text("Предметы", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Пока пусто",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        val examinable = examineActionFor(item) != null
                        ItemCard(
                            item = item,
                            examinable = examinable,
                            onClick = { if (examinable) examine(item) }
                        )
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
private fun ItemCard(item: Item, examinable: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable(enabled = examinable, onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🎒", fontSize = 40.sp)
                if (item.quantity > 1) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "×${item.quantity}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(item.name, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            if (item.description.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
            if (examinable) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Рассмотреть",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
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
