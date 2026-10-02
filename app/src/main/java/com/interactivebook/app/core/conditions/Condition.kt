package com.interactivebook.app.core.conditions

import com.interactivebook.app.core.model.Archetype

/**
 * Condition отвечает только за доступность контента (раздел 29), не за успех проверки.
 * Поддерживаются AND/OR (раздел 30) и базовые категории (раздел 31): предмет, информация, флаг,
 * ресурс, архетип персонажа. Остальные категории (характеристика, навык, отношение, текущая
 * сцена) будут добавлены по мере надобности конкретному контенту.
 *
 * Сериализация для загрузки из JSON (Этап 12) будет добавлена отдельно через полиморфные
 * сериализаторы; пока контент авторизуется прямо в Kotlin.
 */
sealed interface Condition {
    data class HasFlag(val flagId: String, val value: Boolean = true) : Condition
    data class HasItem(val itemId: String) : Condition
    data class HasInformation(val informationId: String) : Condition
    data class ResourceCompare(val resourceId: String, val comparison: Comparison, val value: Int) : Condition
    data class HasArchetype(val archetype: Archetype) : Condition
    data class And(val conditions: List<Condition>) : Condition
    data class Or(val conditions: List<Condition>) : Condition

    enum class Comparison { AT_LEAST, AT_MOST }
}
