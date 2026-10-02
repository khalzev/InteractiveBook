package com.interactivebook.app.ui.scene

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Тайминги для FAST — прежний темп по умолчанию; SLOW — вдвое медленнее (новый темп по
// умолчанию, пункт 3 правок). INSTANT анимацию не использует вообще.
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
 * Экран Scene — "страница книги" (раздел 79): иллюстрация сверху, текст, действия снизу.
 *
 * Появление идёт по этапам: иллюстрация → текст по буквам → кнопки по одной.
 * - Короткий тап по экрану мгновенно завершает ТЕКУЩИЙ этап (не всю анимацию разом) — доступно
 *   всегда, независимо от настройки скорости.
 * - Удержание пальца на экране ставит текущий этап на паузу, пока палец не отпустят.
 * - revealSpeed = INSTANT — анимации нет вообще, сцена появляется сразу целиком.
 * - revealedAlready — сцена уже была показана полностью (например, игрок вернулся из
 *   инвентаря): тогда она сразу отображается целиком, без повторной анимации.
 */
@Composable
fun SceneScreen(
    scene: Scene,
    displayText: String,
    actions: List<Action>,
    newActionIds: Set<String>,
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
    var menuExpanded by remember { mutableStateOf(false) }
    // Активная проверка: пока не null — показан CheckRollOverlay поверх экрана.
    var pendingCheck by remember { mutableStateOf<Pair<Action, CheckResolution>?>(null) }
    // Действие, ожидающее подтверждения (Action.confirmation != null), и коллбэк, который
    // нужно вызвать после подтверждения.
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

    // Пока идёт бросок, сцена под оверлеем размывается (blur работает на Android 12+,
    // на более старых версиях остаётся только затемнение оверлея).
    val blurRadius by animateDpAsState(
        targetValue = if (pendingCheck != null) 14.dp else 0.dp,
        animationSpec = tween(300),
        label = "sceneBlur"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius)
                .pointerInput(scene.id, stage) {
                    if (stage == RevealStage.DONE) return@pointerInput
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            try {
                                awaitRelease()
                            } finally {
                                isPaused = false
                            }
                        },
                        onTap = {
                            // Короткий тап завершает ТЕКУЩИЙ этап мгновенно — доступно всегда.
                            when (stage) {
                                RevealStage.ILLUSTRATION -> skipIllustration = true
                                RevealStage.TEXT -> skipText = true
                                RevealStage.ACTIONS -> skipActions = true
                                RevealStage.DONE -> {}
                            }
                        }
                    )
                }
        ) {

            Column(modifier = Modifier.fillMaxSize()) {

                // Иллюстрация сцены — плейсхолдер, пока изображения не подключены (раздел 81).
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

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    // Весь текст сразу занимает своё место (невидимая часть — прозрачная),
                    // поэтому строки не "прыгают" при переносе слов по мере набора.
                    val shownChars = if (stage == RevealStage.DONE) displayText.length
                    else revealedChars.coerceAtMost(displayText.length)
                    val annotated = remember(displayText, shownChars) {
                        buildAnnotatedString {
                            append(displayText.substring(0, shownChars))
                            withStyle(SpanStyle(color = Color.Transparent)) {
                                append(displayText.substring(shownChars))
                            }
                        }
                    }
                    Text(text = annotated, style = MaterialTheme.typography.bodyLarge)
                }

                // Индикатор ресурсов (раздел 52-55) — виден постоянно, если у главы есть активные ресурсы.
                if (resources.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        resources.values.forEach { resource ->
                            Text(
                                text = "${resourceIcon(resource.id)} ${resource.currentValue}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Панель действий: ограниченная высота + скролл внутри неё — "невидимая рамка",
                // за пределы которой кнопки не выходят, плюс отступ под системную панель навигации.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    actions.forEachIndexed { index, action ->
                        // Кнопки проявляются по одной; место под них зарезервировано заранее,
                        // поэтому раскладка не прыгает. Пока кнопка не появилась — она неактивна.
                        val visible = stage == RevealStage.DONE || index < shownActions
                        ActionButton(
                            action = action,
                            visible = visible,
                            isNew = action.id in newActionIds,
                            onClick = { onActionClicked(action) }
                        )
                    }
                }
            }

            // Кнопка меню в правом верхнем углу, с тёмной подложкой для читаемости на любом фоне.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(R.string.scene_menu_content_description),
                        tint = Color.White
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.scene_menu_inventory)) },
                        onClick = {
                            menuExpanded = false
                            onOpenInventory()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.scene_menu_journal)) },
                        onClick = {
                            menuExpanded = false
                            onOpenJournal()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.scene_menu_settings)) },
                        onClick = {
                            menuExpanded = false
                            onOpenSettings()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.scene_action_back_to_menu)) },
                        onClick = {
                            menuExpanded = false
                            onOpenMainMenu()
                        }
                    )
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
}

