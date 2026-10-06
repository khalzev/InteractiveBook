package com.interactivebook.app.ui.scene

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
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

// ---- Палитра ----
// Цвета «чернил» текста на странице книги.
private val InkColor = Color(0xFF3B2A1A)
private val InkDimColor = Color(0xFF6B5A44)
private val GoldColor = Color(0xFFB8925A)

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
 * Экран сцены — страница книги с закладками.
 *
 * Компоновка (сверху вниз, все три зоны внутри страницы):
 *   1. Иллюстрация сцены с золотой рамкой — 25% высоты.
 *   2. Текст сцены (с внутренним скроллом) — 50% высоты.
 *   3. Список действий с ромбами (с внутренним скроллом) — 25% высоты.
 *
 * Закладки (bookmarks.png) — накладываются в правом верхнем углу, поверх страницы.
 * Программно поверх иконок закладок — невидимые зоны нажатия.
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

    var stage by remember(scene.id) {
        mutableStateOf(if (wasRevealed) RevealStage.DONE else RevealStage.ILLUSTRATION)
    }
    var skipIllustration by remember(scene.id) { mutableStateOf(false) }
    var skipText by remember(scene.id) { mutableStateOf(false) }
    var skipActions by remember(scene.id) { mutableStateOf(false) }
    var isPaused by remember(scene.id) { mutableStateOf(false) }

    val illustrationAlpha = remember(scene.id) { Animatable(if (wasRevealed) 1f else 0f) }
    var revealedChars by remember(scene.id) {
        mutableIntStateOf(if (wasRevealed) displayText.length else 0)
    }
    var shownActions by remember(scene.id) {
        mutableIntStateOf(if (wasRevealed) actions.size else 0)
    }

    LaunchedEffect(scene.id, revealSpeed) {
        if (stage == RevealStage.DONE) return@LaunchedEffect

        // Этап 1: иллюстрация.
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

        // Этап 2: текст по буквам.
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

        // Этап 3: действия по одному.
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

    // Корневой Box — ловим тап и удержание по всему экрану.
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
        // ---- Фон: страница открытой книги ----
        AssetImage(
            fileName = "book_open.png",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // ---- Всё содержимое страницы, с отступами под разворот книги ----
        // Левая страница книги (уходит за край) — декоративная, ничего не показываем.
        // Правая страница — на ней весь контент. Отступы подобраны под пропорции
        // страницы (примерно 55% ширины от левого края).
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = 118.dp, end = 28.dp, top = 130.dp, bottom = 40.dp)
        ) {

            // ---- Зона 1: иллюстрация (25%) ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.25f)
                    .graphicsLayer { alpha = illustrationAlpha.value },
                contentAlignment = Alignment.Center
            ) {
                IllustrationWithFrame(stage)
            }

            Spacer(Modifier.height(12.dp))

            // ---- Зона 2: текст сцены (50%) ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.50f)
                    .fillMaxSize()
            ) {
                TextArea(
                    displayText = displayText,
                    revealedChars = revealedChars,
                    stage = stage
                )
            }

            Spacer(Modifier.height(8.dp))

            // ---- Разделитель между текстом и действиями ----
            OrnamentalDivider()

            Spacer(Modifier.height(8.dp))

            // ---- Зона 3: действия (25%) ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.25f)
                    .fillMaxSize()
            ) {
                ActionsList(
                    actions = actions,
                    shownActions = shownActions,
                    stage = stage,
                    isNew = { it.id in newActionIds },
                    isUsed = { it.id in usedActionIds },
                    costHintFor = costHintFor,
                    onClick = { onActionClicked(it) }
                )
            }
        }

        // ---- Закладки в правом верхнем углу (поверх страницы) ----
        BookmarksRow(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 8.dp, end = 8.dp),
            onInventory = onOpenInventory,
            onJournal = onOpenJournal,
            onSettings = onOpenSettings
        )

        // ---- Ресурсы (золото и т.д.) — под закладками справа ----
        ResourcesRow(
            resources = resources,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 68.dp, end = 24.dp)
        )
    }

    // ---- Оверлей броска кубика ----
    pendingCheck?.let { (action, resolution) ->
        CheckRollOverlay(
            resolution = resolution,
            onContinue = {
                onApplyCheckOutcome(action, resolution.outcome)
                pendingCheck = null
            }
        )
    }

    // ---- Подтверждение действия (если задано) ----
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
}

// ---------------------------------------------------------------------------------------------
// Компоненты страницы сцены
// ---------------------------------------------------------------------------------------------

/**
 * Иллюстрация сцены с золотой рамкой, нарисованной программно (тонкие линии по периметру).
 * Пока картинки нет — показывается плейсхолдер. Позже сюда подставится картинка по id сцены.
 */
@Composable
private fun IllustrationWithFrame(stage: RevealStage) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Внутренняя зона — сюда позже ляжет картинка сцены.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .background(Color(0xFF2B2118)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.placeholder_illustration),
                color = Color(0xFFB5A27C),
                style = MaterialTheme.typography.labelLarge
            )
        }

        // Рамка — тонкая золотая линия по периметру, рисуется поверх.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .border(
                    width = 1.dp,
                    color = GoldColor.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}

