package com.interactivebook.app.core.checks

import com.interactivebook.app.core.model.SkillType
import com.interactivebook.app.core.model.StatType

/**
 * Описание проверки, заданное автором контента (раздел 18: stat/skill/DC задаются явно).
 */
data class Check(
    val stat: StatType,
    val skill: SkillType? = null,
    val difficulty: Int
)

/** Четыре результата проверки (раздел 20), в фиксированном порядке. */
enum class CheckOutcome {
    CRITICAL_SUCCESS,
    SUCCESS,
    FAILURE,
    CRITICAL_FAILURE
}

/** Один модификатор для отображения в UI проверки (раздел 26). */
data class CheckModifierBreakdown(val label: String, val value: Int)

/**
 * Результат конкретного броска — то, что движок передаёт UI для визуализации (раздел 26):
 * название, DC, натуральный бросок, модификаторы по отдельности, итог, результат.
 */
data class CheckResolution(
    val actionTitle: String,
    val difficulty: Int,
    val rawRoll: Int,
    val modifiers: List<CheckModifierBreakdown>,
    val total: Int,
    val outcome: CheckOutcome
)
