package com.interactivebook.app.ui.scene

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interactivebook.app.R
import com.interactivebook.app.core.checks.CheckOutcome
import com.interactivebook.app.core.checks.CheckResolution
import com.interactivebook.app.core.model.Action
import com.interactivebook.app.core.model.GameResource
import com.interactivebook.app.core.model.Scene
import com.interactivebook.app.core.settings.RevealSpeed
import com.interactivebook.app.ui.check.CheckRollOverlay
import com.interactivebook.app.ui.common.AssetImage
import com.interactivebook.app.ui.common.BookPageBackground
import com.interactivebook.app.ui.common.BookPageContent
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

private val InkColor = Color(0xFF3B2A1A)
private val InkDimColor = Color(0xFF6B5A44)
private val BurgundyColor = Color(0xFF7A2525)
private val GoldTextColor = Color(0xFFE8C766)

private val ResourcePlateWidth = 113.dp
private val ResourcePlateHeight = 57.dp
private val ResourcePlateNumberSize = 22.sp

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

@Composable
fun SceneScreen(
    scene: Scene,
    displayText: String,
    actions: List<Action>,
    newActionIds: Set<String>,
    usedActionIds: Set<String>,
    costHintFor: (Action) -> String?,
    resources: Map<String, GameResource>,
    resourcesEverSeen: Set<String>,
    onResourceSeen: (String) -> Unit,
    revealedAlready: Boolean,
    revealSpeed: RevealSpeed,
    onRevealComplete: () -> Unit,
    onSimpleAction: (Action) -> Unit,
    onResolveCheck: (Action) -> CheckResolution?,
    onApplyCheckOutcome: (Action, CheckOutcome) -> Unit,
    onTriggerCharacterCreation: (Action) -> Unit,
    onOpenMainMenu: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenJournal: () -> Unit
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

    // ---- Анимация монет и счётчика золота ----
    val density = LocalDensity.current
    val screenHeightPx = with(density) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }

    val goldValue = resources["gold"]?.currentValue ?: 0

    // Отображаемое значение (может отличаться от фактического, пока идёт анимация).
    var displayedGold by remember { mutableStateOf(goldValue) }
    // Последнее "показанное" значение (для отлова изменений).
    var previousGold by remember { mutableStateOf<Int?>(goldValue) }

    var plateCenter by remember { mutableStateOf(Offset.Zero) }
    var lastActionY by remember { mutableStateOf(0f) }
    var coins by remember { mutableStateOf<List<CoinFlight>>(emptyList()) }
    // Значение, к которому нужно привести счётчик после завершения монет.
    var pendingFinalGold by remember { mutableStateOf<Int?>(null) }
    // Тип анимации: появление (0 → N) или изменение (перебор).
    var pendingMode by remember { mutableStateOf(GoldAnimMode.NONE) }

    // Первое появление золота в сессии.
    LaunchedEffect(scene.id) {
        if (goldValue > 0 && "gold" !in resourcesEverSeen) {
            displayedGold = 0
            pendingMode = GoldAnimMode.APPEARANCE
            pendingFinalGold = goldValue

            val count = minOf(goldValue, 5)
            val newCoins = (0 until count).map { i ->
                CoinFlight(
                    id = System.nanoTime() + i,
                    start = Offset(0f, screenHeightPx),
                    end = plateCenter,
                    delayMs = i * 150L
                )
            }
            coins = coins + newCoins
            onResourceSeen("gold")
        }
    }

    // Изменение золота в процессе.
    LaunchedEffect(goldValue) {
        val prev = previousGold
        previousGold = goldValue
        if (prev == null || prev == goldValue) return@LaunchedEffect
        // Игнорируем случай, когда это "первое появление" (уже обработано).
        if (pendingMode == GoldAnimMode.APPEARANCE) return@LaunchedEffect

        // Плавный перебор счётчика от prev к goldValue.
        val step = if (goldValue > prev) 1 else -1
        var current = prev
        while (current != goldValue) {
            current += step
            displayedGold = current
            delay(150)
        }

        // Монеты.
        val delta = goldValue - prev
        val count = minOf(abs(delta), 5)
        val fromPlate = delta < 0
        val outsidePoint = if (lastActionY > 0f) {
            Offset(0f, lastActionY)
        } else {
            Offset(0f, screenHeightPx)
        }
        val newCoins = (0 until count).map { i ->
            CoinFlight(
                id = System.nanoTime() + i,
                start = if (fromPlate) plateCenter else outsidePoint,
                end = if (fromPlate) outsidePoint else plateCenter,
                delayMs = i * 150L
            )
        }
        coins = coins + newCoins
        lastActionY = 0f
    }

    // При завершении ВСЕХ монет — если был режим "появление", докатываем счётчик.
    LaunchedEffect(coins) {
        if (coins.isEmpty() && pendingMode == GoldAnimMode.APPEARANCE) {
            val target = pendingFinalGold
            if (target != null) {
                displayedGold = target
            }
            pendingMode = GoldAnimMode.NONE
            pendingFinalGold = null
        }
    }

    LaunchedEffect(scene.id, revealSpeed) {
        if (stage == RevealStage.DONE) return@LaunchedEffect

        stage = RevealStage.ILLUSTRATION
        if (!skipIllustration) {
            coroutineScope {
                val animJob = launch {
                    illustrationAlpha.animateTo(1f, tween(timing.illustrationFadeMs))
                }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(scene.id, stage) {
                if (stage == RevealStage.DONE) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )
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
        BookPageBackground()

        BookPageContent {
            Column(modifier = Modifier.fillMaxSize()) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.25f)
                        .graphicsLayer { alpha = illustrationAlpha.value },
                    contentAlignment = Alignment.Center
                ) {
                    IllustrationWithFrame()
                }

                Spacer(Modifier.height(8.dp))

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

                Spacer(Modifier.height(6.dp))

                OrnamentalDivider()

                Spacer(Modifier.height(6.dp))

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
                        onClick = { action, y ->
                            lastActionY = y
                            onActionClicked(action)
                        }
                    )
                }
            }
        }

        // Верхний правый угол: плашка ресурса + закладки.
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 30.dp, end = 0.dp),
            horizontalAlignment = Alignment.End
        ) {
            val gold = resources["gold"]
            if (gold != null) {
                Box(
                    modifier = Modifier
                        .width(ResourcePlateWidth)
                        .height(ResourcePlateHeight)
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInRoot()
                            plateCenter = Offset(
                                pos.x + coords.size.width / 2f,
                                pos.y + coords.size.height / 2f
                            )
                        }
                ) {
                    AssetImage(
                        fileName = "gold_resource_plate.png",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                    Text(
                        text = displayedGold.toString(),
                        color = GoldTextColor,
                        fontSize = ResourcePlateNumberSize,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 34.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
            }

            BookmarksRow(
                onInventory = onOpenInventory,
                onJournal = onOpenJournal,
                onMenu = onOpenMainMenu
            )
        }

        CoinFlightLayer(
            coins = coins,
            onCoinFinished = { id -> coins = coins.filterNot { it.id == id } }
        )
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
}

private enum class GoldAnimMode { NONE, APPEARANCE }

@Composable
private fun IllustrationWithFrame() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp)
                .background(Color(0xFF2B2118)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.placeholder_illustration),
                color = Color(0xFFB5A27C),
                style = MaterialTheme.typography.labelLarge
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .border(
                    width = 2.5.dp,
                    color = BurgundyColor.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}

