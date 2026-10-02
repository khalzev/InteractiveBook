package com.interactivebook.app.core.model

import kotlinx.serialization.Serializable

/**
 * Предмет инвентаря. Заготовка — полноценная логика инвентаря (стакающиеся предметы,
 * активные слоты, модификаторы) добавляется на Этапе 8.
 */
@Serializable
data class Item(
    val id: String,
    val name: String,
    val description: String = "",
    val quantity: Int = 1,
    val image: String? = null,
    val isEquipped: Boolean = false
)
