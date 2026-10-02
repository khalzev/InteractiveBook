package com.interactivebook.app.core.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

// Настройки хранятся отдельно от сохранения игры: новая игра их не сбрасывает.
private val Context.settingsDataStore by preferencesDataStore(name = "settings")
private val REVEAL_SPEED_KEY = stringPreferencesKey("reveal_speed")

object SettingsRepository {

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
}
