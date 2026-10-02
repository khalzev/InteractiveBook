package com.interactivebook.app.ui.scene

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
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
import kotlin.math.max
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ---- Тайминги анимации появления сцены ----
// FAST — прежний темп; SLOW — вдвое медленнее (новый темп по умолчанию);
// INSTANT — анимации нет вообще, сцена появляется сразу целиком.

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

// Порог различения "тап" vs "удержание" (мс). Если игрок держал палец дольше — это удержание,
// а не тап: пауза снимается, но текущий этап анимации НЕ завершается принудительно.
private const val TAP_MAX_DURATION_MS = 250L

// Пергаментная палитра панели текста. Не берём из темы, чтобы панель одинаково читалась
// и в тёмной, и в светлой теме приложения.
private val Parchment = Color(0xFFE8DBC0)
private val ParchmentInk = Color(0xFF2B2118)
private val ParchmentInkDim = Color(0xFF5A4A34)
private val ParchmentBorder = Color(0xFF8A6A3E)

private enum class RevealStage { ILLUSTRATION, TEXT, ACTIONS, DONE }

/**
 * Пауза, которую можно "заморозить": если isPaused() возвращает true — не тикаем,
 * просто ждём. Удобно для удержания пальца на экране.
 */
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
 * Экран Scene — "страница книги" (раздел 79): иллюстрация сверху, пергаментная панель с
 * текстом, стек кнопок действий снизу.
 *
 * Шапка навигации: слева — "Домой" (с подтверждением), справа — Журнал, Инвентарь,
 * Настройки и текущее значение ресурса "золото". Гамбургер-меню полностью убран.
 *
 * Тап по любому месту экрана завершает ТЕКУЩИЙ этап анимации. Удержание ставит этап на паузу
 * до отпускания. Отпускание после удержания НЕ считается тапом (порог 250 мс).
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
    // Активная проверка: пока не null — показан CheckRollOverlay поверх экрана.
    var pendingCheck by remember { mutableStateOf<Pair<Action, CheckResolution>?>(null) }
    // Действие, ожидающее подтверждения (Action.confirmation != null).
    var pendingConfirmation by remember { mutableStateOf<Action?>(null) }
    // Диалог подтверждения выхода в главное меню.
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

    // ---- Состояние постепенного появления сцены ----
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

        // Этап 2: текст по буквам, с учётом паузы при удержании.
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

        // Этап 3: кнопки действий по одной, тоже с паузой.
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

    // Пока идёт бросок, сцена под оверлеем размывается (blur работает на Android 12+).
    val blurRadius by animateDpAsState(
        targetValue = if (pendingCheck != null) 14.dp else 0.dp,
        animationSpec = tween(300),
        label = "sceneBlur"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Обработчик тапа / удержания по ВСЕМУ экрану: ставим в PointerEventPass.Initial,
            // чтобы ловить касания везде, но НЕ поглощать их — тогда кнопки ниже продолжают
            // работать как кнопки (иконки шапки, кнопки действий).
            .pointerInput(scene.id, stage) {
                if (stage == RevealStage.DONE) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                    val startTime = System.currentTimeMillis()
                    isPaused = true
                    // Ждём отпускания ИЛИ отмены (например, если палец уехал за экран).
                    var released = false
                    while (!released) {
                        val event = awaitPointerEvent(pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            released = true
                        }
                    }
                    val duration = System.currentTimeMillis() - startTime
                    isPaused = false
                    // Короткое касание = тап: завершаем текущий этап.
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {

            Column(modifier = Modifier.fillMaxSize()) {

                // ---- Шапка навигации ----
                SceneTopBar(
                    resources = resources,
                    onHome = { showExitConfirm = true },
                    onJournal = onOpenJournal,
                    onInventory = onOpenInventory,
                    onSettings = onOpenSettings
                )

                // ---- Иллюстрация сцены ----
                // Пока реальные картинки не подключены (раздел 81) — плейсхолдер.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .graphicsLayer { alpha = illustrationAlpha.value }
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.placeholder_illustration),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // ---- Пергаментная панель с текстом ----
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    ParchmentTextPanel(
                        speakerName = scene.speakerName,
                        displayText = displayText,
                        revealedChars = revealedChars,
                        stage = stage
                    )
                }

                // ---- Стек кнопок действий ----
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                    }
                }
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