/**
 * Текст сцены с постепенным проявлением (эффект печатной машинки).
 * С внутренним скроллом и мягким градиентом у нижнего края, если есть скрытый текст.
 */
@Composable
private fun TextArea(
    displayText: String,
    revealedChars: Int,
    stage: RevealStage
) {
    val scrollState = rememberScrollState()
    val showScrollHint = scrollState.canScrollForward

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(end = 6.dp)
        ) {
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
                color = InkColor
            )
        }

        // Градиент затухания + стрелка вниз — если есть скрытый текст.
        if (showScrollHint) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFE8DBC0).copy(alpha = 0.95f)
                            )
                        )
                    )
            )
            Text(
                text = "⌄",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .alpha(0.6f),
                color = InkDimColor,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

/**
 * Декоративный разделитель между текстом и действиями — горизонтальная линия
 * с ромбом посередине. Рисуется программно.
 */
@Composable
private fun OrnamentalDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(GoldColor.copy(alpha = 0.6f))
        )
        Text(
            text = "◆",
            color = GoldColor,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(GoldColor.copy(alpha = 0.6f))
        )
    }
}

/**
 * Список действий с ромбами-маркерами. С внутренним скроллом.
 * Пройденные действия — приглушены, с галочкой. Новые — с лёгким выделением.
 */
@Composable
private fun ActionsList(
    actions: List<Action>,
    shownActions: Int,
    stage: RevealStage,
    isNew: (Action) -> Boolean,
    isUsed: (Action) -> Boolean,
    costHintFor: (Action) -> String?,
    onClick: (Action) -> Unit
) {
    val scrollState = rememberScrollState()
    val showScrollHint = scrollState.canScrollForward

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(end = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            actions.forEachIndexed { index, action ->
                val visible = stage == RevealStage.DONE || index < shownActions
                ActionRow(
                    action = action,
                    visible = visible,
                    isNew = isNew(action),
                    isUsed = isUsed(action),
                    costHint = costHintFor(action),
                    onClick = { onClick(action) }
                )
            }
        }

        if (showScrollHint) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFE8DBC0).copy(alpha = 0.95f)
                            )
                        )
                    )
            )
            Text(
                text = "⌄",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .alpha(0.6f),
                color = InkDimColor,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

/**
 * Одна строка действия — текст с ромбом-маркером слева.
 * Пройденные — приглушены, с галочкой вместо ромба.
 */
@Composable
private fun ActionRow(
    action: Action,
    visible: Boolean,
    isNew: Boolean,
    isUsed: Boolean,
    costHint: String?,
    onClick: () -> Unit
) {
    val contentDisabled = action.disabledReason != null
    val alphaAnim by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(350),
        label = "actionAlpha"
    )

    val rowColor = when {
        contentDisabled -> InkDimColor.copy(alpha = 0.5f)
        isUsed -> InkDimColor
        else -> InkColor
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = alphaAnim }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = visible && !contentDisabled,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = if (isUsed) "✓" else "◆",
                color = if (isUsed) GoldColor.copy(alpha = 0.5f) else GoldColor,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .padding(end = 10.dp)
                    .width(20.dp)
            )

            Text(
                text = action.text,
                color = rowColor,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isNew) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (!costHint.isNullOrBlank() && !contentDisabled) {
                Text(
                    text = "  $costHint",
                    color = InkDimColor,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        if (contentDisabled) {
            Text(
                text = action.disabledReason.orEmpty(),
                color = InkDimColor,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 30.dp, top = 2.dp)
            )
        }
    }
}

/**
 * Три закладки в правом верхнем углу — картинка bookmarks.png.
 * Поверх картинки — три невидимые зоны нажатия.
 */
@Composable
private fun BookmarksRow(
    modifier: Modifier = Modifier,
    onInventory: () -> Unit,
    onJournal: () -> Unit,
    onSettings: () -> Unit
) {
    Box(
        modifier = modifier
            .height(120.dp)
            .width(180.dp)
    ) {
        AssetImage(
            fileName = "bookmarks.png",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Три невидимые зоны нажатия — примерно под тремя иконками закладок.
        // Левая (бордовая) — инвентарь, средняя (тёмная) — журнал, правая (коричневая) — настройки.
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = onInventory
                    )
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = onJournal
                    )
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = onSettings
                    )
            )
        }
    }
}

/**
 * Ресурсы (золото и др.) — отображаются текстом справа под закладками.
 * Пока без иконок, визуал будет доработан позже.
 */
@Composable
private fun ResourcesRow(
    resources: Map<String, GameResource>,
    modifier: Modifier = Modifier
) {
    if (resources.isEmpty()) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        resources.values.forEach { resource ->
            Text(
                text = "${resourceIcon(resource.id)} ${resource.currentValue}",
                color = InkColor,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun resourceIcon(resourceId: String): String = when (resourceId) {
    "gold" -> "🪙"
    "time" -> "⏳"
    else -> "◆"
}