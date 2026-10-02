package com.interactivebook.app.core.checks

import com.interactivebook.app.core.model.Character
import com.interactivebook.app.core.model.SkillType
import com.interactivebook.app.core.model.StatType
import kotlin.random.Random

/**
 * Бросок d20, применение модификаторов, определение результата (разделы 16-20).
 * Крит определяется ТОЛЬКО по натуральному броску, независимо от модификаторов (раздел 19).
 */
object CheckResolver {

    private const val SURVIVAL_UNIVERSAL_BONUS = 1

    fun resolve(actionTitle: String, check: Check, character: Character): CheckResolution {
        val rawRoll = Random.nextInt(1, 21) // 1..20 включительно

        val statModifier = character.stats.modifierFor(check.stat)
        val modifiers = mutableListOf(
            CheckModifierBreakdown(statLabel(check.stat), statModifier)
        )

        var skillBonus = 0

        check.skill?.let { skillType ->
            if (character.skills.contains(skillType)) {
                val bonus = SkillBonusCalculator.bonusFor(skillType, check.stat)
                modifiers.add(CheckModifierBreakdown(skillLabel(skillType), bonus))
                skillBonus += bonus
            }
        }

        // Выживание — универсальный +1 к любой проверке, где основным навыком выступает
        // ДРУГОЙ навык (Атлетика/Знание/Красноречие/Наблюдательность). Проверок "в природе"
        // не предполагается, поэтому Выживание никогда не выступает как check.skill само по себе.
        if (character.skills.contains(SkillType.SURVIVAL) &&
            check.skill != null &&
            check.skill != SkillType.SURVIVAL
        ) {
            modifiers.add(CheckModifierBreakdown(skillLabel(SkillType.SURVIVAL), SURVIVAL_UNIVERSAL_BONUS))
            skillBonus += SURVIVAL_UNIVERSAL_BONUS
        }

        val total = rawRoll + statModifier + skillBonus

        val outcome = when {
            rawRoll == 20 -> CheckOutcome.CRITICAL_SUCCESS
            rawRoll == 1 -> CheckOutcome.CRITICAL_FAILURE
            total >= check.difficulty -> CheckOutcome.SUCCESS
            else -> CheckOutcome.FAILURE
        }

        return CheckResolution(
            actionTitle = actionTitle,
            difficulty = check.difficulty,
            rawRoll = rawRoll,
            modifiers = modifiers,
            total = total,
            outcome = outcome
        )
    }

    private fun statLabel(stat: StatType): String = when (stat) {
        StatType.STRENGTH -> "Сила"
        StatType.AGILITY -> "Ловкость"
        StatType.INTELLECT -> "Интеллект"
    }

    private fun skillLabel(skill: SkillType): String = when (skill) {
        SkillType.ATHLETICS -> "Атлетика"
        SkillType.KNOWLEDGE -> "Знание"
        SkillType.ELOQUENCE -> "Красноречие"
        SkillType.SURVIVAL -> "Выживание"
        SkillType.OBSERVATION -> "Наблюдательность"
    }
}
