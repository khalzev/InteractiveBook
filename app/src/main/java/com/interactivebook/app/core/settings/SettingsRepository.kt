package com.interactivebook.app.core.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.settingsDataStore by preferencesDataStore(name = "settings")
private val REVEAL_SPEED_KEY = stringPreferencesKey("reveal_speed")
private val PAGE_TURN_ENABLED_KEY = stringPreferencesKey("page_turn_enabled")

object SettingsRepository {

    // ---------- Скорость появления сцены ----------

    suspend fun loadRevealSpeed(context: Context): RevealSpeed {
        val raw = context.settingsDataStore.data.first()[REVEAL_SPEED_KEY] ?: return RevealSpeed.DEFAULT
        return try {
            RevealSpeed.valueOf(raw)
        } catch (e: IllegalArgumentException) {
            RevealSpeed.DEFAULT
        }
    }

    suspend fun saveRevealSpeed(context: Context, speed: RevealSpeed) {
        context.settingsDataStore.edit { it[REVEAL_SPEED_KEY] = speed.name }
    }

    // ---------- Перелистывание страниц ----------

    /** true — перелистывание включено (по умолчанию). */
    suspend fun loadPageTurnEnabled(context: Context): Boolean {
        val raw = context.settingsDataStore.data.first()[PAGE_TURN_ENABLED_KEY] ?: return true
        return raw.toBooleanStrictOrNull() ?: true
    }

    suspend fun savePageTurnEnabled(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { it[PAGE_TURN_ENABLED_KEY] = enabled.toString() }
    }
}