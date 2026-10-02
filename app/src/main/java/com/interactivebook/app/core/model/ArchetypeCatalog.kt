package com.interactivebook.app.core.model

/**
 * Фиксированный набор архетипов с их характеристиками (раздел 13) и навыками по полу
 * (раздел 15). Для Приключенца skillsByGender == null — игрок сам выбирает любые 2 навыка
 * (раздел 13: "Приключенец самостоятельно выбирает любые 2 навыка").
 */
data class ArchetypeInfo(
    val archetype: Archetype,
    val displayName: String,
    val stats: Stats,
    val skillsByGender: Map<Gender, Set<SkillType>>?
)

object ArchetypeCatalog {

    val all: List<ArchetypeInfo> = listOf(
        ArchetypeInfo(
            archetype = Archetype.VETERAN,
            displayName = "Ветеран",
            stats = Stats(strength = StatLevel.HIGH, agility = StatLevel.MEDIUM, intellect = StatLevel.LOW),
            skillsByGender = mapOf(
                Gender.MALE to setOf(SkillType.ATHLETICS, SkillType.SURVIVAL),
                Gender.FEMALE to setOf(SkillType.ATHLETICS, SkillType.ELOQUENCE)
            )
        ),
        ArchetypeInfo(
            archetype = Archetype.DETECTIVE,
            displayName = "Сыщик",
            stats = Stats(strength = StatLevel.LOW, agility = StatLevel.HIGH, intellect = StatLevel.MEDIUM),
            skillsByGender = mapOf(
                Gender.MALE to setOf(SkillType.ELOQUENCE, SkillType.OBSERVATION),
                Gender.FEMALE to setOf(SkillType.KNOWLEDGE, SkillType.OBSERVATION)
            )
        ),
        ArchetypeInfo(
            archetype = Archetype.HISTORIAN,
            displayName = "Историк",
            stats = Stats(strength = StatLevel.LOW, agility = StatLevel.MEDIUM, intellect = StatLevel.HIGH),
            skillsByGender = mapOf(
                Gender.MALE to setOf(SkillType.KNOWLEDGE, SkillType.SURVIVAL),
                Gender.FEMALE to setOf(SkillType.ELOQUENCE, SkillType.SURVIVAL)
            )
        ),
        ArchetypeInfo(
            archetype = Archetype.INNKEEPER,
            displayName = "Трактирщик",
            stats = Stats(strength = StatLevel.MEDIUM, agility = StatLevel.HIGH, intellect = StatLevel.LOW),
            skillsByGender = mapOf(
                Gender.MALE to setOf(SkillType.ELOQUENCE, SkillType.OBSERVATION),
                Gender.FEMALE to setOf(SkillType.KNOWLEDGE, SkillType.ELOQUENCE)
            )
        ),
        ArchetypeInfo(
            archetype = Archetype.KNIGHT,
            displayName = "Рыцарь",
            stats = Stats(strength = StatLevel.HIGH, agility = StatLevel.LOW, intellect = StatLevel.MEDIUM),
            skillsByGender = mapOf(
                Gender.MALE to setOf(SkillType.ATHLETICS, SkillType.ELOQUENCE),
                Gender.FEMALE to setOf(SkillType.ATHLETICS, SkillType.KNOWLEDGE)
            )
        ),
        ArchetypeInfo(
            archetype = Archetype.PHYSICIAN,
            displayName = "Врач",
            stats = Stats(strength = StatLevel.MEDIUM, agility = StatLevel.LOW, intellect = StatLevel.HIGH),
            skillsByGender = mapOf(
                Gender.MALE to setOf(SkillType.ATHLETICS, SkillType.OBSERVATION),
                Gender.FEMALE to setOf(SkillType.SURVIVAL, SkillType.OBSERVATION)
            )
        ),
        ArchetypeInfo(
            archetype = Archetype.ADVENTURER,
            displayName = "Приключенец",
            stats = Stats(strength = StatLevel.MEDIUM, agility = StatLevel.MEDIUM, intellect = StatLevel.MEDIUM),
            skillsByGender = null // игрок выбирает 2 любых навыка сам
        )
    )

    fun infoFor(archetype: Archetype): ArchetypeInfo =
        all.first { it.archetype == archetype }
}
