package com.interactivebook.app.core.checks

import com.interactivebook.app.core.model.SkillType
import com.interactivebook.app.core.model.StatType

/**
 * Числовой бонус навыка зависит от характеристики, с которой он используется в проверке
 * (раздел 14). Атлетика даёт разный бонус в зависимости от Силы/Ловкости; остальные навыки
 * дают фиксированный бонус (+3) независимо от характеристики (раздел 14: "Основа — Интеллект" и т.п.,
 * без дифференциации по величине).
 */
object SkillBonusCalculator {

    fun bonusFor(skill: SkillType, stat: StatType): Int = when (skill) {
        SkillType.ATHLETICS -> when (stat) {
            StatType.STRENGTH -> 3
            StatType.AGILITY -> 2
            StatType.INTELLECT -> 0
        }
        SkillType.KNOWLEDGE -> 3
        SkillType.ELOQUENCE -> 3
        // Выживание больше не привязано к "проверкам в природе" (их не будет в истории).
        // Теперь это универсальный +1 к любой проверке, использующей ДРУГОЙ навык
        // (Атлетика/Знание/Красноречие/Наблюдательность) — см. CheckResolver.SURVIVAL_UNIVERSAL_BONUS.
        SkillType.SURVIVAL -> 0
        SkillType.OBSERVATION -> 3
    }
}
