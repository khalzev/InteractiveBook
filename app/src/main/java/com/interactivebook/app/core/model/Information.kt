package com.interactivebook.app.core.model

import kotlinx.serialization.Serializable

/** Тип записи журнала: заметка-рассуждение или цель, указывающая куда двигаться игроку. */
@Serializable
enum class InformationType { NOTE, GOAL }

/**
 * Запись "Журнала героя". linkedTo — id других записей, с которыми эта запись оказалась
 * связана (см. Effect.AddInformation.linkIfPresent) — например, два независимых наблюдения,
 * которые впоследствии складываются в одну картину.
 */
@Serializable
data class Information(
    val id: String,
    val title: String,
    val description: String = "",
    val topic: String = "",
    val type: InformationType = InformationType.NOTE,
    val image: String? = null,
    val linkedTo: Set<String> = emptySet()
)
