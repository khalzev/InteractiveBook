package com.interactivebook.app.core.effects

import com.interactivebook.app.core.model.InformationType

/**
 * Effect изменяет GameState (раздел 32). Transition — тоже Effect (раздел 33), отдельного
 * "смени текущую сцену" не требуется — переход сам меняет currentSceneId/currentLocationId.
 */
sealed interface Effect {
    data class Transition(val sceneId: String, val locationId: String? = null) : Effect
    data class AddItem(
        val itemId: String,
        val name: String,
        val description: String = "",
        val quantity: Int = 1,
        val image: String? = null
    ) : Effect
    data class RemoveItem(val itemId: String, val quantity: Int = 1) : Effect
    data class AddInformation(
        val informationId: String,
        val title: String,
        val description: String = "",
        val topic: String = "",
        val type: InformationType = InformationType.NOTE,
        // Если запись с этим id уже есть в журнале — обе записи (эта и та) помечаются
        // связанными друг с другом (см. JournalScreen).
        val linkIfPresent: String? = null
    ) : Effect
    data class SetFlag(val flagId: String, val value: Boolean = true) : Effect
    data class ChangeResource(
        val resourceId: String,
        val delta: Int,
        val name: String = resourceId,
        // Необязательные границы — если заданы, значение зажимается в них (например, чтобы
        // растущий счётчик попыток не улетал за пределы поддерживаемых уровней сложности).
        val min: Int? = null,
        val max: Int? = null
    ) : Effect
    data class ChangeRelationship(val npcId: String, val delta: Int) : Effect
    data class SetCheckpoint(val sceneId: String) : Effect
    /** Устанавливает АБСОЛЮТНОЕ значение ресурса (в отличие от ChangeResource — не прибавляет,
     * а именно выставляет). Нужно для старта новой главы с фиксированным количеством ресурса. */
    data class SetResource(
        val resourceId: String,
        val value: Int,
        val name: String = resourceId,
        val min: Int? = null,
        val max: Int? = null
    ) : Effect
}
