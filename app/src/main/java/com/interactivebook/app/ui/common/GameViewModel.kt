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

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = GameEngine()

    var gameState by mutableStateOf<GameState?>(null)
        private set

    var revealSpeed by mutableStateOf(RevealSpeed.DEFAULT)
        private set

    var pageTurnEnabled by mutableStateOf(true)
        private set

    var resourcesEverSeen by mutableStateOf<Set<String>>(emptySet())
        private set

    fun markResourceSeen(resourceId: String) {
        if (resourceId !in resourcesEverSeen) {
            resourcesEverSeen = resourcesEverSeen + resourceId
        }
    }

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

    private var revealedSceneId: String? = null
    private var pendingTransitionSceneId: String? = null
    private var pendingTransitionLocationId: String? = null

    fun hasPendingTransition(): Boolean = pendingTransitionSceneId != null

    fun performActionWithoutTransition(action: Action) {
        if (action.check != null) return
        val state = gameState ?: return

        val transition = action.effects.filterIsInstance<Effect.Transition>().firstOrNull()
        val effectsWithoutTransition = action.effects.filter { it !is Effect.Transition }

        val willAnimate = action.effects.any { effect ->
            when (effect) {
                is Effect.AddItem -> state.inventory.none { it.id == effect.itemId }
                is Effect.AddInformation -> state.information.none { it.id == effect.informationId }
                is Effect.ChangeResource -> effect.delta != 0
                else -> false
            }
        }

        val sceneId = state.currentSceneId
        val withUsed = if (action.isNavigation) state
        else markActionUsed(state, sceneId, action.id)

        if (transition != null && willAnimate) {
            pendingTransitionSceneId = transition.sceneId
            pendingTransitionLocationId = transition.locationId
            updateState(engine.applyEffects(withUsed, effectsWithoutTransition))
        } else {
            updateState(engine.applyEffects(withUsed, action.effects))
        }
    }

    fun applyPendingTransition() {
        val state = gameState ?: return
        val targetSceneId = pendingTransitionSceneId ?: return
        val targetLocationId = pendingTransitionLocationId
        pendingTransitionSceneId = null
        pendingTransitionLocationId = null

        updateState(
            state.copy(
                currentSceneId = targetSceneId,
                currentLocationId = targetLocationId ?: state.currentLocationId
            )
        )
    }

    // НОВОЕ: Принудительный переход (нужно при отмене создания персонажа)
    fun goToScene(sceneId: String, locationId: String? = null) {
        val state = gameState ?: return
        updateState(
            state.copy(
                currentSceneId = sceneId,
                currentLocationId = locationId ?: state.currentLocationId
            )
        )
    }

    fun isSceneRevealed(sceneId: String): Boolean = revealedSceneId == sceneId
    fun markSceneRevealed(sceneId: String) { revealedSceneId = sceneId }

    init {
        viewModelScope.launch { gameState = SaveRepository.load(getApplication()) }
        viewModelScope.launch { revealSpeed = SettingsRepository.loadRevealSpeed(getApplication()) }
        viewModelScope.launch { pageTurnEnabled = SettingsRepository.loadPageTurnEnabled(getApplication()) }
    }

    private fun updateState(newState: GameState) {
        gameState = newState
        viewModelScope.launch { SaveRepository.save(getApplication(), newState) }
    }

    fun startNewGame() {
        revealedSceneId = null
        resourcesEverSeen = emptySet()
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

    fun resolveSceneText(scene: Scene): String {
        val state = gameState ?: return scene.text
        return scene.textVariants.firstOrNull { ConditionEvaluator.isMet(it.condition, state) }?.text ?: scene.text
    }

    fun examineAction(item: Item): Action? = DemoContent.itemExamineActions[item.id]

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

    fun newlyAvailableActionIds(scene: Scene): Set<String> {
        val state = gameState ?: return emptySet()
        val visitedBefore = state.seenActionsByScene.containsKey(scene.id)
        if (!visitedBefore) return emptySet()
        val visibleIds = scene.actions.filter { ConditionEvaluator.isMet(it.condition, state) }.map { it.id }.toSet()
        val alreadySeen = state.seenActionsByScene[scene.id] ?: emptySet()
        return visibleIds - alreadySeen
    }

    fun usedActionIds(scene: Scene): Set<String> {
        val state = gameState ?: return emptySet()
        return state.usedActionsByScene[scene.id] ?: emptySet()
    }

    fun markActionsSeen(scene: Scene) {
        val state = gameState ?: return
        val visibleIds = scene.actions.filter { ConditionEvaluator.isMet(it.condition, state) }.map { it.id }.toSet()
        val alreadySeen = state.seenActionsByScene[scene.id]
        if (alreadySeen == null || !alreadySeen.containsAll(visibleIds)) {
            val updated = state.seenActionsByScene + (scene.id to ((alreadySeen ?: emptySet()) + visibleIds))
            updateState(state.copy(seenActionsByScene = updated))
        }
    }

    private fun markActionUsed(state: GameState, sceneId: String, actionId: String): GameState {
        val current = state.usedActionsByScene[sceneId] ?: emptySet()
        if (actionId in current) return state
        return state.copy(usedActionsByScene = state.usedActionsByScene + (sceneId to (current + actionId)))
    }

    fun performAction(action: Action) {
        if (action.check != null) return
        val state = gameState ?: return
        val sceneId = state.currentSceneId
        val withUsed = if (action.isNavigation) state else markActionUsed(state, sceneId, action.id)
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

    fun applyConsequence(action: Action, outcome: CheckOutcome) {
        val state = gameState ?: return
        val targetSceneId = engine.consequenceSceneIdFor(action, outcome) ?: return
        val withUsed = markActionUsed(state, state.currentSceneId, action.id)
        updateState(withUsed.copy(currentSceneId = targetSceneId))
    }

    fun outcomeEffects(action: Action, outcome: CheckOutcome): List<Effect> = engine.effectsForOutcome(action, outcome)

    fun markPartTitleShown(sceneId: String) {
        val state = gameState ?: return
        if (sceneId in state.shownPartTitleSceneIds) return
        updateState(state.copy(shownPartTitleSceneIds = state.shownPartTitleSceneIds + sceneId))
    }

    fun isPartTitleShown(sceneId: String): Boolean {
        val state = gameState ?: return false
        return sceneId in state.shownPartTitleSceneIds
    }

    fun markInventorySeen() {
        val state = gameState ?: return
        if (!state.hasUnreadItems) return
        updateState(state.copy(hasUnreadItems = false))
    }

    fun markJournalSeen() {
        val state = gameState ?: return
        if (!state.hasUnreadJournal) return
        updateState(state.copy(hasUnreadJournal = false))
    }
}