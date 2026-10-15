package com.interactivebook.app.ui.check

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interactivebook.app.core.checks.CheckModifierBreakdown
import com.interactivebook.app.core.checks.CheckOutcome
import com.interactivebook.app.core.checks.CheckResolution
import com.interactivebook.app.ui.common.AssetImage
import com.interactivebook.app.ui.common.BookPageBackground
import com.interactivebook.app.ui.common.BookPageContent
import com.interactivebook.app.ui.common.BookVideoPlayer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private val InkColor = Color(0xFF3B2A1A)
private val InkDimColor = Color(0xFF6B5A44)
private val BronzeColor = Color(0xFFC8A46A)
private val GreenColor = Color(0xFF4F7A38)
private val RedColor = Color(0xFF8B2020)
private val ContinueTextColor = Color(0xFFE8DBC0)

// Цвет фантомов при полёте — тёмно-коричневый, отличается от оригинала и от чернил.
private val PhantomColor = Color(0xFF3E2810)

// Бордовый — для линий крит-провала.
private val BurgundyColor = Color(0xFF7A2525)

private val NumberFontSize = 29.sp
private val OutcomeFontSize = 22.sp
private val OutcomeFontSizeCrit = 24.sp

private val DiceNumberOffsetY = (-20).dp
private val ModifiersBlockOffsetY = 140.dp
private val ContinueTextBottomPadding = 55.dp
private val DiceScale = 1.2f

private const val CRIT_SPARK_COUNT = 16
private const val CRIT_CRACK_COUNT = 8

private enum class CrumbleTarget { NONE, DC, TOTAL_PHANTOM }
private enum class DicePhase { PAGE_TURN, DICE_VIDEO, ROLL, PAGE_TURN_OUT }

@Composable
fun DiceRollScreen(
    resolution: CheckResolution,
    pageTurnEnabled: Boolean,
    onFinished: () -> Unit
) {
    var phase by remember {
        mutableStateOf(if (pageTurnEnabled) DicePhase.PAGE_TURN else DicePhase.DICE_VIDEO)
    }
    val diceVideoFile = remember { "dice_roll_${Random.nextInt(1, 4)}.mp4" }

    when (phase) {
        DicePhase.PAGE_TURN -> BookVideoPlayer(
            fileName = "page_turn.mp4",
            allowSkip = false,
            onFinished = { phase = DicePhase.DICE_VIDEO }
        )
        DicePhase.DICE_VIDEO -> BookVideoPlayer(
            fileName = diceVideoFile,
            allowSkip = false,
            onFinished = { phase = DicePhase.ROLL }
        )
        DicePhase.ROLL -> RollResultScreen(
            resolution = resolution,
            onFinished = { phase = DicePhase.PAGE_TURN_OUT }
        )
        DicePhase.PAGE_TURN_OUT -> {
            if (pageTurnEnabled) {
                BookVideoPlayer(
                    fileName = "page_turn.mp4",
                    allowSkip = false,
                    onFinished = onFinished
                )
            } else {
                LaunchedEffect(Unit) { onFinished() }
            }
        }
    }
}

