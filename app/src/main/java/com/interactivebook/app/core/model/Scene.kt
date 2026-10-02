package com.interactivebook.app.core.model

import com.interactivebook.app.core.conditions.Condition

/**
 * Текстовый вариант сцены, зависящий от GameState (раздел 73 — "одна Scene может менять
 * текст в зависимости от GameState"). Например: текст сцены зависит от того, какой архетип
 * выбрал игрок. Проверяются по порядку, побеждает первый подошедший; если ни один не подошёл —
 * используется базовый Scene.text.
 */
data class TextVariant(val condition: Condition, val text: String)

/**
 * Заголовок части/акта истории (например "Часть I «Город у реки»" / "Пролог") — показывается
 * отдельным экраном перед сценой, если у неё задан partTitle (см. ui/common/PartTitleScreen).
 */
data class PartTitle(
    val partLabel: String,
    val title: String,
    val chapterLabel: String
)

/**
 * Scene — "страница" интерактивной книги (раздел 7).
 * Не @Serializable: сцены — это контент, определённый в коде (DemoContent), а не часть
 * сохраняемого состояния GameState. Загрузка сцен из JSON (Этап 12) добавит сериализацию
 * отдельно, через content_loader.
 */
data class Scene(
    val id: String,
    val locationId: String,
    val image: String? = null,
    val text: String,
    val textVariants: List<TextVariant> = emptyList(),
    val actions: List<Action> = emptyList(),
    val partTitle: PartTitle? = null
)
