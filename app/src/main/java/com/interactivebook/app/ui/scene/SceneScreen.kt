package com.interactivebook.app.ui.scene

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.interactivebook.app.R
import com.interactivebook.app.core.checks.CheckOutcome
import com.interactivebook.app.core.checks.CheckResolution
import com.interactivebook.app.core.model.Action
import com.interactivebook.app.core.model.GameResource
import com.interactivebook.app.core.model.Scene
import com.interactivebook.app.core.settings.RevealSpeed
import com.interactivebook.app.ui.check.CheckRollOverlay
import com.interactivebook.app.ui.common.AssetImage
import kotlin.math.max
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ---- Тайминги анимации появления сцены ----
private data class RevealTiming(
    val illustrationFadeMs: Int,
    val textBudgetMs: Long,
    val minCharDelayMs: Long,
    val maxCharDelayMs: Long,
    val actionStaggerMs: Long
)

private val FAST_TIMING = RevealTiming(800, 9000L, 8L, 28L, 220L)
private val SLOW_TIMING = RevealTiming(1600, 18000L, 16L, 56L, 440L)
private const val PAUSE_AFTER_ILLUSTRATION_MS = 250L
private const val PAUSE_AFTER_TEXT_MS = 250L
private const val PAUSE_POLL_MS = 16L
private const val TAP_MAX_DURATION_MS = 250L

// Палитра интерфейса (для тех мест, где нет картинки — фолбэк-цвета).
private val InkBackgroundFallback = Color(0xFF1C1712)
private val ParchmentFallback = Color(0xFFE8DBC0)
private val ParchmentInk = Color(0xFF2B2118)
private val ParchmentInkDim = Color(0xFF5A4A34)

private enum class RevealStage { ILLUSTRATION, TEXT, ACTIONS, DONE }

private suspend fun pausableDelay(totalMs: Long, isPaused: () -> Boolean) {
    var remaining = totalMs
    while (remaining > 0) {
        if (isPaused()) {
            delay(PAUSE_POLL_MS)
        } else {
            val step = minOf(PAUSE_POLL_MS, remaining)
            delay(step)
            remaining -= step
        }
    }
}

/**
 * Экран Scene — "страница книги".
 *
 * Компоновка:
 *   ┌──────────────────────────────────┐
 *   │  ШАПКА (поверх иллюстрации)      │  ← иконки навигации + ресурс
 *   │                                  │
 *   │  ИЛЛЮСТРАЦИЯ (с рамкой)          │  ← ~35% высоты
 *   ├──────────────────────────────────┤
 *   │                                  │
 *   │  ПЕРГАМЕНТ (текст + кнопки)      │  ← ~65% высоты, внутренний скролл
 *   │                                  │
 *   └──────────────────────────────────┘
 *
 * Тап по любому месту завершает текущий этап анимации.
 * Удержание ставит этап на паузу. Отпускание после удержания НЕ считается тапом (порог 250 мс).
 */