private fun resourceIcon(resourceId: String): String = when (resourceId) {
    "gold" -> "🪙"
    "time" -> "⏳"
    else -> "◆"
}

// Временный нейтральный цвет для отметки новых действий — жёлтые звёзды было плохо видно на
// светлых кнопках. Сменить, когда появится общий визуальный стиль приложения.
private val NewActionMarkColor = Color(0xFF6B6259)
private const val SPARK_COUNT = 6
private const val SPARK_DURATION_MS = 650

/**
 * Кнопка действия. Если isNew == true (действие появилось после того, как игрок уже был
 * в этой сцене — см. GameViewModel.newlyAvailableActionIds), в момент появления кнопки по ней
 * один раз пробегают короткие искры, а рядом с текстом остаётся маленький кружок — он держится,
 * пока игрок не нажмёт именно эту кнопку (после нажатия сцена обычно меняется, и кружок вместе
 * с ней).
 */
@Composable
private fun ActionButton(
    action: Action,
    visible: Boolean,
    isNew: Boolean,
    onClick: () -> Unit
) {
    val contentDisabled = action.disabledReason != null
    val actionAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(350),
        label = "actionAlpha"
    )

    // По одному Animatable на искру — тот же надёжный механизм, что и в CheckRollOverlay:
    // чтение .value в Canvas гарантированно вызывает перерисовку.
    val sparkProgress = remember(action.id) { List(SPARK_COUNT) { Animatable(0f) } }
    var sparksPlayed by remember(action.id) { mutableStateOf(false) }

    LaunchedEffect(visible, isNew, contentDisabled) {
        if (visible && isNew && !contentDisabled && !sparksPlayed) {
            sparksPlayed = true
            coroutineScope {
                sparkProgress.forEach { anim ->
                    launch { anim.animateTo(1f, tween(SPARK_DURATION_MS)) }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = actionAlpha }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onClick,
                enabled = visible && !contentDisabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(action.text, modifier = Modifier.weight(1f, fill = false))
                    if (isNew && !contentDisabled) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NewActionMarkColor)
                        )
                    }
                }
            }

            // Искры рисуются поверх кнопки — Canvas должен лежать внутри Box,
            // только там доступен matchParentSize().
            if (isNew && sparksPlayed) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val cx = size.width - 20f
                    val cy = size.height / 2f
                    sparkProgress.forEachIndexed { index, anim ->
                        val p = anim.value
                        if (p <= 0f || p >= 1f) return@forEachIndexed
                        val angle = (index / SPARK_COUNT.toFloat()) * 2f * PI.toFloat()
                        val eased = 1f - (1f - p) * (1f - p)
                        val distance = 22f * eased
                        val x = cx + cos(angle) * distance
                        val y = cy + sin(angle) * distance
                        drawCircle(
                            color = NewActionMarkColor.copy(alpha = 1f - p),
                            radius = 3f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }

        if (contentDisabled) {
            Text(
                text = action.disabledReason.orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}
