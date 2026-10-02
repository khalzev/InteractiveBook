package com.interactivebook.app.core.model

import kotlinx.serialization.Serializable

/**
 * Отношение героя к конкретному NPC, диапазон -10..+10 (раздел 56).
 * Категории для отображения игроку (раздел 58) будут реализованы в UI на этапе Relationships (Этап 10).
 */
@Serializable
data class Relationship(
    val npcId: String,
    val value: Int = 0
)