@Composable
private fun RollResultScreen(
    resolution: CheckResolution,
    onFinished: () -> Unit
) {
    val isCritSuccess = resolution.outcome == CheckOutcome.CRITICAL_SUCCESS
    val isCritFailure = resolution.outcome == CheckOutcome.CRITICAL_FAILURE
    val isCrit = isCritSuccess || isCritFailure
    val isSuccess = resolution.outcome == CheckOutcome.SUCCESS || isCritSuccess

    var showTitle by remember { mutableStateOf(false) }
    var showRollNumber by remember { mutableStateOf(false) }
    var modifiersShown by remember { mutableIntStateOf(0) }
    var showTotal by remember { mutableStateOf(false) }
    var totalValue by remember { mutableIntStateOf(resolution.rawRoll) }
    var showOutcome by remember { mutableStateOf(false) }
    var showContinue by remember { mutableStateOf(false) }

    val diceToTotal = remember { Animatable(0f) }
    val modifierFlies = remember(resolution.modifiers.size) {
        List(resolution.modifiers.size) { Animatable(0f) }
    }
    val totalToDc = remember { Animatable(0f) }
    val shakePhase = remember { Animatable(0f) }
    val crumbleProgress = remember { Animatable(0f) }
    var crumbleTarget by remember { mutableStateOf(CrumbleTarget.NONE) }

    // Прогресс анимации крита: искры при 20, трещины при 1.
    val critEffect = remember { Animatable(0f) }

    var containerRootOffset by remember { mutableStateOf(Offset.Zero) }
    var diceNumberCenter by remember { mutableStateOf(Offset.Zero) }
    var diceNumberTopLeft by remember { mutableStateOf(Offset.Zero) }
    var totalNumberTopLeft by remember { mutableStateOf(Offset.Zero) }
    var dcNumberTopLeft by remember { mutableStateOf(Offset.Zero) }
    var modifierTopLefts by remember {
        mutableStateOf(List(resolution.modifiers.size) { Offset.Zero })
    }

    LaunchedEffect(Unit) {
        delay(200)
        showTitle = true
        delay(400)
        showRollNumber = true

        if (isCrit) {
            delay(600)
            launch {
                critEffect.snapTo(0f)
                critEffect.animateTo(1f, tween(1200))
            }
            delay(400)
            showOutcome = true
            delay(800)
            showContinue = true
            return@LaunchedEffect
        }

        delay(400)
        for (i in resolution.modifiers.indices) {
            modifiersShown = i + 1
            delay(400)
        }

        showTotal = true
        delay(300)

        diceToTotal.snapTo(0f)
        diceToTotal.animateTo(1f, tween(800))
        delay(200)

        for (i in resolution.modifiers.indices) {
            modifierFlies[i].snapTo(0f)
            modifierFlies[i].animateTo(1f, tween(500))
            totalValue += resolution.modifiers[i].value
            delay(220)
        }

        delay(400)

        totalToDc.snapTo(0f)
        totalToDc.animateTo(1f, tween(800))
        delay(150)

        crumbleTarget = if (isSuccess) CrumbleTarget.DC else CrumbleTarget.TOTAL_PHANTOM
        shakePhase.snapTo(0f)
        shakePhase.animateTo(1f, tween(400))
        crumbleProgress.snapTo(0f)
        crumbleProgress.animateTo(1f, tween(600))

        delay(150)
        showOutcome = true
        delay(500)
        showContinue = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BookPageBackground()

        if (showContinue) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onFinished
                    )
            )
        }

        BookPageContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { containerRootOffset = it.positionInRoot() }
            ) {

                AssetImage(
                    fileName = "dice.png",
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = DiceScale
                            scaleY = DiceScale
                        },
                    contentScale = ContentScale.FillBounds
                )

                // Число на кубике — обычный текст, без обводки, без увеличения.
                if (showRollNumber) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = DiceNumberOffsetY)
                            .onGloballyPositioned { coords ->
                                val p = coords.positionInRoot()
                                diceNumberTopLeft = p
                                diceNumberCenter = Offset(
                                    p.x + coords.size.width / 2f,
                                    p.y + coords.size.height / 2f
                                )
                            }
                    ) {
                        Text(
                            text = resolution.rawRoll.toString(),
                            color = BronzeColor,
                            fontSize = NumberFontSize,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Заголовок + DC + результат.
                if (showTitle) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = resolution.actionTitle.trimEnd('.', ' '),
                            color = InkColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Сложность",
                            color = InkColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        )

                        val dcIsCrumbleTarget = crumbleTarget == CrumbleTarget.DC
                        val dcShake = if (dcIsCrumbleTarget) {
                            sin(shakePhase.value * 40f) * 10f * (1f - crumbleProgress.value)
                        } else 0f
                        val dcAlpha = if (dcIsCrumbleTarget) {
                            (1f - crumbleProgress.value * 0.65f).coerceIn(0f, 1f)
                        } else 1f

                        Text(
                            text = resolution.difficulty.toString(),
                            color = InkColor,
                            fontSize = NumberFontSize,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .onGloballyPositioned { coords ->
                                    dcNumberTopLeft = coords.positionInRoot()
                                }
                                .graphicsLayer {
                                    rotationZ = dcShake
                                    alpha = dcAlpha
                                }
                        )

                        if (showOutcome) {
                            Text(
                                text = outcomeText(resolution.outcome),
                                color = outcomeColor(resolution.outcome),
                                fontSize = if (isCrit) OutcomeFontSizeCrit else OutcomeFontSize,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            )
                        }
                    }
                }

                // Модификаторы + ИТОГ.
                if (!isCrit && (modifiersShown > 0 || showTotal)) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = ModifiersBlockOffsetY),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        resolution.modifiers.forEachIndexed { index, m ->
                            if (index < modifiersShown) {
                                Box(
                                    modifier = Modifier
                                        .onGloballyPositioned { coords ->
                                            if (index < modifierTopLefts.size) {
                                                val newList = modifierTopLefts.toMutableList()
                                                newList[index] = coords.positionInRoot()
                                                modifierTopLefts = newList
                                            }
                                        }
                                ) {
                                    ModifierLine(modifier = m)
                                }
                            }
                        }

                        if (showTotal) {
                            Spacer(Modifier.height(16.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "ИТОГ",
                                    color = InkColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = totalValue.toString(),
                                    color = if (showOutcome) {
                                        if (isSuccess) GreenColor else RedColor
                                    } else InkColor,
                                    fontSize = NumberFontSize,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .onGloballyPositioned { coords ->
                                            totalNumberTopLeft = coords.positionInRoot()
                                        }
                                )
                            }
                        }
                    }
                }

                val container = containerRootOffset

                // Фантом числа кубика — тёмно-коричневый.
                if (!isCrit &&
                    diceToTotal.value > 0f &&
                    diceNumberTopLeft != Offset.Zero &&
                    totalNumberTopLeft != Offset.Zero
                ) {
                    val startLocal = diceNumberTopLeft - container
                    val endLocal = totalNumberTopLeft - container
                    Box(
                        modifier = Modifier
                            .offset {
                                val t = diceToTotal.value
                                val x = startLocal.x + (endLocal.x - startLocal.x) * t
                                val y = startLocal.y + (endLocal.y - startLocal.y) * t
                                IntOffset(x.roundToInt(), y.roundToInt())
                            }
                            .graphicsLayer {
                                alpha = (1f - diceToTotal.value * 1.1f).coerceIn(0f, 1f)
                            }
                    ) {
                        Text(
                            text = resolution.rawRoll.toString(),
                            color = PhantomColor,
                            fontSize = NumberFontSize,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Фантомы модификаторов — тёмно-коричневые.
                if (!isCrit && totalNumberTopLeft != Offset.Zero) {
                    resolution.modifiers.forEachIndexed { index, m ->
                        val fly = modifierFlies.getOrNull(index)
                        if (fly != null && fly.value > 0f) {
                            val startRoot = modifierTopLefts.getOrNull(index) ?: Offset.Zero
                            if (startRoot != Offset.Zero) {
                                val startLocal = startRoot - container
                                val endLocal = totalNumberTopLeft - container
                                val phantomSigned = when {
                                    m.value > 0 -> "+ ${m.value}"
                                    m.value < 0 -> "- ${-m.value}"
                                    else -> "0"
                                }
                                Box(
                                    modifier = Modifier
                                        .offset {
                                            val t = fly.value
                                            val x = startLocal.x + (endLocal.x - startLocal.x) * t
                                            val y = startLocal.y + (endLocal.y - startLocal.y) * t
                                            IntOffset(x.roundToInt(), y.roundToInt())
                                        }
                                        .graphicsLayer {
                                            alpha = (1f - fly.value * 1.1f).coerceIn(0f, 1f)
                                        }
                                ) {
                                    Text(
                                        text = "${m.label}: $phantomSigned",
                                        color = PhantomColor,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Фантом ИТОГА — тёмно-коричневый.
                if (!isCrit &&
                    showTotal &&
                    totalToDc.value > 0f &&
                    totalNumberTopLeft != Offset.Zero &&
                    dcNumberTopLeft != Offset.Zero
                ) {
                    val startLocal = totalNumberTopLeft - container
                    val endLocal = dcNumberTopLeft - container
                    val isTarget = crumbleTarget == CrumbleTarget.TOTAL_PHANTOM
                    val fly = totalToDc
                    Box(
                        modifier = Modifier
                            .offset {
                                val t = fly.value
                                val x = startLocal.x + (endLocal.x - startLocal.x) * t
                                val y = startLocal.y + (endLocal.y - startLocal.y) * t
                                IntOffset(x.roundToInt(), y.roundToInt())
                            }
                            .graphicsLayer {
                                val t = fly.value
                                rotationZ = if (isTarget && t >= 0.99f) {
                                    sin(shakePhase.value * 40f) * 10f * (1f - crumbleProgress.value)
                                } else 0f
                                alpha = when {
                                    isTarget -> (1f - crumbleProgress.value).coerceIn(0f, 1f)
                                    isSuccess && t >= 0.99f -> (1f - crumbleProgress.value).coerceIn(0f, 1f)
                                    else -> 1f
                                }
                            }
                    ) {
                        Text(
                            text = totalValue.toString(),
                            color = PhantomColor,
                            fontSize = NumberFontSize,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Частицы распада (не крит).
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val progress = crumbleProgress.value
                    if (progress <= 0f || crumbleTarget == CrumbleTarget.NONE) return@Canvas
                    val centerRoot = when (crumbleTarget) {
                        CrumbleTarget.DC -> dcNumberTopLeft + Offset(22f, 28f)
                        CrumbleTarget.TOTAL_PHANTOM -> dcNumberTopLeft + Offset(22f, 28f)
                        CrumbleTarget.NONE -> Offset.Zero
                    }
                    val localCenter = centerRoot - containerRootOffset
                    val count = 12
                    for (i in 0 until count) {
                        val angle = (i.toFloat() / count) * 2f * PI.toFloat()
                        val distance = progress * 200f
                        val px = localCenter.x + cos(angle) * distance
                        val py = localCenter.y + sin(angle) * distance
                        val alpha = (1f - progress).coerceIn(0f, 1f)
                        val radius = 9f * (1f - progress * 0.5f)
                        drawCircle(
                            color = Color(0xFF3B2A1A).copy(alpha = alpha * 0.85f),
                            radius = radius,
                            center = Offset(px, py)
                        )
                    }
                }

                // Крит-эффекты: искры (успех) или трещины (провал) вокруг числа.
                if (isCrit && diceNumberCenter != Offset.Zero) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val progress = critEffect.value
                        if (progress <= 0f) return@Canvas
                        val localCenter = diceNumberCenter - containerRootOffset

                        if (isCritSuccess) {
                            // Искры — золотые точки, расходящиеся от числа.
                            for (i in 0 until CRIT_SPARK_COUNT) {
                                val angle = (i.toFloat() / CRIT_SPARK_COUNT) * 2f * PI.toFloat()
                                val distance = progress * 160f
                                val px = localCenter.x + cos(angle) * distance
                                val py = localCenter.y + sin(angle) * distance
                                val alpha = (1f - progress).coerceIn(0f, 1f)
                                val radius = 6f * (1f - progress * 0.4f)
                                drawCircle(
                                    color = Color(0xFFFFE08A).copy(alpha = alpha),
                                    radius = radius,
                                    center = Offset(px, py)
                                )
                                drawCircle(
                                    color = Color(0xFFFFC24A).copy(alpha = alpha * 0.6f),
                                    radius = radius * 0.5f,
                                    center = Offset(
                                        px + cos(angle + 0.5f) * 10f,
                                        py + sin(angle + 0.5f) * 10f
                                    )
                                )
                            }
                        } else {
                            // Трещины — бордовые линии, толстые, расходящиеся от числа.
                            for (i in 0 until CRIT_CRACK_COUNT) {
                                val angle = (i.toFloat() / CRIT_CRACK_COUNT) * 2f * PI.toFloat() + 0.35f
                                val inner = 18f
                                val length = progress * 130f
                                val x1 = localCenter.x + cos(angle) * inner
                                val y1 = localCenter.y + sin(angle) * inner
                                val x2 = localCenter.x + cos(angle) * (inner + length)
                                val y2 = localCenter.y + sin(angle) * (inner + length)
                                drawLine(
                                    color = BurgundyColor.copy(
                                        alpha = (1f - progress * 0.35f).coerceIn(0f, 1f)
                                    ),
                                    start = Offset(x1, y1),
                                    end = Offset(x2, y2),
                                    strokeWidth = 7f
                                )
                            }
                        }
                    }
                }

                // Нажмите для продолжения.
                if (showContinue) {
                    Text(
                        text = "Нажмите для продолжения",
                        color = ContinueTextColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .offset(y = ContinueTextBottomPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModifierLine(modifier: CheckModifierBreakdown) {
    val valueColor = when {
        modifier.value > 0 -> GreenColor
        modifier.value < 0 -> RedColor
        else -> InkDimColor
    }
    val signed = when {
        modifier.value > 0 -> "+ ${modifier.value}"
        modifier.value < 0 -> "- ${-modifier.value}"
        else -> "0"
    }

    Row(
        modifier = Modifier.padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${modifier.label}: ",
            color = InkColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = signed,
            color = valueColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun outcomeText(outcome: CheckOutcome): String = when (outcome) {
    CheckOutcome.CRITICAL_SUCCESS -> "КРИТИЧЕСКИЙ УСПЕХ"
    CheckOutcome.SUCCESS -> "УСПЕХ"
    CheckOutcome.FAILURE -> "ПРОВАЛ"
    CheckOutcome.CRITICAL_FAILURE -> "КРИТИЧЕСКИЙ ПРОВАЛ"
}

private fun outcomeColor(outcome: CheckOutcome): Color = when (outcome) {
    CheckOutcome.CRITICAL_SUCCESS -> GreenColor
    CheckOutcome.SUCCESS -> GreenColor
    CheckOutcome.FAILURE -> RedColor
    CheckOutcome.CRITICAL_FAILURE -> RedColor
}