package com.interactivebook.app.ui.scene

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.interactivebook.app.core.model.Action
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// Временный нейтральный цвет для отметки новых действий — жёлтые звёзды было плохо видно на
// светлых кнопках. Сменить, когда появится общий визуальный стиль приложения.
private val NewActionMarkColor = Color(0xFF6B6259)
private const val SPARK_COUNT = 6
private const val SPARK_DURATION_MS = 650

/**
 * Кнопка действия. Отвечает за всю визуальную логику одной кнопки:
 * - проявление по мере анимации появления сцены (visible = false → невидима),
 * - стрелка ">" справа (книжная семантика "перейти"),
 * - подсказка стоимости (costHint) — серым курсивом, если действие что-то отнимает,
 * - пометка "пройдено" (used = true) — галочка + приглушение,
 * - метка "новое" (isNew = true) — искры при появлении и кружок до нажатия,
 * - disabledReason — если задан, действие показано, но нажимать нельзя, снизу подпись.
 */
@Composable
fun ActionButton(
    action: Action,
    visible: Boolean,
    isNew: Boolean,
    used: Boolean,
    costHint: String?,
    onClick: () -> Unit
) {
    val contentDisabled = action.disabledReason != null
    val actionAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(350),
        label = "actionAlpha"
    )
    // Приглушение пройденных действий — 0.55 к обычной непрозрачности.
    val usedDim = if (used) 0.55f else 1f

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
            .graphicsLayer { alpha = actionAlpha * usedDim }
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
                    // Галочка пройденного — слева от текста.
                    if (used && !contentDisabled) {
                        Text(
                            text = "✓",
                            modifier = Modifier.padding(end = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = action.text,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Подсказка стоимости — серым курсивом сразу после текста.
                    if (!costHint.isNullOrBlank() && !contentDisabled) {
                        Text(
                            text = "  $costHint",
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f)
                        )
                    }

                    // Кружок "новое" — перед стрелкой.
                    if (isNew && !contentDisabled) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NewActionMarkColor)
                        )
                    }

                    // Стрелка ">" — всегда, деликатная.
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 6.dp),
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.55f)
                    )
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


