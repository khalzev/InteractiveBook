package com.interactivebook.app.ui.check

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.interactivebook.app.core.checks.CheckModifierBreakdown
import com.interactivebook.app.core.checks.CheckOutcome
import com.interactivebook.app.core.checks.CheckResolution
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val Gold = Color(0xFFE8C766)
private val GoldDark = Color(0xFF8A6A2B)
private val Green = Color(0xFF6FCF7A)
private val Red = Color(0xFFE5645A)
private val Parchment = Color(0xFFF3E6C4)
private val PlateBg = Color(0xFF2B2118)

private val PlateWidth = 92.dp
private val PlateHeight = 76.dp
private val DiceSize = 130.dp

// ---------------------------------------------------------------------------------------------
// Частицы: искры и осколки
// ---------------------------------------------------------------------------------------------

private class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val size: Float,
    val color: Color,
    val shard: Boolean,
    var rotation: Float,
    val spin: Float,
    val gravity: Float
)

private class ParticleField {
    val items = ArrayList<Particle>()

    fun burst(
        x: Float,
        y: Float,
        count: Int,
        colors: List<Color>,
        speed: Float,
        size: Float,
        shard: Boolean
    ) {
        repeat(count) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val v = speed * (0.4f + Random.nextFloat() * 0.8f)
            val life = 0.6f + Random.nextFloat() * 0.7f
            items.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * v,
                    vy = sin(angle) * v - (if (shard) speed * 0.3f else 0f),
                    life = life,
                    maxLife = life,
                    size = size * (0.6f + Random.nextFloat() * 0.8f),
                    color = colors[Random.nextInt(colors.size)],
                    shard = shard,
                    rotation = Random.nextFloat() * 360f,
                    spin = (Random.nextFloat() - 0.5f) * 720f,
                    gravity = if (shard) 1500f else 260f
                )
            )
        }
    }

    fun step(dt: Float) {
        val iterator = items.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.life -= dt
            if (p.life <= 0f) {
                iterator.remove()
                continue
            }
            p.vy += p.gravity * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.rotation += p.spin * dt
        }
    }
}

