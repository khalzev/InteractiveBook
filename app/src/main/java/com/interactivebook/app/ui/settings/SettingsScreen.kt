package com.interactivebook.app.ui.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.interactivebook.app.R
import com.interactivebook.app.core.settings.RevealSpeed
import com.interactivebook.app.ui.common.BookPageBackground
import com.interactivebook.app.ui.common.BookPageContent

private val InkColor = Color(0xFF3B2A1A)
private val InkDimColor = Color(0xFF6B5A44)

/**
 * Настройки — страница книги. Контент внутри BookPageContent (правая страница).
 */
@Composable
fun SettingsScreen(
    revealSpeed: RevealSpeed,
    pageTurnEnabled: Boolean,
    onRevealSpeedChange: (RevealSpeed) -> Unit,
    onPageTurnChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        BookPageBackground()

        BookPageContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.back), color = InkDimColor)
                }

                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = InkColor
                )
                Spacer(Modifier.height(20.dp))

                // --- Скорость появления сцены ---
                Text(
                    text = stringResource(R.string.settings_reveal_speed_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = InkColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_reveal_speed_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkDimColor
                )
                Spacer(Modifier.height(10.dp))

                RevealSpeedRow(
                    label = stringResource(R.string.settings_reveal_instant),
                    desc = stringResource(R.string.settings_reveal_instant_desc),
                    selected = revealSpeed == RevealSpeed.INSTANT,
                    onSelect = { onRevealSpeedChange(RevealSpeed.INSTANT) }
                )
                RevealSpeedRow(
                    label = stringResource(R.string.settings_reveal_fast),
                    desc = stringResource(R.string.settings_reveal_fast_desc),
                    selected = revealSpeed == RevealSpeed.FAST,
                    onSelect = { onRevealSpeedChange(RevealSpeed.FAST) }
                )
                RevealSpeedRow(
                    label = stringResource(R.string.settings_reveal_slow),
                    desc = stringResource(R.string.settings_reveal_slow_desc),
                    selected = revealSpeed == RevealSpeed.SLOW,
                    onSelect = { onRevealSpeedChange(RevealSpeed.SLOW) }
                )

                Spacer(Modifier.height(24.dp))

                // --- Перелистывание страниц ---
                Text(
                    text = stringResource(R.string.settings_page_turn_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = InkColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_page_turn_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkDimColor
                )
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (pageTurnEnabled) {
                            stringResource(R.string.settings_page_turn_on)
                        } else {
                            stringResource(R.string.settings_page_turn_off)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = InkColor
                    )
                    Switch(
                        checked = pageTurnEnabled,
                        onCheckedChange = onPageTurnChange
                    )
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun RevealSpeedRow(
    label: String,
    desc: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(label, style = MaterialTheme.typography.titleLarge, color = InkColor)
            Text(desc, style = MaterialTheme.typography.bodyLarge, color = InkDimColor)
        }
    }
}