package com.interactivebook.app

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.interactivebook.app.content.scenes.DemoContent
import com.interactivebook.app.core.model.Action
import com.interactivebook.app.ui.character.CharacterCreationScreen
import com.interactivebook.app.ui.common.AssetImage
import com.interactivebook.app.ui.common.BookVideoPlayer
import com.interactivebook.app.ui.common.GameViewModel
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
                onFinished = { startupStage = StartupStage.CLOSED_BOOK }
            )
        }

        StartupStage.CLOSED_BOOK -> {
            ClosedBookScreen(onContinue = { startupStage = StartupStage.OPENING_VIDEO })
        }

        StartupStage.OPENING_VIDEO -> {
            BookVideoPlayer(
                fileName = "book_opening.mp4",
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
        AssetImage(
            fileName = "book_closed.png",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (showHint) {
            Text(
                text = stringResource(R.string.intro_tap_to_continue),
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFE8DBC0),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 56.dp)
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
    NavHost(navController = navController, startDestination = ROUTE_MENU) {

        composable(ROUTE_MENU) {
            MainMenuPage(
                hasSavedGame = viewModel.hasSavedGame(),
                onNewGame = {
                    viewModel.startNewGame()
                    navController.navigate(ROUTE_SCENE)
                },
                onContinue = {
                    if (viewModel.hasSavedGame()) {
                        navController.navigate(ROUTE_SCENE)
                    }
                },
                onSettings = { navController.navigate(ROUTE_SETTINGS) },
                onExit = onExit
            )
        }

        composable(ROUTE_CHARACTER_CREATION) {
            CharacterCreationScreen(
                onCharacterReady = { character ->
                    val action = pendingCharacterCreationAction
                    viewModel.setCharacterAndApplyEffects(character, action?.effects ?: emptyList())
                    setPendingCharacterCreationAction(null)
                    navController.navigate(ROUTE_SCENE) {
                        popUpTo(ROUTE_CHARACTER_CREATION) { inclusive = true }
                    }
                }
            )
        }

        composable(ROUTE_SCENE) {
            val scene = viewModel.currentScene() ?: DemoContent.startingScene
            var showingPartTitle by rememberSaveable(scene.id) { mutableStateOf(scene.partTitle != null) }

            val newActionIds = remember(scene.id) { viewModel.newlyAvailableActionIds(scene) }
            val usedActionIds = viewModel.usedActionIds(scene)

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
                    usedActionIds = usedActionIds,
                    costHintFor = { action -> viewModel.costHintFor(action) },
                    resources = viewModel.gameState?.resources ?: emptyMap(),
                    revealedAlready = viewModel.isSceneRevealed(scene.id),
                    revealSpeed = viewModel.revealSpeed,
                    onRevealComplete = { viewModel.markSceneRevealed(scene.id) },
                    onSimpleAction = { action -> viewModel.performAction(action) },
                    onResolveCheck = { action -> viewModel.resolveCheck(action) },
                    onApplyCheckOutcome = { action, outcome -> viewModel.applyCheckOutcome(action, outcome) },
                    onTriggerCharacterCreation = { action ->
                        setPendingCharacterCreationAction(action)
                        navController.navigate(ROUTE_CHARACTER_CREATION)
                    },
                    onOpenMainMenu = { navController.popBackStack(ROUTE_MENU, inclusive = false) },
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
                pageTurnEnabled = viewModel.pageTurnEnabled,
                onRevealSpeedChange = { viewModel.updateRevealSpeed(it) },
                onPageTurnChange = { viewModel.updatePageTurnEnabled(it) },
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