private fun DrawScope.drawParticles(field: ParticleField) {
    for (p in field.items) {
        val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
        if (p.shard) {
            rotate(degrees = p.rotation, pivot = Offset(p.x, p.y)) {
                val path = Path().apply {
                    moveTo(p.x - p.size, p.y + p.size * 0.6f)
                    lineTo(p.x + p.size * 0.2f, p.y - p.size)
                    lineTo(p.x + p.size, p.y + p.size * 0.8f)
                    close()
                }
                drawPath(path, color = p.color.copy(alpha = alpha))
            }
        } else {
            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.size * alpha.coerceAtLeast(0.3f),
                center = Offset(p.x, p.y)
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Оверлей проверки
// ---------------------------------------------------------------------------------------------

private fun centerOf(c: LayoutCoordinates): Offset =
    c.positionInRoot() + Offset(c.size.width / 2f, c.size.height / 2f)

/**
 * Анимация проверки поверх сцены (сама сцена под ней размывается в SceneScreen):
 * кубик вылетает, крутится и фиксируется в центре → число фиксируется → по очереди
 * влетают модификаторы (характеристика, навыки) и прибавляются к значению кубика →
 * итог фиксируется со вспышкой искр → итог сталкивается со сложностью (DC): при успехе
 * разбивается DC, при неудаче — разбивается итог.
 *
 * Итог уже вычислен движком (CheckResolver) — этот компонент только визуализирует его
 * (анимация полностью отделена от игровой логики, раздел 26).
 */
@Composable
fun CheckRollOverlay(
    resolution: CheckResolution,
    onContinue: () -> Unit
) {
    val density = LocalDensity.current
    val field = remember { ParticleField() }
    var frameTick by remember { mutableLongStateOf(0L) }

    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    var diceCenter by remember { mutableStateOf(Offset.Zero) }
    var totalCenter by remember { mutableStateOf(Offset.Zero) }
    var dcCenter by remember { mutableStateOf(Offset.Zero) }

    val scrim = remember { Animatable(0f) }
    var titleVisible by remember { mutableStateOf(false) }

    val diceX = remember { Animatable(0f) }
    val diceY = remember { Animatable(0f) }
    val diceRot = remember { Animatable(0f) }
    val diceScale = remember { Animatable(0f) }
    var diceValue by remember { mutableIntStateOf(1) }
    var diceSettled by remember { mutableStateOf(false) }

    val chipAppear = remember { resolution.modifiers.map { Animatable(0f) } }
    val chipFly = remember { resolution.modifiers.map { Animatable(0f) } }

    val resultAlpha = remember { Animatable(0f) }
    val totalShift = remember { Animatable(0f) }
    val totalPulse = remember { Animatable(1f) }
    val dcShake = remember { Animatable(0f) }
    var totalBroken by remember { mutableStateOf(false) }
    var dcBroken by remember { mutableStateOf(false) }
    var showVersus by remember { mutableStateOf(true) }

    var outcomeVisible by remember { mutableStateOf(false) }
    var continueVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Цикл кадров для частиц.
        launch {
            var last = withFrameNanos { it }
            while (isActive) {
                val now = withFrameNanos { it }
                val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                last = now
                field.step(dt)
                frameTick = now
            }
        }

        val size = snapshotFlow { overlaySize }.first { it.width > 0 && it.height > 0 }
        val w = size.width.toFloat()
        val h = size.height.toFloat()
        val plateWidthPx = with(density) { PlateWidth.toPx() }
        val isCrit = resolution.outcome == CheckOutcome.CRITICAL_SUCCESS ||
            resolution.outcome == CheckOutcome.CRITICAL_FAILURE
        val critMul = if (isCrit) 1.6f else 1f

        // 1. Затемнение, заголовок.
        scrim.animateTo(1f, tween(300))
        titleVisible = true
        delay(300)

        // 2. Бросок: кубик вылетает и крутится, число мелькает.
        diceX.snapTo(w * 0.55f)
        diceY.snapTo(h * 0.5f)
        diceRot.snapTo(0f)
        diceScale.snapTo(0.45f)
        val cycler = launch {
            while (isActive) {
                diceValue = Random.nextInt(1, 21)
                delay(55)
            }
        }
        val throwEasing = CubicBezierEasing(0.15f, 0.75f, 0.25f, 1f)
        coroutineScope {
            launch { diceX.animateTo(0f, tween(1000, easing = throwEasing)) }
            launch { diceY.animateTo(0f, tween(1000, easing = throwEasing)) }
            launch { diceRot.animateTo(1080f, tween(1000, easing = throwEasing)) }
            launch { diceScale.animateTo(1f, tween(1000, easing = throwEasing)) }
        }
        cycler.cancel()

        // 3. Кубик зафиксировался: число, толчок, искры.
        diceValue = resolution.rawRoll
        diceSettled = true
        val diceRel = diceCenter - overlayOrigin
        val landColors = when (resolution.rawRoll) {
            20 -> listOf(Gold, Color.White, GoldDark)
            1 -> listOf(Red, Color(0xFF7A2A2A), Color.White)
            else -> listOf(Color(0xFFD9C8A0), Color.White)
        }
        val landCount = if (resolution.rawRoll == 20 || resolution.rawRoll == 1) 36 else 16
        field.burst(diceRel.x, diceRel.y, landCount, landColors, 420f, 5f, false)
        coroutineScope {
            launch {
                diceScale.animateTo(1.2f, tween(90))
                diceScale.animateTo(1f, tween(170))
            }
            launch {
                diceY.animateTo(-36f, tween(110))
                diceY.animateTo(0f, tween(150, easing = FastOutLinearInEasing))
            }
        }
        delay(600)

        // 4. Модификаторы влетают в кубик по одному и прибавляются.
        var running = resolution.rawRoll
        resolution.modifiers.forEachIndexed { i, m ->
            chipAppear[i].animateTo(1f, tween(260))
            delay(420)
            chipFly[i].animateTo(1f, tween(320))
            running += m.value
            diceValue = running
            if (m.value != 0) {
                field.burst(
                    diceRel.x, diceRel.y, 12,
                    listOf(if (m.value > 0) Green else Red, Color.White),
                    280f, 4f, false
                )
            }
            diceScale.animateTo(1.15f, tween(80))
            diceScale.animateTo(1f, tween(120))
            delay(140)
        }

        // 5. Итог зафиксирован — вспышка искр.
        delay(250)
        diceValue = resolution.total
        field.burst(diceRel.x, diceRel.y, 44, listOf(Gold, Color.White, GoldDark), 560f, 6f, false)
        diceScale.animateTo(1.25f, tween(120))
        diceScale.animateTo(1f, tween(200))
        delay(500)

        // 6. Итог и сложность рядом.
        coroutineScope {
            launch { resultAlpha.animateTo(1f, tween(350)) }
            launch { diceScale.animateTo(0f, tween(350)) }
        }
        delay(700)

        // 7. Столкновение.
        showVersus = false
        val dcRel = dcCenter - overlayOrigin
        val distance = (dcCenter.x - totalCenter.x) - plateWidthPx
        totalShift.animateTo(distance, tween(380, easing = FastOutLinearInEasing))
        val impact = Offset(dcRel.x - plateWidthPx / 2f, dcRel.y)

        val success = resolution.outcome == CheckOutcome.SUCCESS ||
            resolution.outcome == CheckOutcome.CRITICAL_SUCCESS
        if (success) {
            // Сложность разбивается.
            dcBroken = true
            field.burst(
                dcRel.x, dcRel.y, (26 * critMul).toInt(),
                listOf(GoldDark, Gold, Color(0xFF5A4A2A)), 720f, 10f, true
            )
            field.burst(
                impact.x, impact.y, (32 * critMul).toInt(),
                listOf(Gold, Color.White), 640f, 6f, false
            )
            totalPulse.animateTo(1.2f, tween(100))
            totalPulse.animateTo(1f, tween(160))
        } else {
            // Итог разбивается о сложность.
            totalBroken = true
            field.burst(
                impact.x, impact.y, (26 * critMul).toInt(),
                listOf(Red, Color(0xFF7A2A2A), Color(0xFFCCCCCC)), 720f, 10f, true
            )
            field.burst(
                impact.x, impact.y, (20 * critMul).toInt(),
                listOf(Color(0xFFFF9A6A), Color.White), 520f, 5f, false
            )
            repeat(4) { n ->
                dcShake.animateTo(if (n % 2 == 0) 14f else -14f, tween(45))
            }
            dcShake.animateTo(0f, tween(60))
        }

        delay(300)
        outcomeVisible = true
        delay(600)
        continueVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { overlaySize = it }
            .onGloballyPositioned { overlayOrigin = it.positionInRoot() }
            .pointerInput(Unit) { detectTapGestures { } } // блокируем нажатия на сцену под оверлеем
    ) {
        // Затемнение.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrim.value }
                .background(Color(0xCC0B0805))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(visible = titleVisible, enter = fadeIn(tween(400))) {
                Text(
                    text = resolution.actionTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = Parchment,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(16.dp))

            // Кубик. Внешний Box неподвижен (по нему берём позицию), анимируется внутренний.
            Box(
                modifier = Modifier
                    .size(DiceSize)
                    .onGloballyPositioned { diceCenter = centerOf(it) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = diceX.value
                            translationY = diceY.value
                            rotationZ = diceRot.value
                            scaleX = diceScale.value
                            scaleY = diceScale.value
                        },
                    contentAlignment = Alignment.Center
                ) {
                    DiceFace(
                        glow = when {
                            !diceSettled -> null
                            resolution.rawRoll == 20 -> Gold
                            resolution.rawRoll == 1 -> Red
                            else -> null
                        }
                    )
                    Text(
                        text = diceValue.toString(),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 40.sp),
                        fontWeight = FontWeight.Bold,
                        color = Parchment
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Модификаторы.
            resolution.modifiers.forEachIndexed { i, m ->
                ModifierChip(
                    modifier = m,
                    appear = { chipAppear[i].value },
                    fly = { chipFly[i].value },
                    distancePx = with(density) { (110 + 46 * i).dp.toPx() }
                )
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(14.dp))

            // Итог против сложности.
            Row(
                modifier = Modifier.graphicsLayer { alpha = resultAlpha.value },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(PlateWidth, PlateHeight)
                        .onGloballyPositioned { totalCenter = centerOf(it) }
                ) {
                    Plate(
                        label = "ИТОГ",
                        value = resolution.total.toString(),
                        accent = Gold,
                        modifier = Modifier.graphicsLayer {
                            translationX = totalShift.value
                            scaleX = totalPulse.value
                            scaleY = totalPulse.value
                            alpha = if (totalBroken) 0f else 1f
                        }
                    )
                }
                Text(
                    text = "против",
                    style = MaterialTheme.typography.labelLarge,
                    color = Parchment.copy(alpha = if (showVersus) 0.8f else 0f)
                )
                Box(
                    modifier = Modifier
                        .size(PlateWidth, PlateHeight)
                        .onGloballyPositioned { dcCenter = centerOf(it) }
                ) {
                    Plate(
                        label = "DC",
                        value = resolution.difficulty.toString(),
                        accent = Color(0xFFB9B2A4),
                        modifier = Modifier.graphicsLayer {
                            translationX = dcShake.value
                            alpha = if (dcBroken) 0f else 1f
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            AnimatedVisibility(visible = outcomeVisible, enter = fadeIn() + scaleIn()) {
                Text(
                    text = outcomeLabel(resolution.outcome),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = outcomeColor(resolution.outcome),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(12.dp))

            AnimatedVisibility(visible = continueVisible, enter = fadeIn()) {
                Button(onClick = onContinue) {
                    Text("Продолжить")
                }
            }
        }

        // Частицы — поверх всего.
        Canvas(modifier = Modifier.fillMaxSize()) {
            @Suppress("UNUSED_VARIABLE")
            val tick = frameTick // подписка на перерисовку каждый кадр
            drawParticles(field)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Части интерфейса
// ---------------------------------------------------------------------------------------------

@Composable
private fun DiceFace(glow: Color?) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val c = center
        val r = size.minDimension / 2f * 0.92f

        fun vertex(angleDeg: Float, radius: Float): Offset {
            val a = angleDeg * PI.toFloat() / 180f
            return Offset(c.x + cos(a) * radius, c.y + sin(a) * radius)
        }

        if (glow != null) {
            drawCircle(color = glow.copy(alpha = 0.35f), radius = r * 1.18f, center = c)
        }

        val outer = List(6) { vertex(-90f + 60f * it, r) }
        val hex = Path().apply {
            moveTo(outer[0].x, outer[0].y)
            for (i in 1..5) lineTo(outer[i].x, outer[i].y)
            close()
        }
        drawPath(
            path = hex,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF5B4426), Color(0xFF2E2110)),
                startY = c.y - r,
                endY = c.y + r
            )
        )
        drawPath(path = hex, color = Gold, style = Stroke(width = 5f))

        val inner = List(3) { vertex(-90f + 120f * it, r * 0.52f) }
        val tri = Path().apply {
            moveTo(inner[0].x, inner[0].y)
            lineTo(inner[1].x, inner[1].y)
            lineTo(inner[2].x, inner[2].y)
            close()
        }
        drawPath(path = tri, color = Gold.copy(alpha = 0.7f), style = Stroke(width = 3f))
        for (i in 0..2) {
            val k = 2 * i
            listOf(k, (k + 1) % 6, (k + 5) % 6).forEach { idx ->
                drawLine(
                    color = Gold.copy(alpha = 0.55f),
                    start = inner[i],
                    end = outer[idx],
                    strokeWidth = 2.5f
                )
            }
        }
    }
}

@Composable
private fun ModifierChip(
    modifier: CheckModifierBreakdown,
    appear: () -> Float,
    fly: () -> Float,
    distancePx: Float
) {
    val valueColor = when {
        modifier.value > 0 -> Green
        modifier.value < 0 -> Red
        else -> Color(0xFFB9B2A4)
    }
    val signed = if (modifier.value >= 0) "+${modifier.value}" else "${modifier.value}"

    Box(
        modifier = Modifier
            .height(40.dp)
            .graphicsLayer {
                val a = appear()
                val f = fly()
                alpha = a * (1f - f)
                translationY = (1f - a) * 30f - f * distancePx
                val s = 1f - 0.35f * f
                scaleX = s
                scaleY = s
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF3A2C1A))
            .border(1.dp, valueColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = modifier.label,
                style = MaterialTheme.typography.bodyLarge,
                color = Parchment
            )
            Text(
                text = signed,
                style = MaterialTheme.typography.titleLarge,
                color = valueColor
            )
        }
    }
}

@Composable
private fun Plate(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(PlateWidth, PlateHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(PlateBg)
            .border(2.dp, accent, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = accent)
            Text(value, style = MaterialTheme.typography.headlineMedium, color = Parchment)
        }
    }
}

private fun outcomeLabel(outcome: CheckOutcome): String = when (outcome) {
    CheckOutcome.CRITICAL_SUCCESS -> "КРИТИЧЕСКИЙ УСПЕХ"
    CheckOutcome.SUCCESS -> "УСПЕХ"
    CheckOutcome.FAILURE -> "НЕУДАЧА"
    CheckOutcome.CRITICAL_FAILURE -> "КРИТИЧЕСКАЯ НЕУДАЧА"
}

private fun outcomeColor(outcome: CheckOutcome): Color = when (outcome) {
    CheckOutcome.CRITICAL_SUCCESS -> Gold
    CheckOutcome.SUCCESS -> Green
    CheckOutcome.FAILURE -> Red
    CheckOutcome.CRITICAL_FAILURE -> Color(0xFFC62828)
}
