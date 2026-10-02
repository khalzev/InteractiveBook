package com.interactivebook.app.core.model

import kotlinx.serialization.Serializable

/**
 * GameState — единое глобальное состояние игрока и мира (раздел 8).
 *
 * Важно (раздел 71, "обратная совместимость save"): все поля имеют значения по умолчанию.
 * При добавлении нового поля в будущем старые сохранения (в которых это поле отсутствует
 * в JSON) продолжат десериализоваться благодаря kotlinx.serialization + default values,
 * не ломая уже существующие save-файлы.
 */
@Serializable
data class GameState(
    val character: Character = Character(),

    val currentSceneId: String = "",
    val currentLocationId: String = "",

    val inventory: List<Item> = emptyList(),
    val information: List<Information> = emptyList(),
    val flags: Map<String, Boolean> = emptyMap(),
    val resources: Map<String, GameResource> = emptyMap(),
    val relationships: Map<String, Relationship> = emptyMap(),

    val checkpointSceneId: String? = null,

    val storyDate: String? = null,
    val storyTime: String? = null,

    // ID действий, которые игрок уже видел в каждой сцене (ключ — Scene.id). Если при повторном
    // визите в сцену появилось действие, которого здесь ещё нет — оно считается новым и
    // подсвечивается в UI (см. GameViewModel.newlyAvailableActionIds).
    val seenActionsByScene: Map<String, Set<String>> = emptyMap(),

    // ID действий, которые игрок уже РЕАЛЬНО НАЖИМАЛ в каждой сцене (ключ — Scene.id).
    // Используется, чтобы визуально помечать пройденные действия (галочка + приглушение),
    // чтобы игрок не ходил по кругу одних и тех же реплик. Отличается от seenActionsByScene:
    // там — то, что игрок видел; здесь — то, что он выбрал.
    val usedActionsByScene: Map<String, Set<String>> = emptyMap()
) {
    companion object {
        /** Пустое состояние для новой игры (раздел 69 — New Game сбрасывает GameState). */
        fun newGame(startingSceneId: String, startingLocationId: String): GameState =
            GameState(
                currentSceneId = startingSceneId,
                currentLocationId = startingLocationId,
                checkpointSceneId = startingSceneId
            )
    }
}