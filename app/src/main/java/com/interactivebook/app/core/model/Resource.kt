package com.interactivebook.app.core.model

import kotlinx.serialization.Serializable

/**
 * Числовой ресурс, специфичный для главы (золото, время, доверие и т.п.).
 * Движок не интерпретирует границы автоматически (раздел 54) — это делает автор контента.
 */
@Serializable
data class GameResource(
    val id: String,
    val name: String,
    val currentValue: Int,
    val min: Int? = null,
    val max: Int? = null
)
