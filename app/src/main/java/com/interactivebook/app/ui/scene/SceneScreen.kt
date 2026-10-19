package com.interactivebook.app.ui.scene

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.Shadow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interactivebook.app.R
import com.interactivebook.app.core.checks.CheckOutcome
import com.interactivebook.app.core.checks.CheckResolution
import com.interactivebook.app.core.effects.Effect
import com.interactivebook.app.core.model.Action
import com.interactivebook.app.core.model.GameResource
import com.interactivebook.app.core.model.Scene
import com.interactivebook.app.core.settings.RevealSpeed
import com.interactivebook.app.ui.check.CheckRollOverlay
import com.interactivebook.app.ui.check.DiceRollScreen
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
private const val REWARDS_END_PAUSE_MS = 500L

private val InkColor = Color(0xFF3B2A1A)
private val InkDimColor = Color(0xFF6B5A44)
private val DarkBorderColor = Color(0xFF1A110A) // Темно-коричневый, почти черный
private val GoldTextColor = Color(0xFFE8C766)
private val SuccessColor = Color(0xFF4F7A38)
private val FailureColor = Color(0xFF8B2020)

private val ResourcePlateWidth = 113.dp
private val ResourcePlateHeight = 57.dp
private val ResourcePlateNumberSize = 22.sp

private val TopGroupPadding = 85.dp
private val BookmarksOffsetY = (-43).dp
private val PlateOffsetX = (-4).dp