@Composable
private fun TextArea(
    displayText: String,
    revealedChars: Int,
    stage: RevealStage
) {
    val scrollState = rememberScrollState()

    // Авто-скролл вниз во время проявления текста.
    LaunchedEffect(revealedChars, stage) {
        if (stage != RevealStage.DONE && scrollState.canScrollForward) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    // При мгновенном появлении (тап) — прокрутить вниз.
    LaunchedEffect(stage) {
        if (stage == RevealStage.DONE && scrollState.canScrollForward) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    // Логика затухания:
    //  - Если текст уходит за ВЕРХНЮЮ границу (можно скроллить вверх) — затухает ВЕРХ.
    //  - Если текст уходит за НИЖНЮЮ границу (можно скроллить вниз) — затухает НИЗ.
    val canScrollUp = scrollState.canScrollBackward
    val canScrollDown = scrollState.canScrollForward

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()

                // Маска-градиент: где 0 — прозрачно, где 1 — видно.
                val topStop = if (canScrollUp) 0f else 1f
                val bottomStop = if (canScrollDown) 0f else 1f

                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = topStop),
                            0.18f to Color.Black,
                            0.82f to Color.Black,
                            1f to Color.Black.copy(alpha = bottomStop)
                        )
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(end = 4.dp)
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
                color = InkColor,
                fontSize = 15.sp,
                lineHeight = 21.sp
            )
        }

        // Стрелки вверх/вниз поверх области.
        if (canScrollUp) {
            Text(
                text = "⌃",
                color = InkDimColor.copy(alpha = 0.55f),
                fontSize = 18.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp)
            )
        }
        if (canScrollDown) {
            Text(
                text = "⌄",
                color = InkDimColor.copy(alpha = 0.55f),
                fontSize = 18.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
            )
        }
    }
}

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
                .height(2.5.dp)
                .background(BurgundyColor.copy(alpha = 0.85f))
        )
        Text(
            text = "◆",
            color = BurgundyColor,
            fontSize = 22.sp,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.5.dp)
                .background(BurgundyColor.copy(alpha = 0.85f))
        )
    }
}

