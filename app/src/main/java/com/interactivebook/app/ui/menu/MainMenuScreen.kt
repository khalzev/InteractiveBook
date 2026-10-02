package com.interactivebook.app.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.interactivebook.app.R

/**
 * Главное меню (раздел 70). "Продолжить" — первая и визуально самая крупная кнопка
 * (продолжение — основное ожидаемое действие). "Новая игра" оформлена как менее заметная
 * outline-кнопка, так как её нажатие потенциально опасно (сброс прогресса, раздел 69).
 */
@Composable
fun MainMenuScreen(
    hasSavedGame: Boolean,
    onNewGame: () -> Unit,
    onContinue: () -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit
) {
    var showNewGameConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Тайны Риверстоуна",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(56.dp))

        Button(
            onClick = onContinue,
            enabled = hasSavedGame,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Text(
                text = stringResource(R.string.menu_continue),
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                if (hasSavedGame) showNewGameConfirm = true else onNewGame()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Text(stringResource(R.string.menu_new_game))
        }

        OutlinedButton(
            onClick = onSettings,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Text(stringResource(R.string.menu_settings))
        }

        TextButton(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Text(stringResource(R.string.menu_exit))
        }
    }

    if (showNewGameConfirm) {
        AlertDialog(
            onDismissRequest = { showNewGameConfirm = false },
            title = { Text(stringResource(R.string.menu_new_game_confirm_title)) },
            text = { Text(stringResource(R.string.menu_new_game_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showNewGameConfirm = false
                    onNewGame()
                }) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGameConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
