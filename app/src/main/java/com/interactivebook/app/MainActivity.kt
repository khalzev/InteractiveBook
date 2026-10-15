package com.interactivebook.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.interactivebook.app.content.scenes.DemoContent
import com.interactivebook.app.core.effects.Effect
import com.interactivebook.app.core.model.Action
import com.interactivebook.app.ui.character.CharacterCreationScreen
import com.interactivebook.app.ui.common.AssetImage
import com.interactivebook.app.ui.common.BookPageBackground
import com.interactivebook.app.ui.common.BookVideoPlayer
import com.interactivebook.app.ui.common.GameViewModel
import com.interactivebook.app.ui.common.PageTurnTransition
import com.interactivebook.app.ui.common.PartTitleScreen
import com.interactivebook.app.ui.inventory.InventoryScreen
import com.interactivebook.app.ui.journal.JournalScreen
import com.interactivebook.app.ui.menu.MainMenuPage
import com.interactivebook.app.ui.scene.SceneScreen
import com.interactivebook.app.ui.settings.SettingsScreen
import com.interactivebook.app.ui.theme.InteractiveBookTheme
import kotlinx.coroutines.delay

private const val ROUTE_MENU = "menu"
private const val ROUTE_CHARACTER_CREATION = "character_creation"
private const val ROUTE_SCENE = "scene"
private const val ROUTE_INVENTORY = "inventory"
private const val ROUTE_JOURNAL = "journal"
private const val ROUTE_SETTINGS = "settings"

private enum class StartupStage { INTRO_VIDEO, CLOSED_BOOK, OPENING_VIDEO, DONE }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Прозрачные системные бары — и верхний, и нижний.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = AndroidColor.TRANSPARENT,
                darkScrim = AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        window.isNavigationBarContrastEnforced = false

        setContent {
            InteractiveBookTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    InteractiveBookApp()
                }
            }
        }
    }
}

@Composable
private fun InteractiveBookApp() {
    val viewModel: GameViewModel = viewModel()
    val navController: NavHostController = rememberNavController()
    val activity = LocalContext.current as? ComponentActivity

    var startupStage by rememberSaveable { mutableStateOf(StartupStage.INTRO_VIDEO) }
    var pendingCharacterCreationAction by remember { mutableStateOf<Action?>(null) }

    when (startupStage) {
        StartupStage.INTRO_VIDEO -> {
            BookVideoPlayer(
                fileName = "intro_book.mp4",
                allowSkip = true,
                onFinished = { startupStage = StartupStage.CLOSED_BOOK }
            )
        }

        StartupStage.CLOSED_BOOK -> {
            ClosedBookScreen(onContinue = { startupStage = StartupStage.OPENING_VIDEO })
        }

        StartupStage.OPENING_VIDEO -> {
            BookVideoPlayer(
                fileName = "book_opening.mp4",
                allowSkip = true,
                onFinished = { startupStage = StartupStage.DONE }
            )
        }

        StartupStage.DONE -> {
            InteractiveBookNavHost(
                viewModel = viewModel,
                navController = navController,
                onExit = { activity?.finish() },
                pendingCharacterCreationAction = pendingCharacterCreationAction,
                setPendingCharacterCreationAction = { pendingCharacterCreationAction = it }
            )
        }
    }
}

@Composable
private fun ClosedBookScreen(onContinue: () -> Unit) {
    var showHint by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        showHint = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onContinue)
    ) {
        BookPageBackground()

        AssetImage(
            fileName = "book_closed.png",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        if (showHint) {
            Text(
                text = "Нажмите для продолжения",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                color = Color(0xFFE8DBC0),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 105.dp)
            )
        }
    }
}