@Composable
private fun ActionsList(
    actions: List<Action>,
    shownActions: Int,
    stage: RevealStage,
    isNew: (Action) -> Boolean,
    isUsed: (Action) -> Boolean,
    costHintFor: (Action) -> String?,
    onClick: (Action, Float) -> Unit
) {
    val scrollState = rememberScrollState()
    val showScrollHint = scrollState.canScrollForward

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            actions.forEachIndexed { index, action ->
                val visible = stage == RevealStage.DONE || index < shownActions
                ActionRow(
                    action = action,
                    visible = visible,
                    isNew = isNew(action),
                    isUsed = isUsed(action),
                    costHint = costHintFor(action),
                    onClick = { y -> onClick(action, y) }
                )
            }
        }

        if (showScrollHint) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFE8DBC0).copy(alpha = 0.95f)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun ActionRow(
    action: Action,
    visible: Boolean,
    isNew: Boolean,
    isUsed: Boolean,
    costHint: String?,
    onClick: (Float) -> Unit
) {
    val contentDisabled = action.disabledReason != null
    val alphaAnim by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(350),
        label = "actionAlpha"
    )

    val rowColor = when {
        contentDisabled -> InkDimColor.copy(alpha = 0.5f)
        isUsed -> InkDimColor
        else -> InkColor
    }

    var yPosition by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = alphaAnim }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    yPosition = coords.positionInRoot().y + coords.size.height / 2f
                }
                .clickable(
                    enabled = visible && !contentDisabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onClick(yPosition) }
                )
                .padding(start = 20.dp, top = 1.dp, bottom = 1.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = if (isUsed) "✓" else "◆",
                color = if (isUsed) BurgundyColor.copy(alpha = 0.5f) else BurgundyColor,
                fontSize = 18.sp,
                modifier = Modifier
                    .padding(end = 10.dp)
                    .width(20.dp)
            )

            Text(
                text = action.text,
                color = rowColor,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = if (isNew) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (!costHint.isNullOrBlank() && !contentDisabled) {
                Text(
                    text = "  $costHint",
                    color = InkDimColor,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }

        if (contentDisabled) {
            Text(
                text = action.disabledReason.orEmpty(),
                color = InkDimColor,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 24.dp, top = 1.dp)
            )
        }
    }
}

@Composable
private fun BookmarksRow(
    onInventory: () -> Unit,
    onJournal: () -> Unit,
    onMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(140.dp)
            .width(170.dp)
    ) {
        AssetImage(
            fileName = "bookmarks.png",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onInventory
                    )
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onJournal
                    )
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onMenu
                    )
            )
        }
    }
}