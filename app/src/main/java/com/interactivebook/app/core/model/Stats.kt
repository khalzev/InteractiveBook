package com.interactivebook.app.core.model

import kotlinx.serialization.Serializable

/**
 * Три характеристики персонажа (раздел 11 паспорта).
 * Движок не решает сам, какая характеристика используется в проверке —
 * это всегда задаёт автор контента (раздел 18).
 */
@Serializable
enum class StatType {
    STRENGTH,   // Сила
    AGILITY,    // Ловкость
    INTELLECT   // Интеллект
}

/**
 * Уровень характеристики и его числовой модификатор (раздел 12).
 * HIGH = +2, MEDIUM = 0, LOW = -2
 */
@Serializable
enum class StatLevel(val modifier: Int) {
    HIGH(2),
    MEDIUM(0),
    LOW(-2)
}

@Serializable
data class Stats(
    val strength: StatLevel = StatLevel.MEDIUM,
    val agility: StatLevel = StatLevel.MEDIUM,
    val intellect: StatLevel = StatLevel.MEDIUM
) {
    fun modifierFor(type: StatType): Int = when (type) {
        StatType.STRENGTH -> strength.modifier
        StatType.AGILITY -> agility.modifier
        StatType.INTELLECT -> intellect.modifier
    }
}