private const val SIDE_PADDING_FRACTION = 0.05f

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
    resources: Map<String, GameResource>,
    resourcesEverSeen: Set<String>,
    onResourceSeen: (String) -> Unit,
    hasUnreadItems: Boolean,
    hasUnreadJournal: Boolean,
    inventoryItemIds: Set<String>,
    informationIds: Set<String>,
    revealedAlready: Boolean,
    revealSpeed: RevealSpeed,
    onRevealComplete: () -> Unit,
    onSimpleAction: (Action) -> Unit,
    onResolveCheck: (Action) -> CheckResolution?,
    onApplyCheckOutcome: (Action, CheckOutcome) -> Unit,
    onApplyConsequence: (Action, CheckOutcome) -> Unit,
    onRewardsAnimationComplete: () -> Unit,
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

    val density = LocalDensity.current
    val screenHeightPx = with(density) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }

    val goldValue = resources["gold"]?.currentValue ?: 0

    var displayedGold by remember { mutableStateOf(goldValue) }
    var previousGold by remember { mutableStateOf<Int?>(goldValue) }
    var previousItemIds by remember { mutableStateOf(inventoryItemIds) }
    var previousInfoIds by remember { mutableStateOf(informationIds) }

    var plateCenter by remember { mutableStateOf(Offset.Zero) }
    var bagCenter by remember { mutableStateOf(Offset.Zero) }
    var journalCenter by remember { mutableStateOf(Offset.Zero) }
    var lastActionY by remember { mutableStateOf(0f) }
    var rewards by remember { mutableStateOf<List<RewardFlight>>(emptyList()) }
    var pendingFinalGold by remember { mutableStateOf<Int?>(null) }
    var pendingMode by remember { mutableStateOf(GoldAnimMode.NONE) }
    var rewardsEndedAt by remember { mutableStateOf<Long?>(null) }

    val inputBlocked = rewards.isNotEmpty()

    fun outsidePoint(): Offset =
        if (lastActionY > 0f) Offset(0f, lastActionY)
        else Offset(0f, screenHeightPx)

    LaunchedEffect(scene.id) {
        if (goldValue > 0 && "gold" !in resourcesEverSeen) {
            displayedGold = 0
            pendingMode = GoldAnimMode.APPEARANCE
            pendingFinalGold = goldValue

            val count = minOf(goldValue, 5)
            val newRewards = (0 until count).map { i ->
                RewardFlight(
                    id = System.nanoTime() + i,
                    kind = RewardKind.COIN,
                    start = Offset(0f, screenHeightPx),
                    end = plateCenter,
                    delayMs = i * 300L
                )
            }
            rewards = rewards + newRewards
            onResourceSeen("gold")
        }
    }

    LaunchedEffect(goldValue) {
        val prev = previousGold
        previousGold = goldValue
        if (prev == null || prev == goldValue) return@LaunchedEffect
        if (pendingMode == GoldAnimMode.APPEARANCE) return@LaunchedEffect

        val step = if (goldValue > prev) 1 else -1
        var current = prev
        while (current != goldValue) {
            current += step
            displayedGold = current
            delay(150)
        }

        val delta = goldValue - prev
        val count = minOf(abs(delta), 5)
        val fromPlate = delta < 0
        val outside = outsidePoint()
        val newRewards = (0 until count).map { i ->
            RewardFlight(
                id = System.nanoTime() + i,
                kind = RewardKind.COIN,
                start = if (fromPlate) plateCenter else outside,
                end = if (fromPlate) outside else plateCenter,
                delayMs = i * 300L
            )
        }
        rewards = rewards + newRewards
        lastActionY = 0f
    }

    LaunchedEffect(inventoryItemIds) {
        val added = inventoryItemIds - previousItemIds
        previousItemIds = inventoryItemIds
        if (added.isEmpty()) return@LaunchedEffect

        val outside = outsidePoint()
        val count = minOf(added.size, 5)
        val newRewards = (0 until count).map { i ->
            RewardFlight(
                id = System.nanoTime() + i,
                kind = RewardKind.ITEM,
                start = outside,
                end = bagCenter,
                delayMs = i * 300L
            )
        }
        rewards = rewards + newRewards
        lastActionY = 0f
    }

    LaunchedEffect(informationIds) {
        val added = informationIds - previousInfoIds
        previousInfoIds = informationIds
        if (added.isEmpty()) return@LaunchedEffect

        val outside = outsidePoint()
        val count = minOf(added.size, 5)
        val newRewards = (0 until count).map { i ->
            RewardFlight(
                id = System.nanoTime() + i,
                kind = RewardKind.SPARK,
                start = outside,
                end = journalCenter,
                delayMs = i * 300L
            )
        }
        rewards = rewards + newRewards
        lastActionY = 0f
    }

    LaunchedEffect(rewards) {
        if (rewards.isEmpty()) {
            if (pendingMode == GoldAnimMode.APPEARANCE) {
                val target = pendingFinalGold
                if (target != null) {
                    displayedGold = target
                }
                pendingMode = GoldAnimMode.NONE
                pendingFinalGold = null
            }
            val ended = rewardsEndedAt
            if (ended != null) {
                val elapsed = System.currentTimeMillis() - ended
                val remaining = REWARDS_END_PAUSE_MS - elapsed
                if (remaining > 0) delay(remaining)
                rewardsEndedAt = null
                onRewardsAnimationComplete()
            }
        } else {
            rewardsEndedAt = System.currentTimeMillis()
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
            .pointerInput(scene.id, stage, inputBlocked) {
                if (inputBlocked) return@pointerInput
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

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.25f)
                ) {
                    val sidePadding = maxWidth * SIDE_PADDING_FRACTION
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = sidePadding)
                            .graphicsLayer { alpha = illustrationAlpha.value },
                        contentAlignment = Alignment.Center
                    ) {
                        IllustrationWithFrame()
                    }
                }

                Spacer(Modifier.height(8.dp))

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.50f)
                ) {
                    val sidePadding = maxWidth * SIDE_PADDING_FRACTION
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = sidePadding)
                    ) {
                        scene.consequenceTitle?.let { title ->
                            ConsequenceTitle(title)
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            TextArea(
                                sceneId = scene.id,
                                displayText = displayText,
                                revealedChars = revealedChars,
                                stage = stage
                            )
                        }
                    }
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
                        inputBlocked = inputBlocked,
                        onClick = { action, y ->
                            lastActionY = y
                            onActionClicked(action)
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = TopGroupPadding, end = 0.dp),
            horizontalAlignment = Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .offset(x = PlateOffsetX)
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
                val gold = resources["gold"]
                if (gold != null) {
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
            }

            Box(modifier = Modifier.offset(y = BookmarksOffsetY)) {
                BookmarksRow(
                    hasUnreadItems = hasUnreadItems,
                    hasUnreadJournal = hasUnreadJournal,
                    inputBlocked = inputBlocked,
                    onInventory = onOpenInventory,
                    onJournal = onOpenJournal,
                    onMenu = onOpenMainMenu,
                    onBagPositioned = { bagCenter = it },
                    onJournalPositioned = { journalCenter = it }
                )
            }
        }

        FlyingRewardsLayer(
            rewards = rewards,
            onRewardFinished = { id -> rewards = rewards.filterNot { it.id == id } }
        )
    }

    pendingCheck?.let { (action, resolution) ->
        if (action.consequences != null) {
            DiceRollScreen(
                resolution = resolution,
                pageTurnEnabled = true,
                onFinished = {
                    val outcome = resolution.outcome
                    pendingCheck = null
                    onApplyConsequence(action, outcome)
                }
            )
        } else {
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

private enum class GoldAnimMode { NONE, APPEARANCE }

@Composable
private fun ConsequenceTitle(rawTitle: String) {
    val idx = rawTitle.indexOf(": ")
    val (actionPart, outcomePart) = if (idx > 0) {
        rawTitle.substring(0, idx) to rawTitle.substring(idx + 2)
    } else {
        rawTitle to ""
    }

    val actionDisplay = actionPart.lowercase().replaceFirstChar { it.uppercase() }
    val outcomeDisplay = outcomePart.lowercase().replaceFirstChar { it.uppercase() }

    val isSuccess = outcomeDisplay.contains("успех", ignoreCase = true)
    val outcomeColor = if (isSuccess) SuccessColor else FailureColor

    val outcomeShadow = Shadow(
        color = Color.Black.copy(alpha = 0.55f),
        offset = Offset(1f, 1f),
        blurRadius = 3f
    )

    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = InkColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(actionDisplay)
            }
            if (outcomeDisplay.isNotEmpty()) {
                withStyle(
                    SpanStyle(
                        color = InkColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(": ")
                }
                withStyle(
                    SpanStyle(
                        color = outcomeColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        shadow = outcomeShadow
                    )
                ) {
                    append(outcomeDisplay)
                }
            }
        },
        textAlign = TextAlign.Center,
        lineHeight = 24.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    )
}

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
                    width = 5.dp,
                    color = DarkBorderColor.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}

