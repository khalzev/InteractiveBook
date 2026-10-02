package com.interactivebook.app.ui.common

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.interactivebook.app.content.scenes.DemoContent
import com.interactivebook.app.core.checks.CheckOutcome
import com.interactivebook.app.core.checks.CheckResolution
import com.interactivebook.app.core.checks.CheckResolver
import com.interactivebook.app.core.conditions.ConditionEvaluator
import com.interactivebook.app.core.effects.Effect
import com.interactivebook.app.core.engine.GameEngine
import com.interactivebook.app.core.model.Action
import com.interactivebook.app.core.model.Character
import com.interactivebook.app.core.model.GameState
import com.interactivebook.app.core.model.Item
import com.interactivebook.app.core.model.Scene
import com.interactivebook.app.core.save.SaveRepository
import com.interactivebook.app.core.settings.RevealSpeed
import com.interactivebook.app.core.settings.SettingsRepository
import kotlinx.coroutines.launch

/**
 * GameState хранится в памяти (mutableStateOf) и синхронно отражается в UI, а также
 * автоматически сохраняется на диск через SaveRepository — единственный автосейв (раздел 66),
 * без слотов и ручного сохранения. ViewModel не содержит игровых правил: Conditions/Check/Effects
 * выполняются через ConditionEvaluator / CheckResolver / GameEngine.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = GameEngine()

    var gameState by mutableStateOf<GameState?>(null)
        private set

    /** Настройка: скорость появления сцены (INSTANT/FAST/SLOW). По умолчанию SLOW. */
    var revealSpeed by mutableStateOf(RevealSpeed.DEFAULT)
        private set

    fun updateRevealSpeed(speed: RevealSpeed) {
        revealSpeed = speed
        viewModelScope.launch {
            SettingsRepository.saveRevealSpeed(getApplication(), speed)
        }
    }

    // Сцена, которая уже была показана полностью. Нужна, чтобы при возврате из инвентаря/журнала/
    // настроек сцена не проигрывала анимацию появления заново. Хранится только в памяти.
    private var revealedSceneId: String? = null

    fun isSceneRevealed(sceneId: String): Boolean = revealedSceneId == sceneId

    fun markSceneRevealed(sceneId: String) {
        revealedSceneId = sceneId
    }

    init {
        // Пока сохранение/настройки не загружены с диска, значения по умолчанию временно
        // видны в UI — это нормально, загрузка почти мгновенная, экран перерисуется сам.
        viewModelScope.launch {
            gameState = SaveRepository.load(getApplication())
        }
        viewModelScope.launch {
            revealSpeed = SettingsRepository.loadRevealSpeed(getApplication())
        }
    }

    /** Меняет состояние и сразу же сохраняет его — автосейв при каждом изменении (раздел 67). */
    private fun updateState(newState: GameState) {
        gameState = newState
        viewModelScope.launch {
            SaveRepository.save(getApplication(), newState)
        }
    }

    /**
     * New Game (раздел 69): полностью пересоздаёт GameState и сразу стартует в пролог —
     * без персонажа (он появится позже, когда сюжет дойдёт до момента выбора класса —
     * см. requiresCharacterCreation в Action).
     */
    fun startNewGame() {
        revealedSceneId = null
        val newState = GameState.newGame(
            startingSceneId = DemoContent.startingScene.id,
            startingLocationId = DemoContent.startingScene.locationId
        )
        updateState(newState)
    }

    /**
     * Завершение экрана создания персонажа: заменяет Character в GameState (остальной прогресс —
     * инвентарь/журнал/флаги/ресурсы — не затрагивается) и сразу применяет effects действия,
     * которое открыло создание персонажа (обычно — Transition к следующей сцене).
     * Используется и для первого выбора класса после пролога, и для смены героя в начале главы.
     */
    fun setCharacterAndApplyEffects(character: Character, effects: List<Effect>) {
        val state = gameState ?: return
        updateState(engine.applyEffects(state.copy(character = character), effects))
    }

    fun hasSavedGame(): Boolean = gameState != null

    fun currentScene(): Scene? {
        val state = gameState ?: return null
        return DemoContent.scenes.firstOrNull { it.id == state.currentSceneId }
    }

    /** Текст сцены с учётом textVariants (раздел 73 — текст, зависящий от GameState). */
    fun resolveSceneText(scene: Scene): String {
        val state = gameState ?: return scene.text
        return scene.textVariants.firstOrNull { ConditionEvaluator.isMet(it.condition, state) }?.text
            ?: scene.text
    }

    /** Действие осмотра предмета (если для него есть examine-контент — см. DemoContent). */
    fun examineAction(item: Item): Action? = DemoContent.itemExamineActions[item.id]

    /** Действия, доступные при текущем GameState (раздел 29 — Conditions решают доступность). */
    fun visibleActions(scene: Scene): List<Action> {
        val state = gameState ?: return emptyList()
        return scene.actions.filter { ConditionEvaluator.isMet(it.condition, state) }
    }

    /**
     * ID действий, которые появились ПОСЛЕ прошлого визита в эту сцену — то есть условие
     * стало выполняться из-за изменения GameState (флаг, информация, ресурс и т.п.) уже после
     * того, как игрок впервые видел эту сцену.
     *
     * ВАЖНО: при первом визите в сцену ничего не считается "новым" — подсвечивать нечего,
     * это и есть первый набор действий. seenActionsByScene не содержит ключа scene.id ровно
     * до первого вызова markActionsSeen, поэтому наличие ключа — это и есть признак "был здесь
     * раньше".
     */
    fun newlyAvailableActionIds(scene: Scene): Set<String> {
        val state = gameState ?: return emptySet()
        val visitedBefore = state.seenActionsByScene.containsKey(scene.id)
        if (!visitedBefore) return emptySet()
        val visibleIds = visibleActions(scene).map { it.id }.toSet()
        val alreadySeen = state.seenActionsByScene[scene.id] ?: emptySet()
        return visibleIds - alreadySeen
    }

    /**
     * Запоминает текущий набор видимых действий сцены как "уже виденный" — вызывается при
     * каждом входе в сцену, ПОСЛЕ того, как newlyAvailableActionIds уже сохранил список для
     * подсветки в этом визите (иначе подсветка гасла бы мгновенно).
     */
    fun markActionsSeen(scene: Scene) {
        val state = gameState ?: return
        val visibleIds = visibleActions(scene).map { it.id }.toSet()
        val alreadySeen = state.seenActionsByScene[scene.id]
        if (alreadySeen == null || !alreadySeen.containsAll(visibleIds)) {
            val updated = state.seenActionsByScene + (scene.id to ((alreadySeen ?: emptySet()) + visibleIds))
            updateState(state.copy(seenActionsByScene = updated))
        }
    }

    /** Для действий без Check — effects применяются сразу. */
    fun performAction(action: Action) {
        if (action.check != null) return
        val state = gameState ?: return
        updateState(engine.applyEffects(state, action.effects))
    }

    /**
     * Вычисляет результат проверки (не меняя GameState) — анимация в UI отделена от логики
     * (раздел 26): сначала считаем итог здесь, затем UI его просто визуализирует.
     */
    fun resolveCheck(action: Action): CheckResolution? {
        val state = gameState ?: return null
        val check = action.check ?: return null
        return CheckResolver.resolve(action.text, check, state.character)
    }

    /** Применяет Effects нужной ветки результата после того, как UI показал исход. */
    fun applyCheckOutcome(action: Action, outcome: CheckOutcome) {
        val state = gameState ?: return
        updateState(engine.applyCheckOutcome(action, outcome, state))
    }

    /** Effects, которые применятся для данного исхода — не меняя состояние. Нужно UI (инвентарь),
     * чтобы понять заранее, произойдёт ли переход сцены после подтверждения результата. */
    fun outcomeEffects(action: Action, outcome: CheckOutcome): List<Effect> =
        engine.effectsForOutcome(action, outcome)
}
