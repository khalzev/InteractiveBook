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

    /** Настройка: перелистывание страниц. По умолчанию — включено. */
    var pageTurnEnabled by mutableStateOf(true)
        private set

    fun updateRevealSpeed(speed: RevealSpeed) {
        revealSpeed = speed
        viewModelScope.launch {
            SettingsRepository.saveRevealSpeed(getApplication(), speed)
        }
    }

    fun updatePageTurnEnabled(enabled: Boolean) {
        pageTurnEnabled = enabled
        viewModelScope.launch {
            SettingsRepository.savePageTurnEnabled(getApplication(), enabled)
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
        viewModelScope.launch {
            gameState = SaveRepository.load(getApplication())
        }
        viewModelScope.launch {
            revealSpeed = SettingsRepository.loadRevealSpeed(getApplication())
        }
        viewModelScope.launch {
            pageTurnEnabled = SettingsRepository.loadPageTurnEnabled(getApplication())
        }
    }

    /** Меняет состояние и сразу же сохраняет его — автосейв при каждом изменении (раздел 67). */
    private fun updateState(newState: GameState) {
        gameState = newState
        viewModelScope.launch {
            SaveRepository.save(getApplication(), newState)
        }
    }

    fun startNewGame() {
        revealedSceneId = null
        val newState = GameState.newGame(
            startingSceneId = DemoContent.startingScene.id,
            startingLocationId = DemoContent.startingScene.locationId
        )
        updateState(newState)
    }

    fun setCharacterAndApplyEffects(character: Character, effects: List<Effect>) {
        val state = gameState ?: return
        updateState(engine.applyEffects(state.copy(character = character), effects))
    }

    fun hasSavedGame(): Boolean = gameState != null

    fun currentScene(): Scene? {
        val state = gameState ?: return null
        return DemoContent.scenes.firstOrNull { it.id == state.currentSceneId }
    }

    /** Текст сцены с учётом textVariants. */
    fun resolveSceneText(scene: Scene): String {
        val state = gameState ?: return scene.text
        return scene.textVariants.firstOrNull { ConditionEvaluator.isMet(it.condition, state) }?.text
            ?: scene.text
    }

    /** Действие осмотра предмета (если для него есть examine-контент — см. DemoContent). */
    fun examineAction(item: Item): Action? = DemoContent.itemExamineActions[item.id]

    /**
     * Действия, доступные при текущем GameState.
     * Порядок: 1) новые — наверху; 2) непройденные; 3) пройденные — внизу.
     * Стабильная сортировка — авторский порядок внутри группы сохраняется.
     */
    fun visibleActions(scene: Scene): List<Action> {
        val state = gameState ?: return emptyList()
        val visible = scene.actions.filter { ConditionEvaluator.isMet(it.condition, state) }
        val newIds = newlyAvailableActionIds(scene)
        val usedIds = state.usedActionsByScene[scene.id] ?: emptySet()
        return visible.sortedBy { action ->
            when {
                action.id in newIds -> 0
                action.id in usedIds -> 2
                else -> 1
            }
        }
    }

    /**
     * ID действий, которые появились ПОСЛЕ прошлого визита в эту сцену.
     */
    fun newlyAvailableActionIds(scene: Scene): Set<String> {
        val state = gameState ?: return emptySet()
        val visitedBefore = state.seenActionsByScene.containsKey(scene.id)
        if (!visitedBefore) return emptySet()
        val visibleIds = scene.actions
            .filter { ConditionEvaluator.isMet(it.condition, state) }
            .map { it.id }
            .toSet()
        val alreadySeen = state.seenActionsByScene[scene.id] ?: emptySet()
        return visibleIds - alreadySeen
    }

    /** ID действий, которые игрок уже реально нажимал в этой сцене. */
    fun usedActionIds(scene: Scene): Set<String> {
        val state = gameState ?: return emptySet()
        return state.usedActionsByScene[scene.id] ?: emptySet()
    }

    /**
     * Автоподсказка стоимости — для действий с отрицательным ChangeResource.
     */
    fun costHintFor(action: Action): String? {
        if (action.hideCostHint) return null
        val losses = action.effects.filterIsInstance<Effect.ChangeResource>()
            .filter { it.delta < 0 }
        if (losses.isEmpty()) return null
        return losses.joinToString(", ") { effect ->
            val label = effect.name.ifBlank { effect.resourceId }
            "(–${-effect.delta} $label)"
        }
    }

    fun markActionsSeen(scene: Scene) {
        val state = gameState ?: return
        val visibleIds = scene.actions
            .filter { ConditionEvaluator.isMet(it.condition, state) }
            .map { it.id }
            .toSet()
        val alreadySeen = state.seenActionsByScene[scene.id]
        if (alreadySeen == null || !alreadySeen.containsAll(visibleIds)) {
            val updated = state.seenActionsByScene + (scene.id to ((alreadySeen ?: emptySet()) + visibleIds))
            updateState(state.copy(seenActionsByScene = updated))
        }
    }

    private fun markActionUsed(state: GameState, sceneId: String, actionId: String): GameState {
        val current = state.usedActionsByScene[sceneId] ?: emptySet()
        if (actionId in current) return state
        return state.copy(
            usedActionsByScene = state.usedActionsByScene + (sceneId to (current + actionId))
        )
    }

    /** Для действий без Check — effects применяются сразу. */
    fun performAction(action: Action) {
        if (action.check != null) return
        val state = gameState ?: return
        val sceneId = state.currentSceneId
        val withUsed = markActionUsed(state, sceneId, action.id)
        updateState(engine.applyEffects(withUsed, action.effects))
    }

    fun resolveCheck(action: Action): CheckResolution? {
        val state = gameState ?: return null
        val check = action.check ?: return null
        return CheckResolver.resolve(action.text, check, state.character)
    }

    fun applyCheckOutcome(action: Action, outcome: CheckOutcome) {
        val state = gameState ?: return
        val sceneId = state.currentSceneId
        val withUsed = markActionUsed(state, sceneId, action.id)
        updateState(engine.applyCheckOutcome(action, outcome, withUsed))
    }

    fun outcomeEffects(action: Action, outcome: CheckOutcome): List<Effect> =
        engine.effectsForOutcome(action, outcome)
}