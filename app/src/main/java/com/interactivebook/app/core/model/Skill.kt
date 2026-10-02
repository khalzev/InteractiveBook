package com.interactivebook.app.core.model

import kotlinx.serialization.Serializable

/**
 * Пять навыков персонажа (раздел 14 паспорта).
 * Персонаж просто ВЛАДЕЕТ навыком (Set<SkillType>) — конкретный числовой бонус зависит
 * от того, с какой характеристикой навык используется в конкретной проверке
 * (например, Атлетика: +3 к проверкам на Силе, +2 на Ловкости — раздел 14).
 * Расчёт бонуса — в core/checks/SkillBonusCalculator.
 */
@Serializable
enum class SkillType {
    ATHLETICS,       // Атлетика
    KNOWLEDGE,       // Знание
    ELOQUENCE,       // Красноречие
    SURVIVAL,        // Выживание — универсальный +1 к остальным навыкам (не самостоятельный бонус)
    OBSERVATION      // Наблюдательность
}
