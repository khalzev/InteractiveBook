package com.interactivebook.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.interactivebook.app.content.scenes.DemoContent
import com.interactivebook.app.core.model.Action
import com.interactivebook.app.ui.character.CharacterCreationScreen
import com.interactivebook.app.ui.common.GameViewModel
import com.interactivebook.app.ui.common.PartTitleScreen
import com.interactivebook.app.ui.inventory.InventoryScreen
import com.interactivebook.app.ui.journal.JournalScreen
import com.interactivebook.app.ui.menu.MainMenuScreen
import com.interactivebook.app.ui.scene.SceneScreen
import com.interactivebook.app.ui.settings.SettingsScreen
import com.interactivebook.app.ui.theme.InteractiveBookTheme

private const val ROUTE_MENU = "menu"
private const val ROUTE_CHARACTER_CREATION = "character_creation"
private const val ROUTE_SCENE = "scene"
private const val ROUTE_INVENTORY = "inventory"
private const val ROUTE_JOURNAL = "journal"
private const val ROUTE_SETTINGS = "settings"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge: иллюстрация сцены может уходить под статус-бар (раздел 80 — погружение),
        // при этом интерактивные элементы сами добавляют себе нужные отступы (statusBarsPadding /
        // navigationBarsPadding / safeDrawingPadding) в соответствующих экранах.
        enableEdgeToEdge()
        setContent {
            InteractiveBookTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    InteractiveBookApp()
                }
            }
        }
    }
}

/**
 * Базовая навигация: главное меню <-> экран сцены.
 * UI обращается только к GameViewModel (которая делегирует в GameEngine/ConditionEvaluator/
 * CheckResolver) — сам не содержит игровой логики (раздел 76).
 */
@Composable
private fun InteractiveBookApp() {
    val viewModel: GameViewModel = viewModel()
    val navController: NavHostController = rememberNavController()
    val activity = LocalContext.current as? ComponentActivity

    // Действие, которое инициировало переход на экран создания персонажа — его effects
    // (обычно Transition к следующей сцене) применяются сразу после завершения создания.
    var pendingCharacterCreationAction by remember { mutableStateOf<Action?>(null) }

    NavHost(navController = navController, startDestination = ROUTE_MENU) {

        composable(ROUTE_MENU) {
            MainMenuScreen(
                hasSavedGame = viewModel.hasSavedGame(),
                onNewGame = {
                    // Новая игра сразу стартует в пролог, без персонажа — выбор класса
                    // происходит позже, когда сюжет до него дойдёт (requiresCharacterCreation).
                    viewModel.startNewGame()
                    navController.navigate(ROUTE_SCENE)
                },
                onContinue = {
                    if (viewModel.hasSavedGame()) {
                        navController.navigate(ROUTE_SCENE)
                    }
                },
                onSettings = { navController.navigate(ROUTE_SETTINGS) },
                onExit = {
                    activity?.finish()
                }
            )
        }

        composable(ROUTE_CHARACTER_CREATION) {
            CharacterCreationScreen(
                onCharacterReady = { character ->
                    val action = pendingCharacterCreationAction
                    viewModel.setCharacterAndApplyEffects(character, action?.effects ?: emptyList())
                    pendingCharacterCreationAction = null
                    navController.navigate(ROUTE_SCENE) {
                        popUpTo(ROUTE_CHARACTER_CREATION) { inclusive = true }
                    }
                }
            )
        }

        composable(ROUTE_SCENE) {
            val scene = viewModel.currentScene() ?: DemoContent.startingScene
            // Если у сцены задан partTitle (пункт 3) — сначала показываем заголовок части
            // ("Часть I" → название → "Пролог") с тапом для продолжения, и только потом саму
            // сцену. remember(scene.id) сбрасывает флаг при переходе на новую сцену с partTitle.
            var showingPartTitle by rememberSaveable(scene.id) { mutableStateOf(scene.partTitle != null) }

            // "Новые" действия — те, что появились в этой сцене после её прошлого посещения
            // (условие стало выполняться). Вычисляем один раз при входе (remember), чтобы список
            // не "схлопывался" на середине анимации из-за отметки об увиденном.
            val newActionIds = remember(scene.id) { viewModel.newlyAvailableActionIds(scene) }
            LaunchedEffect(scene.id) {
                viewModel.markActionsSeen(scene)
            }

            if (showingPartTitle && scene.partTitle != null) {
                PartTitleScreen(
                    partTitle = scene.partTitle,
                    onContinue = { showingPartTitle = false }
                )
            } else {
                SceneScreen(
                    scene = scene,
                    displayText = viewModel.resolveSceneText(scene),
                    actions = viewModel.visibleActions(scene),
                    newActionIds = newActionIds,
                    resources = viewModel.gameState?.resources ?: emptyMap(),
                    revealedAlready = viewModel.isSceneRevealed(scene.id),
                    revealSpeed = viewModel.revealSpeed,
                    onRevealComplete = { viewModel.markSceneRevealed(scene.id) },
                    onSimpleAction = { action -> viewModel.performAction(action) },
                    onResolveCheck = { action -> viewModel.resolveCheck(action) },
                    onApplyCheckOutcome = { action, outcome -> viewModel.applyCheckOutcome(action, outcome) },
                    onTriggerCharacterCreation = { action ->
                        pendingCharacterCreationAction = action
                        navController.navigate(ROUTE_CHARACTER_CREATION)
                    },
                    onOpenMainMenu = {
                        navController.popBackStack(ROUTE_MENU, inclusive = false)
                    },
                    onOpenInventory = { navController.navigate(ROUTE_INVENTORY) },
                    onOpenJournal = { navController.navigate(ROUTE_JOURNAL) },
                    onOpenSettings = { navController.navigate(ROUTE_SETTINGS) }
                )
            }
        }

        composable(ROUTE_INVENTORY) {
            InventoryScreen(
                items = viewModel.gameState?.inventory ?: emptyList(),
                resources = viewModel.gameState?.resources ?: emptyMap(),
                examineActionFor = { item -> viewModel.examineAction(item) },
                onSimpleAction = { action -> viewModel.performAction(action) },
                onResolveCheck = { action -> viewModel.resolveCheck(action) },
                onApplyCheckOutcome = { action, outcome -> viewModel.applyCheckOutcome(action, outcome) },
                outcomeEffects = { action, outcome -> viewModel.outcomeEffects(action, outcome) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                revealSpeed = viewModel.revealSpeed,
                onRevealSpeedChange = { viewModel.updateRevealSpeed(it) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_JOURNAL) {
            JournalScreen(
                information = viewModel.gameState?.information ?: emptyList(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
