package com.interactivebook.app.core.model

import com.interactivebook.app.core.checks.Check
import com.interactivebook.app.core.checks.CheckOutcome
import com.interactivebook.app.core.conditions.Condition
import com.interactivebook.app.core.effects.Effect

data class ActionConfirmation(
    val title: String,
    val message: String,
    val confirmLabel: String,
    val cancelLabel: String
)

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
    val consequences: Map<CheckOutcome, String>? = null,
    val requiresCharacterCreation: Boolean = false,
    val confirmation: ActionConfirmation? = null,
    val disabledReason: String? = null,
    val hideCostHint: Boolean = false,
    val isNavigation: Boolean = false
)