@Composable
private fun TextArea(
    sceneId: String,
    displayText: String,
    revealedChars: Int,
    stage: RevealStage
) {
    val scrollState = remember(sceneId) { ScrollState(0) }

    val shown = if (stage == RevealStage.DONE) displayText.length
    else revealedChars.coerceAtMost(displayText.length)

    LaunchedEffect(shown) {
        if (scrollState.maxValue > 0 && scrollState.value < scrollState.maxValue) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    val canScrollUp = scrollState.canScrollBackward
    val canScrollDown = scrollState.canScrollForward

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()

                val topAlpha = if (canScrollUp) 0.45f else 1f
                val bottomAlpha = if (canScrollDown) 0.45f else 1f

                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = topAlpha),
                            0.10f to Color.Black,
                            0.90f to Color.Black,
                            1f to Color.Black.copy(alpha = bottomAlpha)
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
            Text(
                text = displayText.substring(0, shown),
                color = InkColor,
                fontSize = 15.sp,
                lineHeight = 21.sp
            )
        }

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
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val sidePadding = maxWidth * SIDE_PADDING_FRACTION
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = sidePadding) // ДОБАВИЛИ ОТСТУПЫ КАК У ТЕКСТА
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .background(DarkBorderColor.copy(alpha = 0.85f))
            )
            Text(
                text = "◆",
                color = DarkBorderColor,
                fontSize = 22.sp,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .background(DarkBorderColor.copy(alpha = 0.85f))
            )
        }
    }
}

@Composable
private fun ActionsList(
    actions: List<Action>,
    shownActions: Int,
    stage: RevealStage,
    isNew: (Action) -> Boolean,
    isUsed: (Action) -> Boolean,
    inputBlocked: Boolean,
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
                    inputBlocked = inputBlocked,
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
    inputBlocked: Boolean,
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
                    enabled = visible && !contentDisabled && !inputBlocked,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onClick(yPosition) }
                )
                .padding(start = 20.dp, top = 1.dp, bottom = 1.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = if (isUsed) "✓" else "◆",
                color = if (isUsed) DarkBorderColor.copy(alpha = 0.5f) else DarkBorderColor,
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
    hasUnreadItems: Boolean,
    hasUnreadJournal: Boolean,
    inputBlocked: Boolean,
    onInventory: () -> Unit,
    onJournal: () -> Unit,
    onMenu: () -> Unit,
    onBagPositioned: (Offset) -> Unit,
    onJournalPositioned: (Offset) -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "bookmarkPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .height(130.dp)
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
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        onBagPositioned(
                            Offset(
                                pos.x + coords.size.width / 2f,
                                pos.y + coords.size.height / 2f
                            )
                        )
                    }
                    .graphicsLayer {
                        alpha = if (hasUnreadItems) pulseAlpha * 0.35f else 0f
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(GoldTextColor.copy(alpha = 0.9f), Color.Transparent)
                        )
                    )
                    .clickable(
                        enabled = !inputBlocked,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onInventory
                    )
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        onJournalPositioned(
                            Offset(
                                pos.x + coords.size.width / 2f,
                                pos.y + coords.size.height / 2f
                            )
                        )
                    }
                    .graphicsLayer {
                        alpha = if (hasUnreadJournal) pulseAlpha * 0.35f else 0f
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(GoldTextColor.copy(alpha = 0.9f), Color.Transparent)
                        )
                    )
                    .clickable(
                        enabled = !inputBlocked,
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
                        enabled = !inputBlocked,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onMenu
                    )
            )
        }
    }
}