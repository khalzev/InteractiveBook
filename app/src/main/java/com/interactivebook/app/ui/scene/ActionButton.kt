package com.interactivebook.app.ui.scene

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.interactivebook.app.core.model.Action
import com.interactivebook.app.ui.common.AssetImage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// Цвета, согласованные с пергаментной палитрой SceneScreen.
private val ButtonTextColor = Color(0xFFE8DBC0)
private val ButtonTextDim = Color(0xFFB5A27C)
private val NewActionMarkColor = Color(0xFFC9A34A)
private const val SPARK_COUNT = 6
private const val SPARK_DURATION_MS = 650

/**
 * Кнопка действия. Рисуется поверх картинки action_button.png (кожаная плашка с золотой
 * окантовкой). Поверх картинки:
 * - текст действия (слева), опционально с галочкой "пройдено" и меткой "новое",
 * - подсказка стоимости (costHint) — серым курсивом,
 * - стрелка ">" справа,
 * - disabledReason — под кнопкой серой подписью, если действие заблокировано.
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                .clickable(
                    enabled = visible && !contentDisabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        ) {
            // Фон кнопки — картинка из assets.
            AssetImage(
                fileName = "action_button.png",
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                contentScale = ContentScale.FillBounds,
                fallbackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Слой содержимого поверх картинки.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (used && !contentDisabled) {
                    Text(
                        text = "✓",
                        modifier = Modifier.padding(end = 10.dp),
                        fontWeight = FontWeight.Bold,
                        color = ButtonTextColor
                    )
                }

                Text(
                    text = action.text,
                    modifier = Modifier.weight(1f, fill = false),
                    color = if (contentDisabled) ButtonTextDim else ButtonTextColor
                )

                if (!costHint.isNullOrBlank() && !contentDisabled) {
                    Text(
                        text = "  $costHint",
                        fontStyle = FontStyle.Italic,
                        color = ButtonTextDim
                    )
                }

                if (isNew && !contentDisabled) {
                    Box(
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NewActionMarkColor)
                    )
                }

                // Стрелка ">" — рисуется как текст, чтобы не тащить иконку.
                Text(
                    text = "›",
                    modifier = Modifier.padding(start = 12.dp),
                    fontWeight = FontWeight.Bold,
                    color = ButtonTextDim
                )
            }

            // Искры поверх кнопки при появлении нового действия.
            if (isNew && sparksPlayed) {
                Canvas(modifier = Modifier.fillMaxWidth().height(64.dp)) {
                    val cx = size.width - 40f
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
                color = ButtonTextDim,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
            )
        }
    }
}