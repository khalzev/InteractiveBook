package com.interactivebook.app.core.effects

import com.interactivebook.app.core.model.GameResource
import com.interactivebook.app.core.model.GameState
import com.interactivebook.app.core.model.Information
import com.interactivebook.app.core.model.Item
import com.interactivebook.app.core.model.Relationship

/**
 * Применяет Effects к GameState последовательно и предсказуемо (раздел 34).
 */
object EffectEngine {

    fun apply(state: GameState, effects: List<Effect>): GameState {
        var current = state
        for (effect in effects) {
            current = applyOne(current, effect)
        }
        return current
    }

    private fun applyOne(state: GameState, effect: Effect): GameState = when (effect) {

        is Effect.Transition -> state.copy(
            currentSceneId = effect.sceneId,
            currentLocationId = effect.locationId ?: state.currentLocationId
        )

        is Effect.AddItem -> {
            val existing = state.inventory.firstOrNull { it.id == effect.itemId }
            val newInventory = if (existing != null) {
                // Одинаковые предметы стакаются одной записью (раздел 36).
                state.inventory.map {
                    if (it.id == effect.itemId) it.copy(quantity = it.quantity + effect.quantity) else it
                }
            } else {
                state.inventory + Item(
                    id = effect.itemId,
                    name = effect.name,
                    description = effect.description,
                    quantity = effect.quantity,
                    image = effect.image
                )
            }
            state.copy(inventory = newInventory)
        }

        is Effect.RemoveItem -> {
            val newInventory = state.inventory.mapNotNull { item ->
                if (item.id == effect.itemId) {
                    val remaining = item.quantity - effect.quantity
                    if (remaining > 0) item.copy(quantity = remaining) else null
                } else item
            }
            state.copy(inventory = newInventory)
        }

        is Effect.AddInformation -> {
            // Информация не должна дублироваться (раздел 47).
            if (state.information.any { it.id == effect.informationId }) {
                state
            } else {
                val newEntry = Information(
                    id = effect.informationId,
                    title = effect.title,
                    description = effect.description,
                    topic = effect.topic,
                    type = effect.type
                )
                val linkTargetId = effect.linkIfPresent
                if (linkTargetId != null && state.information.any { it.id == linkTargetId }) {
                    // Связываем новую запись с уже существующей — обе получают ссылку друг на друга.
                    val linkedNew = newEntry.copy(linkedTo = newEntry.linkedTo + linkTargetId)
                    val updatedExisting = state.information.map {
                        if (it.id == linkTargetId) it.copy(linkedTo = it.linkedTo + newEntry.id) else it
                    }
                    state.copy(information = updatedExisting + linkedNew)
                } else {
                    state.copy(information = state.information + newEntry)
                }
            }
        }

        is Effect.SetFlag -> state.copy(flags = state.flags + (effect.flagId to effect.value))

        is Effect.ChangeResource -> {
            val existing = state.resources[effect.resourceId]
            val min = effect.min ?: existing?.min
            val max = effect.max ?: existing?.max
            var newValue = (existing?.currentValue ?: 0) + effect.delta
            if (min != null) newValue = newValue.coerceAtLeast(min)
            if (max != null) newValue = newValue.coerceAtMost(max)
            val updated = (existing ?: GameResource(id = effect.resourceId, name = effect.name, currentValue = 0))
                .copy(currentValue = newValue, min = min, max = max)
            state.copy(resources = state.resources + (effect.resourceId to updated))
        }

        is Effect.ChangeRelationship -> {
            val existing = state.relationships[effect.npcId]
            // Диапазон -10..+10 (раздел 56).
            val newValue = ((existing?.value ?: 0) + effect.delta).coerceIn(-10, 10)
            state.copy(
                relationships = state.relationships + (effect.npcId to Relationship(effect.npcId, newValue))
            )
        }

        is Effect.SetCheckpoint -> state.copy(checkpointSceneId = effect.sceneId)

        is Effect.SetResource -> {
            var value = effect.value
            if (effect.min != null) value = value.coerceAtLeast(effect.min)
            if (effect.max != null) value = value.coerceAtMost(effect.max)
            val updated = GameResource(
                id = effect.resourceId,
                name = effect.name,
                currentValue = value,
                min = effect.min,
                max = effect.max
            )
            state.copy(resources = state.resources + (effect.resourceId to updated))
        }
    }
}
