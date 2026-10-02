package com.interactivebook.app.core.model

import kotlinx.serialization.Serializable

/**
 * Пол персонажа (раздел 10). На данном этапе не изменяет характеристики автоматически,
 * влияет на текстовые обращения и набор навыков по архетипу.
 */
@Serializable
enum class Gender {
    MALE,
    FEMALE
}

/**
 * Фиксированный набор архетипов (раздел 13). Реализация подбора характеристик/навыков
 * по архетипу появится начиная с этапа Character creation (Этап 7).
 */
@Serializable
enum class Archetype {
    VETERAN,        // Ветеран
    DETECTIVE,      // Сыщик
    HISTORIAN,      // Историк (ранее Картограф)
    INNKEEPER,      // Трактирщик
    KNIGHT,         // Рыцарь
    PHYSICIAN,      // Врач
    ADVENTURER      // Приключенец — сам выбирает 2 навыка
}

@Serializable
data class Character(
    val firstName: String = "",
    val lastName: String = "",
    val gender: Gender = Gender.MALE,
    val archetype: Archetype = Archetype.ADVENTURER,
    val stats: Stats = Stats(),
    val skills: Set<SkillType> = emptySet()
) {
    val fullName: String get() = "$firstName $lastName".trim()
}