@Composable
fun SceneScreen(
    scene: Scene,
    displayText: String,
    actions: List<Action>,
    newActionIds: Set<String>,
    usedActionIds: Set<String>,
    costHintFor: (Action) -> String?,
    resources: Map<String, GameResource>,
    revealedAlready: Boolean,
    revealSpeed: RevealSpeed,
    onRevealComplete: () -> Unit,
    onSimpleAction: (Action) -> Unit,
    onResolveCheck: (Action) -> CheckResolution?,
    onApplyCheckOutcome: (Action, CheckOutcome) -> Unit,
    onTriggerCharacterCreation: (Action) -> Unit,
    onOpenMainMenu: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var pendingCheck by remember { mutableStateOf<Pair<Action, CheckResolution>?>(null) }
    var pendingConfirmation by remember { mutableStateOf<Action?>(null) }
    var showExitConfirm by remember { mutableStateOf(false) }

    fun dispatch(action: Action) {
        when {
            action.requiresCharacterCreation -> onTriggerCharacterCreation(action)
            action.check != null -> {
                val resolution = onResolveCheck(action)
                if (resolution != null) pendingCheck = action to resolution
            }
            else -> onSimpleAction(action)
        }
    }

    fun onActionClicked(action: Action) {
        if (action.confirmation != null) {
            pendingConfirmation = action
        } else {
            dispatch(action)
        }
    }

    val wasRevealed = remember(scene.id) { revealedAlready || revealSpeed == RevealSpeed.INSTANT }
    val timing = if (revealSpeed == RevealSpeed.SLOW) SLOW_TIMING else FAST_TIMING

    var stage by remember(scene.id) { mutableStateOf(if (wasRevealed) RevealStage.DONE else RevealStage.ILLUSTRATION) }
    var skipIllustration by remember(scene.id) { mutableStateOf(false) }
    var skipText by remember(scene.id) { mutableStateOf(false) }
    var skipActions by remember(scene.id) { mutableStateOf(false) }
    var isPaused by remember(scene.id) { mutableStateOf(false) }

    val illustrationAlpha = remember(scene.id) { Animatable(if (wasRevealed) 1f else 0f) }
    var revealedChars by remember(scene.id) { mutableIntStateOf(if (wasRevealed) displayText.length else 0) }
    var shownActions by remember(scene.id) { mutableIntStateOf(if (wasRevealed) actions.size else 0) }

    LaunchedEffect(scene.id, revealSpeed) {
        if (stage == RevealStage.DONE) return@LaunchedEffect

        stage = RevealStage.ILLUSTRATION
        if (!skipIllustration) {
            coroutineScope {
                val animJob = launch { illustrationAlpha.animateTo(1f, tween(timing.illustrationFadeMs)) }
                val watchJob = launch {
                    snapshotFlow { skipIllustration }.first { it }
                    animJob.cancel()
                }
                animJob.join()
                watchJob.cancel()
            }
        }
        illustrationAlpha.snapTo(1f)
        delay(PAUSE_AFTER_ILLUSTRATION_MS)

        stage = RevealStage.TEXT
        if (!skipText) {
            val total = displayText.length
            val perChar = (timing.textBudgetMs / max(1, total))
                .coerceIn(timing.minCharDelayMs, timing.maxCharDelayMs)
            var i = 0
            while (i < total && !skipText) {
                i++
                revealedChars = i
                pausableDelay(perChar) { isPaused }
            }
        }
        revealedChars = displayText.length
        delay(PAUSE_AFTER_TEXT_MS)

        stage = RevealStage.ACTIONS
        if (!skipActions) {
            var i = 0
            while (i < actions.size && !skipActions) {
                i++
                shownActions = i
                pausableDelay(timing.actionStaggerMs) { isPaused }
            }
        }
        shownActions = actions.size
        stage = RevealStage.DONE
        onRevealComplete()
    }

    // Корневой Box: поверх него ловим тап/удержание по всему экрану,
    // при этом кнопки ниже обрабатывают свои клики (мы не поглощаем события).
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(scene.id, stage) {
                if (stage == RevealStage.DONE) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val startTime = System.currentTimeMillis()
                    isPaused = true
                    var released = false
                    while (!released) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            released = true
                        }
                    }
                    val duration = System.currentTimeMillis() - startTime
                    isPaused = false
                    if (duration < TAP_MAX_DURATION_MS) {
                        when (stage) {
                            RevealStage.ILLUSTRATION -> skipIllustration = true
                            RevealStage.TEXT -> skipText = true
                            RevealStage.ACTIONS -> skipActions = true
                            RevealStage.DONE -> {}
                        }
                    }
                }
            }
    ) {
        // Фон экрана — картинка scene_background.png, растянутая на весь экран.
        AssetImage(
            fileName = "scene_background.png",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            fallbackColor = InkBackgroundFallback
        )

        // Основная колонка: иллюстрация сверху, пергамент снизу.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {

            // ---- Иллюстрация (35% высоты) ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
                    .graphicsLayer { alpha = illustrationAlpha.value }
            ) {
                // Рамка иллюстрации (16:9 по ширине). Внутри — место под картинку сцены.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .align(Alignment.TopCenter)
                ) {
                    // Внутренняя область под картинку сцены — пока плейсхолдер.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                            .background(Color(0xFF2B2118)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.placeholder_illustration),
                            color = Color(0xFFB5A27C),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    // Рамка поверх картинки.
                    AssetImage(
                        fileName = "illustration_frame.png",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                }
            }

            // ---- Пергамент с текстом и кнопками (65% высоты) ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f)
                    .fillMaxSize()
            ) {
                // Пергамент под текст.
                AssetImage(
                    fileName = "parchment_panel.png",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                    fallbackColor = ParchmentFallback
                )

                // Содержимое пергамента: текст сверху + кнопки ниже. Всё вместе скроллится.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 28.dp, vertical = 24.dp)
                ) {
                    // Плашка спикера (если задан speakerName) — над текстом.
                    if (!scene.speakerName.isNullOrBlank()) {
                        Text(
                            text = scene.speakerName,
                            style = MaterialTheme.typography.labelLarge,
                            color = ParchmentInkDim,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Текст сцены (эффект печатной машинки).
                    val shown = if (stage == RevealStage.DONE) displayText.length
                    else revealedChars.coerceAtMost(displayText.length)
                    val annotated = remember(displayText, shown) {
                        buildAnnotatedString {
                            append(displayText.substring(0, shown))
                            withStyle(SpanStyle(color = Color.Transparent)) {
                                append(displayText.substring(shown))
                            }
                        }
                    }
                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ParchmentInk
                    )

                    Spacer(Modifier.height(20.dp))

                    // Кнопки действий — визуально часть пергамента.
                    actions.forEachIndexed { index, action ->
                        val visible = stage == RevealStage.DONE || index < shownActions
                        ActionButton(
                            action = action,
                            visible = visible,
                            isNew = action.id in newActionIds,
                            used = action.id in usedActionIds,
                            costHint = costHintFor(action),
                            onClick = { onActionClicked(action) }
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // ---- Шапка навигации (поверх иллюстрации) ----
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavIconButton(
                fileName = "icon_home.png",
                contentDescription = stringResource(R.string.nav_home_content_description),
                onClick = { showExitConfirm = true }
            )

            Spacer(Modifier.weight(1f))

            // Ресурс "золото".
            val gold = resources["gold"]
            if (gold != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    AssetImage(
                        fileName = "icon_gold.png",
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = gold.currentValue.toString(),
                        color = Color(0xFFE8DBC0),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            NavIconButton(
                fileName = "icon_journal.png",
                contentDescription = stringResource(R.string.nav_journal_content_description),
                onClick = onOpenJournal
            )
            NavIconButton(
                fileName = "icon_inventory.png",
                contentDescription = stringResource(R.string.nav_inventory_content_description),
                onClick = onOpenInventory
            )
            NavIconButton(
                fileName = "icon_settings.png",
                contentDescription = stringResource(R.string.nav_settings_content_description),
                onClick = onOpenSettings
            )
        }
    }

    pendingCheck?.let { (action, resolution) ->
        CheckRollOverlay(
            resolution = resolution,
            onContinue = {
                onApplyCheckOutcome(action, resolution.outcome)
                pendingCheck = null
            }
        )
    }

    pendingConfirmation?.let { action ->
        val confirmation = action.confirmation ?: return@let
        AlertDialog(
            onDismissRequest = { pendingConfirmation = null },
            title = { Text(confirmation.title) },
            text = { Text(confirmation.message) },
            confirmButton = {
                TextButton(onClick = {
                    pendingConfirmation = null
                    dispatch(action)
                }) {
                    Text(confirmation.confirmLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirmation = null }) {
                    Text(confirmation.cancelLabel)
                }
            }
        )
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text(stringResource(R.string.exit_to_menu_title)) },
            text = { Text(stringResource(R.string.exit_to_menu_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirm = false
                    onOpenMainMenu()
                }) {
                    Text(stringResource(R.string.exit_to_menu_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text(stringResource(R.string.exit_to_menu_cancel))
                }
            }
        )
    }
}

/**
 * Круглая кнопка-иконка для шапки навигации. Иконка — картинка из assets,
 * фон — полупрозрачный тёмный круг, чтобы читалась на любой иллюстрации.
 */
@Composable
private fun NavIconButton(
    fileName: String,
    contentDescription: String?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .padding(4.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AssetImage(
            fileName = fileName,
            contentDescription = contentDescription,
            modifier = Modifier.size(32.dp),
            contentScale = ContentScale.Fit
        )
    }
}