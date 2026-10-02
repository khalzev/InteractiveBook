package com.interactivebook.app.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interactivebook.app.core.settings.RevealSpeed

/**
 * Настройки. Пока одна функция: скорость появления сцены — мгновенно / быстро / медленно
 * (пункт 3 правок). Независимо от выбора (кроме "мгновенно" — там скипать нечего), тап по
 * экрану во время сцены всегда может ускорить текущий этап анимации — это не настройка,
 * а всегда доступная подсказка для нетерпеливых игроков.
 */
@Composable
fun SettingsScreen(
    revealSpeed: RevealSpeed,
    onRevealSpeedChange: (RevealSpeed) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Назад") }
        Text("Настройки", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))

        Text("Скорость появления сцены", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Изображение, текст и варианты действий могут появляться постепенно, как " +
                "страница книги, или сразу целиком. Во время постепенного появления короткое " +
                "нажатие на экран ускоряет текущий этап, а удержание — приостанавливает набор текста.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        RevealSpeedOption(
            title = "Мгновенно",
            description = "Сцена появляется сразу целиком, без анимации.",
            selected = revealSpeed == RevealSpeed.INSTANT,
            onSelect = { onRevealSpeedChange(RevealSpeed.INSTANT) }
        )
        Spacer(Modifier.height(10.dp))
        RevealSpeedOption(
            title = "Быстро",
            description = "Постепенное появление в обычном темпе.",
            selected = revealSpeed == RevealSpeed.FAST,
            onSelect = { onRevealSpeedChange(RevealSpeed.FAST) }
        )
        Spacer(Modifier.height(10.dp))
        RevealSpeedOption(
            title = "Медленно",
            description = "Постепенное появление вдвое медленнее — по умолчанию.",
            selected = revealSpeed == RevealSpeed.SLOW,
            onSelect = { onRevealSpeedChange(RevealSpeed.SLOW) }
        )
    }
}

@Composable
private fun RevealSpeedOption(
    title: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
