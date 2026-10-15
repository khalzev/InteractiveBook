package com.interactivebook.app.core.engine

import com.interactivebook.app.core.checks.CheckOutcome
import com.interactivebook.app.core.effects.Effect
import com.interactivebook.app.core.effects.EffectEngine
import com.interactivebook.app.core.model.Action
import com.interactivebook.app.core.model.GameState

/**
 * GameEngine — единая точка выполнения игровой логики (раздел 76).
 * UI не содержит игровых правил и обращается только сюда / к ViewModel, который делегирует
 * сюда же.
 */
class GameEngine {

    /** Применяет список Effects к состоянию (для действий без Check). */
    fun applyEffects(state: GameState, effects: List<Effect>): GameState =
        EffectEngine.apply(state, effects)

    /** Effects нужной ветки по результату проверки (раздел 20), без применения к состоянию. */
    fun effectsForOutcome(action: Action, outcome: CheckOutcome): List<Effect> = when (outcome) {
        CheckOutcome.CRITICAL_SUCCESS -> action.onCriticalSuccess ?: action.onSuccess ?: emptyList()
        CheckOutcome.SUCCESS -> action.onSuccess ?: emptyList()
        CheckOutcome.FAILURE -> action.onFailure ?: emptyList()
        CheckOutcome.CRITICAL_FAILURE -> action.onCriticalFailure ?: action.onFailure ?: emptyList()
    }

    /**
     * Применяет Effects нужной ветки по результату проверки (раздел 20).
     * Бросок и определение результата уже выполнены снаружи (core/checks/CheckResolver) —
     * движок здесь только применяет авторские последствия.
     */
    fun applyCheckOutcome(action: Action, outcome: CheckOutcome, state: GameState): GameState =
        EffectEngine.apply(state, effectsForOutcome(action, outcome))

    /**
     * Если у действия задан маппинг `consequences` — возвращает ID consequence-сцены
     * для конкретного исхода проверки. Иначе — null (работает старый механизм
     * onSuccess/onFailure).
     */
    fun consequenceSceneIdFor(action: Action, outcome: CheckOutcome): String? =
        action.consequences?.get(outcome)
}
