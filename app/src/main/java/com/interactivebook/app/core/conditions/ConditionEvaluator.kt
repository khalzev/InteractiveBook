package com.interactivebook.app.core.conditions

import com.interactivebook.app.core.model.GameState

/**
 * Вычисляет доступность контента по GameState (раздел 29-31).
 * Условие == null означает "всегда доступно".
 */
object ConditionEvaluator {

    fun isMet(condition: Condition?, state: GameState): Boolean {
        if (condition == null) return true
        return when (condition) {
            is Condition.HasFlag ->
                (state.flags[condition.flagId] ?: false) == condition.value

            is Condition.HasItem ->
                state.inventory.any { it.id == condition.itemId && it.quantity > 0 }

            is Condition.HasInformation ->
                state.information.any { it.id == condition.informationId }

            is Condition.ResourceCompare -> {
                val current = state.resources[condition.resourceId]?.currentValue ?: 0
                when (condition.comparison) {
                    Condition.Comparison.AT_LEAST -> current >= condition.value
                    Condition.Comparison.AT_MOST -> current <= condition.value
                }
            }

            is Condition.HasArchetype ->
                state.character.archetype == condition.archetype

            is Condition.And ->
                condition.conditions.all { isMet(it, state) }

            is Condition.Or ->
                condition.conditions.any { isMet(it, state) }
        }
    }
}
