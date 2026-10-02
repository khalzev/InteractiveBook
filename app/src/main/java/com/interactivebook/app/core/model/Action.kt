package com.interactivebook.app.core.model

import com.interactivebook.app.core.checks.Check
import com.interactivebook.app.core.conditions.Condition
import com.interactivebook.app.core.effects.Effect

/**
 * Плашка подтверждения перед выполнением действия (например "Пропустить пролог?") —
 * показывается до того, как применяются effects/check/requiresCharacterCreation.
 * При отмене действие просто не выполняется, GameState не меняется.
 */
data class ActionConfirmation(
    val title: String,
    val message: String,
    val confirmLabel: String = "Подтвердить",
    val cancelLabel: String = "Отмена"
)

/**
 * Action — универсальное действие игрока (раздел 27-28).
 *
 * Если check == null — действие простое: effects применяются сразу при выборе.
 * Если check != null — используются ветки по результату (раздел 20): onCriticalSuccess/
 * onSuccess/onFailure/onCriticalFailure. Если onCriticalSuccess не задан, используется
 * onSuccess; если onCriticalFailure не задан — onFailure (раздел 20 допускает такое
 * авторское упрощение, если критический исход не должен ничем отличаться от обычного).
 *
 * requiresCharacterCreation — авторский маркер: выбор этого действия открывает экран
 * создания персонажа (Class → Gender → [Skills] → Name) вместо немедленного применения
 * effects. effects при этом применяются ПОСЛЕ завершения создания персонажа (обычно —
 * просто Transition к следующей сцене). Используется как для самого первого выбора класса
 * (в конце пролога), так и для повторного выбора в начале новой главы — движок не различает
 * эти два случая, это просто авторски размеченное действие в нужном месте истории.
 *
 * confirmation — если задано, перед выполнением действия (любого: простого, с Check или с
 * requiresCharacterCreation) сначала показывается плашка подтверждения.
 *
 * disabledReason — если задано, действие проходит Condition (то есть видно игроку), но
 * нажать на него нельзя; рядом показывается эта подпись (например "нужно подумать, что
 * сделать ещё"). Отличается от отсутствия в списке действий: игрок видит, что вариант
 * существует, но пока недоступен, а не то, что он его никогда не увидит.
 */
data class Action(
    val id: String,
    val text: String,
    val condition: Condition? = null,
    val check: Check? = null,
    val effects: List<Effect> = emptyList(),
    val onCriticalSuccess: List<Effect>? = null,
    val onSuccess: List<Effect>? = null,
    val onFailure: List<Effect>? = null,
    val onCriticalFailure: List<Effect>? = null,
    val requiresCharacterCreation: Boolean = false,
    val confirmation: ActionConfirmation? = null,
    val disabledReason: String? = null
)
