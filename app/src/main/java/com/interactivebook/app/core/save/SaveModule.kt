package com.interactivebook.app.core.save

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.interactivebook.app.core.model.GameState
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.gameDataStore by preferencesDataStore(name = "game_state")
private val GAME_STATE_KEY = stringPreferencesKey("game_state_json")

/**
 * ignoreUnknownKeys — старый save с полями, которых больше нет в модели, не ломает загрузку.
 * encodeDefaults — новое поле, добавленное в GameState в будущем и отсутствующее в старом save,
 * получает объявленное значение по умолчанию, а не падает с ошибкой (раздел 71 — обратная
 * совместимость: "добавление нового поля не должно ломать старые save").
 */
private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Единственный автоматический save (раздел 66) — без слотов, без ручного сохранения/загрузки.
 * Autosave вызывается из GameViewModel при каждом изменении GameState (раздел 67 — как минимум
 * при входе в новую Scene, но и любое другое изменение состояния безопасно сохранять сразу же).
 */
object SaveRepository {

    suspend fun save(context: Context, state: GameState) {
        context.gameDataStore.edit { prefs ->
            prefs[GAME_STATE_KEY] = json.encodeToString(state)
        }
    }

    suspend fun load(context: Context): GameState? {
        val prefs = context.gameDataStore.data.first()
        val raw = prefs[GAME_STATE_KEY] ?: return null
        return try {
            json.decodeFromString<GameState>(raw)
        } catch (e: Exception) {
            // Повреждённый или несовместимый save не должен приводить к краху приложения —
            // в этом случае просто считаем, что сохранения нет.
            null
        }
    }

    suspend fun clear(context: Context) {
        context.gameDataStore.edit { it.remove(GAME_STATE_KEY) }
    }
}