@Composable
private fun InteractiveBookNavHost(
    viewModel: GameViewModel,
    navController: NavHostController,
    onExit: () -> Unit,
    pendingCharacterCreationAction: Action?,
    setPendingCharacterCreationAction: (Action?) -> Unit
) {
    var pendingNavigation by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun withPageTurn(action: () -> Unit) {
        if (viewModel.pageTurnEnabled) {
            pendingNavigation = action
        } else {
            action()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        NavHost(navController = navController, startDestination = ROUTE_MENU) {

            composable(ROUTE_MENU) {
                MainMenuPage(
                    hasSavedGame = viewModel.hasSavedGame(),
                    onNewGame = {
                        viewModel.startNewGame()
                        withPageTurn { navController.navigate(ROUTE_SCENE) }
                    },
                    onContinue = {
                        if (viewModel.hasSavedGame()) {
                            withPageTurn { navController.navigate(ROUTE_SCENE) }
                        }
                    },
                    onSettings = {
                        withPageTurn { navController.navigate(ROUTE_SETTINGS) }
                    },
                    onExit = onExit
                )
            }

            composable(ROUTE_CHARACTER_CREATION) {
                CharacterCreationScreen(
                    onCharacterReady = { character ->
                        val action = pendingCharacterCreationAction
                        viewModel.setCharacterAndApplyEffects(character, action?.effects ?: emptyList())
                        setPendingCharacterCreationAction(null)
                        withPageTurn {
                            navController.navigate(ROUTE_SCENE) {
                                popUpTo(ROUTE_CHARACTER_CREATION) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(ROUTE_SCENE) {
                val scene = viewModel.currentScene() ?: DemoContent.startingScene
                var showingPartTitle by rememberSaveable(scene.id) {
                    mutableStateOf(
                        scene.partTitle != null && !viewModel.isPartTitleShown(scene.id)
                    )
                }

                val newActionIds = remember(scene.id) { viewModel.newlyAvailableActionIds(scene) }
                val usedActionIds = viewModel.usedActionIds(scene)

                LaunchedEffect(scene.id) {
                    viewModel.markActionsSeen(scene)
                }

                if (showingPartTitle && scene.partTitle != null) {
                    PartTitleScreen(
                        partTitle = scene.partTitle,
                        onContinue = {
                            viewModel.markPartTitleShown(scene.id)
                            withPageTurn { showingPartTitle = false }
                        }
                    )
                } else {
                    SceneScreen(
                        scene = scene,
                        displayText = viewModel.resolveSceneText(scene),
                        actions = viewModel.visibleActions(scene),
                        newActionIds = newActionIds,
                        usedActionIds = usedActionIds,
                        resources = viewModel.gameState?.resources ?: emptyMap(),
                        resourcesEverSeen = viewModel.resourcesEverSeen,
                        onResourceSeen = { viewModel.markResourceSeen(it) },
                        hasUnreadItems = viewModel.gameState?.hasUnreadItems ?: false,
                        hasUnreadJournal = viewModel.gameState?.hasUnreadJournal ?: false,
                        inventoryItemIds = viewModel.gameState?.inventory?.map { it.id }?.toSet() ?: emptySet(),
                        informationIds = viewModel.gameState?.information?.map { it.id }?.toSet() ?: emptySet(),
                        revealedAlready = viewModel.isSceneRevealed(scene.id),
                        revealSpeed = viewModel.revealSpeed,
                        onRevealComplete = { viewModel.markSceneRevealed(scene.id) },
                        onSimpleAction = { action ->
                            val currentSceneId = viewModel.gameState?.currentSceneId
                            val transition = action.effects
                                .filterIsInstance<Effect.Transition>()
                                .firstOrNull()
                            val hasTransition = transition != null &&
                                    transition.sceneId != currentSceneId
                            val hasAnimatableEffect = action.effects.any {
                                it is Effect.AddItem ||
                                        it is Effect.AddInformation ||
                                        it is Effect.ChangeResource
                            }

                            when {
                                hasTransition && hasAnimatableEffect -> {
                                    viewModel.performActionWithoutTransition(action)
                                }
                                hasTransition -> {
                                    withPageTurn { viewModel.performAction(action) }
                                }
                                else -> {
                                    viewModel.performAction(action)
                                }
                            }
                        },
                        onResolveCheck = { action -> viewModel.resolveCheck(action) },
                        onApplyCheckOutcome = { action, outcome ->
                            val currentSceneId = viewModel.gameState?.currentSceneId
                            val effects = viewModel.outcomeEffects(action, outcome)
                            val hasTransition = effects.any {
                                it is Effect.Transition && it.sceneId != currentSceneId
                            }
                            if (hasTransition) {
                                withPageTurn { viewModel.applyCheckOutcome(action, outcome) }
                            } else {
                                viewModel.applyCheckOutcome(action, outcome)
                            }
                        },
                        onApplyConsequence = { action, outcome ->
                            viewModel.applyConsequence(action, outcome)
                        },
                        onRewardsAnimationComplete = {
                            if (viewModel.hasPendingTransition()) {
                                withPageTurn { viewModel.applyPendingTransition() }
                            }
                        },
                        onTriggerCharacterCreation = { action ->
                            setPendingCharacterCreationAction(action)
                            withPageTurn { navController.navigate(ROUTE_CHARACTER_CREATION) }
                        },
                        onOpenMainMenu = {
                            withPageTurn {
                                navController.navigate(ROUTE_MENU) {
                                    popUpTo(ROUTE_MENU) { inclusive = true }
                                }
                            }
                        },
                        onOpenInventory = {
                            viewModel.markInventorySeen()
                            withPageTurn { navController.navigate(ROUTE_INVENTORY) }
                        },
                        onOpenJournal = {
                            viewModel.markJournalSeen()
                            withPageTurn { navController.navigate(ROUTE_JOURNAL) }
                        }
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
                    onApplyCheckOutcome = { action, outcome ->
                        viewModel.applyCheckOutcome(action, outcome)
                    },
                    outcomeEffects = { action, outcome -> viewModel.outcomeEffects(action, outcome) },
                    onBack = {
                        withPageTurn { navController.popBackStack() }
                    }
                )
            }

            composable(ROUTE_SETTINGS) {
                SettingsScreen(
                    revealSpeed = viewModel.revealSpeed,
                    pageTurnEnabled = viewModel.pageTurnEnabled,
                    onRevealSpeedChange = { viewModel.updateRevealSpeed(it) },
                    onPageTurnChange = { viewModel.updatePageTurnEnabled(it) },
                    onBack = {
                        withPageTurn { navController.popBackStack() }
                    }
                )
            }

            composable(ROUTE_JOURNAL) {
                JournalScreen(
                    information = viewModel.gameState?.information ?: emptyList(),
                    onBack = {
                        withPageTurn { navController.popBackStack() }
                    }
                )
            }
        }

        // Универсальный оверлей перехода.
        pendingNavigation?.let { action ->
            PageTurnTransition(
                onFinished = {
                    val a = pendingNavigation
                    pendingNavigation = null
                    a?.invoke()
                }
            )
        }
    }
}