// ---------------------------------------------------------------------------------------------
// Шапка навигации и пергаментная панель
// ---------------------------------------------------------------------------------------------

/**
 * Верхняя шапка сцены: слева одна иконка "Домой", справа группа — ресурс (золото),
 * Журнал, Инвентарь, Настройки. Гамбургер-меню полностью убран.
 *
 * Иконки в Material Outlined — временно, до появления кастомной векторной графики.
 * Каждая — в полупрозрачном тёмном круге, чтобы читалась на любой иллюстрации.
 */
@Composable
private fun SceneTopBar(
    resources: Map<String, GameResource>,
    onHome: () -> Unit,
    onJournal: () -> Unit,
    onInventory: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavIconButton(
            icon = { Icon(Icons.Outlined.Home, contentDescription = stringResource(R.string.nav_home_content_description), tint = Color.White) },
            onClick = onHome
        )

        Spacer(Modifier.weight(1f))

        // Ресурс "золото" — если он есть в состоянии, показываем иконку и число.
        val gold = resources["gold"]
        if (gold != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Paid,
                    contentDescription = stringResource(R.string.nav_resource_content_description),
                    tint = Color(0xFFE8C766),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = gold.currentValue.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        NavIconButton(
            icon = { Icon(Icons.Outlined.Paid, contentDescription = stringResource(R.string.nav_journal_content_description), tint = Color.White) },
            onClick = onJournal
        )
        NavIconButton(
            icon = { Icon(Icons.Outlined.Backpack, contentDescription = stringResource(R.string.nav_inventory_content_description), tint = Color.White) },
            onClick = onInventory
        )
        NavIconButton(
            icon = { Icon(Icons.Outlined.Tune, contentDescription = stringResource(R.string.nav_settings_content_description), tint = Color.White) },
            onClick = onSettings
        )
    }
}

/**
 * Круглая кнопка-иконка в шапке: полупрозрачный тёмный круг, чтобы иконка
 * читалась на любой иллюстрации. Без Material-ripple — только лёгкая полупрозрачность.
 */
@Composable
private fun NavIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

/**
 * Пергаментная панель текста. Сверху (если задан speakerName) — тёмная плашка с именем
 * говорящего. Внутри — текст, который "печатается" по мере reveal: видимая часть — обычным
 * цветом, ещё не появившаяся — прозрачным (чтобы строки не прыгали). Пока текст длиннее
 * видимой области — внизу мягкий градиент и "дышащий" шеврон вниз.
 */
@Composable
private fun ParchmentTextPanel(
    speakerName: String?,
    displayText: String,
    revealedChars: Int,
    stage: RevealStage
) {
    val scrollState = rememberScrollState()
    // Показывать подсказку о скрытом тексте, если до низа ещё есть куда скроллить.
    val showScrollHint = scrollState.canScrollForward

    Column(modifier = Modifier.fillMaxWidth()) {

        // Плашка спикера — над пергаментом, слева, слегка "выступает".
        if (!speakerName.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .background(ParchmentInk)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = speakerName,
                    color = Parchment,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Parchment)
                .padding(1.5.dp)
                .background(Parchment)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Parchment)
                    .padding(horizontal = 2.dp, vertical = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 18.dp, vertical = 16.dp)
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
                        color = ParchmentInk
                    )
                }

                // Градиент у нижнего края + шеврон — только если текст ещё есть куда скроллить.
                if (showScrollHint) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Parchment.copy(alpha = 0.95f)
                                    )
                                )
                            )
                    )
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = ParchmentInkDim,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                            .size(28.dp)
                    )
                }
            }
        }
    }
}